package com.smartplacement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.dto.auth.LoginRequest;
import com.smartplacement.entity.*;
import com.smartplacement.repository.MentorAssignmentRepository;
import com.smartplacement.repository.StudentRepository;
import com.smartplacement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class RoleDashboardAndAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MentorAssignmentRepository mentorAssignmentRepository;

    private String mentorToken;
    private String facultyToken;
    private String tpoToken;
    private String recruiterToken;
    private String adminToken;
    private String studentToken;

    private Long assignedStudentId;

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest req = LoginRequest.builder().email(email).password(password).build();
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        String token = (String) data.get("token");
        if (token == null) {
            token = (String) data.get("accessToken");
        }
        return token;
    }

    @BeforeEach
    void setUp() throws Exception {
        mentorToken = obtainToken("mentor@smartplacement.ai", "Password123!");
        facultyToken = obtainToken("faculty@smartplacement.ai", "Password123!");
        tpoToken = obtainToken("tpo@smartplacement.ai", "Password123!");
        recruiterToken = obtainToken("recruiter@smartplacement.ai", "Password123!");
        adminToken = obtainToken("admin@smartplacement.ai", "Password123!");
        studentToken = obtainToken("student@smartplacement.ai", "Password123!");

        List<Student> students = studentRepository.findAll();
        if (!students.isEmpty()) {
            assignedStudentId = students.get(0).getId();
        }
    }

    @Test
    void testMentorDashboardSmoke() throws Exception {
        mockMvc.perform(get("/api/mentor/dashboard")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedStudentsCount").exists());
    }

    @Test
    void testFacultyDashboardSmoke() throws Exception {
        mockMvc.perform(get("/api/faculty/dashboard")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedStudentsCount").exists());
    }

    @Test
    void testTpoDashboardSmoke() throws Exception {
        mockMvc.perform(get("/api/tpo/dashboard")
                        .header("Authorization", "Bearer " + tpoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCompanies").exists());
    }

    @Test
    void testRecruiterDashboardSmoke() throws Exception {
        mockMvc.perform(get("/api/recruiter/dashboard")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeJobsCount").exists());
    }

    @Test
    void testAdminDashboardSmoke() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").exists());
    }

    @Test
    void testStudentDashboardSmoke() throws Exception {
        mockMvc.perform(get("/api/students/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.readinessScore").exists());
    }

    @Test
    void testRoleGuardsForbidden() throws Exception {
        // Student accessing Admin endpoint should be 403 Forbidden
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Student accessing Mentor endpoint should be 403 Forbidden
        mockMvc.perform(get("/api/mentor/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testMentorAccessAssignedStudentSuccess() throws Exception {
        if (assignedStudentId != null) {
            mockMvc.perform(get("/api/mentor/students/" + assignedStudentId)
                            .header("Authorization", "Bearer " + mentorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(assignedStudentId));
        }
    }

    @Test
    void testMentorAccessUnassignedStudentForbidden() throws Exception {
        // Accessing an unassigned student ID must return 403 Forbidden
        mockMvc.perform(get("/api/mentor/students/999999")
                        .header("Authorization", "Bearer " + mentorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testMentorCreateFeedback() throws Exception {
        if (assignedStudentId != null) {
            Map<String, Object> req = new HashMap<>();
            req.put("studentId", assignedStudentId);
            req.put("comment", "Great improvement on data structures and system design mock interview!");
            req.put("category", "TECHNICAL");

            mockMvc.perform(post("/api/mentor/feedback")
                            .header("Authorization", "Bearer " + mentorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comment").value("Great improvement on data structures and system design mock interview!"));
        }
    }
}
