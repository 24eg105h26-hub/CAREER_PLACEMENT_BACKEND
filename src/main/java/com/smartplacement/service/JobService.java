package com.smartplacement.service;

import com.smartplacement.entity.Company;
import com.smartplacement.entity.Job;
import com.smartplacement.entity.Recruiter;
import com.smartplacement.entity.Skill;
import com.smartplacement.entity.JobSkill;
import com.smartplacement.exception.BadRequestException;
import com.smartplacement.exception.ForbiddenAccessException;
import com.smartplacement.exception.ResourceNotFoundException;
import com.smartplacement.repository.*;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final RecruiterRepository recruiterRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<Job> getOpenJobs(String location, String jobType, Double minCgpa) {
        return jobRepository.filterJobs(location, jobType, minCgpa);
    }

    @Transactional(readOnly = true)
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with ID: " + id));
    }

    @Transactional
    public Job createJob(Job jobInput, List<String> requiredSkillNames, List<String> preferredSkillNames, UserPrincipal principal) {
        Recruiter recruiter = recruiterRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));

        if (recruiter.getUser().getApprovalStatus() != com.smartplacement.entity.ApprovalStatus.APPROVED) {
            throw new ForbiddenAccessException("Recruiter account is pending TPO/Admin approval");
        }

        jobInput.setRecruiter(recruiter);
        jobInput.setCompany(recruiter.getCompany());
        if (jobInput.getStatus() == null) {
            jobInput.setStatus("OPEN");
        }

        Job savedJob = jobRepository.save(jobInput);

        if (requiredSkillNames != null) {
            for (String sName : requiredSkillNames) {
                Skill skill = skillRepository.findByName(sName)
                        .orElseGet(() -> skillRepository.save(Skill.builder().name(sName).category("General").build()));
                jobSkillRepository.save(JobSkill.builder().job(savedJob).skill(skill).skillType("REQUIRED").weight(3).build());
            }
        }

        if (preferredSkillNames != null) {
            for (String sName : preferredSkillNames) {
                Skill skill = skillRepository.findByName(sName)
                        .orElseGet(() -> skillRepository.save(Skill.builder().name(sName).category("General").build()));
                jobSkillRepository.save(JobSkill.builder().job(savedJob).skill(skill).skillType("PREFERRED").weight(1).build());
            }
        }

        return savedJob;
    }
}
