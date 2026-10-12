package com.smartplacement.ai.util;

import com.smartplacement.entity.JobSkill;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PlacementMathEngine {

    @Data
    @Builder
    public static class MatchResult {
        private double score;
        private List<String> matchedSkills;
        private List<String> missingSkills;
        private Map<String, Object> details;
    }

    @Data
    @Builder
    public static class ReadinessBreakdown {
        private Double overallScore;
        private Double technicalScore;
        private Double resumeScore;
        private Double codingScore;
        private Double interviewScore;
        private Map<String, Boolean> hasEnoughData;
        private String disclaimer;
    }

    /**
     * Calculates deterministic job match score based on weighted skill coverage.
     * Required skills weight = 3, Preferred skills weight = 1.
     */
    public MatchResult calculateJobMatch(Set<String> studentSkillNames, List<JobSkill> jobSkills) {
        if (jobSkills == null || jobSkills.isEmpty()) {
            return MatchResult.builder()
                    .score(100.0)
                    .matchedSkills(Collections.emptyList())
                    .missingSkills(Collections.emptyList())
                    .details(Map.of("info", "Job has no specified skills"))
                    .build();
        }

        Set<String> lowerStudentSkills = studentSkillNames.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        double totalWeight = 0.0;
        double matchedWeight = 0.0;
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (JobSkill js : jobSkills) {
            String skillName = js.getSkill().getName();
            int weight = "REQUIRED".equalsIgnoreCase(js.getSkillType()) ? 3 : 1;
            totalWeight += weight;

            if (lowerStudentSkills.contains(skillName.toLowerCase())) {
                matchedWeight += weight;
                matched.add(skillName);
            } else {
                missing.add(skillName);
            }
        }

        double score = totalWeight > 0 ? (matchedWeight / totalWeight) * 100.0 : 0.0;
        // Round to 1 decimal place
        score = Math.round(score * 10.0) / 10.0;

        return MatchResult.builder()
                .score(score)
                .matchedSkills(matched)
                .missingSkills(missing)
                .details(Map.of("totalWeight", totalWeight, "matchedWeight", matchedWeight))
                .build();
    }

    /**
     * Calculates Placement Readiness Score from available components.
     * Weights: Technical (35%), Resume (25%), Coding (20%), Interview (20%).
     */
    public ReadinessBreakdown calculateReadiness(Double technicalScore, Double resumeScore, Double codingScore, Double interviewScore) {
        Map<String, Boolean> dataFlags = new HashMap<>();
        dataFlags.put("technical", technicalScore != null);
        dataFlags.put("resume", resumeScore != null);
        dataFlags.put("coding", codingScore != null);
        dataFlags.put("interview", interviewScore != null);

        double totalWeightSum = 0.0;
        double weightedScoreSum = 0.0;

        if (technicalScore != null) {
            weightedScoreSum += technicalScore * 0.35;
            totalWeightSum += 0.35;
        }
        if (resumeScore != null) {
            weightedScoreSum += resumeScore * 0.25;
            totalWeightSum += 0.25;
        }
        if (codingScore != null) {
            weightedScoreSum += codingScore * 0.20;
            totalWeightSum += 0.20;
        }
        if (interviewScore != null) {
            weightedScoreSum += interviewScore * 0.20;
            totalWeightSum += 0.20;
        }

        Double overall = totalWeightSum > 0 ? (weightedScoreSum / totalWeightSum) : null;
        if (overall != null) {
            overall = Math.round(overall * 10.0) / 10.0;
        }

        return ReadinessBreakdown.builder()
                .overallScore(overall)
                .technicalScore(technicalScore)
                .resumeScore(resumeScore)
                .codingScore(codingScore)
                .interviewScore(interviewScore)
                .hasEnoughData(dataFlags)
                .disclaimer("Estimate derived algorithmically from stored student performance and resume data.")
                .build();
    }
}
