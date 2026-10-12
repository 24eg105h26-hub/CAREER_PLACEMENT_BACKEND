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

@Slf4j
@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final StudentRepository studentRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationRepository notificationRepository;

    private Faculty getFacultyOrThrow(Long userId) {
        return facultyRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty profile not found"));
    }

    private void verifyFacultyAssignment(Long facultyId, Long studentId) {
        boolean assigned = facultyAssignmentRepository.existsByFacultyIdAndStudentId(facultyId, studentId);
        if (!assigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Student is not assigned to this faculty");
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
        Faculty faculty = getFacultyOrThrow(userId);
        List<FacultyAssignment> assignments = facultyAssignmentRepository.findByFacultyId(faculty.getId());
        List<Student> students = assignments.stream().map(FacultyAssignment::getStudent).toList();

        int totalCount = students.size();
        double avgCgpa = students.isEmpty() ? 0.0 :
                students.stream().mapToDouble(s -> s.getCgpa() != null ? s.getCgpa() : 0.0).average().orElse(0.0);
        avgCgpa = Math.round(avgCgpa * 100.0) / 100.0;

        double avgReadiness = students.isEmpty() ? 0.0 :
                students.stream().mapToDouble(this::calculateStudentReadiness).average().orElse(0.0);
        avgReadiness = Math.round(avgReadiness * 10.0) / 10.0;

        long atRiskCount = students.stream()
                .filter(s -> (s.getCgpa() != null && s.getCgpa() < 7.0) || calculateStudentReadiness(s) < 60.0)
                .count();

        List<Assessment> assessments = assessmentRepository.findAllByOrderByCreatedAtDesc();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("facultyName", faculty.getUser().getFullName());
        dashboard.put("department", faculty.getDepartment() != null ? faculty.getDepartment().getName() : "Computer Science");
        dashboard.put("designation", faculty.getDesignation());
        dashboard.put("assignedStudentsCount", totalCount);
        dashboard.put("averageCgpa", avgCgpa);
        dashboard.put("averageReadiness", avgReadiness);
        dashboard.put("atRiskCount", atRiskCount);
        dashboard.put("totalAssessments", assessments.size());
        return dashboard;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssignedStudents(Long userId) {
        Faculty faculty = getFacultyOrThrow(userId);
        List<FacultyAssignment> assignments = facultyAssignmentRepository.findByFacultyId(faculty.getId());

        return assignments.stream().map(a -> {
            Student s = a.getStudent();
            double readiness = calculateStudentReadiness(s);
            String riskLevel = readiness >= 75.0 ? "LOW" : readiness >= 55.0 ? "MEDIUM" : "HIGH";

            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getUser().getFullName());
            map.put("email", s.getUser().getEmail());
            map.put("rollNumber", s.getRollNumber());
            map.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "General");
            map.put("departmentCode", s.getDepartment() != null ? s.getDepartment().getCode() : "GEN");
            map.put("cgpa", s.getCgpa());
            map.put("readiness", readiness);
            map.put("riskLevel", riskLevel);
            map.put("skillsCount", studentSkillRepository.findByStudentId(s.getId()).size());
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentDetail(Long userId, Long studentId) {
        Faculty faculty = getFacultyOrThrow(userId);
        verifyFacultyAssignment(faculty.getId(), studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        double readiness = calculateStudentReadiness(student);
        String riskLevel = readiness >= 75.0 ? "LOW" : readiness >= 55.0 ? "MEDIUM" : "HIGH";

        // Skills
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

        // Assessment results
        List<AssessmentResult> results = assessmentResultRepository.findByStudentIdOrderByCompletedAtDesc(studentId);
        List<Map<String, Object>> resultsList = results.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getAssessment().getTitle());
            m.put("topic", r.getAssessment().getTopic());
            m.put("scoreObtained", r.getScore());
            m.put("totalMarks", r.getTotalMarks());
            m.put("percentageScore", r.getPercentage());
            m.put("passed", "COMPLETED".equalsIgnoreCase(r.getStatus()));
            return m;
        }).toList();

        // Feedback
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
        detail.put("rollNumber", student.getRollNumber());
        detail.put("department", student.getDepartment() != null ? student.getDepartment().getName() : "General");
        detail.put("cgpa", student.getCgpa());
        detail.put("readiness", readiness);
        detail.put("riskLevel", riskLevel);
        detail.put("skills", skillsList);
        detail.put("assessmentResults", resultsList);
        detail.put("feedbackHistory", feedbackList);
        return detail;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEarlyWarnings(Long userId) {
        Faculty faculty = getFacultyOrThrow(userId);
        List<FacultyAssignment> assignments = facultyAssignmentRepository.findByFacultyId(faculty.getId());

        List<Map<String, Object>> warnings = new ArrayList<>();
        for (FacultyAssignment a : assignments) {
            Student s = a.getStudent();
            double readiness = calculateStudentReadiness(s);
            Double cgpa = s.getCgpa() != null ? s.getCgpa() : 0.0;

            if (cgpa < 7.0 || readiness < 60.0) {
                String reason;
                String risk;
                if (cgpa < 6.0) {
                    reason = "Critical CGPA (" + cgpa + ") below minimum on-campus drive cutoff (6.0)";
                    risk = "CRITICAL";
                } else if (cgpa < 7.0) {
                    reason = "CGPA (" + cgpa + ") below tier-1 criteria; requires academic revision";
                    risk = "HIGH";
                } else {
                    reason = "Readiness score (" + readiness + "%) below placement threshold; lacking core skills";
                    risk = "MEDIUM";
                }

                Map<String, Object> m = new HashMap<>();
                m.put("studentId", s.getId());
                m.put("name", s.getUser().getFullName());
                m.put("rollNumber", s.getRollNumber());
                m.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "General");
                m.put("cgpa", cgpa);
                m.put("readiness", readiness);
                m.put("reason", reason);
                m.put("riskLevel", risk);
                warnings.add(m);
            }
        }
        return warnings;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssessments(Long userId) {
        List<Assessment> list = assessmentRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(a -> {
            List<AssessmentResult> results = assessmentResultRepository.findByAssessmentIdOrderByCompletedAtDesc(a.getId());
            int taken = results.size();
            double avg = results.isEmpty() ? 0.0 :
                    results.stream().mapToDouble(r -> r.getPercentage() != null ? r.getPercentage() : 0.0).average().orElse(0.0);
            avg = Math.round(avg * 10.0) / 10.0;

            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("description", a.getDescription());
            m.put("topic", a.getTopic());
            m.put("durationMinutes", a.getDurationMinutes());
            m.put("totalMarks", a.getTotalMarks());
            m.put("studentsTaken", taken);
            m.put("avgScore", avg);
            m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createAssessment(Long userId, Map<String, Object> req) {
        String title = (String) req.get("title");
        String topic = (String) req.getOrDefault("topic", "General");
        String description = (String) req.getOrDefault("description", "");
        Integer duration = req.get("durationMinutes") != null ? Integer.valueOf(req.get("durationMinutes").toString()) : 30;
        Integer marks = req.get("totalMarks") != null ? Integer.valueOf(req.get("totalMarks").toString()) : 100;

        Assessment assessment = assessmentRepository.save(Assessment.builder()
                .title(title)
                .topic(topic)
                .description(description)
                .durationMinutes(duration)
                .totalMarks(marks)
                .createdByUserId(userId)
                .build());

        Map<String, Object> m = new HashMap<>();
        m.put("id", assessment.getId());
        m.put("title", assessment.getTitle());
        m.put("topic", assessment.getTopic());
        m.put("description", assessment.getDescription());
        m.put("durationMinutes", assessment.getDurationMinutes());
        m.put("totalMarks", assessment.getTotalMarks());
        m.put("studentsTaken", 0);
        m.put("avgScore", 0.0);
        return m;
    }

    @Transactional
    public Map<String, Object> createFeedback(Long userId, Map<String, Object> req) {
        Faculty faculty = getFacultyOrThrow(userId);
        Long studentId = Long.valueOf(req.get("studentId").toString());
        verifyFacultyAssignment(faculty.getId(), studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        String comment = (String) req.get("comment");
        String category = (String) req.getOrDefault("category", "ACADEMIC");

        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .student(student)
                .authorUser(faculty.getUser())
                .comment(comment)
                .category(category)
                .build());

        // Notify student
        notificationRepository.save(Notification.builder()
                .user(student.getUser())
                .title("New Faculty Academic Advisory")
                .message(faculty.getUser().getFullName() + " shared feedback: \"" + (comment.length() > 60 ? comment.substring(0, 60) + "..." : comment) + "\"")
                .type("ACADEMIC")
                .link("/student/profile")
                .build());

        Map<String, Object> res = new HashMap<>();
        res.put("id", feedback.getId());
        res.put("studentId", student.getId());
        res.put("studentName", student.getUser().getFullName());
        res.put("comment", feedback.getComment());
        res.put("category", feedback.getCategory());
        return res;
    }
}