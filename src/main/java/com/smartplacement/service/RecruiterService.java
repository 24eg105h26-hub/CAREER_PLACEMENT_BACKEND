package com.smartplacement.service;

import com.smartplacement.entity.*;
import com.smartplacement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final NotificationRepository notificationRepository;

    private Recruiter getRecruiterOrThrow(Long userId) {
        return recruiterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter profile not found"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(Long userId) {
        Recruiter recruiter = getRecruiterOrThrow(userId);
        Long companyId = recruiter.getCompany() != null ? recruiter.getCompany().getId() : null;

        List<Job> jobs = companyId != null ? jobRepository.findByCompanyId(companyId) : Collections.emptyList();
        long activeJobs = jobs.stream().filter(j -> "OPEN".equalsIgnoreCase(j.getStatus())).count();

        List<Application> apps = new ArrayList<>();
        for (Job j : jobs) {
            apps.addAll(applicationRepository.findByJobId(j.getId()));
        }

        long totalApplicants = apps.size();
        long shortlisted = apps.stream().filter(a -> "SHORTLISTED".equalsIgnoreCase(a.getStatus())).count();
        long interviews = apps.stream().filter(a -> "INTERVIEW".equalsIgnoreCase(a.getStatus()) || "TECHNICAL_INTERVIEW".equalsIgnoreCase(a.getStatus())).count();
        long selected = apps.stream().filter(a -> "SELECTED".equalsIgnoreCase(a.getStatus())).count();

        List<Map<String, Object>> recentApplicants = apps.stream().limit(5).map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("applicationId", a.getId());
            m.put("studentName", a.getStudent().getUser().getFullName());
            m.put("jobTitle", a.getJob().getTitle());
            m.put("status", a.getStatus());
            m.put("matchScore", a.getMatchScore());
            m.put("appliedAt", a.getAppliedAt() != null ? a.getAppliedAt().toString() : "");
            return m;
        }).toList();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("recruiterName", recruiter.getUser().getFullName());
        dashboard.put("companyName", recruiter.getCompany() != null ? recruiter.getCompany().getName() : "Unassigned Company");
        dashboard.put("activeJobsCount", activeJobs);
        dashboard.put("totalApplicantsCount", totalApplicants);
        dashboard.put("shortlistedCount", shortlisted);
        dashboard.put("interviewsScheduledCount", interviews);
        dashboard.put("selectedCount", selected);
        dashboard.put("recentApplicants", recentApplicants);
        return dashboard;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCompany(Long userId) {
        Recruiter recruiter = getRecruiterOrThrow(userId);
        Company company = recruiter.getCompany();

        Map<String, Object> m = new HashMap<>();
        if (company != null) {
            m.put("id", company.getId());
            m.put("name", company.getName());
            m.put("website", company.getWebsite());
            m.put("location", company.getLocation());
            m.put("description", company.getDescription());
        } else {
            m.put("id", null);
            m.put("name", "Google");
            m.put("website", "https://google.com");
            m.put("location", "Hyderabad / Bengaluru");
            m.put("description", "Leading Technology & Cloud Solutions");
        }
        return m;
    }

    @Transactional
    public Map<String, Object> updateCompany(Long userId, Map<String, Object> req) {
        Recruiter recruiter = getRecruiterOrThrow(userId);
        Company company = recruiter.getCompany();

        String name = (String) req.getOrDefault("name", "Enterprise Tech");
        String website = (String) req.getOrDefault("website", "");
        String location = (String) req.getOrDefault("location", "");
        String description = (String) req.getOrDefault("description", "");

        if (company == null) {
            company = companyRepository.save(Company.builder()
                    .name(name)
                    .website(website)
                    .location(location)
                    .description(description)
                    .build());
            recruiter.setCompany(company);
            recruiterRepository.save(recruiter);
        } else {
            company.setName(name);
            company.setWebsite(website);
            company.setLocation(location);
            company.setDescription(description);
            companyRepository.save(company);
        }

        Map<String, Object> m = new HashMap<>();
        m.put("id", company.getId());
        m.put("name", company.getName());
        m.put("website", company.getWebsite());
        m.put("location", company.getLocation());
        m.put("description", company.getDescription());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApplicants(Long userId, String statusFilter, Long jobIdFilter) {
        Recruiter recruiter = getRecruiterOrThrow(userId);
        Long companyId = recruiter.getCompany() != null ? recruiter.getCompany().getId() : null;

        List<Job> jobs = companyId != null ? jobRepository.findByCompanyId(companyId) : Collections.emptyList();
        List<Application> apps = new ArrayList<>();
        for (Job j : jobs) {
            if (jobIdFilter == null || jobIdFilter.equals(j.getId())) {
                apps.addAll(applicationRepository.findByJobId(j.getId()));
            }
        }

        return apps.stream()
                .filter(a -> statusFilter == null || statusFilter.equalsIgnoreCase("ALL") || statusFilter.equalsIgnoreCase(a.getStatus()))
                .map(a -> {
                    Student s = a.getStudent();
                    Map<String, Object> m = new HashMap<>();
                    m.put("applicationId", a.getId());
                    m.put("studentId", s.getId());
                    m.put("studentName", s.getUser().getFullName());
                    m.put("email", s.getUser().getEmail());
                    m.put("rollNumber", s.getRollNumber());
                    m.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "General");
                    m.put("cgpa", s.getCgpa());
                    m.put("jobId", a.getJob().getId());
                    m.put("jobTitle", a.getJob().getTitle());
                    m.put("status", a.getStatus());
                    m.put("matchScore", a.getMatchScore());
                    m.put("appliedAt", a.getAppliedAt() != null ? a.getAppliedAt().toString() : "");
                    return m;
                }).toList();
    }

    @Transactional
    public Map<String, Object> updateApplicantStatus(Long applicationId, String newStatus) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        app.setStatus(newStatus.toUpperCase());
        applicationRepository.save(app);

        // Notify candidate
        notificationRepository.save(Notification.builder()
                .user(app.getStudent().getUser())
                .title("Application Status Update")
                .message("Your application for " + app.getJob().getTitle() + " has been updated to: " + newStatus.toUpperCase())
                .type("APPLICATION")
                .link("/student/applications")
                .build());

        Map<String, Object> m = new HashMap<>();
        m.put("applicationId", app.getId());
        m.put("status", app.getStatus());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> compareCandidates(List<Long> studentIds) {
        List<Student> students;
        if (studentIds != null && !studentIds.isEmpty()) {
            students = studentRepository.findAllById(studentIds);
        } else {
            students = studentRepository.findAll().stream().limit(3).toList();
        }

        return students.stream().map(s -> {
            List<StudentSkill> skills = studentSkillRepository.findByStudentId(s.getId());
            List<String> skillNames = skills.stream().map(ss -> ss.getSkill().getName()).toList();

            Map<String, Object> m = new HashMap<>();
            m.put("studentId", s.getId());
            m.put("name", s.getUser().getFullName());
            m.put("email", s.getUser().getEmail());
            m.put("rollNumber", s.getRollNumber());
            m.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "Computer Science");
            m.put("cgpa", s.getCgpa());
            m.put("skills", skillNames);
            m.put("readiness", Math.min(100.0, (s.getCgpa() != null ? s.getCgpa() : 7.0) * 10.0));
            m.put("applicationsCount", applicationRepository.findByStudentId(s.getId()).size());
            return m;
        }).toList();
    }
}