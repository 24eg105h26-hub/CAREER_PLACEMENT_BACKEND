package com.smartplacement.controller;

import com.smartplacement.dto.ApiResponse;
import com.smartplacement.dto.auth.AuthResponse;
import com.smartplacement.dto.auth.LoginRequest;
import com.smartplacement.dto.auth.RegisterRequest;
import com.smartplacement.dto.auth.UserProfileResponse;
import com.smartplacement.security.UserPrincipal;
import com.smartplacement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        String msg = (response.getApprovalStatus() == com.smartplacement.entity.ApprovalStatus.PENDING)
                ? "Registration successful. Recruiter account is pending TPO/Admin approval."
                : "Registration successful";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(msg, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated"));
        }
        UserProfileResponse response = authService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", response));
    }
}
