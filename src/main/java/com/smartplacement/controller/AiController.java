package com.smartplacement.controller;

import com.smartplacement.ai.util.PlacementMathEngine;
import com.smartplacement.dto.ApiResponse;
import com.smartplacement.dto.chat.*;
import com.smartplacement.service.ChatService;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.AiFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiFeatureService aiFeatureService;
    private final ChatService chatService;
    private final com.smartplacement.service.ResumeService resumeService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiHealth() {
        return ResponseEntity.ok(ApiResponse.success("AI Infrastructure Status", Map.of("configured", true, "mode", "dev-fallback")));
    }

    @PostMapping("/analyze-resume")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeResume(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Map<String, Object> result = resumeService.analyzeResume(file, null, null, principal);
        return ResponseEntity.ok(ApiResponse.success("Resume analyzed successfully", result));
    }

    @PostMapping("/ats-analysis")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeAts(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.analyzeAts(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("ATS analysis completed", result));
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'MENTOR')")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ChatResponse response = chatService.processChat(request, principal);
        return ResponseEntity.ok(ApiResponse.success("Study buddy response", response));
    }

    @PostMapping("/study-buddy/chat")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'MENTOR')")
    public ResponseEntity<ApiResponse<ChatResponse>> studyBuddyChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return chat(request, principal);
    }

    @GetMapping("/chat/conversations")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'MENTOR')")
    public ResponseEntity<ApiResponse<java.util.List<ConversationDto>>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        java.util.List<ConversationDto> conversations = chatService.getConversations(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved", conversations));
    }

    @GetMapping("/chat/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'MENTOR')")
    public ResponseEntity<ApiResponse<ConversationDetailDto>> getConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConversationDetailDto conversation = chatService.getConversation(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", conversation));
    }

    @DeleteMapping("/chat/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'MENTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        chatService.deleteConversation(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }

    @PostMapping("/interview/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startInterview(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.startMockInterview(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Mock interview started", result));
    }

    @PostMapping("/interview/answer")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> answerInterview(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.evaluateInterviewAnswer(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Interview answer evaluated", result));
    }

    @PostMapping("/code-review")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reviewCode(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.reviewCode(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Code review completed", result));
    }

    @PostMapping("/cover-letter")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateCoverLetter(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.generateCoverLetter(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Cover letter generated", result));
    }

    @PostMapping("/career-path")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateCareerPath(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.generateCareerPath(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Career path generated", result));
    }

    @PostMapping("/daily-plan")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateDailyPlan(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.generateDailyPlan(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Daily plan generated", result));
    }

    @PostMapping("/communication")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeCommunication(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiFeatureService.analyzeCommunication(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Communication analysis completed", result));
    }

    @GetMapping("/skill-gap/{studentId}/{jobId}")
    public ResponseEntity<ApiResponse<PlacementMathEngine.MatchResult>> getSkillGap(
            @PathVariable Long studentId,
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        PlacementMathEngine.MatchResult result = aiFeatureService.calculateSkillGap(studentId, jobId, principal);
        return ResponseEntity.ok(ApiResponse.success("Skill gap analysis calculated", result));
    }

    @GetMapping("/readiness")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PlacementMathEngine.ReadinessBreakdown>> getPlacementReadiness(
            @AuthenticationPrincipal UserPrincipal principal) {
        PlacementMathEngine.ReadinessBreakdown result = aiFeatureService.getPlacementReadiness(principal);
        return ResponseEntity.ok(ApiResponse.success("Placement readiness score calculated", result));
    }
}
