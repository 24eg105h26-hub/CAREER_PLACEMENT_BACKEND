package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.entity.Job;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Job>>> getJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Double minCgpa) {
        List<Job> jobs = jobService.getOpenJobs(location, jobType, minCgpa);
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success("Job details fetched successfully", job));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER','TPO','ADMIN')")
    public ResponseEntity<ApiResponse<Job>> createJob(
            @RequestBody Job job,
            @RequestParam(required = false) List<String> requiredSkills,
            @RequestParam(required = false) List<String> preferredSkills,
            @AuthenticationPrincipal UserPrincipal principal) {
        Job created = jobService.createJob(job, requiredSkills, preferredSkills, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Job created successfully", created));
    }
}
