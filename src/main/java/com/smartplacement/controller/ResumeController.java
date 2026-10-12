package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobId", required = false) Long jobId,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Map<String, Object> result = resumeService.analyzeResume(file, jobId, jobDescription, principal);
        return ResponseEntity.ok(ApiResponse.success("Resume analyzed successfully", result));
    }

    @PostMapping("/compare-job")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareJob(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long resumeId = Long.valueOf(req.get("resumeId").toString());
        Long jobId = req.get("jobId") != null ? Long.valueOf(req.get("jobId").toString()) : null;
        String jobDescription = (String) req.get("jobDescription");

        Map<String, Object> analysis = resumeService.getResumeAnalysis(resumeId, principal);
        String resumeText = analysis.get("extractedText") != null ? analysis.get("extractedText").toString() : analysis.toString();

        Map<String, Object> result = resumeService.compareResumeWithJob(resumeText, jobDescription != null ? jobDescription : "");
        return ResponseEntity.ok(ApiResponse.success("Job comparison completed", result));
    }

    @PostMapping("/rewrite-bullet")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rewriteBullet(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long resumeId = Long.valueOf(req.get("resumeId").toString());
        String bullet = (String) req.get("bullet");
        String context = (String) req.getOrDefault("context", "");

        Map<String, Object> result = resumeService.rewriteBullet(resumeId, bullet, context, principal);
        return ResponseEntity.ok(ApiResponse.success("Bullet point rewritten successfully", result));
    }

    @PostMapping("/add-skills")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addSkills(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal principal) {
        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) req.get("skills");
        Map<String, Object> result = resumeService.addSkillsToProfile(skills != null ? skills : List.of(), principal);
        return ResponseEntity.ok(ApiResponse.success("Skills added to profile successfully", result));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Map<String, Object>> history = resumeService.getResumeHistory(principal);
        return ResponseEntity.ok(ApiResponse.success("Resume history retrieved", history));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> analysis = resumeService.getResumeAnalysis(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Resume analysis retrieved", analysis));
    }

    @GetMapping("/{id}/pdf-report")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> downloadPdfReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        byte[] pdfBytes = resumeService.generatePdfReport(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Resume_ATS_Audit_Report_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
