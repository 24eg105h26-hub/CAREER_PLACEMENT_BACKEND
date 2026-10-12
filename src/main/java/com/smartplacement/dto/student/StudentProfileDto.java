package com.smartplacement.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDto {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String rollNumber;
    private Double cgpa;
    private Integer graduationYear;
    private String collegeName;
    private String departmentName;
    private String bio;
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private List<String> skills;
}
