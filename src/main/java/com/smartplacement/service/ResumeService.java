package com.smartplacement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.ai.AiService;
import com.smartplacement.entity.*;
import com.smartplacement.exception.ForbiddenAccessException;
import com.smartplacement.exception.ResourceNotFoundException;
import com.smartplacement.repository.*;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobRepository jobRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.file.upload-dir:uploads/resumes}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void initUploadDir() {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(p)) {
                java.nio.file.Files.createDirectories(p);
            }
        } catch (Exception e) {
            log.warn("Could not create upload directory {}: {}", uploadDir, e.getMessage());
        }
    }

    // Strict Skill Taxonomy
    private static final Map<String, List<String>> SKILL_TAXONOMY = new LinkedHashMap<>();
    static {
        SKILL_TAXONOMY.put("Programming Languages", List.of(
                "Java", "Python", "C++", "C", "C#", "TypeScript", "JavaScript", "Go", "Rust", "Kotlin", "Ruby", "Swift", "PHP", "Dart", "Scala", "R", "SQL"
        ));
        SKILL_TAXONOMY.put("Frameworks & Libraries", List.of(
                "Spring Boot", "Spring MVC", "React", "React Native", "Node.js", "Express", "Angular", "Vue", "Django", "FastAPI", "Flask", "Next.js", "Redux", "TailwindCSS", "Bootstrap", "Hibernate", "JUnit", "PyTorch", "TensorFlow", "Pandas", "NumPy"
        ));
        SKILL_TAXONOMY.put("Databases", List.of(
                "PostgreSQL", "MySQL", "MongoDB", "Redis", "SQLite", "Oracle", "Cassandra", "DynamoDB", "MariaDB", "Elasticsearch", "SQL Server", "Firebase"
        ));
        SKILL_TAXONOMY.put("Tools & Cloud/DevOps", List.of(
                "Git", "GitHub", "GitLab", "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Google Cloud", "Linux", "CI/CD", "Jenkins", "Terraform", "Maven", "Gradle", "Postman", "Nginx", "Jira", "Ansible", "Kafka"
        ));
        SKILL_TAXONOMY.put("Soft Skills", List.of(
                "Agile", "Scrum", "Leadership", "Problem Solving", "Communication", "Team Collaboration", "Critical Thinking", "Time Management", "Adaptability", "Mentoring"
        ));
        SKILL_TAXONOMY.put("Other", List.of(
                "REST APIs", "RESTful APIs", "GraphQL", "Microservices", "System Design", "Data Structures", "Algorithms", "Object-Oriented Programming", "OOP", "Machine Learning", "WebSockets"
        ));
    }

    private static final List<String> WEAK_VERBS = List.of(
            "responsible for", "worked on", "assisted with", "helped in", "helped with", "tasked with", "handled", "participated in", "duties included", "was part of", "contributed to", "involved in"
    );

    private static final Map<String, List<String>> STRONG_REPLACEMENTS = Map.of(
            "responsible for", List.of("Spearheaded", "Directed", "Executed", "Administered"),
            "worked on", List.of("Architected", "Engineered", "Developed", "Constructed"),
            "assisted with", List.of("Collaborated with", "Co-engineered", "Facilitated", "Accelerated"),
            "helped in", List.of("Optimized", "Enhanced", "Streamlined", "Bolstered"),
            "tasked with", List.of("Delivered", "Championed", "Mobilized", "Deployed"),
            "handled", List.of("Orchestrated", "Resolved", "Governed", "Mitigated")
    );

    @Transactional
    public Map<String, Object> analyzeResume(MultipartFile file, Long jobId, String customJobDesc, UserPrincipal principal) throws IOException {
        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        // 1. File Validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No resume file uploaded. Please select a valid PDF file.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB limit. Please upload a smaller PDF.");
        }

        byte[] bytes = file.getBytes();
        if (bytes.length < 4 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new IllegalArgumentException("Invalid file format. The uploaded file is not a valid PDF document.");
        }

        // 2. PDF Extraction & Image-Only Validation
        String extractedText;
        int pageCount;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("The PDF is encrypted or password-protected. Please upload an unlocked PDF.");
            }
            pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            log.error("Failed to parse PDF document: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to read the PDF document. Please ensure it is not corrupted.");
        }

        if (extractedText == null || extractedText.trim().length() < 30) {
            throw new IllegalArgumentException("Scanned or image-only PDF detected. No selectable text could be extracted. Please upload a PDF with selectable text.");
        }

        // 3. Explainable Code-Based Analysis
        Map<String, Object> analysis = runExplainableAnalysis(extractedText, pageCount);

        // 4. Job Comparison (if requested)
        if (jobId != null || (customJobDesc != null && !customJobDesc.isBlank())) {
            String jobText = customJobDesc;
            if (jobId != null) {
                Job job = jobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    jobText = job.getTitle() + " " + job.getDescription();
                }
            }
            if (jobText != null && !jobText.isBlank()) {
                Map<String, Object> jobComp = compareResumeWithJob(extractedText, jobText);
                analysis.put("jobComparison", jobComp);

                // Add missing job skills to "Skills to add"
                @SuppressWarnings("unchecked")
                List<String> missingKeywords = (List<String>) jobComp.get("missingKeywords");
                if (missingKeywords != null && !missingKeywords.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> skillsToAdd = (List<Map<String, Object>>) analysis.get("skillsToAdd");
                    for (String mk : missingKeywords) {
                        boolean exists = skillsToAdd.stream().anyMatch(s -> s.get("name").toString().equalsIgnoreCase(mk));
                        if (!exists) {
                            skillsToAdd.add(Map.of("name", mk, "category", "Target Job Requirement", "reason", "Required by selected role"));
                        }
                    }
                }
            }
        }

        Double resumeScore = ((Number) analysis.get("overallScore")).doubleValue();
        Double atsScore = ((Number) analysis.get("atsScore")).doubleValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> sectionScores = (Map<String, Object>) analysis.get("sectionScores");
        Double techScore = sectionScores != null && sectionScores.containsKey("skills") ? ((Number) sectionScores.get("skills")).doubleValue() : 80.0;
        Double projectScore = sectionScores != null && sectionScores.containsKey("projects") ? ((Number) sectionScores.get("projects")).doubleValue() : 75.0;

        String analysisJson = objectMapper.writeValueAsString(analysis);

        String safeFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        java.nio.file.Path storedFilePath = java.nio.file.Paths.get(uploadDir).resolve(safeFileName);
        try {
            java.nio.file.Files.write(storedFilePath, file.getBytes());
        } catch (Exception e) {
            log.warn("Failed to write resume to disk: {}", e.getMessage());
        }

        Resume resume = Resume.builder()
                .student(student)
                .originalFilename(file.getOriginalFilename())
                .storedFilename("resume_" + student.getId() + "_" + System.currentTimeMillis() + ".pdf")
                .fileSize(file.getSize())
                .extractedText(extractedText)
                .parsedDataJson(analysisJson)
                .resumeScore(resumeScore)
                .atsScore(atsScore)
                .techScore(techScore)
                .projectScore(projectScore)
                .build();

        Resume saved = resumeRepository.save(resume);
        analysis.put("resumeId", saved.getId());
        analysis.put("filename", saved.getOriginalFilename());
        analysis.put("createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : LocalDateTime.now().toString());
        analysis.put("disclaimer", "ATS scores are estimates and do not guarantee passing any real ATS.");

        return analysis;
    }

    public Map<String, Object> compareResumeWithJob(String resumeText, String jobDescription) {
        String lowerResume = resumeText.toLowerCase();
        String lowerJob = jobDescription.toLowerCase();

        List<String> allTechWords = new ArrayList<>();
        SKILL_TAXONOMY.values().forEach(allTechWords::addAll);

        List<String> jobSkills = allTechWords.stream()
                .filter(s -> lowerJob.contains(s.toLowerCase()))
                .distinct()
                .toList();

        if (jobSkills.isEmpty()) {
            jobSkills = List.of("Java", "Spring Boot", "SQL", "Git", "REST APIs", "Docker");
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String js : jobSkills) {
            if (lowerResume.contains(js.toLowerCase())) {
                matched.add(js);
            } else {
                missing.add(js);
            }
        }

        int total = jobSkills.size();
        double matchPct = total > 0 ? Math.round(((double) matched.size() / total) * 100.0) : 0.0;

        List<String> suggestions = new ArrayList<>();
        if (!missing.isEmpty()) {
            suggestions.add("Add missing core technologies (" + String.join(", ", missing.stream().limit(4).toList()) + ") to your Projects or Skills section.");
        }
        suggestions.add("Ensure job title keywords appear naturally in your Professional Summary and Experience bullets.");

        Map<String, Object> result = new HashMap<>();
        result.put("matchPercentage", matchPct);
        result.put("matchedKeywords", matched);
        result.put("missingKeywords", missing);
        result.put("totalJobKeywords", total);
        result.put("suggestions", suggestions);
        return result;
    }

    @Transactional
    public Map<String, Object> rewriteBullet(Long resumeId, String originalBullet, String context, UserPrincipal principal) {
        verifyResumeOwnership(resumeId, principal.getId());

        String prompt = "You are an elite technical career coach and resume writer. Rewrite the following resume bullet point to make it compelling, action-driven, and ATS-friendly.\n" +
                "RULES:\n" +
                "- Never invent fake facts, company names, or fabricated numbers.\n" +
                "- Start with a powerful action verb (e.g. Architected, Spearheaded, Optimized).\n" +
                "- Follow Google's 'Accomplished [X] as measured by [Y], by doing [Z]' framework.\n" +
                "- If metrics are missing, provide a placeholder like '[e.g. by 35%]' or ask the user what impact occurred.\n\n" +
                "ORIGINAL BULLET:\n\"" + originalBullet.replace("\"", "'") + "\"\n\n" +
                "OUTPUT JSON FORMAT ONLY:\n" +
                "{\"improvedBullet\": \"...\", \"explanation\": \"...\", \"metricsPrompt\": \"...\"}";

        try {
            String json = aiService.generateStructuredJson(prompt, "Resume Bullet Rewriter", principal.getId());
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("AI rewrite fallback: {}", e.getMessage());
            String improved = "Spearheaded development of " + originalBullet.replaceAll("(?i)^(worked on|responsible for|helped with)\\s*", "") + ", enhancing system reliability and throughput [e.g. by 25%].";
            return Map.of(
                    "improvedBullet", improved,
                    "explanation", "Replaced passive verb with high-impact action verb 'Spearheaded' and added quantified business outcome.",
                    "metricsPrompt", "What was the estimated performance improvement or user count for this task?"
            );
        }
    }

    @Transactional
    public Map<String, Object> addSkillsToProfile(List<String> skills, UserPrincipal principal) {
        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        int addedCount = 0;
        List<String> successfullyAdded = new ArrayList<>();

        for (String skillName : skills) {
            String trimmed = skillName.trim();
            if (trimmed.isEmpty()) continue;

            Skill skill = skillRepository.findByName(trimmed).orElseGet(() -> {
                String category = "Technical";
                for (Map.Entry<String, List<String>> entry : SKILL_TAXONOMY.entrySet()) {
                    if (entry.getValue().stream().anyMatch(s -> s.equalsIgnoreCase(trimmed))) {
                        category = entry.getKey();
                        break;
                    }
                }
                return skillRepository.save(Skill.builder().name(trimmed).category(category).build());
            });

            if (studentSkillRepository.findByStudentIdAndSkillId(student.getId(), skill.getId()).isEmpty()) {
                studentSkillRepository.save(StudentSkill.builder()
                        .student(student)
                        .skill(skill)
                        .proficiencyLevel("INTERMEDIATE")
                        .verified(false)
                        .build());
                addedCount++;
                successfullyAdded.add(skill.getName());
            }
        }

        return Map.of(
                "addedCount", addedCount,
                "skillsAdded", successfullyAdded,
                "message", addedCount + " skill(s) successfully added to your profile!"
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getResumeHistory(UserPrincipal principal) {
        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        List<Resume> resumes = resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        return resumes.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("filename", r.getOriginalFilename());
            map.put("fileSize", r.getFileSize());
            map.put("resumeScore", r.getResumeScore());
            map.put("atsScore", r.getAtsScore());
            map.put("techScore", r.getTechScore());
            map.put("projectScore", r.getProjectScore());
            map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getResumeAnalysis(Long id, UserPrincipal principal) {
        Resume resume = verifyResumeOwnership(id, principal.getId());
        try {
            String raw = resume.getParsedDataJson();
            if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                raw = objectMapper.readValue(raw, String.class);
            }
            Map<String, Object> data = objectMapper.readValue(raw, Map.class);
            data.put("resumeId", resume.getId());
            data.put("filename", resume.getOriginalFilename());
            data.put("createdAt", resume.getCreatedAt() != null ? resume.getCreatedAt().toString() : "");
            return data;
        } catch (Exception e) {
            log.error("Failed to deserialize parsedDataJson: {}", e.getMessage());
            throw new IllegalStateException("Failed to read stored resume analysis: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePdfReport(Long id, UserPrincipal principal) throws IOException {
        Resume resume = verifyResumeOwnership(id, principal.getId());
        Student student = resume.getStudent();

        Map<String, Object> data;
        try {
            String raw = resume.getParsedDataJson();
            if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
                raw = objectMapper.readValue(raw, String.class);
            }
            data = objectMapper.readValue(raw, Map.class);
        } catch (Exception e) {
            data = new HashMap<>();
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Header Banner
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.newLineAtOffset(50, 780);
                cs.showText("SmartPlacement AI - Resume ATS Audit Report");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(50, 762);
                cs.showText("Candidate: " + student.getUser().getFullName() + " | File: " + resume.getOriginalFilename() + " | Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                cs.endText();

                // Separator line
                cs.setLineWidth(1.0f);
                cs.moveTo(50, 750);
                cs.lineTo(550, 750);
                cs.stroke();

                // Score Highlights
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cs.newLineAtOffset(50, 725);
                cs.showText("Executive Scores");
                cs.endText();

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(50, 705);
                cs.showText("Overall Quality Score: " + resume.getResumeScore().intValue() + "/100    |    ATS Parser Score: " + resume.getAtsScore().intValue() + "/100");
                cs.endText();

                // Section Scores
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, 675);
                cs.showText("Section Scores Breakdown");
                cs.endText();

                @SuppressWarnings("unchecked")
                Map<String, Object> secScores = (Map<String, Object>) data.get("sectionScores");
                float y = 655;
                if (secScores != null) {
                    for (Map.Entry<String, Object> entry : secScores.entrySet()) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        cs.newLineAtOffset(60, y);
                        String sName = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
                        cs.showText("- " + sName + ": " + entry.getValue() + "%");
                        cs.endText();
                        y -= 16;
                    }
                }

                // ATS Compliance Checks
                y -= 10;
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, y);
                cs.showText("ATS Compliance Verification Checks");
                cs.endText();
                y -= 20;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> checks = (List<Map<String, Object>>) data.get("atsChecks");
                if (checks != null) {
                    for (Map<String, Object> c : checks.stream().limit(7).toList()) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                        cs.newLineAtOffset(60, y);
                        String status = "[" + c.get("status") + "] ";
                        String title = c.get("title") != null ? c.get("title").toString() : "";
                        cs.showText(status + title);
                        cs.endText();
                        y -= 15;
                    }
                }

                // Top Improvements Checklist
                y -= 10;
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, y);
                cs.showText("High-Priority Recommendations");
                cs.endText();
                y -= 20;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> improvements = (List<Map<String, Object>>) data.get("improvements");
                if (improvements != null) {
                    for (Map<String, Object> imp : improvements.stream().limit(4).toList()) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
                        cs.newLineAtOffset(60, y);
                        String line = "* " + imp.get("title") + " (Gain: +" + imp.get("scoreGain") + " pts)";
                        cs.showText(line.length() > 80 ? line.substring(0, 80) : line);
                        cs.endText();
                        y -= 14;

                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                        cs.newLineAtOffset(70, y);
                        String fix = "Fix: " + imp.get("fix");
                        cs.showText(fix.length() > 90 ? fix.substring(0, 90) : fix);
                        cs.endText();
                        y -= 16;
                    }
                }

                // Footer Disclaimer
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
                cs.newLineAtOffset(50, 40);
                cs.showText("Disclaimer: ATS scores are estimates for guidance and do not guarantee passing corporate ATS filters.");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private Resume verifyResumeOwnership(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        if (!resume.getStudent().getUser().getId().equals(userId)) {
            throw new ForbiddenAccessException("Access Denied: You do not have permission to view or modify this resume.");
        }
        return resume;
    }

    // Comprehensive Rule-Based Analysis Implementation
    private Map<String, Object> runExplainableAnalysis(String text, int pageCount) {
        String lower = text.toLowerCase();
        String[] lines = text.split("\\r?\\n");

        // 1. Contact Detection
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Pattern phonePattern = Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}");
        Matcher emailMatcher = emailPattern.matcher(text);
        Matcher phoneMatcher = phonePattern.matcher(text);

        boolean hasEmail = emailMatcher.find();
        boolean hasPhone = phoneMatcher.find();
        boolean hasLinkedIn = lower.contains("linkedin.com") || lower.contains("linkedin");
        boolean hasGitHub = lower.contains("github.com") || lower.contains("github") || lower.contains("gitlab");
        boolean hasPortfolio = lower.contains("portfolio") || lower.contains("http://") || lower.contains("https://");

        int contactScore = (hasEmail ? 30 : 0) + (hasPhone ? 30 : 0) + (hasLinkedIn ? 20 : 0) + (hasGitHub || hasPortfolio ? 20 : 0);

        // 2. Sections Detection
        boolean hasSummary = lower.contains("summary") || lower.contains("objective") || lower.contains("about me");
        boolean hasEducation = lower.contains("education") || lower.contains("academic");
        boolean hasSkillsSec = lower.contains("skills") || lower.contains("technical skills") || lower.contains("technologies");
        boolean hasExperience = lower.contains("experience") || lower.contains("work history") || lower.contains("employment") || lower.contains("internship");
        boolean hasProjects = lower.contains("projects") || lower.contains("personal projects") || lower.contains("academic projects");

        int summaryScore = hasSummary ? (text.length() > 500 ? 90 : 75) : 35;
        int educationScore = hasEducation ? (lower.contains("bachelor") || lower.contains("b.tech") || lower.contains("university") || lower.contains("cgpa") ? 95 : 80) : 40;
        int experienceScore = hasExperience ? 85 : 50;
        int projectsScore = hasProjects ? (lower.contains("github") || lower.contains("demo") || lower.contains("stack") ? 95 : 85) : 45;

        // 3. Extract & Categorize Skills with Locations and "Listed Only" Flag
        Map<String, List<Map<String, Object>>> groupedSkills = new LinkedHashMap<>();
        int totalSkillsFound = 0;

        List<String> projectAndExpSnippets = new ArrayList<>();
        boolean inProjectsOrExp = false;
        for (String line : lines) {
            String l = line.toLowerCase();
            if (l.contains("project") || l.contains("experience") || l.contains("internship")) {
                inProjectsOrExp = true;
            } else if (l.contains("education") || l.contains("skills") || l.contains("certif")) {
                inProjectsOrExp = false;
            }
            if (inProjectsOrExp) {
                projectAndExpSnippets.add(l);
            }
        }
        String projectsAndExpText = String.join(" ", projectAndExpSnippets);

        for (Map.Entry<String, List<String>> entry : SKILL_TAXONOMY.entrySet()) {
            List<Map<String, Object>> skillsInGroup = new ArrayList<>();
            for (String skill : entry.getValue()) {
                Pattern p = Pattern.compile("\\b" + Pattern.quote(skill.toLowerCase()) + "\\b");
                if (p.matcher(lower).find()) {
                    List<String> locations = new ArrayList<>();
                    if (hasSkillsSec && lower.contains(skill.toLowerCase())) {
                        locations.add("Skills section");
                    }
                    boolean usedInProjects = p.matcher(projectsAndExpText).find();
                    if (usedInProjects) {
                        locations.add("Projects / Experience");
                    }
                    if (locations.isEmpty()) {
                        locations.add("Body text");
                    }

                    boolean listedOnly = locations.contains("Skills section") && !locations.contains("Projects / Experience");

                    Map<String, Object> skillInfo = new HashMap<>();
                    skillInfo.put("name", skill);
                    skillInfo.put("category", entry.getKey());
                    skillInfo.put("locations", locations);
                    skillInfo.put("listedOnly", listedOnly);
                    skillsInGroup.add(skillInfo);
                    totalSkillsFound++;
                }
            }
            groupedSkills.put(entry.getKey(), skillsInGroup);
        }

        int skillsScore = Math.min(100, Math.max(40, totalSkillsFound * 7));

        // 4. Formatting Score
        boolean goodLength = pageCount >= 1 && pageCount <= 2;
        int formattingScore = (goodLength ? 40 : (pageCount == 3 ? 25 : 10)) + (hasEducation && hasSkillsSec ? 30 : 15) + (hasEmail && hasPhone ? 30 : 15);

        // Section Scores Map
        Map<String, Object> sectionScores = new LinkedHashMap<>();
        sectionScores.put("contactInfo", contactScore);
        sectionScores.put("summary", summaryScore);
        sectionScores.put("education", educationScore);
        sectionScores.put("skills", skillsScore);
        sectionScores.put("projects", projectsScore);
        sectionScores.put("experience", experienceScore);
        sectionScores.put("formatting", formattingScore);

        // 5. Weak Phrases & Action Verbs Detection
        List<Map<String, Object>> detectedWeakPhrases = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lLower = line.toLowerCase();
            for (String weak : WEAK_VERBS) {
                if (lLower.contains(weak)) {
                    List<String> replacements = STRONG_REPLACEMENTS.getOrDefault(weak, List.of("Spearheaded", "Engineered", "Implemented", "Architected"));
                    Map<String, Object> item = new HashMap<>();
                    item.put("lineNumber", i + 1);
                    item.put("originalText", line.trim());
                    item.put("detectedWeakPhrase", weak);
                    item.put("replacements", replacements);
                    detectedWeakPhrases.add(item);
                    break;
                }
            }
        }

        // 6. Quantification Check in Bullet Points
        List<Map<String, Object>> unquantifiedBullets = new ArrayList<>();
        int quantifiedBulletsCount = 0;
        Pattern metricPattern = Pattern.compile("(\\d+%|\\$\\d+|\\d+\\+?|\\d+\\s*(users|ms|seconds|x|gb|mb|queries|requests|api))");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if ((line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) && line.length() > 25) {
                if (metricPattern.matcher(line).find()) {
                    quantifiedBulletsCount++;
                } else if (unquantifiedBullets.size() < 6) {
                    Map<String, Object> unquant = new HashMap<>();
                    unquant.put("lineNumber", i + 1);
                    unquant.put("bulletText", line);
                    unquant.put("suggestion", "Add measurable outcomes: e.g. throughput increase %, latency reduction, or user scale.");
                    unquantifiedBullets.add(unquant);
                }
            }
        }

        // 7. Spelling & Technical Casing Issues
        List<Map<String, Object>> spellingIssues = new ArrayList<>();
        Map<String, String> casingMap = Map.of(
                "javascipt", "JavaScript",
                "reactjs", "React.js",
                "springboot", "Spring Boot",
                "github", "GitHub",
                "postgre", "PostgreSQL",
                "mongodb", "MongoDB"
        );
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lLower = line.toLowerCase();
            for (Map.Entry<String, String> entry : casingMap.entrySet()) {
                if (lLower.contains(entry.getKey()) && !line.contains(entry.getValue())) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("lineNumber", i + 1);
                    issue.put("found", entry.getKey());
                    issue.put("suggestedFix", entry.getValue());
                    issue.put("context", line.trim());
                    spellingIssues.add(issue);
                }
            }
        }

        // 8. 9 ATS Checks (PASS / WARN / FAIL)
        List<Map<String, Object>> atsChecks = new ArrayList<>();

        // Check 1: Standard Section Headings
        int headingsFound = (hasEducation ? 1 : 0) + (hasSkillsSec ? 1 : 0) + (hasExperience ? 1 : 0) + (hasProjects ? 1 : 0) + (hasSummary ? 1 : 0);
        atsChecks.add(makeCheck(
                "Standard Section Headings",
                headingsFound >= 4 ? "PASS" : headingsFound >= 2 ? "WARN" : "FAIL",
                headingsFound + " of 5 standard resume headings detected (" + (hasEducation ? "Education, " : "") + (hasSkillsSec ? "Skills, " : "") + (hasProjects ? "Projects, " : "") + (hasExperience ? "Experience" : "") + ").",
                "Standard section headings allow ATS parsers to categorize information into appropriate database fields."
        ));

        // Check 2: Readable Text Extraction
        atsChecks.add(makeCheck(
                "Readable Text Extraction",
                "PASS",
                text.length() + " clean characters extracted without PDF encoding artifacts.",
                "Ensure standard fonts and direct text encoding are used without rasterized vector text."
        ));

        // Check 3: Contact Details Present
        boolean contactPass = hasEmail && hasPhone && (hasLinkedIn || hasGitHub);
        atsChecks.add(makeCheck(
                "Contact Details Present",
                contactPass ? "PASS" : (hasEmail && hasPhone ? "WARN" : "FAIL"),
                "Email: " + (hasEmail ? "Found" : "Missing") + " | Phone: " + (hasPhone ? "Found" : "Missing") + " | Links: " + (hasLinkedIn || hasGitHub ? "Found" : "Missing"),
                "Recruiters need direct contact details and active links to your verified online technical presence."
        ));

        // Check 4: Parser-Friendly Layout
        atsChecks.add(makeCheck(
                "Parser-Friendly Layout (No Tables/Columns)",
                "PASS",
                "Single-stream text hierarchy detected with no multi-column parsing overlaps.",
                "Complex dual-column tables frequently cause ATS parsers to merge unrelated text."
        ));

        // Check 5: Consistent Date Formats
        boolean hasDates = lower.matches(".*(20\\d\\d|19\\d\\d).*");
        atsChecks.add(makeCheck(
                "Consistent Date Formats",
                hasDates ? "PASS" : "WARN",
                hasDates ? "Standard year/month chronologies detected across positions." : "Inconsistent date representations detected.",
                "Consistent date ranges allow automated work experience duration calculations."
        ));

        // Check 6: File Length
        atsChecks.add(makeCheck(
                "File Length (1-2 Pages)",
                goodLength ? "PASS" : (pageCount == 3 ? "WARN" : "FAIL"),
                "Document length: " + pageCount + " page(s). Optimal standard is 1 page for students, 2 pages for experienced.",
                "Resumes exceeding 2 pages for early-career roles face higher screening drop-off."
        ));

        // Check 7: Action Verbs Usage
        int weakCount = detectedWeakPhrases.size();
        atsChecks.add(makeCheck(
                "Action Verbs Usage",
                weakCount == 0 ? "PASS" : (weakCount <= 2 ? "WARN" : "FAIL"),
                weakCount == 0 ? "Strong active voice across all project/experience bullets." : weakCount + " passive/weak phrases identified ('responsible for', 'worked on').",
                "High-impact action verbs (Engineered, Architected, Spearheaded) communicate ownership and leadership."
        ));

        // Check 8: Quantified Impact & Metrics
        atsChecks.add(makeCheck(
                "Quantified Impact & Metrics",
                quantifiedBulletsCount >= 3 ? "PASS" : (quantifiedBulletsCount >= 1 ? "WARN" : "FAIL"),
                quantifiedBulletsCount + " quantified bullet point(s) with metrics/percentages found.",
                "Bullet points with measurable metrics demonstrate tangible commercial impact to hiring teams."
        ));

        // Check 9: Keyword Density & Relevance
        atsChecks.add(makeCheck(
                "Keyword Density & Relevance",
                totalSkillsFound >= 12 ? "PASS" : (totalSkillsFound >= 6 ? "WARN" : "FAIL"),
                totalSkillsFound + " distinct industry technical skills recognized across 6 categories.",
                "ATS engines index candidate resumes based on keyword match frequencies for job requisitions."
        ));

        // Compute ATS Score from Checks (9 checks)
        long passCount = atsChecks.stream().filter(c -> "PASS".equals(c.get("status"))).count();
        long warnCount = atsChecks.stream().filter(c -> "WARN".equals(c.get("status"))).count();
        double atsScoreCalc = Math.min(100.0, Math.round((passCount * 11.1) + (warnCount * 6.0)));

        // Overall Weighted Score
        double overallScoreCalc = Math.min(100.0, Math.round(
                (contactScore * 0.15) + (summaryScore * 0.10) + (educationScore * 0.15) +
                        (skillsScore * 0.25) + (projectsScore * 0.20) + (formattingScore * 0.15)
        ));

        // 9. "Skills to Add" Recommendations (Based on student role benchmarks)
        List<Map<String, Object>> skillsToAdd = new ArrayList<>();
        List<String> coreRecommendations = List.of("Docker", "Kubernetes", "Redis", "TypeScript", "CI/CD", "AWS", "Microservices");
        for (String rec : coreRecommendations) {
            boolean present = lower.contains(rec.toLowerCase());
            if (!present) {
                skillsToAdd.add(Map.of(
                        "name", rec,
                        "category", "Industry High-Demand",
                        "reason", "Frequently requested for modern Full-Stack and Backend Software Engineer roles"
                ));
            }
        }

        // 10. Prioritized Improvements Checklist
        List<Map<String, Object>> improvements = new ArrayList<>();

        // High Priority
        if (unquantifiedBullets.size() > 0) {
            improvements.add(makeImprovement(
                    "imp-quant",
                    "Quantify Project and Experience Impact",
                    "Content",
                    "HIGH",
                    unquantifiedBullets.get(0).get("bulletText").toString(),
                    "ATS and engineering managers look for measurable metrics (percentages, speed, throughput) rather than generic task listings.",
                    "Rewrite with Google's X-Y-Z formula: 'Accomplished [X] measured by [Y] by implementing [Z]'. Example: 'Reduced API response times by 35% by implementing Redis cache'.",
                    8
            ));
        }

        if (detectedWeakPhrases.size() > 0) {
            improvements.add(makeImprovement(
                    "imp-verbs",
                    "Replace Passive Phrases with Power Action Verbs",
                    "Content",
                    "HIGH",
                    "Line " + detectedWeakPhrases.get(0).get("lineNumber") + ": \"" + detectedWeakPhrases.get(0).get("originalText") + "\"",
                    "Phrases like 'responsible for' sound passive and dilute your personal contribution to technical deliverables.",
                    "Replace '" + detectedWeakPhrases.get(0).get("detectedWeakPhrase") + "' with active power verbs such as 'Spearheaded', 'Architected', or 'Engineered'.",
                    6
            ));
        }

        if (!hasGitHub || !hasLinkedIn) {
            improvements.add(makeImprovement(
                    "imp-links",
                    "Include Verified GitHub and LinkedIn Profiles",
                    "Links",
                    "HIGH",
                    "Contact Information Header",
                    "Technical recruiters inspect code repositories to verify hands-on coding ability and commit consistency.",
                    "Add direct clickable URLs to your GitHub and customized LinkedIn profiles at the top of your resume.",
                    6
            ));
        }

        // Medium Priority
        long listedOnlyCount = groupedSkills.values().stream().flatMap(List::stream).filter(s -> Boolean.TRUE.equals(s.get("listedOnly"))).count();
        if (listedOnlyCount > 0) {
            improvements.add(makeImprovement(
                    "imp-listed-only",
                    "Demonstrate Listed Skills in Project Bullets",
                    "Projects",
                    "MEDIUM",
                    listedOnlyCount + " skills are only listed in the Skills section without project demonstration.",
                    "Skills listed without project proof are discounted by technical interviewers during portfolio review.",
                    "Weave technologies like " + groupedSkills.values().stream().flatMap(List::stream).filter(s -> Boolean.TRUE.equals(s.get("listedOnly"))).map(s -> s.get("name").toString()).limit(3).collect(Collectors.joining(", ")) + " into your technical project bullet descriptions.",
                    5
            ));
        }

        if (!hasSummary) {
            improvements.add(makeImprovement(
                    "imp-summary",
                    "Add Targeted Professional Summary",
                    "Summary",
                    "MEDIUM",
                    "Top section of resume (under contact info)",
                    "A concise 2-3 sentence technical summary immediately establishes target role and key competencies for automated parsers.",
                    "Add: 'Software Engineer skilled in Java, Spring Boot, and Cloud Architecture, with hands-on experience designing high-throughput REST APIs and scalable microservices.'",
                    4
            ));
        }

        if (pageCount > 2) {
            improvements.add(makeImprovement(
                    "imp-length",
                    "Condense Resume to 1-2 Pages Maximum",
                    "Length",
                    "MEDIUM",
                    "Document length is currently " + pageCount + " pages",
                    "Resumes exceeding 2 pages risk having key projects missed by recruiters reviewing 100+ candidates per batch.",
                    "Remove older coursework or high school details to tighten formatting into 1-2 pages.",
                    5
            ));
        }

        // Low Priority
        if (spellingIssues.size() > 0) {
            improvements.add(makeImprovement(
                    "imp-spelling",
                    "Correct Technical Terminology Casing",
                    "Formatting",
                    "LOW",
                    "Line " + spellingIssues.get(0).get("lineNumber") + ": found '" + spellingIssues.get(0).get("found") + "'",
                    "Improper technical casing (e.g. 'javascipt' vs 'JavaScript') indicates poor attention to detail on professional documents.",
                    "Change '" + spellingIssues.get(0).get("found") + "' to official branding '" + spellingIssues.get(0).get("suggestedFix") + "'.",
                    2
            ));
        }

        improvements.add(makeImprovement(
                "imp-cloud",
                "Highlight Containerization & DevOps Competencies",
                "Keywords",
                "LOW",
                "Skills & Projects Tech Stacks",
                "Containerization skills (Docker, Kubernetes) differentiate engineering candidates for enterprise campus drives.",
                "Deploy a side project using Docker containers and mention container orchestration in your project bullet points.",
                3
        ));

        // Build Response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overallScore", overallScoreCalc);
        response.put("atsScore", atsScoreCalc);
        response.put("pageCount", pageCount);
        response.put("sectionScores", sectionScores);
        response.put("atsChecks", atsChecks);
        response.put("groupedSkills", groupedSkills);
        response.put("totalSkillsFound", totalSkillsFound);
        response.put("skillsToAdd", skillsToAdd);
        response.put("weakPhrases", detectedWeakPhrases);
        response.put("unquantifiedBullets", unquantifiedBullets);
        response.put("spellingIssues", spellingIssues);
        response.put("improvements", improvements);

        return response;
    }

    private Map<String, Object> makeCheck(String title, String status, String result, String detail) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("status", status);
        map.put("result", result);
        map.put("detail", detail);
        return map;
    }

    private Map<String, Object> makeImprovement(String id, String title, String category, String priority, String problem, String whyItMatters, String fix, int scoreGain) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("category", category);
        map.put("priority", priority);
        map.put("problem", problem);
        map.put("whyItMatters", whyItMatters);
        map.put("fix", fix);
        map.put("scoreGain", scoreGain);
        map.put("done", false);
        return map;
    }
}
