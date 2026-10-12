package com.smartplacement.service;

import com.smartplacement.ai.util.PlacementMathEngine;
import com.smartplacement.entity.*;
import com.smartplacement.exception.BadRequestException;
import com.smartplacement.exception.ConflictException;
import com.smartplacement.exception.ResourceNotFoundException;
import com.smartplacement.repository.*;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final StudentRepository studentRepository;
    private final JobRepository jobRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobSkillRepository jobSkillRepository;
    private final NotificationRepository notificationRepository;
    private final PlacementMathEngine mathEngine;

    @Transactional
    public Application applyForJob(Long jobId, UserPrincipal principal) {
        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        if (!"OPEN".equalsIgnoreCase(job.getStatus())) {
            throw new BadRequestException("This job posting is closed for applications");
        }

        if (job.getDeadline() != null && LocalDate.now().isAfter(job.getDeadline())) {
            throw new BadRequestException("Application deadline has passed");
        }

        if (student.getCgpa() != null && job.getMinCgpa() != null && student.getCgpa() < job.getMinCgpa()) {
            throw new BadRequestException("Your CGPA (" + student.getCgpa() + ") does not meet the minimum requirement (" + job.getMinCgpa() + ")");
        }

        if (applicationRepository.existsByStudentIdAndJobId(student.getId(), job.getId())) {
            throw new ConflictException("You have already applied for this job opportunity");
        }

        // Calculate deterministic match score
        List<StudentSkill> studentSkills = studentSkillRepository.findByStudentId(student.getId());
        Set<String> sSkillNames = studentSkills.stream().map(ss -> ss.getSkill().getName()).collect(Collectors.toSet());
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());

        PlacementMathEngine.MatchResult matchResult = mathEngine.calculateJobMatch(sSkillNames, jobSkills);

        Application application = Application.builder()
                .student(student)
                .job(job)
                .status("APPLIED")
                .matchScore(matchResult.getScore())
                .build();

        application = applicationRepository.save(application);

        // Save history
        historyRepository.save(ApplicationStatusHistory.builder()
                .application(application)
                .previousStatus(null)
                .newStatus("APPLIED")
                .comment("Student submitted application")
                .changedByUserId(principal.getId())
                .build());

        // Notify Recruiter
        if (job.getRecruiter() != null && job.getRecruiter().getUser() != null) {
            notificationRepository.save(Notification.builder()
                    .user(job.getRecruiter().getUser())
                    .title("New Application Received")
                    .message(student.getUser().getFullName() + " applied for " + job.getTitle() + " (" + matchResult.getScore() + "% match)")
                    .type("APPLICATION")
                    .build());
        }

        return application;
    }

    @Transactional(readOnly = true)
    public List<Application> getStudentApplications(UserPrincipal principal) {
        Student student = studentRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        return applicationRepository.findByStudentId(student.getId());
    }

    @Transactional
    public Application updateApplicationStatus(Long applicationId, String newStatus, String comment, UserPrincipal principal) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        String oldStatus = application.getStatus();
        application.setStatus(newStatus.toUpperCase());
        applicationRepository.save(application);

        // Audit History
        historyRepository.save(ApplicationStatusHistory.builder()
                .application(application)
                .previousStatus(oldStatus)
                .newStatus(newStatus.toUpperCase())
                .comment(comment)
                .changedByUserId(principal.getId())
                .build());

        // Notify Student
        notificationRepository.save(Notification.builder()
                .user(application.getStudent().getUser())
                .title("Application Status Update")
                .message("Your application for " + application.getJob().getTitle() + " has been updated to " + newStatus)
                .type("APPLICATION_UPDATE")
                .build());

        return application;
    }
}
