package com.smartplacement.service;

import com.smartplacement.entity.*;
import com.smartplacement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final MentorRepository mentorRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        long totalUsers = userRepository.count();
        long totalStudents = studentRepository.count();
        long totalFaculty = facultyRepository.count();
        long totalMentors = mentorRepository.count();
        long totalCompanies = companyRepository.count();
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();
        long pendingApprovals = userRepository.findAll().stream()
                .filter(u -> u.getApprovalStatus() == ApprovalStatus.PENDING)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("totalFaculty", totalFaculty);
        stats.put("totalMentors", totalMentors);
        stats.put("totalCompanies", totalCompanies);
        stats.put("totalJobs", totalJobs);
        stats.put("totalApplications", totalApplications);
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("systemHealth", "OPTIMAL");
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUsers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("email", u.getEmail());
            m.put("fullName", u.getFullName());
            m.put("phone", u.getPhone());
            m.put("role", u.getRole() != null ? u.getRole().name() : "STUDENT");
            m.put("approvalStatus", u.getApprovalStatus() != null ? u.getApprovalStatus().name() : "APPROVED");
            m.put("enabled", u.getEnabled());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createUser(Map<String, Object> req) {
        String email = (String) req.get("email");
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        String fullName = (String) req.getOrDefault("fullName", "New User");
        String password = (String) req.getOrDefault("password", "Password123!");
        String roleStr = (String) req.getOrDefault("role", "STUDENT");
        Role role = Role.valueOf(roleStr.toUpperCase());

        User user = userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .approvalStatus(ApprovalStatus.APPROVED)
                .enabled(true)
                .build());

        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("fullName", user.getFullName());
        m.put("role", user.getRole().name());
        m.put("enabled", user.getEnabled());
        return m;
    }

    @Transactional
    public Map<String, Object> updateApprovalStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setApprovalStatus(ApprovalStatus.valueOf(status.toUpperCase()));
        userRepository.save(user);

        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("approvalStatus", user.getApprovalStatus().name());
        return m;
    }

    @Transactional
    public Map<String, Object> updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);

        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("enabled", user.getEnabled());
        return m;
    }

    @Transactional
    public Map<String, Object> updateUserRole(Long id, String roleStr) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(Role.valueOf(roleStr.toUpperCase()));
        userRepository.save(user);

        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("role", user.getRole().name());
        return m;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartments() {
        return departmentRepository.findAll().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("code", d.getCode());
            m.put("studentsCount", studentRepository.findAll().stream().filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(d.getId())).count());
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createDepartment(Map<String, Object> req) {
        String name = (String) req.get("name");
        String code = (String) req.get("code");

        Department d = departmentRepository.save(Department.builder().name(name).code(code).build());
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("code", d.getCode());
        m.put("studentsCount", 0);
        return m;
    }

    @Transactional
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCompanies() {
        return companyRepository.findAll().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("website", c.getWebsite());
            m.put("location", c.getLocation());
            m.put("description", c.getDescription());
            m.put("jobsCount", jobRepository.findByCompanyId(c.getId()).size());
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createCompany(Map<String, Object> req) {
        String name = (String) req.get("name");
        String website = (String) req.getOrDefault("website", "");
        String location = (String) req.getOrDefault("location", "");
        String description = (String) req.getOrDefault("description", "");

        Company c = companyRepository.save(Company.builder().name(name).website(website).location(location).description(description).build());
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("website", c.getWebsite());
        m.put("location", c.getLocation());
        return m;
    }

    @Transactional
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssignments() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FacultyAssignment fa : facultyAssignmentRepository.findAll()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", fa.getId());
            m.put("type", "FACULTY");
            m.put("mentorOrFacultyName", fa.getFaculty().getUser().getFullName());
            m.put("studentId", fa.getStudent().getId());
            m.put("studentName", fa.getStudent().getUser().getFullName());
            m.put("department", fa.getStudent().getDepartment() != null ? fa.getStudent().getDepartment().getName() : "General");
            list.add(m);
        }
        for (MentorAssignment ma : mentorAssignmentRepository.findAll()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ma.getId());
            m.put("type", "MENTOR");
            m.put("mentorOrFacultyName", ma.getMentor().getUser().getFullName());
            m.put("studentId", ma.getStudent().getId());
            m.put("studentName", ma.getStudent().getUser().getFullName());
            m.put("department", ma.getStudent().getDepartment() != null ? ma.getStudent().getDepartment().getName() : "General");
            list.add(m);
        }
        return list;
    }

    @Transactional
    public Map<String, Object> assignStudent(Map<String, Object> req) {
        String type = (String) req.getOrDefault("type", "FACULTY");
        Long studentId = Long.valueOf(req.get("studentId").toString());
        Long mentorOrFacultyId = Long.valueOf(req.get("mentorOrFacultyId").toString());

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if ("FACULTY".equalsIgnoreCase(type)) {
            Faculty faculty = facultyRepository.findById(mentorOrFacultyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found"));
            FacultyAssignment fa = facultyAssignmentRepository.save(FacultyAssignment.builder().faculty(faculty).student(student).build());
            Map<String, Object> m = new HashMap<>();
            m.put("id", fa.getId());
            m.put("type", "FACULTY");
            return m;
        } else {
            Mentor mentor = mentorRepository.findById(mentorOrFacultyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor not found"));
            MentorAssignment ma = mentorAssignmentRepository.save(MentorAssignment.builder().mentor(mentor).student(student).build());
            Map<String, Object> m = new HashMap<>();
            m.put("id", ma.getId());
            m.put("type", "MENTOR");
            return m;
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAiLogs() {
        return aiUsageLogRepository.findAll().stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("userId", log.getUserId());
            m.put("feature", log.getFeatureName());
            m.put("model", log.getModel());
            m.put("promptTokens", log.getPromptTokens());
            m.put("completionTokens", log.getCompletionTokens());
            m.put("totalTokens", (log.getPromptTokens() != null ? log.getPromptTokens() : 0) + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0));
            m.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivities() {
        return activityLogRepository.findAll().stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("userId", a.getUserId());
            m.put("action", a.getAction());
            m.put("entityType", a.getEntityType());
            m.put("entityId", a.getEntityId());
            m.put("details", a.getDetails());
            m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            return m;
        }).toList();
    }
}