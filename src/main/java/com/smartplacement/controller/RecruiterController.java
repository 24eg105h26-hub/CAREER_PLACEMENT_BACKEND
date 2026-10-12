package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
public class RecruiterController {
    private final com.smartplacement.service.JobService jobService;


    private final RecruiterService recruiterService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> data = recruiterService.getDashboard(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Recruiter dashboard retrieved", data));
    }

    @GetMapping("/company")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCompany(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> company = recruiterService.getCompany(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Company profile retrieved", company));
    }

    @PutMapping("/company")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCompany(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, Object> req) {
        Map<String, Object> updated = recruiterService.updateCompany(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Company profile updated", updated));
    }

    @GetMapping("/applicants")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getApplicants(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long jobId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> applicants = recruiterService.getApplicants(currentUser.getId(), status, jobId);
        return ResponseEntity.ok(ApiResponse.success("Applicants retrieved", applicants));
    }

    @PutMapping("/applicants/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Map<String, Object> res = recruiterService.updateApplicantStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Application status updated", res));
    }

    @GetMapping("/compare")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> compareCandidates(
            @RequestParam(required = false) List<Long> studentIds) {
        List<Map<String, Object>> comparison = recruiterService.compareCandidates(studentIds);
        return ResponseEntity.ok(ApiResponse.success("Candidate comparison data retrieved", comparison));
    }
}