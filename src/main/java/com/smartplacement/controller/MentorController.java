package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mentor")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(@AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> data = mentorService.getDashboard(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Mentor dashboard data retrieved", data));
    }

    @GetMapping({"/mentees", "/students"})
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMentees(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> mentees = mentorService.getAssignedMentees(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Assigned mentees retrieved", mentees));
    }

    @GetMapping("/students/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> detail = mentorService.getStudentDetail(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Mentee details retrieved", detail));
    }

    @GetMapping("/progress")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProgress(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> progress = mentorService.getLearningProgress(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Mentee progress data retrieved", progress));
    }

    @GetMapping("/feedback")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFeedback(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> feedback = mentorService.getFeedback(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Mentor feedback retrieved", feedback));
    }

    @PostMapping("/feedback")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createFeedback(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> feedback = mentorService.createFeedback(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully", feedback));
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActivities(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> activities = mentorService.getActivities(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Mentor activities retrieved", activities));
    }

    @PostMapping("/activities")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createActivity(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> activity = mentorService.createActivity(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Activity logged successfully", activity));
    }

    @DeleteMapping("/activities/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        mentorService.deleteActivity(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Activity deleted successfully", null));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecommendations(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Map<String, Object>> recs = mentorService.getAiRecommendations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("AI recommendations retrieved", recs));
    }
}