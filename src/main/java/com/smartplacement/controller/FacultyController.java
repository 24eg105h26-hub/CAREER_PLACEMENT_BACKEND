package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.FacultyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService facultyService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> data = facultyService.getDashboard(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Faculty dashboard data retrieved", data));
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignedStudents(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> students = facultyService.getAssignedStudents(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Assigned students retrieved", students));
    }

    @GetMapping("/students/{id}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> detail = facultyService.getStudentDetail(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Student details retrieved", detail));
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEarlyWarnings(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> warnings = facultyService.getEarlyWarnings(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Early warning list retrieved", warnings));
    }

    @GetMapping("/assessments")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssessments(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> assessments = facultyService.getAssessments(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Faculty assessments retrieved", assessments));
    }

    @PostMapping("/assessments")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAssessment(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> assessment = facultyService.createAssessment(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Assessment created successfully", assessment));
    }

    @PostMapping("/feedback")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createFeedback(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> feedback = facultyService.createFeedback(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully", feedback));
    }
}