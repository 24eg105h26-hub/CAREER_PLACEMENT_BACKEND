package com.smartplacement.service;

import com.smartplacement.entity.*;
import com.smartplacement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TpoService {

    private final PlacementDriveRepository driveRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        long totalStudents = studentRepository.count();
        long totalCompanies = companyRepository.count();
        long totalDrives = driveRepository.count();
        long activeDrives = driveRepository.findAll().stream().filter(d -> "ONGOING".equalsIgnoreCase(d.getStatus()) || "UPCOMING".equalsIgnoreCase(d.getStatus())).count();
        long totalJobs = jobRepository.count();
        long totalApplications = applicationRepository.count();

        long placedCount = applicationRepository.findAll().stream()
                .filter(a -> "SELECTED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getStudent().getId())
                .distinct()
                .count();

        double placementRate = totalStudents > 0 ? Math.round(((double) placedCount / totalStudents * 100.0) * 10.0) / 10.0 : 0.0;

        Map<String, Object> map = new HashMap<>();
        map.put("totalStudents", totalStudents);
        map.put("totalCompanies", totalCompanies);
        map.put("totalDrives", totalDrives);
        map.put("activeDrives", activeDrives);
        map.put("totalJobs", totalJobs);
        map.put("totalApplications", totalApplications);
        map.put("placedStudentsCount", placedCount);
        map.put("placementRate", placementRate);
        map.put("averagePackage", "14.2 LPA");
        return map;
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

        Company company = companyRepository.save(Company.builder()
                .name(name)
                .website(website)
                .location(location)
                .description(description)
                .build());

        Map<String, Object> m = new HashMap<>();
        m.put("id", company.getId());
        m.put("name", company.getName());
        m.put("website", company.getWebsite());
        m.put("location", company.getLocation());
        m.put("description", company.getDescription());
        m.put("jobsCount", 0);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDrives() {
        return driveRepository.findAll().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("title", d.getTitle());
            m.put("description", d.getDescription());
            m.put("companyId", d.getCompany().getId());
            m.put("companyName", d.getCompany().getName());
            m.put("location", d.getLocation());
            m.put("driveDate", d.getDriveDate() != null ? d.getDriveDate().toString() : "");
            m.put("minCgpa", d.getMinCgpa());
            m.put("status", d.getStatus());
            m.put("jobId", d.getJob() != null ? d.getJob().getId() : null);
            m.put("jobTitle", d.getJob() != null ? d.getJob().getTitle() : null);
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> createDrive(Map<String, Object> req, Long userId) {
        String title = (String) req.get("title");
        String description = (String) req.getOrDefault("description", "");
        String location = (String) req.getOrDefault("location", "Campus");
        Double minCgpa = req.get("minCgpa") != null ? Double.valueOf(req.get("minCgpa").toString()) : 6.0;
        String dateStr = (String) req.getOrDefault("driveDate", LocalDate.now().plusDays(14).toString());
        LocalDate driveDate = LocalDate.parse(dateStr);

        Long companyId = req.get("companyId") != null ? Long.valueOf(req.get("companyId").toString()) : null;
        Company company;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        } else {
            company = companyRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No partner company available"));
        }

        PlacementDrive drive = driveRepository.save(PlacementDrive.builder()
                .title(title)
                .description(description)
                .location(location)
                .minCgpa(minCgpa)
                .driveDate(driveDate)
                .company(company)
                .status("UPCOMING")
                .createdByUserId(userId)
                .build());

        Map<String, Object> m = new HashMap<>();
        m.put("id", drive.getId());
        m.put("title", drive.getTitle());
        m.put("companyName", drive.getCompany().getName());
        m.put("status", drive.getStatus());
        m.put("driveDate", drive.getDriveDate().toString());
        m.put("minCgpa", drive.getMinCgpa());
        return m;
    }

    @Transactional
    public Map<String, Object> closeDrive(Long id) {
        PlacementDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Placement drive not found"));
        drive.setStatus("COMPLETED");
        driveRepository.save(drive);

        Map<String, Object> m = new HashMap<>();
        m.put("id", drive.getId());
        m.put("title", drive.getTitle());
        m.put("status", drive.getStatus());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getJobs() {
        return jobRepository.findAll().stream().map(j -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", j.getId());
            m.put("title", j.getTitle());
            m.put("companyName", j.getCompany().getName());
            m.put("location", j.getLocation());
            m.put("salaryRange", j.getSalaryRange());
            m.put("minCgpa", j.getMinCgpa());
            m.put("status", j.getStatus());
            m.put("deadline", j.getDeadline() != null ? j.getDeadline().toString() : "");
            m.put("applicationsCount", applicationRepository.findByJobId(j.getId()).size());
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> updateJobStatus(Long jobId, String status) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        job.setStatus(status);
        jobRepository.save(job);

        Map<String, Object> m = new HashMap<>();
        m.put("id", job.getId());
        m.put("title", job.getTitle());
        m.put("status", job.getStatus());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEligibleStudents(Double minCgpa) {
        return studentRepository.findAll().stream()
                .filter(s -> s.getCgpa() != null && s.getCgpa() >= minCgpa)
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("name", s.getUser().getFullName());
                    map.put("email", s.getUser().getEmail());
                    map.put("cgpa", s.getCgpa());
                    map.put("rollNumber", s.getRollNumber());
                    map.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "Computer Science");
                    return map;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        long totalStudents = studentRepository.count();
        long totalDrives = driveRepository.count();

        // Department wise placement breakdown
        Map<String, Long> deptCounts = studentRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null)
                .collect(Collectors.groupingBy(s -> s.getDepartment().getCode(), Collectors.counting()));

        // Application status breakdown
        Map<String, Long> appStatusCounts = applicationRepository.findAll().stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        long placed = appStatusCounts.getOrDefault("SELECTED", 0L);
        double rate = totalStudents > 0 ? Math.round(((double) placed / totalStudents * 100.0) * 10.0) / 10.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalDrives", totalDrives);
        stats.put("totalJobs", jobRepository.count());
        stats.put("totalApplications", applicationRepository.count());
        stats.put("placementRate", rate);
        stats.put("averagePackage", "14.2 LPA");
        stats.put("departmentBreakdown", deptCounts);
        stats.put("applicationFunnel", appStatusCounts);
        return stats;
    }
}