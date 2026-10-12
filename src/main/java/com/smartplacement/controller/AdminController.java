package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> data = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard data retrieved", data));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers() {
        List<Map<String, Object>> users = adminService.getUsers();
        return ResponseEntity.ok(ApiResponse.success("Users list retrieved", users));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUser(@RequestBody Map<String, Object> req) {
        Map<String, Object> user = adminService.createUser(req);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
    }

    @PutMapping("/users/{id}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateApprovalStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Map<String, Object> res = adminService.updateApprovalStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("User approval status updated", res));
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        Map<String, Object> res = adminService.updateUserStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success("User status updated", res));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        Map<String, Object> res = adminService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("User role updated", res));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartments() {
        List<Map<String, Object>> depts = adminService.getDepartments();
        return ResponseEntity.ok(ApiResponse.success("Departments retrieved", depts));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDepartment(@RequestBody Map<String, Object> req) {
        Map<String, Object> created = adminService.createDepartment(req);
        return ResponseEntity.ok(ApiResponse.success("Department created successfully", created));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Department deleted successfully", null));
    }

    @GetMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCompanies() {
        List<Map<String, Object>> list = adminService.getCompanies();
        return ResponseEntity.ok(ApiResponse.success("Companies retrieved", list));
    }

    @PostMapping("/companies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCompany(@RequestBody Map<String, Object> req) {
        Map<String, Object> created = adminService.createCompany(req);
        return ResponseEntity.ok(ApiResponse.success("Company created successfully", created));
    }

    @DeleteMapping("/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        adminService.deleteCompany(id);
        return ResponseEntity.ok(ApiResponse.success("Company deleted successfully", null));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignments() {
        List<Map<String, Object>> list = adminService.getAssignments();
        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved", list));
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignStudent(@RequestBody Map<String, Object> req) {
        Map<String, Object> assigned = adminService.assignStudent(req);
        return ResponseEntity.ok(ApiResponse.success("Assignment created successfully", assigned));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemAnalytics() {
        Map<String, Object> stats = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("System analytics retrieved", stats));
    }

    @GetMapping("/ai-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAiLogs() {
        List<Map<String, Object>> logs = adminService.getAiLogs();
        return ResponseEntity.ok(ApiResponse.success("AI usage logs retrieved", logs));
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActivities() {
        List<Map<String, Object>> activities = adminService.getActivities();
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved", activities));
    }
}