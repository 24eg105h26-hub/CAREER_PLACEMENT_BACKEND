package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.entity.Application;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/{jobId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Application>> applyForJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Application application = applicationService.applyForJob(jobId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Application submitted successfully", application));
    }

    @GetMapping({"/my", ""})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<Application>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Application> apps = applicationService.getStudentApplications(principal);
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", apps));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER','TPO','ADMIN')")
    public ResponseEntity<ApiResponse<Application>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String status = body.get("status");
        String comment = body.getOrDefault("comment", "Status updated by employer");
        Application updated = applicationService.updateApplicationStatus(id, status, comment, principal);
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + status, updated));
    }
}
