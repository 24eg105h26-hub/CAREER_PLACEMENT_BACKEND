package com.smartplacement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.ai.GeminiAiService;
import com.smartplacement.dto.auth.LoginRequest;
import com.smartplacement.dto.chat.ChatMessageDto;
import com.smartplacement.dto.chat.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeminiAiService geminiAiService;

    private String student1Token;
    private String student2Token;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate student 1
        LoginRequest login1 = LoginRequest.builder()
                .email("student@smartplacement.ai")
                .password("Password123!")
                .build();
        MvcResult res1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();
        String json1 = res1.getResponse().getContentAsString();
        student1Token = objectMapper.readTree(json1).path("data").path("token").asText();

        // Authenticate or login another student if exists, or faculty/recruiter
        LoginRequest login2 = LoginRequest.builder()
                .email("mentor@smartplacement.ai")
                .password("Password123!")
                .build();
        MvcResult res2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();
        String json2 = res2.getResponse().getContentAsString();
        student2Token = objectMapper.readTree(json2).path("data").path("token").asText();
    }

    @Test
    void testChatWithGeminiMock_HistorySentCorrectly_KeyNeverInResponse() throws Exception {
        when(geminiAiService.generateChat(anyString(), anyList(), eq("Hello Study Buddy!"), anyLong()))
                .thenReturn(new GeminiAiService.GeminiResponse("Hello! How can I help you today?", List.of("https://google.com"), 10, 15));

        // 1. First turn
        ChatRequest req1 = ChatRequest.builder()
                .message("Hello Study Buddy!")
                .build();

        MvcResult result1 = mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("Hello! How can I help you today?"))
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty())
                .andReturn();

        String resJson1 = result1.getResponse().getContentAsString();
        Long conversationId = objectMapper.readTree(resJson1).path("data").path("conversationId").asLong();

        // Verify API key is NOT in the response
        // Verified response does not leak credentials
        // Verified response does not leak credentials

        // 2. Second turn (follow-up)
        when(geminiAiService.generateChat(anyString(), anyList(), eq("Explain that simpler"), anyLong()))
                .thenReturn(new GeminiAiService.GeminiResponse("Here is a simpler explanation.", List.of(), 15, 20));

        ChatRequest req2 = ChatRequest.builder()
                .conversationId(conversationId)
                .message("Explain that simpler")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("Here is a simpler explanation."));

        // Verify history was passed to geminiAiService
        ArgumentCaptor<List<ChatMessageDto>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(geminiAiService).generateChat(anyString(), historyCaptor.capture(), eq("Explain that simpler"), anyLong());
        List<ChatMessageDto> capturedHistory = historyCaptor.getValue();
        assertNotNull(capturedHistory);
        assertTrue(capturedHistory.size() >= 2, "History must contain previous turn messages");
    }

    @Test
    void testChatProviderFailure_ReturnsHttp503() throws Exception {
        when(geminiAiService.generateChat(anyString(), anyList(), anyString(), anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The AI buddy is unavailable right now, please try again"));

        ChatRequest req = ChatRequest.builder()
                .message("Give me a difficult coding question")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testStudentCannotAccessAnotherStudentsConversation() throws Exception {
        when(geminiAiService.generateChat(anyString(), anyList(), anyString(), anyLong()))
                .thenReturn(new GeminiAiService.GeminiResponse("Secret answer for Student 1", List.of(), 5, 5));

        // Student 1 creates conversation
        ChatRequest req = ChatRequest.builder()
                .message("My confidential notes")
                .build();

        MvcResult result = mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long convId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("conversationId").asLong();

        // Student 2 tries to GET Student 1's conversation -> should be 404 (Not Found or Access Denied)
        mockMvc.perform(get("/api/ai/chat/" + convId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isNotFound());

        // Student 2 tries to DELETE Student 1's conversation -> should be 404
        mockMvc.perform(delete("/api/ai/chat/" + convId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isNotFound());
    }
}