package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.TpoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tpo")
@RequiredArgsConstructor
public class TpoController {

    private final TpoService tpoService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> data = tpoService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("TPO dashboard data retrieved", data));
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCompanies() {
        List<Map<String, Object>> companies = tpoService.getCompanies();
        return ResponseEntity.ok(ApiResponse.success("Companies retrieved", companies));
    }

    @PostMapping("/companies")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCompany(@RequestBody Map<String, Object> req) {
        Map<String, Object> created = tpoService.createCompany(req);
        return ResponseEntity.ok(ApiResponse.success("Company created successfully", created));
    }

    @GetMapping("/drives")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDrives() {
        List<Map<String, Object>> drives = tpoService.getDrives();
        return ResponseEntity.ok(ApiResponse.success("Placement drives retrieved", drives));
    }

    @PostMapping("/drives")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDrive(
            @RequestBody Map<String, Object> drive,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> saved = tpoService.createDrive(drive, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Placement drive created successfully", saved));
    }

    @PutMapping("/drives/{id}/close")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> closeDrive(@PathVariable Long id) {
        Map<String, Object> closed = tpoService.closeDrive(id);
        return ResponseEntity.ok(ApiResponse.success("Placement drive closed successfully", closed));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getJobs() {
        List<Map<String, Object>> jobs = tpoService.getJobs();
        return ResponseEntity.ok(ApiResponse.success("All placement jobs retrieved", jobs));
    }

    @PutMapping("/jobs/{id}/status")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateJobStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Map<String, Object> updated = tpoService.updateJobStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Job status updated", updated));
    }

    @GetMapping({"/eligible-students", "/students/eligible"})
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEligibleStudents(@RequestParam(defaultValue = "6.0") Double minCgpa) {
        List<Map<String, Object>> eligible = tpoService.getEligibleStudents(minCgpa);
        return ResponseEntity.ok(ApiResponse.success("Eligible students retrieved", eligible));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('TPO', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        Map<String, Object> stats = tpoService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success("TPO analytics retrieved", stats));
    }
}