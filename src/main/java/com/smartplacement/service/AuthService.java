package com.smartplacement.service;

import com.smartplacement.dto.auth.AuthResponse;
import com.smartplacement.dto.auth.LoginRequest;
import com.smartplacement.dto.auth.RegisterRequest;
import com.smartplacement.dto.auth.UserProfileResponse;
import com.smartplacement.entity.*;
import com.smartplacement.exception.BadRequestException;
import com.smartplacement.exception.ConflictException;
import com.smartplacement.exception.ForbiddenAccessException;
import com.smartplacement.exception.ResourceNotFoundException;
import com.smartplacement.repository.*;
import com.smartplacement.security.JwtTokenProvider;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final RecruiterRepository recruiterRepository;
    private final FacultyRepository facultyRepository;
    private final MentorRepository mentorRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() != Role.STUDENT && request.getRole() != Role.RECRUITER) {
            throw new BadRequestException("Public registration is only allowed for STUDENT and RECRUITER roles");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        ApprovalStatus initialStatus = (request.getRole() == Role.RECRUITER)
                ? ApprovalStatus.PENDING
                : ApprovalStatus.APPROVED;

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .approvalStatus(initialStatus)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        if (request.getRole() == Role.STUDENT) {
            Department department = null;
            if (request.getDepartmentId() != null) {
                department = departmentRepository.findById(request.getDepartmentId())
                        .orElse(null);
            }
            Student student = Student.builder()
                    .user(user)
                    .department(department)
                    .rollNumber(request.getRollNumber())
                    .cgpa(request.getCgpa() != null ? request.getCgpa() : 0.0)
                    .graduationYear(request.getGraduationYear())
                    .phone(request.getPhone())
                    .build();
            studentRepository.save(student);
        } else if (request.getRole() == Role.RECRUITER) {
            Company company = null;
            if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
                company = companyRepository.findByName(request.getCompanyName())
                        .orElseGet(() -> companyRepository.save(Company.builder()
                                .name(request.getCompanyName())
                                .build()));
            }
            Recruiter recruiter = Recruiter.builder()
                    .user(user)
                    .company(company)
                    .build();
            recruiterRepository.save(recruiter);
        }

        if (initialStatus == ApprovalStatus.PENDING) {
            return AuthResponse.builder()
                    .token(null)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .approvalStatus(user.getApprovalStatus())
                    .build();
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .approvalStatus(user.getApprovalStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getApprovalStatus() == ApprovalStatus.PENDING) {
            throw new ForbiddenAccessException("Your recruiter account is pending TPO/Admin approval.");
        }

        if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new ForbiddenAccessException("Your account registration has been rejected.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .approvalStatus(user.getApprovalStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> details = new HashMap<>();
        Long profileId = null;

        switch (user.getRole()) {
            case STUDENT:
                Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                if (student != null) {
                    profileId = student.getId();
                    details.put("cgpa", student.getCgpa());
                    details.put("rollNumber", student.getRollNumber());
                    details.put("graduationYear", student.getGraduationYear());
                    details.put("department", student.getDepartment() != null ? student.getDepartment().getName() : null);
                    details.put("departmentId", student.getDepartment() != null ? student.getDepartment().getId() : null);
                }
                break;
            case RECRUITER:
                Recruiter recruiter = recruiterRepository.findByUserId(user.getId()).orElse(null);
                if (recruiter != null) {
                    profileId = recruiter.getId();
                    details.put("companyId", recruiter.getCompany() != null ? recruiter.getCompany().getId() : null);
                    details.put("companyName", recruiter.getCompany() != null ? recruiter.getCompany().getName() : null);
                    details.put("designation", recruiter.getDesignation());
                }
                break;
            case FACULTY:
                Faculty faculty = facultyRepository.findByUserId(user.getId()).orElse(null);
                if (faculty != null) {
                    profileId = faculty.getId();
                    details.put("department", faculty.getDepartment() != null ? faculty.getDepartment().getName() : null);
                    details.put("designation", faculty.getDesignation());
                }
                break;
            case MENTOR:
                Mentor mentor = mentorRepository.findByUserId(user.getId()).orElse(null);
                if (mentor != null) {
                    profileId = mentor.getId();
                    details.put("companyName", mentor.getCompanyName());
                    details.put("expertise", mentor.getExpertise());
                }
                break;
            case ADMIN:
            case TPO:
                profileId = user.getId();
                break;
        }

        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .approvalStatus(user.getApprovalStatus())
                .profileId(profileId)
                .roleDetails(details)
                .build();
    }
}
