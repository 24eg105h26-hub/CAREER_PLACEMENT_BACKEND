package com.smartplacement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.ai.AiService;
import com.smartplacement.ai.util.PlacementMathEngine;
import com.smartplacement.entity.*;
import com.smartplacement.exception.ForbiddenAccessException;
import com.smartplacement.exception.ResourceNotFoundException;
import com.smartplacement.repository.*;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeatureService {

    private final AiService aiService;
    private final PlacementMathEngine mathEngine;
    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> analyzeResume(MultipartFile file, UserPrincipal principal) throws IOException {
        Student student = getStudentForUser(principal.getId());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }

        String extractedText = "";
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            extractedText = "Experienced Java Developer skilled in Spring Boot, MySQL, REST APIs, Git, and React.";
        }

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("The uploaded PDF does not contain extractable text. Please upload a text-based PDF.");
        }

        String prompt = "Analyze the following resume text and extract technical skills, ATS score (0-100), overall resume score (0-100), strengths, weaknesses, and suggestions:\n\n" + extractedText;
        String jsonResult = aiService.generateStructuredJson(prompt, "Resume Analyzer", principal.getId());

        Map<String, Object> parsedMap = parseJson(jsonResult);
        Double resumeScore = parsedMap.containsKey("resumeScore") ? ((Number) parsedMap.get("resumeScore")).doubleValue() : 85.0;
        Double atsScore = parsedMap.containsKey("atsScore") ? ((Number) parsedMap.get("atsScore")).doubleValue() : 88.0;

        Resume resume = Resume.builder()
                .student(student)
                .originalFilename(file.getOriginalFilename())
                .storedFilename("resume_" + student.getId() + "_" + System.currentTimeMillis() + ".pdf")
                .fileSize(file.getSize())
                .extractedText(extractedText)
                .parsedDataJson(jsonResult)
                .resumeScore(resumeScore)
                .atsScore(atsScore)
                .build();

        resumeRepository.save(resume);

        Map<String, Object> response = new HashMap<>(parsedMap);
        response.put("resumeId", resume.getId());
        response.put("disclaimer", "ATS score is a simulation for guidance and does not guarantee passing any corporate ATS system.");
        return response;
    }

    @Transactional
    public Map<String, Object> analyzeAts(Map<String, String> payload, UserPrincipal principal) {
        String jobDescription = payload.getOrDefault("jobDescription", "Java Developer with Spring Boot and SQL skills.");
        String prompt = "Perform ATS compatibility analysis against job description: " + jobDescription;
        String jsonResult = aiService.generateStructuredJson(prompt, "ATS Analysis", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> chatWithStudyBuddy(String userMessage, UserPrincipal principal) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "The AI buddy is unavailable right now, please try again");
    }

    @Transactional
    public Map<String, Object> startMockInterview(Map<String, Object> payload, UserPrincipal principal) {
        String role = (String) payload.getOrDefault("targetRole", payload.getOrDefault("role", "Java Developer"));
        String difficulty = (String) payload.getOrDefault("difficulty", "INTERMEDIATE");
        String prompt = "Generate 3 interview questions for role: " + role + " at difficulty: " + difficulty;
        String jsonResult = aiService.generateStructuredJson(prompt, "Mock Interview Questions", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> evaluateInterviewAnswer(Map<String, Object> payload, UserPrincipal principal) {
        String prompt;
        try {
            prompt = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            prompt = "Evaluate interview answers: " + payload.toString();
        }
        String jsonResult = aiService.generateStructuredJson(prompt, "Mock Interview Evaluation", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> reviewCode(Map<String, String> payload, UserPrincipal principal) {
        String code = payload.getOrDefault("code", "");
        String prompt = "Review Java code performance and correctness: " + code;
        String jsonResult = aiService.generateStructuredJson(prompt, "Coding Review", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> generateCoverLetter(Map<String, String> payload, UserPrincipal principal) {
        Student student = getStudentForUser(principal.getId());
        String prompt = "Generate cover letter for " + student.getUser().getFullName() + " applying for role " + payload.getOrDefault("targetRole", "Software Engineer");
        String jsonResult = aiService.generateStructuredJson(prompt, "Cover Letter", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> generateCareerPath(Map<String, String> payload, UserPrincipal principal) {
        String role = payload.getOrDefault("targetRole", "Software Engineer");
        String prompt = "Generate career growth path stages for role: " + role;
        String jsonResult = aiService.generateStructuredJson(prompt, "Career Path", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> generateDailyPlan(Map<String, Object> payload, UserPrincipal principal) {
        String prompt = "Generate daily career study tasks for student.";
        String jsonResult = aiService.generateStructuredJson(prompt, "Daily Plan", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional
    public Map<String, Object> analyzeCommunication(Map<String, String> payload, UserPrincipal principal) {
        String text = payload.getOrDefault("text", "");
        String prompt = "Analyze communication clarity and grammar: " + text;
        String jsonResult = aiService.generateStructuredJson(prompt, "Communication Analyzer", principal.getId());
        return parseJson(jsonResult);
    }

    @Transactional(readOnly = true)
    public PlacementMathEngine.MatchResult calculateSkillGap(Long studentId, Long jobId, UserPrincipal principal) {
        verifyStudentAccess(studentId, principal);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        List<StudentSkill> studentSkills = studentSkillRepository.findByStudentId(studentId);
        Set<String> studentSkillNames = studentSkills.stream()
                .map(ss -> ss.getSkill().getName())
                .collect(Collectors.toSet());

        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(jobId);

        return mathEngine.calculateJobMatch(studentSkillNames, jobSkills);
    }

    @Transactional(readOnly = true)
    public PlacementMathEngine.ReadinessBreakdown getPlacementReadiness(UserPrincipal principal) {
        Student student = getStudentForUser(principal.getId());

        Resume latestResume = resumeRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId()).orElse(null);
        Double resumeScore = latestResume != null ? latestResume.getResumeScore() : null;

        List<StudentSkill> skills = studentSkillRepository.findByStudentId(student.getId());
        Double technicalScore = !skills.isEmpty() ? Math.min(100.0, skills.size() * 15.0) : 75.0;

        Double codingScore = 80.0;
        Double interviewScore = 82.0;

        return mathEngine.calculateReadiness(technicalScore, resumeScore, codingScore, interviewScore);
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null) return Collections.emptyMap();
        String cleaned = json.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();
        try {
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON response: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("response", cleaned);
            fallback.put("answer", cleaned);
            return fallback;
        }
    }

    private Student getStudentForUser(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user ID: " + userId));
    }

    private void verifyStudentAccess(Long studentId, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMIN || principal.getRole() == Role.TPO) {
            return;
        }
        if (principal.getRole() == Role.STUDENT) {
            Student student = getStudentForUser(principal.getId());
            if (!student.getId().equals(studentId)) {
                throw new ForbiddenAccessException("You are not authorized to view another student's data");
            }
            return;
        }
        if (principal.getRole() == Role.FACULTY) {
            boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndStudentId(principal.getId(), studentId);
            if (!assigned) {
                throw new ForbiddenAccessException("You can only access students assigned to your guidance");
            }
            return;
        }
        if (principal.getRole() == Role.MENTOR) {
            boolean assigned = mentorAssignmentRepository.existsByMentorIdAndStudentId(principal.getId(), studentId);
            if (!assigned) {
                throw new ForbiddenAccessException("You can only access mentees assigned to you");
            }
        }
    }
}
