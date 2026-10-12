package com.smartplacement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.ai.GeminiAiService;
import com.smartplacement.dto.chat.*;
import com.smartplacement.entity.*;
import com.smartplacement.repository.*;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiAiService geminiAiService;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Per-user rate limiting: max 20 messages per minute
    private final Map<Long, ConcurrentLinkedQueue<Long>> userRateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_HISTORY_MESSAGES = 12;

    private String cachedSystemPrompt = null;

    private synchronized String loadSystemPrompt() {
        if (cachedSystemPrompt != null) {
            return cachedSystemPrompt;
        }
        try {
            ClassPathResource resource = new ClassPathResource("ai/prompts/study-buddy.txt");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    cachedSystemPrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                    return cachedSystemPrompt;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load study-buddy.txt: {}", e.getMessage());
        }

        cachedSystemPrompt = "You are a warm, friendly study buddy for college students, like a smart senior who genuinely wants them to succeed. Answer ANY topic: academics, coding, maths, careers, placements, interviews, life advice, motivation, general knowledge, and casual chat. Talk naturally in the student's language and tone. Be accurate: give direct, correct answers first, then brief explanation or examples; use code blocks for code. Keep answers concise unless asked to go deep.";
        return cachedSystemPrompt;
    }

    private void checkRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> timestamps = userRateLimitMap.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());

        // Evict expired timestamps
        while (!timestamps.isEmpty() && (now - timestamps.peek()) > RATE_LIMIT_WINDOW_MS) {
            timestamps.poll();
        }

        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. You can send up to 20 messages per minute. Please wait a moment.");
        }

        timestamps.add(now);
    }

    private String buildStudentContext(Long userId) {
        Optional<Student> studentOpt = studentRepository.findByUserId(userId);
        if (studentOpt.isEmpty()) {
            return "";
        }

        Student student = studentOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("\n\nStudent context (use only if relevant):");

        if (student.getDepartment() != null) {
            sb.append("\n- Department: ").append(student.getDepartment().getName());
        }
        if (student.getCgpa() != null && student.getCgpa() > 0) {
            sb.append("\n- CGPA: ").append(student.getCgpa());
        }

        try {
            List<StudentSkill> skills = studentSkillRepository.findByStudentId(student.getId());
            if (skills != null && !skills.isEmpty()) {
                String skillList = skills.stream()
                        .map(s -> s.getSkill() != null ? s.getSkill().getName() : "")
                        .filter(s -> !s.isBlank())
                        .limit(8)
                        .collect(Collectors.joining(", "));
                if (!skillList.isBlank()) {
                    sb.append("\n- Known Skills: ").append(skillList);
                }
            }
        } catch (Exception ignored) {}

        try {
            List<Application> apps = applicationRepository.findByStudentId(student.getId());
            if (apps != null && !apps.isEmpty()) {
                sb.append("\n- Active Job Applications: ").append(apps.size());
            }
        } catch (Exception ignored) {}

        return sb.toString();
    }

    @Transactional
    public ChatResponse processChat(ChatRequest request, UserPrincipal principal) {
        Long userId = principal.getId();
        String message = request.getEffectiveMessage();

        if (message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message exceeds maximum length of 4000 characters.");
        }

        checkRateLimit(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ChatConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findByIdAndUserId(request.getConversationId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found or access denied."));
        } else {
            String title = message.length() > 40 ? message.substring(0, 40).trim() + "..." : message;
            conversation = ChatConversation.builder()
                    .user(user)
                    .title(title)
                    .build();
            conversation = conversationRepository.save(conversation);
        }

        // Fetch recent messages for multi-turn history (last 12)
        List<ChatMessage> existingMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<ChatMessageDto> history = new ArrayList<>();
        int startIndex = Math.max(0, existingMessages.size() - MAX_HISTORY_MESSAGES);
        for (int i = startIndex; i < existingMessages.size(); i++) {
            ChatMessage m = existingMessages.get(i);
            history.add(ChatMessageDto.builder()
                    .id(m.getId())
                    .role(m.getRole())
                    .content(m.getContent())
                    .build());
        }

        // Save current user message
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .role("user")
                .content(message)
                .build();
        messageRepository.save(userMessage);

        // Build full system prompt + safe personal context
        String baseSystemPrompt = loadSystemPrompt();
        String studentContext = buildStudentContext(userId);
        String fullSystemInstruction = baseSystemPrompt + studentContext;

        // Call Gemini
        GeminiAiService.GeminiResponse geminiResponse = geminiAiService.generateChat(
                fullSystemInstruction, history, message, userId
        );

        // Save model reply
        String sourcesJson = null;
        if (geminiResponse.sources() != null && !geminiResponse.sources().isEmpty()) {
            try {
                sourcesJson = objectMapper.writeValueAsString(geminiResponse.sources());
            } catch (Exception ignored) {}
        }

        ChatMessage modelMessage = ChatMessage.builder()
                .conversation(conversation)
                .role("model")
                .content(geminiResponse.reply())
                .sources(sourcesJson)
                .build();
        messageRepository.save(modelMessage);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return ChatResponse.of(conversation.getId(), geminiResponse.reply(), geminiResponse.sources());
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> ConversationDto.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto getConversation(Long conversationId, Long userId) {
        ChatConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found or access denied."));

        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(m -> {
                    List<String> sources = Collections.emptyList();
                    if (m.getSources() != null && !m.getSources().isBlank()) {
                        try {
                            sources = objectMapper.readValue(m.getSources(), new TypeReference<List<String>>() {});
                        } catch (Exception ignored) {}
                    }
                    return ChatMessageDto.builder()
                            .id(m.getId())
                            .role(m.getRole())
                            .content(m.getContent())
                            .sources(sources)
                            .createdAt(m.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return ConversationDetailDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messageDtos)
                .build();
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        ChatConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found or access denied."));
        conversationRepository.delete(conversation);
    }
}