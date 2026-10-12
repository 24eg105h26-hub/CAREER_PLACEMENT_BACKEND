package com.smartplacement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.dto.auth.LoginRequest;
import com.smartplacement.dto.auth.RegisterRequest;
import com.smartplacement.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void testSeededUserLoginSuccess() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("student@smartplacement.ai")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    public void testStudentRegistrationSuccess() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("newstudent@smartplacement.ai")
                .password("SecurePass123!")
                .fullName("New Student")
                .role(Role.STUDENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    public void testRecruiterRegistrationPendingApproval() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("newrecruiter@company.com")
                .password("RecruiterPass123!")
                .fullName("Jane Recruiter")
                .role(Role.RECRUITER)
                .companyName("Acme Corp")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.token").isEmpty());
    }

    @Test
    public void testInvalidRoleRegistrationRejected() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("hacker@smartplacement.ai")
                .password("HackerPass123!")
                .fullName("Hacker Admin")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
