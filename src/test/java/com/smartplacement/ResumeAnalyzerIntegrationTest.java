package com.smartplacement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.dto.auth.LoginRequest;
import com.smartplacement.entity.Student;
import com.smartplacement.entity.User;
import com.smartplacement.repository.ResumeRepository;
import com.smartplacement.repository.StudentRepository;
import com.smartplacement.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class ResumeAnalyzerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    private String student1Token;
    private String student2Token;

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
        if (token == null) token = (String) data.get("accessToken");
        return token;
    }

    private byte[] createPdf(List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (lines != null && !lines.isEmpty()) {
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    cs.newLineAtOffset(50, 750);
                    for (String line : lines) {
                        cs.showText(line);
                        cs.newLineAtOffset(0, -14);
                    }
                    cs.endText();
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        student1Token = obtainToken("student@smartplacement.ai", "Password123!");
        student2Token = obtainToken("priya.sharma@smartplacement.ai", "Password123!");
    }

    @Test
    void testStrongResumeAnalysis() throws Exception {
        List<String> strongLines = List.of(
                "Alex Chen",
                "alex.chen@smartplacement.ai | +1 555-0104 | linkedin.com/in/alexchen | github.com/alexchen",
                "SUMMARY",
                "Results-driven Full Stack Software Engineer with extensive expertise in Java, Spring Boot, React, and Cloud Microservices.",
                "EDUCATION",
                "Bachelor of Technology in Computer Science and Engineering | CGPA: 8.8 | 2025",
                "SKILLS",
                "Java, Spring Boot, React, PostgreSQL, Docker, Kubernetes, AWS, Redis, Python, Git, REST APIs, Microservices, Agile",
                "EXPERIENCE",
                "Software Engineering Intern - Cloud Systems (Jan 2024 - Jun 2024)",
                "- Architected high-throughput microservices using Java and Spring Boot, reducing API latency by 42% for 120,000 daily active users.",
                "- Engineered distributed caching layer with Redis, decreasing database queries by 65% and saving 4500 dollars monthly in cloud costs.",
                "- Deployed automated CI/CD pipeline using Docker and GitHub Actions, accelerating release cycles from 3 days to 45 minutes.",
                "PROJECTS",
                "E-Commerce Microservices Engine",
                "- Implemented distributed order management in Spring Boot and PostgreSQL, handling 2500 requests per second with 99.99% uptime.",
                "- Built intuitive web dashboard in React and TypeScript, boosting conversion rates by 28% across 50,000 active shoppers."
        );

        byte[] strongPdf = createPdf(strongLines);
        MockMultipartFile file = new MockMultipartFile("file", "alex_chen_strong_resume.pdf", "application/pdf", strongPdf);

        MvcResult result = mockMvc.perform(multipart("/api/resumes/analyze")
                        .file(file)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallScore").isNumber())
                .andExpect(jsonPath("$.data.atsScore").isNumber())
                .andExpect(jsonPath("$.data.groupedSkills").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");

        double overallScore = ((Number) data.get("overallScore")).doubleValue();
        double atsScore = ((Number) data.get("atsScore")).doubleValue();
        int totalSkills = ((Number) data.get("totalSkillsFound")).intValue();

        assertTrue(overallScore >= 80.0, "Strong resume should score at least 80, but got: " + overallScore);
        assertTrue(atsScore >= 80.0, "Strong resume ATS score should be at least 80, but got: " + atsScore);
        assertTrue(totalSkills >= 8, "Expected 8+ recognized skills, but got: " + totalSkills);
    }

    @Test
    void testWeakResumeAnalysisFlagsWeakVerbsAndMissingMetrics() throws Exception {
        List<String> weakLines = List.of(
                "John Doe",
                "SKILLS",
                "Java, Python",
                "PROJECTS",
                "- Worked on a simple website with some classmates.",
                "- Responsible for creating basic web pages.",
                "- Assisted with backend database tasks and duties included fixing bugs."
        );

        byte[] weakPdf = createPdf(weakLines);
        MockMultipartFile file = new MockMultipartFile("file", "weak_resume.pdf", "application/pdf", weakPdf);

        MvcResult result = mockMvc.perform(multipart("/api/resumes/analyze")
                        .file(file)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        Map<String, Object> data = (Map<String, Object>) map.get("data");

        double overallScore = ((Number) data.get("overallScore")).doubleValue();
        List<Map<String, Object>> weakPhrases = (List<Map<String, Object>>) data.get("weakPhrases");
        List<Map<String, Object>> improvements = (List<Map<String, Object>>) data.get("improvements");

        assertTrue(overallScore < 70.0, "Weak resume score should be below 70, but got: " + overallScore);
        assertFalse(weakPhrases.isEmpty(), "Weak resume should detect passive phrases like 'worked on' or 'responsible for'");
        assertFalse(improvements.isEmpty(), "Weak resume should have recommended improvement action items");
    }

    @Test
    void testScannedOrImageOnlyPdfRejected() throws Exception {
        // PDF with empty page (0 selectable text)
        byte[] emptyPdf = createPdf(List.of());
        MockMultipartFile file = new MockMultipartFile("file", "scanned_image.pdf", "application/pdf", emptyPdf);

        mockMvc.perform(multipart("/api/resumes/analyze")
                        .file(file)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Scanned or image-only PDF detected")));
    }

    @Test
    void testResumeOwnershipEnforcement() throws Exception {
        // Upload resume as student 1
        List<String> lines = List.of("Alex Chen", "alex@mail.com | 555-0100", "SKILLS", "Java, Python", "PROJECTS", "- Built Java application");
        byte[] pdf = createPdf(lines);
        MockMultipartFile file = new MockMultipartFile("file", "alex_resume.pdf", "application/pdf", pdf);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/resumes/analyze")
                        .file(file)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andReturn();

        String json = uploadRes.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        Long resumeId = ((Number) ((Map<String, Object>) map.get("data")).get("resumeId")).longValue();

        // Student 1 can access their own resume
        mockMvc.perform(get("/api/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk());

        // Student 2 attempting to access Student 1's resume must receive 403 Forbidden!
        mockMvc.perform(get("/api/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPdfReportDownload() throws Exception {
        List<String> lines = List.of(
                "Alex Chen",
                "alex@smartplacement.ai | +1 555-0104",
                "EDUCATION",
                "NIT B.Tech Computer Science 2025",
                "SKILLS",
                "Java, Spring Boot, React, Docker, SQL",
                "PROJECTS",
                "- Architected REST microservices handling 1000 requests per second."
        );
        byte[] pdf = createPdf(lines);
        MockMultipartFile file = new MockMultipartFile("file", "alex_report.pdf", "application/pdf", pdf);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/resumes/analyze")
                        .file(file)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andReturn();

        String json = uploadRes.getResponse().getContentAsString();
        Long resumeId = ((Number) ((Map<String, Object>) objectMapper.readValue(json, Map.class).get("data")).get("resumeId")).longValue();

        // Download generated PDF report
        MvcResult reportRes = mockMvc.perform(get("/api/resumes/" + resumeId + "/pdf-report")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();

        byte[] pdfBytes = reportRes.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 500, "Generated PDF report should be non-trivial");
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}
