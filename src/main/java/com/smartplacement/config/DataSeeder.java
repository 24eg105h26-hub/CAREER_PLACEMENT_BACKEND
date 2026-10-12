package com.smartplacement.config;

import com.smartplacement.entity.*;
import com.smartplacement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final RecruiterRepository recruiterRepository;
    private final MentorRepository mentorRepository;
    private final SkillRepository skillRepository;
    private final BadgeRepository badgeRepository;
    private final FacultyAssignmentRepository facultyAssignmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final ApplicationRepository applicationRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final FeedbackRepository feedbackRepository;
    private final LearningRoadmapRepository learningRoadmapRepository;
    private final LearningRoadmapItemRepository learningRoadmapItemRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.default-password:Password123!}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            log.info("Data seeding is disabled.");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("Data already seeded. Skipping initial seeding.");
            return;
        }

        log.info("Seeding initial rich application data...");
        String encodedPassword = passwordEncoder.encode(defaultPassword);

        // 1. Seed Departments
        Department cse = departmentRepository.save(Department.builder().name("Computer Science & Engineering").code("CSE").build());
        Department it = departmentRepository.save(Department.builder().name("Information Technology").code("IT").build());
        Department ece = departmentRepository.save(Department.builder().name("Electronics & Communication").code("ECE").build());
        Department ds = departmentRepository.save(Department.builder().name("Data Science").code("DS").build());

        // 2. Seed Companies
        Company google = companyRepository.save(Company.builder().name("Google").website("https://google.com").location("Mountain View, CA / Hyderabad").description("Global technology leader in cloud, search, and AI").build());
        Company microsoft = companyRepository.save(Company.builder().name("Microsoft").website("https://microsoft.com").location("Redmond, WA / Bengaluru").description("Software and cloud solutions provider").build());
        Company amazon = companyRepository.save(Company.builder().name("Amazon").website("https://amazon.com").location("Seattle, WA / Hyderabad").description("E-commerce, logistics and cloud computing").build());
        Company tcs = companyRepository.save(Company.builder().name("TCS").website("https://tcs.com").location("Mumbai, India").description("Global IT services and consulting enterprise").build());

        // 3. Seed Skills Metadata
        Skill java = skillRepository.save(Skill.builder().name("Java").category("Backend").difficulty("INTERMEDIATE").durationWeeks(4).build());
        Skill springBoot = skillRepository.save(Skill.builder().name("Spring Boot").category("Backend").difficulty("INTERMEDIATE").durationWeeks(4).build());
        Skill react = skillRepository.save(Skill.builder().name("React").category("Frontend").difficulty("INTERMEDIATE").durationWeeks(3).build());
        Skill mysql = skillRepository.save(Skill.builder().name("MySQL").category("Database").difficulty("BEGINNER").durationWeeks(2).build());
        Skill docker = skillRepository.save(Skill.builder().name("Docker").category("DevOps").difficulty("INTERMEDIATE").durationWeeks(2).build());
        Skill git = skillRepository.save(Skill.builder().name("Git").category("Tools").difficulty("BEGINNER").durationWeeks(1).build());
        Skill python = skillRepository.save(Skill.builder().name("Python").category("Data / AI").difficulty("BEGINNER").durationWeeks(3).build());
        Skill dsa = skillRepository.save(Skill.builder().name("Data Structures & Algorithms").category("CS Fundamentals").difficulty("ADVANCED").durationWeeks(6).build());

        // 4. Seed Badges
        badgeRepository.save(Badge.builder().name("7-Day Coding Streak").code("STREAK_7").description("Completed coding practice 7 days in a row").icon("flame").build());
        badgeRepository.save(Badge.builder().name("Resume Master").code("RESUME_MASTER").description("Achieved an ATS score above 80%").icon("file-text").build());
        badgeRepository.save(Badge.builder().name("Interview Ready").code("INTERVIEW_READY").description("Completed at least 3 mock interviews").icon("video").build());
        badgeRepository.save(Badge.builder().name("Skill Builder").code("SKILL_BUILDER").description("Verified 5 or more technical skills").icon("award").build());

        // 5. Seed Core Role Users
        // Admin
        User adminUser = userRepository.save(User.builder()
                .email("admin@smartplacement.ai")
                .passwordHash(encodedPassword)
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .approvalStatus(ApprovalStatus.APPROVED)
                .enabled(true)
                .build());

        // TPO
        User tpoUser = userRepository.save(User.builder()
                .email("tpo@smartplacement.ai")
                .passwordHash(encodedPassword)
                .fullName("Dr. Robert TPO")
                .phone("+1 555-0101")
                .role(Role.TPO)
                .approvalStatus(ApprovalStatus.APPROVED)
                .enabled(true)
                .build());

        // Faculty
        User facultyUser = userRepository.save(User.builder()
                .email("faculty@smartplacement.ai")
                .passwordHash(encodedPassword)
                .fullName("Prof. Alan Turing")
                .phone("+1 555-0102")
                .role(Role.FACULTY)
                .approvalStatus(ApprovalStatus.APPROVED)
                .enabled(true)
                .build());

        Faculty faculty = facultyRepository.save(Faculty.builder()
                .user(facultyUser)
                .department(cse)
                .designation("Associate Professor & HOD")
                .build());

        // Mentor
        User mentorUser = userRepository.save(User.builder()
                .email("mentor@smartplacement.ai")
                .passwordHash(encodedPassword)
                .fullName("Sarah Jenkins")
                .phone("+1 555-0103")
                .role(Role.MENTOR)
                .approvalStatus(ApprovalStatus.APPROVED)
                .enabled(true)
                .build());

        Mentor mentor = mentorRepository.save(Mentor.builder()
                .user(mentorUser)
                .companyName("Google")
                .expertise("Backend Systems, Distributed Cloud Architecture")
                .designation("Senior Staff Software Engineer")
                .build());

        // Recruiter
        User recruiterUser = userRepository.save(User.builder()
                .email("recruiter@smartplacement.ai")
                .passwordHash(encodedPassword)
                .fullName("Michael Scott")
                .phone("+1 555-0105")
                .role(Role.RECRUITER)
                .approvalStatus(ApprovalStatus.APPROVED)
                .enabled(true)
                .build());

        Recruiter recruiter = recruiterRepository.save(Recruiter.builder()
                .user(recruiterUser)
                .company(google)
                .designation("Lead Tech Talent Acquisition Manager")
                .build());

        // 6. Seed 6 Students at Varied Readiness Levels
        // Student 1: Alex Chen (Ready - 88%)
        User u1 = userRepository.save(User.builder().email("student@smartplacement.ai").passwordHash(encodedPassword).fullName("Alex Chen").phone("+1 555-0104").role(Role.STUDENT).approvalStatus(ApprovalStatus.APPROVED).enabled(true).build());
        Student s1 = studentRepository.save(Student.builder().user(u1).department(cse).rollNumber("2024CSE101").cgpa(8.8).graduationYear(2025).collegeName("National Institute of Technology").bio("Backend enthusiast specializing in Java, Spring Boot, and Redis.").githubUrl("https://github.com/alexchen").linkedinUrl("https://linkedin.com/in/alexchen").build());

        // Student 2: Priya Sharma (Top Performer - 94%)
        User u2 = userRepository.save(User.builder().email("priya.sharma@smartplacement.ai").passwordHash(encodedPassword).fullName("Priya Sharma").phone("+1 555-0106").role(Role.STUDENT).approvalStatus(ApprovalStatus.APPROVED).enabled(true).build());
        Student s2 = studentRepository.save(Student.builder().user(u2).department(cse).rollNumber("2024CSE102").cgpa(9.4).graduationYear(2025).collegeName("National Institute of Technology").bio("Full-stack engineer and competitive programmer with 500+ LeetCode solutions.").githubUrl("https://github.com/priyasharma").linkedinUrl("https://linkedin.com/in/priyasharma").build());

        // Student 3: Rohan Verma (Intermediate - 72%)
        User u3 = userRepository.save(User.builder().email("rohan.verma@smartplacement.ai").passwordHash(encodedPassword).fullName("Rohan Verma").phone("+1 555-0107").role(Role.STUDENT).approvalStatus(ApprovalStatus.APPROVED).enabled(true).build());
        Student s3 = studentRepository.save(Student.builder().user(u3).department(it).rollNumber("2024IT201").cgpa(7.6).graduationYear(2025).collegeName("National Institute of Technology").bio("Frontend and cloud technologies explorer. Actively studying Spring Data.").githubUrl("https://github.com/rohanverma").linkedinUrl("https://linkedin.com/in/rohanverma").build());

        // Student 4: Ananya Patel (At-Risk - 48%)
        User u4 = userRepository.save(User.builder().email("ananya.patel@smartplacement.ai").passwordHash(encodedPassword).fullName("Ananya Patel").phone("+1 555-0108").role(Role.STUDENT).approvalStatus(ApprovalStatus.APPROVED).enabled(true).build());
        Student s4 = studentRepository.save(Student.builder().user(u4).department(ece).rollNumber("2024ECE301").cgpa(6.4).graduationYear(2025).collegeName("National Institute of Technology").bio("Hardware to software transitioner needing extra guidance on data structures.").build());

        // Student 5: Devika Nair (Strong - 82%)
        User u5 = userRepository.save(User.builder().email("devika.nair@smartplacement.ai").passwordHash(encodedPassword).fullName("Devika Nair").phone("+1 555-0109").role(Role.STUDENT).approvalStatus(ApprovalStatus.APPROVED).enabled(true).build());
        Student s5 = studentRepository.save(Student.builder().user(u5).department(ds).rollNumber("2024DS401").cgpa(8.2).graduationYear(2025).collegeName("National Institute of Technology").bio("Data science and machine learning engineer with Python, SQL, and Docker experience.").build());

        // Student 6: Vikram Singh (Critical At-Risk - 38%)
        User u6 = userRepository.save(User.builder().email("vikram.singh@smartplacement.ai").passwordHash(encodedPassword).fullName("Vikram Singh").phone("+1 555-0110").role(Role.STUDENT).approvalStatus(ApprovalStatus.APPROVED).enabled(true).build());
        Student s6 = studentRepository.save(Student.builder().user(u6).department(cse).rollNumber("2024CSE105").cgpa(5.7).graduationYear(2025).collegeName("National Institute of Technology").bio("Working hard to improve semester scores and prepare for campus placement rounds.").build());

        // 7. Assign ALL 6 Students to Mentor and Faculty
        for (Student s : List.of(s1, s2, s3, s4, s5, s6)) {
            facultyAssignmentRepository.save(FacultyAssignment.builder().faculty(faculty).student(s).build());
            mentorAssignmentRepository.save(MentorAssignment.builder().mentor(mentor).student(s).build());
        }

        // 8. Seed Student Skills
        studentSkillRepository.save(StudentSkill.builder().student(s1).skill(java).proficiencyLevel("ADVANCED").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s1).skill(springBoot).proficiencyLevel("ADVANCED").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s1).skill(mysql).proficiencyLevel("INTERMEDIATE").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s1).skill(docker).proficiencyLevel("INTERMEDIATE").verified(false).build());

        studentSkillRepository.save(StudentSkill.builder().student(s2).skill(java).proficiencyLevel("ADVANCED").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s2).skill(react).proficiencyLevel("ADVANCED").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s2).skill(dsa).proficiencyLevel("ADVANCED").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s2).skill(python).proficiencyLevel("INTERMEDIATE").verified(true).build());

        studentSkillRepository.save(StudentSkill.builder().student(s3).skill(react).proficiencyLevel("INTERMEDIATE").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s3).skill(git).proficiencyLevel("INTERMEDIATE").verified(true).build());

        studentSkillRepository.save(StudentSkill.builder().student(s4).skill(python).proficiencyLevel("BEGINNER").verified(false).build());

        studentSkillRepository.save(StudentSkill.builder().student(s5).skill(python).proficiencyLevel("ADVANCED").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s5).skill(mysql).proficiencyLevel("INTERMEDIATE").verified(true).build());
        studentSkillRepository.save(StudentSkill.builder().student(s5).skill(docker).proficiencyLevel("BEGINNER").verified(false).build());

        studentSkillRepository.save(StudentSkill.builder().student(s6).skill(git).proficiencyLevel("BEGINNER").verified(false).build());

        // 9. Seed Jobs
        Job j1 = jobRepository.save(Job.builder()
                .recruiter(recruiter)
                .company(google)
                .title("Software Engineer - Core Backend")
                .description("Design high-throughput distributed backend services using Java, Spring, and Cloud native tools.")
                .location("Hyderabad / Bengaluru")
                .jobType("FULL_TIME")
                .salaryRange("₹24,00,000 - ₹32,00,000")
                .minCgpa(8.0)
                .status("OPEN")
                .deadline(LocalDate.now().plusDays(45))
                .build());

        Job j2 = jobRepository.save(Job.builder()
                .recruiter(recruiter)
                .company(microsoft)
                .title("Cloud Infrastructure Engineer")
                .description("Build and monitor resilient microservices on Azure. Docker, Kubernetes, and C#/Java required.")
                .location("Bengaluru, India")
                .jobType("FULL_TIME")
                .salaryRange("₹22,00,000 - ₹28,00,000")
                .minCgpa(7.5)
                .status("OPEN")
                .deadline(LocalDate.now().plusDays(30))
                .build());

        Job j3 = jobRepository.save(Job.builder()
                .recruiter(recruiter)
                .company(amazon)
                .title("Software Development Engineer (SDE-1)")
                .description("Scale catalog and checkout microservices handling millions of daily customer requests.")
                .location("Hyderabad, India")
                .jobType("FULL_TIME")
                .salaryRange("₹26,00,000 - ₹34,00,000")
                .minCgpa(8.0)
                .status("OPEN")
                .deadline(LocalDate.now().plusDays(60))
                .build());

        Job j4 = jobRepository.save(Job.builder()
                .recruiter(recruiter)
                .company(tcs)
                .title("Systems Engineer - Digital Transformation")
                .description("Enterprise application development with Spring Boot, SQL databases, and modern web frameworks.")
                .location("Pan India / Mumbai")
                .jobType("FULL_TIME")
                .salaryRange("₹7,50,000 - ₹11,00,000")
                .minCgpa(6.0)
                .status("OPEN")
                .deadline(LocalDate.now().plusDays(20))
                .build());

        // Job skills
        jobSkillRepository.save(JobSkill.builder().job(j1).skill(java).skillType("REQUIRED").weight(3).build());
        jobSkillRepository.save(JobSkill.builder().job(j1).skill(springBoot).skillType("REQUIRED").weight(3).build());
        jobSkillRepository.save(JobSkill.builder().job(j2).skill(docker).skillType("REQUIRED").weight(3).build());
        jobSkillRepository.save(JobSkill.builder().job(j3).skill(dsa).skillType("REQUIRED").weight(3).build());
        jobSkillRepository.save(JobSkill.builder().job(j4).skill(mysql).skillType("PREFERRED").weight(1).build());

        // 10. Seed Applications across all statuses
        applicationRepository.save(Application.builder().student(s1).job(j1).status("SHORTLISTED").matchScore(86.5).build());
        applicationRepository.save(Application.builder().student(s1).job(j2).status("APPLIED").matchScore(78.0).build());
        applicationRepository.save(Application.builder().student(s2).job(j3).status("SELECTED").matchScore(96.0).build());
        applicationRepository.save(Application.builder().student(s3).job(j4).status("INTERVIEW").matchScore(74.0).build());
        applicationRepository.save(Application.builder().student(s4).job(j4).status("APPLIED").matchScore(54.0).build());
        applicationRepository.save(Application.builder().student(s5).job(j1).status("REJECTED").matchScore(46.0).build());

        // 11. Seed Placement Drives
        placementDriveRepository.save(PlacementDrive.builder()
                .company(google)
                .job(j1)
                .title("Google Campus Placement Drive 2026")
                .description("On-campus technical screening, coding assessments, and architectural interview rounds.")
                .driveDate(LocalDate.now().plusDays(14))
                .location("Main Campus Auditorium & Lab 3")
                .minCgpa(8.0)
                .status("ONGOING")
                .createdByUserId(tpoUser.getId())
                .build());

        placementDriveRepository.save(PlacementDrive.builder()
                .company(microsoft)
                .job(j2)
                .title("Microsoft University Hiring Program")
                .description("Virtual coding challenge followed by technical interview tracks for 2025/2026 graduates.")
                .driveDate(LocalDate.now().plusDays(28))
                .location("Virtual Interview Rooms (MS Teams)")
                .minCgpa(7.5)
                .status("UPCOMING")
                .createdByUserId(tpoUser.getId())
                .build());

        placementDriveRepository.save(PlacementDrive.builder()
                .company(tcs)
                .job(j4)
                .title("TCS Digital & Ninja Mass Placement Drive")
                .description("Aptitude test, technical interview, and HR interview round for all engineering branches.")
                .driveDate(LocalDate.now().minusDays(10))
                .location("CSE Seminar Hall")
                .minCgpa(6.0)
                .status("COMPLETED")
                .createdByUserId(tpoUser.getId())
                .build());

        // 12. Seed Learning Roadmaps & Items
        LearningRoadmap r1 = learningRoadmapRepository.save(LearningRoadmap.builder().student(s1).targetRole("Senior Java Backend Engineer").build());
        learningRoadmapItemRepository.save(LearningRoadmapItem.builder().roadmap(r1).weekNumber(1).topic("JVM Memory & Garbage Collection").description("Master Heap spaces, G1 vs ZGC pause times").status("COMPLETED").priority("HIGH").build());
        learningRoadmapItemRepository.save(LearningRoadmapItem.builder().roadmap(r1).weekNumber(2).topic("Spring Boot Data JPA Optimization").description("Eliminate N+1 queries using EntityGraphs and JPQL fetch joins").status("COMPLETED").priority("HIGH").build());
        learningRoadmapItemRepository.save(LearningRoadmapItem.builder().roadmap(r1).weekNumber(3).topic("Distributed Caching with Redis").description("Implement Cache-Aside and Redis Distributed Locking").status("IN_PROGRESS").priority("HIGH").build());
        learningRoadmapItemRepository.save(LearningRoadmapItem.builder().roadmap(r1).weekNumber(4).topic("Message Queues with Apache Kafka").description("Partition keys, consumer lag monitoring, and idempotency").status("NOT_STARTED").priority("MEDIUM").build());

        LearningRoadmap r4 = learningRoadmapRepository.save(LearningRoadmap.builder().student(s4).targetRole("Junior Software Developer").build());
        learningRoadmapItemRepository.save(LearningRoadmapItem.builder().roadmap(r4).weekNumber(1).topic("Data Structures Foundations").description("Arrays, Linked Lists, Stacks, Queues").status("IN_PROGRESS").priority("HIGH").build());
        learningRoadmapItemRepository.save(LearningRoadmapItem.builder().roadmap(r4).weekNumber(2).topic("SQL Relational Queries").description("Joins, Group By, Subqueries, Aggregations").status("NOT_STARTED").priority("MEDIUM").build());

        // 13. Seed Feedback
        feedbackRepository.save(Feedback.builder()
                .student(s1)
                .authorUser(mentorUser)
                .comment("Alex delivered an outstanding mock interview response on Spring transaction boundaries and concurrency. Recommended reviewing distributed transaction 2PC vs Saga patterns.")
                .category("MOCK_INTERVIEW")
                .build());

        feedbackRepository.save(Feedback.builder()
                .student(s1)
                .authorUser(facultyUser)
                .comment("Solid academic standing and consistent lab assignments. Ready for tier-1 company rounds.")
                .category("ACADEMIC")
                .build());

        feedbackRepository.save(Feedback.builder()
                .student(s4)
                .authorUser(mentorUser)
                .comment("Ananya needs intensive practice on linked list traversal and basic time complexities before company screening rounds next month.")
                .category("EARLY_WARNING")
                .build());

        feedbackRepository.save(Feedback.builder()
                .student(s6)
                .authorUser(facultyUser)
                .comment("Critical CGPA (5.7). Student must clear pending backlogs to meet the minimum eligibility threshold for on-campus drives.")
                .category("EARLY_WARNING")
                .build());

        // 14. Seed Mentoring Activities / Session Logs
        activityLogRepository.save(ActivityLog.builder()
                .userId(mentorUser.getId())
                .action("MOCK_INTERVIEW")
                .entityType("STUDENT")
                .entityId(s1.getId())
                .details("Completed 45-minute live technical mock interview on Spring Boot & System Design. Score: 95/100.")
                .build());

        activityLogRepository.save(ActivityLog.builder()
                .userId(mentorUser.getId())
                .action("RESUME_REVIEW")
                .entityType("STUDENT")
                .entityId(s2.getId())
                .details("Reviewed ATS format and project highlights. ATS score boosted to 92%.")
                .build());

        activityLogRepository.save(ActivityLog.builder()
                .userId(mentorUser.getId())
                .action("SESSION_SCHEDULED")
                .entityType("STUDENT")
                .entityId(s4.getId())
                .details("Upcoming 1-on-1 coaching session scheduled for DSA fundamentals.")
                .build());

        // 15. Seed Assessments & Results
        Assessment a1 = assessmentRepository.save(Assessment.builder()
                .title("Core Java & Concurrency Benchmark")
                .description("30-minute timed quiz covering OOP, Thread pools, ConcurrentHashMap, and Java 21 features.")
                .topic("Java & Concurrency")
                .durationMinutes(30)
                .totalMarks(100)
                .createdByUserId(facultyUser.getId())
                .build());

        Assessment a2 = assessmentRepository.save(Assessment.builder()
                .title("Algorithms & Data Structures Placement Quiz")
                .description("Assessment on sorting, dynamic programming, and binary search tree complexities.")
                .topic("DSA")
                .durationMinutes(45)
                .totalMarks(100)
                .createdByUserId(facultyUser.getId())
                .build());

        assessmentResultRepository.save(AssessmentResult.builder().student(s1).assessment(a1).score(92).totalMarks(100).percentage(92.0).status("COMPLETED").build());
        assessmentResultRepository.save(AssessmentResult.builder().student(s2).assessment(a1).score(98).totalMarks(100).percentage(98.0).status("COMPLETED").build());
        assessmentResultRepository.save(AssessmentResult.builder().student(s3).assessment(a1).score(74).totalMarks(100).percentage(74.0).status("COMPLETED").build());
        assessmentResultRepository.save(AssessmentResult.builder().student(s4).assessment(a1).score(44).totalMarks(100).percentage(44.0).status("FAILED").build());

        // 16. Seed Notifications
        notificationRepository.save(Notification.builder()
                .user(u1)
                .title("Interview Shortlist Notice")
                .message("Congratulations! You have been shortlisted for the Google Core Backend role. Technical interview scheduled soon.")
                .type("PLACEMENT")
                .link("/student/applications")
                .build());

        notificationRepository.save(Notification.builder()
                .user(u1)
                .title("Mentor Feedback Received")
                .message("Sarah Jenkins provided detailed review notes on your Mock Interview.")
                .type("MENTOR")
                .link("/student/interview")
                .build());

        notificationRepository.save(Notification.builder()
                .user(u4)
                .title("Academic Advisory Warning")
                .message("Your CGPA and assessment scores are currently below target. Please schedule a mentorship session.")
                .type("WARNING")
                .link("/student/roadmap")
                .build());

        log.info("Comprehensive data seeding completed successfully! 6 students, jobs, applications, drives, feedback, and assessments created.");
    }
}