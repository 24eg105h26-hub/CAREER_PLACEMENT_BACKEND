package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.dto.student.StudentProfileDto;
import com.smartplacement.entity.Job;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.StudentService;
import com.smartplacement.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/students", "/api/student"})
@RequiredArgsConstructor
public class StudentController {
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> data = studentService.getDashboard(principal);
        return ResponseEntity.ok(ApiResponse.success("Student dashboard retrieved", data));
    }


    private final StudentService studentService;
    private final JobService jobService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole(\"STUDENT\")")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        StudentProfileDto profile = studentService.getProfileByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Student profile retrieved", profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole(\"STUDENT\")")
    public ResponseEntity<ApiResponse<StudentProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody StudentProfileDto dto) {
        StudentProfileDto updated = studentService.updateProfile(currentUser.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("Student profile updated", updated));
    }

    @GetMapping("/skills")
    @PreAuthorize("hasRole(\"STUDENT\")")
    public ResponseEntity<ApiResponse<List<String>>> getSkills(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<String> skills = studentService.getStudentSkills(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Student skills retrieved", skills));
    }

    @PostMapping("/skills")
    @PreAuthorize("hasRole(\"STUDENT\")")
    public ResponseEntity<ApiResponse<String>> addSkill(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam String skill) {
        studentService.addSkill(currentUser.getId(), skill);
        return ResponseEntity.ok(ApiResponse.success("Skill added successfully", skill));
    }

    @GetMapping("/readiness")
    @PreAuthorize("hasRole(\"STUDENT\")")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReadiness(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> readiness = studentService.getReadiness(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Readiness score retrieved", readiness));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole(\"STUDENT\")")
    public ResponseEntity<ApiResponse<List<Job>>> getRecommendations(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Job> jobs = jobService.getOpenJobs(null, null, null);
        return ResponseEntity.ok(ApiResponse.success("Recommended jobs retrieved", jobs));
    }

    @GetMapping("/roadmap")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoadmap(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> roadmap = studentService.getRoadmap(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Learning roadmap retrieved", roadmap));
    }

    @PutMapping("/roadmap/items/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRoadmapItemStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Map<String, Object> updated = studentService.updateRoadmapItemStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Roadmap item updated", updated));
    }

    @GetMapping("/achievements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAchievements(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> badges = studentService.getAchievements(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Student badges retrieved", badges));
    }

}
