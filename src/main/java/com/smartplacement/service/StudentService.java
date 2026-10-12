package com.smartplacement.service;

import com.smartplacement.dto.student.StudentProfileDto;
import com.smartplacement.entity.Skill;
import com.smartplacement.entity.Student;
import com.smartplacement.entity.StudentSkill;
import com.smartplacement.entity.User;
import com.smartplacement.exception.ResourceNotFoundException;
import com.smartplacement.repository.SkillRepository;
import com.smartplacement.repository.StudentRepository;
import com.smartplacement.repository.StudentSkillRepository;
import com.smartplacement.repository.UserRepository;
import com.smartplacement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(UserPrincipal principal) {
        Student student = studentRepository.findByUserId(principal.getId()).orElse(null);
        Map<String, Object> res = new HashMap<>();
        if (student != null) {
            long appCount = applicationRepository.findByStudentId(student.getId()).size();
            var skills = studentSkillRepository.findByStudentId(student.getId());
            res.put("studentName", principal.getFullName());
            res.put("rollNumber", student.getRollNumber());
            res.put("department", student.getDepartment() != null ? student.getDepartment().getName() : "Computer Science");
            res.put("cgpa", student.getCgpa());
            res.put("totalApplications", appCount);
            res.put("skillsCount", skills.size());
        }
        try {
            var breakdown = aiFeatureService.getPlacementReadiness(principal);
            res.put("readinessScore", breakdown.getOverallScore());
        } catch (Exception e) {
            res.put("readinessScore", 75.0);
        }
        return res;
    }


    private final StudentRepository studentRepository;
    private final com.smartplacement.repository.ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final com.smartplacement.repository.LearningRoadmapRepository learningRoadmapRepository;
    private final com.smartplacement.repository.LearningRoadmapItemRepository learningRoadmapItemRepository;
    private final com.smartplacement.repository.BadgeRepository badgeRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final SkillRepository skillRepository;
    private final AiFeatureService aiFeatureService;

    @Transactional(readOnly = true)
    public StudentProfileDto getProfileByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user ID: " + userId));

        List<String> skillNames = studentSkillRepository.findByStudentId(student.getId()).stream()
                .map(ss -> ss.getSkill().getName())
                .collect(Collectors.toList());

        return StudentProfileDto.builder()
                .id(student.getId())
                .userId(student.getUser().getId())
                .fullName(student.getUser().getFullName())
                .email(student.getUser().getEmail())
                .phone(student.getUser().getPhone())
                .rollNumber(student.getRollNumber())
                .cgpa(student.getCgpa())
                .graduationYear(student.getGraduationYear())
                .collegeName(student.getCollegeName())
                .departmentName(student.getDepartment() != null ? student.getDepartment().getName() : "Computer Science")
                .bio(student.getBio())
                .githubUrl(student.getGithubUrl())
                .linkedinUrl(student.getLinkedinUrl())
                .portfolioUrl(student.getPortfolioUrl())
                .skills(skillNames)
                .build();
    }

    @Transactional
    public StudentProfileDto updateProfile(Long userId, StudentProfileDto dto) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        User user = student.getUser();
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        userRepository.save(user);

        if (dto.getRollNumber() != null) student.setRollNumber(dto.getRollNumber());
        if (dto.getCgpa() != null) student.setCgpa(dto.getCgpa());
        if (dto.getGraduationYear() != null) student.setGraduationYear(dto.getGraduationYear());
        if (dto.getBio() != null) student.setBio(dto.getBio());
        if (dto.getGithubUrl() != null) student.setGithubUrl(dto.getGithubUrl());
        if (dto.getLinkedinUrl() != null) student.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getPortfolioUrl() != null) student.setPortfolioUrl(dto.getPortfolioUrl());

        studentRepository.save(student);
        return getProfileByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<String> getStudentSkills(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return studentSkillRepository.findByStudentId(student.getId()).stream()
                .map(ss -> ss.getSkill().getName())
                .collect(Collectors.toList());
    }

    @Transactional
    public void addSkill(Long userId, String skillName) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Skill skill = skillRepository.findByName(skillName.trim())
                .orElseGet(() -> skillRepository.save(Skill.builder().name(skillName.trim()).category("Technical").build()));

        boolean exists = studentSkillRepository.findByStudentId(student.getId()).stream()
                .anyMatch(ss -> ss.getSkill().getId().equals(skill.getId()));

        if (!exists) {
            studentSkillRepository.save(StudentSkill.builder()
                    .student(student)
                    .skill(skill)
                    .proficiencyLevel("INTERMEDIATE")
                    .verified(true)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReadiness(UserPrincipal principal) {
        var breakdown = aiFeatureService.getPlacementReadiness(principal);
        Map<String, Object> res = new HashMap<>();
        res.put("overallScore", breakdown.getOverallScore());
        res.put("technicalScore", breakdown.getTechnicalScore());
        res.put("resumeScore", breakdown.getResumeScore());
        res.put("codingScore", breakdown.getCodingScore());
        res.put("interviewScore", breakdown.getInterviewScore());
        res.put("hasEnoughData", breakdown.getHasEnoughData());
        res.put("isEstimate", true);
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRoadmap(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        java.util.Optional<com.smartplacement.entity.LearningRoadmap> roadmapOpt =
                learningRoadmapRepository.findFirstByStudentIdOrderByCreatedAtDesc(student.getId());

        String targetRole = "Full Stack Software Engineer";
        List<Map<String, Object>> itemsList = new java.util.ArrayList<>();

        if (roadmapOpt.isPresent()) {
            targetRole = roadmapOpt.get().getTargetRole();
            List<com.smartplacement.entity.LearningRoadmapItem> items =
                    learningRoadmapItemRepository.findByRoadmapIdOrderByWeekNumberAsc(roadmapOpt.get().getId());
            for (com.smartplacement.entity.LearningRoadmapItem item : items) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", item.getId());
                m.put("weekNumber", item.getWeekNumber());
                m.put("topic", item.getTopic());
                m.put("description", item.getDescription());
                m.put("priority", item.getPriority());
                m.put("status", item.getStatus());
                itemsList.add(m);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetRole", targetRole);
        result.put("items", itemsList);
        return result;
    }

    @Transactional
    public Map<String, Object> updateRoadmapItemStatus(Long itemId, String newStatus) {
        com.smartplacement.entity.LearningRoadmapItem item = learningRoadmapItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Roadmap item not found"));
        item.setStatus(newStatus.toUpperCase());
        learningRoadmapItemRepository.save(item);

        Map<String, Object> m = new HashMap<>();
        m.put("id", item.getId());
        m.put("status", item.getStatus());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAchievements(Long userId) {
        return badgeRepository.findAll().stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("code", b.getCode());
            m.put("description", b.getDescription());
            m.put("icon", b.getIcon());
            m.put("unlocked", true);
            return m;
        }).toList();
    }

}
