package com.smartplacement.dto.auth;

import com.smartplacement.entity.ApprovalStatus;
import com.smartplacement.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private ApprovalStatus approvalStatus;
}
