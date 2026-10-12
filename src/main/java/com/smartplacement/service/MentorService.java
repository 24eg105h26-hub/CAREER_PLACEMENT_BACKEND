package com.smartplacement.service;

import com.smartplacement.entity.*;
import com.smartplacement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorService {

    private final MentorRepository mentorRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final StudentRepository studentRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final ApplicationRepository applicationRepository;
    private final FeedbackRepository feedbackRepository;
    private final ActivityLogRepository activityLogRepository;
    private final LearningRoadmapRepository learningRoadmapRepository;
    private final LearningRoadmapItemRepository learningRoadmapItemRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final NotificationRepository notificationRepository;

    private Mentor getMentorOrThrow(Long userId) {
        return mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor profile not found"));
    }

    private void verifyMentorAssignment(Long mentorId, Long studentId) {
        boolean assigned = mentorAssignmentRepository.existsByMentorIdAndStudentId(mentorId, studentId);
        if (!assigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Student is not assigned to this mentor");
        }
    }

    private double calculateStudentReadiness(Student student) {
        double cgpaScore = Math.min(100.0, (student.getCgpa() != null ? student.getCgpa() : 6.0) * 10.0);
        long skillCount = studentSkillRepository.findByStudentId(student.getId()).size();
        double skillScore = Math.min(100.0, skillCount * 20.0);
        List<AssessmentResult> assessments = assessmentResultRepository.findByStudentIdOrderByCompletedAtDesc(student.getId());
        double assessmentScore = assessments.isEmpty() ? 65.0 :
                assessments.stream().mapToDouble(a -> a.getPercentage() != null ? a.getPercentage() : 60.0).average().orElse(65.0);

        return Math.round((cgpaScore * 0.35 + skillScore * 0.35 + assessmentScore * 0.30) * 10.0) / 10.0;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(Long userId) {
        Mentor mentor = getMentorOrThrow(userId);
        List<MentorAssignment> assignments = mentorAssignmentRepository.findByMentorId(mentor.getId());
        List<Student> students = assignments.stream().map(MentorAssignment::getStudent).toList();

        int totalCount = students.size();
        double avgReadiness = students.isEmpty() ? 0.0 :
                students.stream().mapToDouble(this::calculateStudentReadiness).average().orElse(0.0);
        avgReadiness = Math.round(avgReadiness * 10.0) / 10.0;

        long needingAttentionCount = students.stream()
                .filter(s -> calculateStudentReadiness(s) < 65.0 || (s.getCgpa() != null && s.getCgpa() < 7.0))
                .count();

        List<Feedback> feedbackList = feedbackRepository.findByAuthorUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> recentFeedback = feedbackList.stream().limit(5).map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("studentId", f.getStudent().getId());
            map.put("studentName", f.getStudent().getUser().getFullName());
            map.put("comment", f.getComment());
            map.put("category", f.getCategory());
            map.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : LocalDateTime.now().toString());
            return map;
        }).toList();

        List<ActivityLog> activities = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> upcomingSessions = activities.stream().limit(5).map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("action", a.getAction());
            map.put("entityType", a.getEntityType());
            map.put("entityId", a.getEntityId());
            map.put("details", a.getDetails());
            map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : LocalDateTime.now().toString());
            return map;
        }).toList();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("mentorName", mentor.getUser().getFullName());
        dashboard.put("companyName", mentor.getCompanyName());
        dashboard.put("designation", mentor.getDesignation());
        dashboard.put("assignedStudentsCount", totalCount);
        dashboard.put("averageReadiness", avgReadiness);
        dashboard.put("studentsNeedingAttentionCount", needingAttentionCount);
        dashboard.put("recentFeedback", recentFeedback);
        dashboard.put("upcomingSessions", upcomingSessions);
        return dashboard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssignedMentees(Long userId) {
        Mentor mentor = getMentorOrThrow(userId);
        List<MentorAssignment> assignments = mentorAssignmentRepository.findByMentorId(mentor.getId());

        return assignments.stream().map(a -> {
            Student s = a.getStudent();
            double readiness = calculateStudentReadiness(s);
            String riskBadge = readiness >= 75.0 ? "LOW" : readiness >= 55.0 ? "MEDIUM" : "HIGH";

            Optional<LearningRoadmap> roadmapOpt = learningRoadmapRepository.findFirstByStudentIdOrderByCreatedAtDesc(s.getId());
            String roadmapStatus = "Week 1: Fundamentals";
            if (roadmapOpt.isPresent()) {
                List<LearningRoadmapItem> items = learningRoadmapItemRepository.findByRoadmapIdOrderByWeekNumberAsc(roadmapOpt.get().getId());
                long completed = items.stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count();
                roadmapStatus = completed + " / " + items.size() + " Milestones Completed";
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getUser().getFullName());
            map.put("email", s.getUser().getEmail());
            map.put("rollNumber", s.getRollNumber());
            map.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "General");
            map.put("departmentCode", s.getDepartment() != null ? s.getDepartment().getCode() : "GEN");
            map.put("cgpa", s.getCgpa());
            map.put("readiness", readiness);
            map.put("riskBadge", riskBadge);
            map.put("roadmapStatus", roadmapStatus);
            map.put("skillsCount", studentSkillRepository.findByStudentId(s.getId()).size());
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentDetail(Long userId, Long studentId) {
        Mentor mentor = getMentorOrThrow(userId);
        verifyMentorAssignment(mentor.getId(), studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        double readiness = calculateStudentReadiness(student);
        String riskBadge = readiness >= 75.0 ? "LOW" : readiness >= 55.0 ? "MEDIUM" : "HIGH";

        // Skills & gaps
        List<StudentSkill> skills = studentSkillRepository.findByStudentId(studentId);
        List<Map<String, Object>> skillsList = skills.stream().map(ss -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ss.getId());
            m.put("name", ss.getSkill().getName());
            m.put("category", ss.getSkill().getCategory());
            m.put("proficiency", ss.getProficiencyLevel());
            m.put("verified", ss.getVerified());
            return m;
        }).toList();

        // Roadmap
        Optional<LearningRoadmap> roadmapOpt = learningRoadmapRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId);
        List<Map<String, Object>> roadmapItems = Collections.emptyList();
        String targetRole = "Software Engineer";
        if (roadmapOpt.isPresent()) {
            targetRole = roadmapOpt.get().getTargetRole();
            roadmapItems = learningRoadmapItemRepository.findByRoadmapIdOrderByWeekNumberAsc(roadmapOpt.get().getId()).stream().map(i -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", i.getId());
                m.put("weekNumber", i.getWeekNumber());
                m.put("topic", i.getTopic());
                m.put("description", i.getDescription());
                m.put("priority", i.getPriority());
                m.put("status", i.getStatus());
                return m;
            }).toList();
        }

        // Applications
        List<Application> apps = applicationRepository.findByStudentId(studentId);
        List<Map<String, Object>> applicationsList = apps.stream().map(app -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", app.getId());
            m.put("jobTitle", app.getJob().getTitle());
            m.put("companyName", app.getJob().getCompany().getName());
            m.put("status", app.getStatus());
            m.put("matchScore", app.getMatchScore());
            m.put("appliedAt", app.getAppliedAt() != null ? app.getAppliedAt().toString() : "");
            return m;
        }).toList();

        // Assessments
        List<AssessmentResult> assessmentResults = assessmentResultRepository.findByStudentIdOrderByCompletedAtDesc(studentId);
        List<Map<String, Object>> scoresList = assessmentResults.stream().map(ar -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ar.getId());
            m.put("assessmentTitle", ar.getAssessment().getTitle());
            m.put("topic", ar.getAssessment().getTopic());
            m.put("scoreObtained", ar.getScore());
            m.put("totalMarks", ar.getTotalMarks());
            m.put("percentageScore", ar.getPercentage());
            m.put("passed", "COMPLETED".equalsIgnoreCase(ar.getStatus()));
            return m;
        }).toList();

        // Feedback history
        List<Feedback> feedbacks = feedbackRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<Map<String, Object>> feedbackList = feedbacks.stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("authorName", f.getAuthorUser().getFullName());
            m.put("comment", f.getComment());
            m.put("category", f.getCategory());
            m.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", student.getId());
        detail.put("name", student.getUser().getFullName());
        detail.put("email", student.getUser().getEmail());
        detail.put("phone", student.getUser().getPhone());
        detail.put("rollNumber", student.getRollNumber());
        detail.put("department", student.getDepartment() != null ? student.getDepartment().getName() : "General");
        detail.put("cgpa", student.getCgpa());
        detail.put("graduationYear", student.getGraduationYear());
        detail.put("bio", student.getBio());
        detail.put("githubUrl", student.getGithubUrl());
        detail.put("linkedinUrl", student.getLinkedinUrl());
        detail.put("readiness", readiness);
        detail.put("riskBadge", riskBadge);
        detail.put("targetRole", targetRole);
        detail.put("skills", skillsList);
        detail.put("roadmapItems", roadmapItems);
        detail.put("applications", applicationsList);
        detail.put("assessmentScores", scoresList);
        detail.put("feedbackHistory", feedbackList);

        return detail;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLearningProgress(Long userId) {
        Mentor mentor = getMentorOrThrow(userId);
        List<MentorAssignment> assignments = mentorAssignmentRepository.findByMentorId(mentor.getId());

        return assignments.stream().map(a -> {
            Student s = a.getStudent();
            Optional<LearningRoadmap> roadmapOpt = learningRoadmapRepository.findFirstByStudentIdOrderByCreatedAtDesc(s.getId());
            int completed = 0;
            int total = 4;
            if (roadmapOpt.isPresent()) {
                List<LearningRoadmapItem> items = learningRoadmapItemRepository.findByRoadmapIdOrderByWeekNumberAsc(roadmapOpt.get().getId());
                total = items.size();
                completed = (int) items.stream().filter(i -> "COMPLETED".equalsIgnoreCase(i.getStatus())).count();
            }

            double readiness = calculateStudentReadiness(s);
            String status = readiness >= 75.0 ? "ON_TRACK" : readiness >= 50.0 ? "NEEDS_ATTENTION" : "AT_RISK";

            Map<String, Object> m = new HashMap<>();
            m.put("menteeId", s.getId());
            m.put("name", s.getUser().getFullName());
            m.put("completedMilestones", completed);
            m.put("totalMilestones", total);
            m.put("status", status);
            m.put("readiness", readiness);
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFeedback(Long userId) {
        List<Feedback> feedbackList = feedbackRepository.findByAuthorUserIdOrderByCreatedAtDesc(userId);
        return feedbackList.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("studentId", f.getStudent().getId());
            map.put("studentName", f.getStudent().getUser().getFullName());
            map.put("comment", f.getComment());
            map.put("category", f.getCategory());
            map.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : LocalDateTime.now().toString());
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createFeedback(Long userId, Map<String, Object> req) {
        Mentor mentor = getMentorOrThrow(userId);
        Long studentId = Long.valueOf(req.get("studentId").toString());
        verifyMentorAssignment(mentor.getId(), studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        String comment = (String) req.get("comment");
        String category = (String) req.getOrDefault("category", "MENTOR_REVIEW");

        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .student(student)
                .authorUser(mentor.getUser())
                .comment(comment)
                .category(category)
                .build());

        // Create notification for student
        notificationRepository.save(Notification.builder()
                .user(student.getUser())
                .title("New Mentor Feedback")
                .message(mentor.getUser().getFullName() + " added new feedback: \"" + (comment.length() > 60 ? comment.substring(0, 60) + "..." : comment) + "\"")
                .type("MENTOR")
                .link("/student/profile")
                .build());

        Map<String, Object> res = new HashMap<>();
        res.put("id", feedback.getId());
        res.put("studentId", student.getId());
        res.put("studentName", student.getUser().getFullName());
        res.put("comment", feedback.getComment());
        res.put("category", feedback.getCategory());
        res.put("createdAt", feedback.getCreatedAt() != null ? feedback.getCreatedAt().toString() : LocalDateTime.now().toString());
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivities(Long userId) {
        List<ActivityLog> logs = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return logs.stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", l.getId());
            m.put("action", l.getAction());
            m.put("entityType", l.getEntityType());
            m.put("entityId", l.getEntityId());
            m.put("details", l.getDetails());
            m.put("createdAt", l.getCreatedAt() != null ? l.getCreatedAt().toString() : LocalDateTime.now().toString());
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createActivity(Long userId, Map<String, Object> req) {
        String action = (String) req.getOrDefault("action", "MENTORING_SESSION");
        String details = (String) req.getOrDefault("details", "");
        Long entityId = req.get("entityId") != null ? Long.valueOf(req.get("entityId").toString()) : null;

        ActivityLog log = activityLogRepository.save(ActivityLog.builder()
                .userId(userId)
                .action(action)
                .entityType("STUDENT")
                .entityId(entityId)
                .details(details)
                .build());

        Map<String, Object> m = new HashMap<>();
        m.put("id", log.getId());
        m.put("action", log.getAction());
        m.put("entityId", log.getEntityId());
        m.put("details", log.getDetails());
        m.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : LocalDateTime.now().toString());
        return m;
    }

    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        ActivityLog log = activityLogRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity log not found"));
        if (!userId.equals(log.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete activity belonging to another user");
        }
        activityLogRepository.delete(log);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAiRecommendations(Long userId) {
        Mentor mentor = getMentorOrThrow(userId);
        List<MentorAssignment> assignments = mentorAssignmentRepository.findByMentorId(mentor.getId());

        List<Map<String, Object>> recs = new ArrayList<>();
        for (MentorAssignment a : assignments) {
            Student s = a.getStudent();
            double readiness = calculateStudentReadiness(s);
            String tip;
            if (readiness < 60.0) {
                tip = "Target foundational DSA (linked lists & binary search) and core OOP principles before scheduling mock interviews.";
            } else if (readiness < 80.0) {
                tip = "Encourage building an end-to-end full-stack portfolio project with Redis caching and Docker deployment.";
            } else {
                tip = "Excellent readiness. Focus on high-concurrency system design scenarios and behavioral leadership stories.";
            }
            Map<String, Object> m = new HashMap<>();
            m.put("studentId", s.getId());
            m.put("studentName", s.getUser().getFullName());
            m.put("readiness", readiness);
            m.put("recommendation", tip);
            recs.add(m);
        }
        return recs;
    }
}