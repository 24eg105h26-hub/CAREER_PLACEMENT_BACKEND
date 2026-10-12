package com.smartplacement.dto.auth;

import com.smartplacement.entity.ApprovalStatus;
import com.smartplacement.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private ApprovalStatus approvalStatus;
    private Long profileId; // Student ID, Recruiter ID, Faculty ID, or Mentor ID
    private Map<String, Object> roleDetails;
}
