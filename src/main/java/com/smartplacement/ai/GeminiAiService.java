package com.smartplacement.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.dto.chat.ChatMessageDto;
import com.smartplacement.entity.AiUsageLog;
import com.smartplacement.repository.AiUsageLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiService {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FallbackChatEngine fallbackChatEngine;

    @Value("${gemini.api-key:${GEMINI_API_KEY:${AI_API_KEY:}}}")
    private String geminiApiKey;

    @Value("${gemini.model:${GEMINI_MODEL:${AI_MODEL:gemini-1.5-flash}}}")
    private String geminiModel;

    @Value("${ai.mode:live}")
    private String aiMode;

    
    @PostConstruct
    public void initFromEnvFile() {
        try {
            java.io.File envFile = new java.io.File(".env");
            if (!envFile.exists()) {
                envFile = new java.io.File(".env");
            }
            if (!envFile.exists()) {
                envFile = new java.io.File("backend/.env");
            }
            if (envFile.exists()) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("#") || !line.contains("=")) continue;
                    String[] parts = line.split("=", 2);
                    String k = parts[0].trim();
                    String v = parts[1].trim();
                    if ("GEMINI_API_KEY".equalsIgnoreCase(k) && (geminiApiKey == null || geminiApiKey.startsWith("mock-") || geminiApiKey.isBlank())) {
                        geminiApiKey = v;
                    } else if ("AI_API_KEY".equalsIgnoreCase(k) && (geminiApiKey == null || geminiApiKey.startsWith("mock-") || geminiApiKey.isBlank())) {
                        geminiApiKey = v;
                    }
                    if ("GEMINI_MODEL".equalsIgnoreCase(k) && !v.isBlank()) {
                        geminiModel = v;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not read .env file: {}", e.getMessage());
        }
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10s
        factory.setReadTimeout(30000);    // 30s timeout per requirement
        return new RestTemplate(factory);
    }

    public record GeminiResponse(String reply, List<String> sources, int promptTokens, int candidateTokens) {}

    public GeminiResponse generateChat(String systemInstruction, List<ChatMessageDto> history, String currentMessage, Long userId) {
        if ("disabled".equalsIgnoreCase(aiMode)) {
            logUsage(userId, "chat", 0, 0, false, "AI service is disabled");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI buddy is currently disabled on this server.");
        }

        String apiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
        if (apiKey.isEmpty() || apiKey.startsWith("mock-") || apiKey.equalsIgnoreCase("your-api-key-here")) {
            log.warn("Gemini API key is not configured or is a mock placeholder.");
            logUsage(userId, "chat", 0, 0, false, "Missing or invalid Gemini API key");
            return fallbackChatEngine.generateResponse(systemInstruction, history, currentMessage, userId);
        }

        String model = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel.trim() : "gemini-1.5-flash";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

        // Build Payload
        Map<String, Object> payload = new LinkedHashMap<>();

        if (systemInstruction != null && !systemInstruction.isBlank()) {
            payload.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))
            ));
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            for (ChatMessageDto msg : history) {
                if (msg.getContent() == null || msg.getContent().isBlank()) continue;
                String role = "model".equalsIgnoreCase(msg.getRole()) ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        // Current message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", currentMessage))
        ));
        payload.put("contents", contents);

        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 1024);
        payload.put("generationConfig", genConfig);

        // Optional search grounding
        payload.put("tools", List.of(Map.of("googleSearch", Collections.emptyMap())));

        RestTemplate restTemplate = createRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        ResponseEntity<String> response = null;
        int attempts = 0;
        boolean enableTools = true;

        while (attempts < 2) {
            attempts++;
            try {
                if (!enableTools) {
                    payload.remove("tools");
                }
                String jsonBody = objectMapper.writeValueAsString(payload);
                HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

                response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    break;
                }
            } catch (HttpStatusCodeException ex) {
                int statusCode = ex.getStatusCode().value();
                // If 400 Bad Request due to search grounding tool unsupported, retry immediately without tools
                if (statusCode == 400 && enableTools) {
                    log.info("Gemini returned 400 with search tools enabled; retrying without tools.");
                    enableTools = false;
                    continue;
                }

                // Retry once on 429 or 5xx with 1000ms backoff
                if ((statusCode == 429 || statusCode >= 500) && attempts < 2) {
                    log.warn("Gemini API returned status {}. Retrying with 1s backoff (attempt {}/2)...", statusCode, attempts);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                    continue;
                }

                log.error("Gemini API error status {} from model {}", statusCode, model);
                logUsage(userId, "chat", 0, 0, false, "Gemini HTTP " + statusCode);
                return fallbackChatEngine.generateResponse(systemInstruction, history, currentMessage, userId);
            } catch (ResourceAccessException ex) { // Timeout or network failure
                if (attempts < 2) {
                    log.warn("Gemini API connection timed out. Retrying with 1s backoff...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                    continue;
                }
                log.error("Gemini API connection timed out: {}", ex.getMessage());
                logUsage(userId, "chat", 0, 0, false, "Timeout: " + ex.getMessage());
                return fallbackChatEngine.generateResponse(systemInstruction, history, currentMessage, userId);
            } catch (Exception ex) {
                log.error("Unexpected error contacting Gemini API: {}", ex.getMessage());
                logUsage(userId, "chat", 0, 0, false, ex.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI buddy is unavailable right now, please try again");
            }
        }

        if (response == null || response.getBody() == null) {
            logUsage(userId, "chat", 0, 0, false, "Null response from Gemini");
            return fallbackChatEngine.generateResponse(systemInstruction, history, currentMessage, userId);
        }

        return parseGeminiResponse(response.getBody(), userId);
    }

    private GeminiResponse parseGeminiResponse(String responseJson, Long userId) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                logUsage(userId, "chat", 0, 0, true, "Empty candidates");
                return new GeminiResponse(
                        "I couldn't generate a response for that prompt. Please try rephrasing your question!",
                        Collections.emptyList(), 0, 0
                );
            }

            JsonNode candidate = candidates.get(0);
            String finishReason = candidate.path("finishReason").asText("");
            if ("SAFETY".equalsIgnoreCase(finishReason) || "BLOCKLIST".equalsIgnoreCase(finishReason)) {
                logUsage(userId, "chat", 0, 0, true, "Safety block");
                return new GeminiResponse(
                        "I couldn't generate a response for that prompt due to safety guidelines. Please try rephrasing your question!",
                        Collections.emptyList(), 0, 0
                );
            }

            JsonNode parts = candidate.path("content").path("parts");
            StringBuilder replyText = new StringBuilder();
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        replyText.append(part.path("text").asText());
                    }
                }
            }

            String reply = replyText.toString().trim();
            if (reply.isEmpty()) {
                reply = "I couldn't generate a response for that prompt. Please try rephrasing your question!";
            }

            List<String> sources = new ArrayList<>();
            JsonNode groundingMetadata = candidate.path("groundingMetadata");
            if (!groundingMetadata.isMissingNode()) {
                JsonNode chunks = groundingMetadata.path("groundingChunks");
                if (chunks.isArray()) {
                    for (JsonNode chunk : chunks) {
                        JsonNode web = chunk.path("web");
                        if (!web.isMissingNode()) {
                            String title = web.path("title").asText("").trim();
                            String uri = web.path("uri").asText("").trim();
                            if (!uri.isEmpty()) {
                                if (!title.isEmpty()) {
                                    sources.add(title + " - " + uri);
                                } else {
                                    sources.add(uri);
                                }
                            }
                        }
                    }
                }
            }

            int promptTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
            int candidateTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);

            logUsage(userId, "chat", promptTokens, candidateTokens, true, null);
            return new GeminiResponse(reply, sources, promptTokens, candidateTokens);

        } catch (Exception ex) {
            log.error("Failed to parse Gemini response: {}", ex.getMessage());
            logUsage(userId, "chat", 0, 0, false, "Parse error: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI buddy is unavailable right now, please try again");
        }
    }

    private void logUsage(Long userId, String feature, int promptTokens, int completionTokens, boolean success, String error) {
        try {
            AiUsageLog logEntry = AiUsageLog.builder()
                    .userId(userId)
                    .featureName(feature)
                    .model(geminiModel != null ? geminiModel : "gemini-1.5-flash")
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .success(success)
                    .errorMessage(error)
                    .build();
            aiUsageLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to persist AI usage log: {}", e.getMessage());
        }
    }
}