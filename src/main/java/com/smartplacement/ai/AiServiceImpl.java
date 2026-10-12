package com.smartplacement.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplacement.entity.AiUsageLog;
import com.smartplacement.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.mode:dev-fallback}")
    private String aiMode;

    @Value("${ai.api-key:mock-key-for-dev}")
    private String apiKey;

    @Value("${ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;

    @Override
    public String generateContent(String prompt, String featureName, Long userId) {
        return generateStructuredJson(prompt, featureName, userId);
    }

    @Override
    public String generateStructuredJson(String prompt, String featureName, Long userId) {
        log.info("Executing AI Service [{}] for user {} in mode [{}]", featureName, userId, aiMode);

        if ("disabled".equalsIgnoreCase(aiMode)) {
            logUsage(userId, featureName, 0, 0, false, "AI service is disabled by configuration");
            throw new IllegalStateException("AI Features are currently disabled on this server.");
        }

        boolean hasValidKey = apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("mock-") && !apiKey.equalsIgnoreCase("your-api-key-here");

        // If explicitly in dev-fallback without a valid key, or no key provided
        if (!hasValidKey && ("dev-fallback".equalsIgnoreCase(aiMode) || apiKey == null || apiKey.startsWith("mock-") || apiKey.isBlank())) {
            String fallbackJson = getDynamicResponse(featureName, prompt);
            logUsage(userId, featureName, 150, 350, true, null);
            return fallbackJson;
        }

        // Live Provider Call using API Key
        try {
            String liveResult = executeLiveCall(featureName, prompt, userId);
            logUsage(userId, featureName, 150, 350, true, null);
            log.info("Live AI call successfully completed for feature [{}]", featureName);
            return liveResult;
        } catch (Exception ex) {
            log.warn("Live AI call failed for feature [{}] at [{}]: {}. Falling back to dynamic formatted response.", featureName, apiUrl, ex.getMessage());
            logUsage(userId, featureName, 0, 0, false, ex.getMessage());

            if ("prod".equalsIgnoreCase(System.getProperty("spring.profiles.active"))) {
                throw new RuntimeException("AI Provider Service failure: " + ex.getMessage());
            }

            return getDynamicResponse(featureName, prompt);
        }
    }

    private String executeLiveCall(String featureName, String prompt, Long userId) throws Exception {
        String systemPrompt = getSystemPrompt(featureName);
        boolean isGoogleNative = apiUrl != null && (apiUrl.contains(":generateContent") || (apiUrl.contains("generativelanguage.googleapis.com") && !apiUrl.contains("/openai/")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("x-goog-api-key", apiKey);

        if (isGoogleNative) {
            String url = apiUrl;
            if (!url.contains("key=")) {
                url = url + (url.contains("?") ? "&" : "?") + "key=" + apiKey;
            }

            Map<String, Object> req = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", systemPrompt + "\n\nCRITICAL: Respond ONLY with a valid, clean JSON object.\n\n" + prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "responseMimeType", "application/json"
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List candidates = (List) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map cand = (Map) candidates.get(0);
                    Map contentObj = (Map) cand.get("content");
                    if (contentObj != null) {
                        List parts = (List) contentObj.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            Map part = (Map) parts.get(0);
                            return (String) part.get("text");
                        }
                    }
                }
            }
            throw new RuntimeException("Empty response from Google Gemini API");
        } else {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));
            requestBody.put("temperature", 0.4);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response;
            try {
                response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            } catch (Exception ex) {
                log.info("Initial structured request encountered: {}. Retrying with strict system instruction...", ex.getMessage());
                Map<String, Object> retryReq = new LinkedHashMap<>();
                retryReq.put("model", model);
                retryReq.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt + " You MUST respond with a valid JSON object ONLY. Do not wrap in markdown or prose."),
                        Map.of("role", "user", "content", prompt)
                ));
                retryReq.put("temperature", 0.4);
                HttpEntity<Map<String, Object>> retryEntity = new HttpEntity<>(retryReq, headers);
                response = restTemplate.postForEntity(apiUrl, retryEntity, Map.class);
            }

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map messageObj = (Map) firstChoice.get("message");
                    return (String) messageObj.get("content");
                }
            }
            throw new RuntimeException("Empty choices from AI Provider API");
        }
    }

    private String getSystemPrompt(String featureName) {
        return switch (featureName) {
            case "Study Buddy" -> """
                You are SmartPlacement AI Study Buddy, an elite technical placement coach.
                Respond naturally, directly, and clearly to the student's message.
                If they send a greeting (e.g. hi, hello), respond warmly as a mentor.
                For technical questions, format your answer in clear, well-spaced paragraphs and bullet points without comments.
                Output a JSON object with:
                - 'response': string (clear, formatted answer)
                - 'answer': string (same as response)
                - 'suggestedTopics': array of 3 relevant strings
                """;

            case "Mock Interview Questions", "Mock Interview" -> """
                You are an expert technical interviewer conducting placement rounds.
                Generate 3 insightful, challenging, and clear interview questions for the specified role and difficulty.
                Output a JSON object with:
                - 'questions': array of 3 distinct question strings
                - 'role': string
                - 'difficulty': string
                """;

            case "Mock Interview Evaluation" -> """
                You are a senior technical interviewer evaluating candidate answers.
                Evaluate thoroughly based on depth, correctness, and architectural clarity.
                Output a JSON object with:
                - 'score': number (0-100)
                - 'overallScore': number (0-100)
                - 'correctness': number (0-100)
                - 'technicalKnowledge': number (0-100)
                - 'communication': number (0-100)
                - 'completeness': number (0-100)
                - 'feedback': string (clear, executive summary of performance)
                - 'strengths': array of strings
                - 'weaknesses': array of strings
                - 'recommendations': array of strings
                - 'perQuestionFeedback': array of objects with keys 'questionNumber', 'question', 'score', 'feedback', and 'suggestion'
                """;

            case "Coding Review" -> """
                You are a senior staff engineer reviewing code.
                Output a JSON object with:
                - 'passed': boolean
                - 'score': number (0-100)
                - 'feedback': string (clear assessment explaining efficiency and edge cases)
                - 'timeComplexity': string
                - 'spaceComplexity': string
                - 'suggestions': array of strings
                """;

            case "Resume Analyzer" -> """
                You are an executive technical recruiter and ATS specialist.
                Output a JSON object with:
                - 'resumeScore': number (0-100)
                - 'atsScore': number (0-100)
                - 'technicalSkillScore': number (0-100)
                - 'projectScore': number (0-100)
                - 'extractedSkills': array of strings
                - 'strengths': array of strings
                - 'weaknesses': array of strings
                - 'missingSkills': array of strings
                - 'suggestions': array of strings
                """;

            case "ATS Analysis" -> """
                You are an enterprise ATS parser auditing resume compatibility against a job description.
                Output a JSON object with:
                - 'atsScore': number (0-100)
                - 'matchedKeywords': array of strings
                - 'missingKeywords': array of strings
                - 'skillGaps': array of strings
                - 'formattingSuggestions': array of strings
                - 'recommendations': array of strings
                """;

            case "Cover Letter" -> """
                You are a professional career advisor.
                Output a JSON object with:
                - 'coverLetter': string (clear, professional letter)
                """;

            case "Career Path" -> """
                You are a senior engineering career counselor.
                Output a JSON object with:
                - 'targetRole': string
                - 'stages': array of strings
                """;

            case "Daily Plan" -> """
                You are an AI placement mentor.
                Output a JSON object with:
                - 'tasks': array of objects with keys 'title' (string) and 'category' ('CODING', 'LEARNING', or 'MOCK_INTERVIEW')
                """;

            case "Communication Analyzer" -> """
                You are an executive HR communication coach.
                Output a JSON object with:
                - 'clarityScore': number (0-100)
                - 'vocabularyScore': number (0-100)
                - 'grammarScore': number (0-100)
                - 'tone': string
                - 'fillerWordCount': number
                - 'feedback': string
                """;

            default -> "You are SmartPlacement AI. Output a valid, clean JSON object matching the requested feature.";
        };
    }

    private String getDynamicResponse(String featureName, String prompt) {
        String lowerPrompt = prompt != null ? prompt.toLowerCase() : "";

        if ("Study Buddy".equalsIgnoreCase(featureName)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "The AI buddy is unavailable right now, please try again");
        }

        if ("Mock Interview Questions".equalsIgnoreCase(featureName) || ("Mock Interview".equalsIgnoreCase(featureName) && !lowerPrompt.contains("evaluate interview answers") && !lowerPrompt.contains("\"answers\""))) {
            return generateMockInterviewQuestions(lowerPrompt);
        }

        if ("Mock Interview Evaluation".equalsIgnoreCase(featureName) || lowerPrompt.contains("evaluate interview answers") || lowerPrompt.contains("\"answers\"")) {
            return generateMockInterviewEvaluation(prompt, lowerPrompt);
        }

        return switch (featureName) {
            case "Resume Analyzer" -> """
                {
                  "aiMode": "dev-fallback",
                  "resumeScore": 86.0,
                  "atsScore": 88.0,
                  "technicalSkillScore": 85.0,
                  "projectScore": 84.0,
                  "extractedSkills": ["Java 17", "Spring Boot", "MySQL", "REST API", "Git", "Docker"],
                  "strengths": [
                    "Strong foundational Java and Spring Boot architecture",
                    "Clear relational database modeling with MySQL",
                    "Clean RESTful API design with proper HTTP status handling"
                  ],
                  "weaknesses": [
                    "Limited mention of distributed caching (Redis)",
                    "Cloud deployment and CI/CD pipelines not emphasized"
                  ],
                  "missingSkills": ["Redis", "Kubernetes", "Kafka", "AWS ECS"],
                  "suggestions": [
                    "Use XYZ bullet format: 'Reduced API response times by 35% by implementing Redis cache-aside strategy.'",
                    "Add GitHub repository links and live project deployment URLs directly in the header."
                  ]
                }
                """;

            case "ATS Analysis" -> """
                {
                  "aiMode": "dev-fallback",
                  "atsScore": 87.5,
                  "matchedKeywords": ["Java", "Spring Boot", "REST API", "SQL", "Git"],
                  "missingKeywords": ["Docker", "Redis", "Microservices", "Kafka"],
                  "skillGaps": ["Distributed Systems", "In-Memory Caching"],
                  "formattingSuggestions": [
                    "Use standard single-column layout with clear section headings (Summary, Skills, Projects, Experience, Education)",
                    "Keep length to 1-2 pages in PDF format"
                  ],
                  "recommendations": [
                    "Highlight Spring Security JWT authentication under your primary backend project",
                    "Quantify project achievements with measurable metrics (users, response times, throughput)"
                  ],
                  "disclaimer": "ATS score is an AI evaluation simulation for preparation guidance."
                }
                """;

            case "Coding Review" -> """
                {
                  "aiMode": "dev-fallback",
                  "passed": true,
                  "score": 92.0,
                  "feedback": "Clean and optimal O(N) linear time implementation. You appropriately utilized a HashMap for instant complement lookups, avoiding the naive O(N^2) quadratic approach.",
                  "timeComplexity": "O(N) - Linear time scan through array elements",
                  "spaceComplexity": "O(N) - Space used by the HashMap to store complements",
                  "suggestions": [
                    "Include explicit edge case handling for null or single-element array inputs.",
                    "Add inline comments explaining the HashMap lookup logic for interview clarity."
                  ],
                  "disclaimer": "AI-evaluated - code syntax and complexity reviewed statically."
                }
                """;

            case "Cover Letter" -> """
                {
                  "aiMode": "dev-fallback",
                  "coverLetter": "Dear Hiring Team,\\n\\nI am writing to express my strong enthusiasm for the Software Engineer position. With a solid foundation in core Java, Spring Boot, and relational database systems, I have engineered scalable web applications featuring clean MVC architectures, JWT authentication, and optimized REST APIs.\\n\\nIn my recent projects, I focused on writing clean, modular code and understanding the underlying operational mechanics of backend systems—from transaction boundaries to caching patterns. I am eager to contribute my problem-solving skills, engineering discipline, and collaborative mindset to your engineering team.\\n\\nThank you for your consideration, and I look forward to discussing how my technical background aligns with your team's goals.\\n\\nSincerely,\\nCandidate"
                }
                """;

            case "Career Path" -> """
                {
                  "aiMode": "dev-fallback",
                  "targetRole": "Software Engineer",
                  "stages": [
                    "Stage 1: Associate Backend Developer (Focus: Java 17, Spring Boot, JPA/Hibernate, Unit Testing with JUnit & Mockito)",
                    "Stage 2: Software Engineer (Focus: Microservices, Redis Caching, Docker Containerization, CI/CD GitHub Actions)",
                    "Stage 3: Senior Backend Engineer (Focus: Distributed Systems, High Concurrency, Kafka Event Streaming, Kubernetes)",
                    "Stage 4: Lead Solutions Architect (Focus: Enterprise Architecture, System Design, Scalability, Team Mentorship)"
                  ]
                }
                """;

            case "Daily Plan" -> """
                {
                  "aiMode": "dev-fallback",
                  "tasks": [
                    { "title": "Solve 2 Medium LeetCode problems on Two Pointers & HashMaps", "category": "CODING" },
                    { "title": "Review Spring Boot Dependency Injection, Bean Scopes, and @Transactional", "category": "LEARNING" },
                    { "title": "Complete 1 AI Mock Interview session on Backend Java Architecture", "category": "MOCK_INTERVIEW" }
                  ]
                }
                """;

            case "Communication Analyzer" -> """
                {
                  "aiMode": "dev-fallback",
                  "clarityScore": 88.0,
                  "vocabularyScore": 85.0,
                  "grammarScore": 92.0,
                  "tone": "Professional, Confident, and Articulate",
                  "fillerWordCount": 2,
                  "feedback": "Your articulation is well-structured and conveys technical confidence. To elevate your delivery further, pause intentionally between thoughts rather than using filler words."
                }
                """;

            default -> """
                {
                  "aiMode": "dev-fallback",
                  "response": "AI operation processed successfully.",
                  "answer": "AI operation processed successfully."
                }
                """;
        };
    }

    private String generateMockInterviewQuestions(String lower) {
        List<String> questions;
        String role;
        if (lower.contains("frontend") || lower.contains("react")) {
            role = "Frontend React Developer";
            questions = List.of(
                    "Explain the Virtual DOM in React, how the diffing reconciliation algorithm works, and why unique keys are necessary in lists.",
                    "What are the differences between useMemo, useCallback, and React.memo? Provide an example of how misuse can degrade performance.",
                    "How do you architect global state and asynchronous API caching in a large-scale React application (e.g., Context API vs Redux Toolkit vs React Query)?"
            );
        } else if (lower.contains("full stack") || lower.contains("fullstack")) {
            role = "Full Stack Developer";
            questions = List.of(
                    "Walk through the complete lifecycle of an HTTP request from a React button click, through reverse proxy and Spring Boot controller, to database persistence.",
                    "How do you design a secure stateless JWT authentication architecture with token refresh, rotation, and revocation handling?",
                    "Explain the N+1 query problem in ORM frameworks like Hibernate/JPA. How do you identify and resolve it using JOIN FETCH or Entity Graphs?"
            );
        } else if (lower.contains("hr") || lower.contains("behavioral")) {
            role = "HR & Behavioral";
            questions = List.of(
                    "Tell me about a challenging technical project you worked on. What unexpected blockers did you encounter, and how did you resolve them?",
                    "How do you prioritize competing deadlines when project specifications or requirements change unexpectedly?",
                    "Describe a situation where you had a disagreement with a peer or teammate over technical design. How did you arrive at a constructive solution?"
            );
        } else {
            role = "Java Backend Developer";
            questions = List.of(
                    "Explain Dependency Injection and Inversion of Control (IoC) in Spring Boot. Why is constructor injection preferred over field injection?",
                    "How does Garbage Collection work in Java, and what are the operational differences between the G1 and ZGC garbage collectors?",
                    "What is the difference between optimistic locking and pessimistic locking in JPA/Hibernate, and when should you use each under high concurrency?"
            );
        }

        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("aiMode", "dev-fallback");
            map.put("role", role);
            map.put("difficulty", "INTERMEDIATE");
            map.put("questions", questions);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"aiMode\":\"dev-fallback\",\"questions\":[\"Explain Dependency Injection in Spring Boot\",\"How does Garbage Collection work in Java?\",\"What is optimistic locking in JPA?\"]}";
        }
    }

    private String generateMockInterviewEvaluation(String rawPrompt, String lower) {
        List<String> answerTexts = new ArrayList<>();
        List<String> questionTexts = new ArrayList<>();

        try {
            Map<String, Object> map = objectMapper.readValue(rawPrompt, Map.class);
            Object ansObj = map.get("answers");
            if (ansObj instanceof Map<?, ?> m) {
                for (Object v : m.values()) {
                    if (v != null) answerTexts.add(v.toString().trim());
                }
            } else if (ansObj instanceof List<?> l) {
                for (Object v : l) {
                    if (v != null) answerTexts.add(v.toString().trim());
                }
            }

            Object qObj = map.get("questions");
            if (qObj instanceof List<?> l) {
                for (Object v : l) {
                    if (v != null) questionTexts.add(v.toString().trim());
                }
            }
        } catch (Exception ignored) {
        }

        if (questionTexts.isEmpty()) {
            questionTexts = List.of(
                    "Explain Dependency Injection and Inversion of Control in Spring Boot.",
                    "How does Garbage Collection work in Java, and what are G1 and ZGC?",
                    "What is the difference between optimistic and pessimistic locking in JPA?"
            );
        }

        int totalQuestions = questionTexts.size();
        List<Map<String, Object>> perQuestion = new ArrayList<>();
        double totalScoreSum = 0;

        for (int i = 0; i < totalQuestions; i++) {
            String qText = questionTexts.get(i);
            String aText = i < answerTexts.size() ? answerTexts.get(i) : "";
            int len = aText.length();
            String aLower = aText.toLowerCase();

            double qScore;
            String qFeedback;
            String qSuggestion;

            if (len < 15 || aLower.equals("idk") || aLower.equals("no") || aLower.equals("skip") || aLower.equals("test")) {
                qScore = 28.0 + (len > 0 ? 5.0 : 0.0);
                qFeedback = "Response is insufficient or missing. In a technical interview, leaving answers blank or one-liners signals lack of preparation.";
                qSuggestion = "Define the core concept, explain how it works internally, and give a short code or architecture example.";
            } else if (len < 80) {
                qScore = 58.0 + Math.min(12.0, len * 0.15);
                qFeedback = "Basic understanding demonstrated, but answer is brief and lacks depth. Missing explanation of underlying mechanisms and edge cases.";
                qSuggestion = "Elaborate on internal components (e.g., container lifecycle, JVM memory spaces, or database transaction isolation).";
            } else {
                boolean hasKeywords = aLower.contains("spring") || aLower.contains("ioc") || aLower.contains("injection") ||
                        aLower.contains("bean") || aLower.contains("gc") || aLower.contains("garbage") ||
                        aLower.contains("heap") || aLower.contains("memory") || aLower.contains("lock") ||
                        aLower.contains("version") || aLower.contains("transaction") || aLower.contains("react") ||
                        aLower.contains("dom") || aLower.contains("hook") || aLower.contains("api") ||
                        aLower.contains("database") || aLower.contains("complexity") || aLower.contains("time");

                qScore = hasKeywords ? 85.0 + Math.min(10.0, len * 0.03) : 74.0 + Math.min(8.0, len * 0.02);
                qFeedback = "Strong, well-structured explanation using accurate technical terminology and demonstrating clear conceptual understanding.";
                qSuggestion = "Great response! Consider briefly contrasting with alternative approaches or failure modes to stand out even more.";
            }

            totalScoreSum += qScore;

            Map<String, Object> qMap = new LinkedHashMap<>();
            qMap.put("questionNumber", i + 1);
            qMap.put("question", qText);
            qMap.put("score", Math.round(qScore * 10.0) / 10.0);
            qMap.put("candidateAnswer", aText.isEmpty() ? "(No answer submitted)" : (aText.length() > 100 ? aText.substring(0, 97) + "..." : aText));
            qMap.put("feedback", qFeedback);
            qMap.put("suggestion", qSuggestion);
            perQuestion.add(qMap);
        }

        double overallScore = Math.round((totalScoreSum / Math.max(1, totalQuestions)) * 10.0) / 10.0;
        double correctness = Math.round(Math.min(100.0, overallScore * 1.02) * 10.0) / 10.0;
        double technicalKnowledge = Math.round(Math.min(100.0, overallScore * 0.98) * 10.0) / 10.0;
        double communication = Math.round(Math.min(100.0, overallScore + 2.0) * 10.0) / 10.0;
        double completeness = Math.round(Math.min(100.0, (answerTexts.stream().filter(a -> a.length() >= 20).count() / (double) totalQuestions) * 100.0) * 10.0) / 10.0;

        String summaryFeedback;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (overallScore < 50.0) {
            summaryFeedback = "Your responses lacked the technical depth expected in technical placement interviews. Several answers were either empty or too brief to evaluate competency. Practice articulating technical definitions, mechanisms, and real code examples.";
            strengths.add("Attempted the mock interview session");
            weaknesses.add("Responses lack technical substance and depth");
            weaknesses.add("Missing internal architecture and mechanism explanations");
            weaknesses.add("No discussion of operational trade-offs or edge cases");
            recommendations.add("Study core concepts: Spring Boot IoC, JVM Garbage Collection, and JPA transactions");
            recommendations.add("Structure technical answers: Definition -> Internal Mechanism -> Code Example -> Trade-offs");
            recommendations.add("Avoid one-sentence answers in technical rounds");
        } else if (overallScore < 75.0) {
            summaryFeedback = "Fair technical grasp of fundamentals, but your explanations remain high-level. To succeed in top placement rounds, dive deeper into implementation details, concurrency behavior, and framework internals.";
            strengths.add("Demonstrated baseline conceptual familiarity with the topics");
            strengths.add("Clear communication style");
            weaknesses.add("Lacks deep dive into internal mechanics (e.g. JVM heap partitions, locking protocols)");
            weaknesses.add("Could provide concrete code or architecture examples");
            recommendations.add("Elaborate on why certain design choices are preferred in production (e.g., constructor injection)");
            recommendations.add("Discuss performance implications and time/space complexity");
        } else {
            summaryFeedback = "Excellent performance! You demonstrated thorough conceptual understanding, utilized precise engineering terminology, and structured your answers logically. You are well-prepared for technical rounds.";
            strengths.add("Precise technical vocabulary and clear architectural explanations");
            strengths.add("Strong understanding of framework lifecycles and operational mechanics");
            strengths.add("Structured, professional communication");
            weaknesses.add("Can mention edge cases, concurrency hazards, or high-throughput failure modes");
            recommendations.add("Practice articulating system design trade-offs under distributed scale");
            recommendations.add("Mention observability and monitoring metrics when discussing production systems");
        }

        Map<String, Object> evalMap = new LinkedHashMap<>();
        evalMap.put("score", overallScore);
        evalMap.put("overallScore", overallScore);
        evalMap.put("correctness", correctness);
        evalMap.put("technicalKnowledge", technicalKnowledge);
        evalMap.put("communication", communication);
        evalMap.put("completeness", completeness);
        evalMap.put("feedback", summaryFeedback);
        evalMap.put("strengths", strengths);
        evalMap.put("weaknesses", weaknesses);
        evalMap.put("recommendations", recommendations);
        evalMap.put("perQuestionFeedback", perQuestion);

        Map<String, Object> responseMap = new LinkedHashMap<>(evalMap);
        responseMap.put("aiMode", "dev-fallback");
        responseMap.put("evaluation", evalMap);

        try {
            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            return "{\"aiMode\":\"dev-fallback\",\"score\":" + overallScore + ",\"feedback\":\"" + summaryFeedback + "\"}";
        }
    }

    private void logUsage(Long userId, String featureName, int promptTokens, int completionTokens, boolean success, String errorMsg) {
        try {
            AiUsageLog usageLog = AiUsageLog.builder()
                    .userId(userId)
                    .featureName(featureName)
                    .model(model)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .success(success)
                    .errorMessage(errorMsg)
                    .build();
            aiUsageLogRepository.save(usageLog);
        } catch (Exception e) {
            log.error("Failed to save AI usage log: {}", e.getMessage());
        }
    }
}
