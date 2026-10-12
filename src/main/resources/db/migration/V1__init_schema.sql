-- Flyway Migration V1: Initial Schema for SmartPlacement AI

-- 1. Departments
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Companies
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    website VARCHAR(255),
    location VARCHAR(150),
    description TEXT,
    logo_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Users (Exactly 6 roles: ADMIN, TPO, FACULTY, STUDENT, RECRUITER, MENTOR)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(30) NOT NULL,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Students
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT,
    roll_number VARCHAR(50) UNIQUE,
    cgpa DOUBLE DEFAULT 0.0,
    graduation_year INT,
    college_name VARCHAR(150),
    bio TEXT,
    github_url VARCHAR(255),
    linkedin_url VARCHAR(255),
    portfolio_url VARCHAR(255),
    phone VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_students_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_students_dept (department_id),
    INDEX idx_students_cgpa (cgpa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Faculty
CREATE TABLE IF NOT EXISTS faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT,
    designation VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_faculty_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Recruiters
CREATE TABLE IF NOT EXISTS recruiters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    company_id BIGINT,
    designation VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_recruiters_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recruiters_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Mentors
CREATE TABLE IF NOT EXISTS mentors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(150),
    expertise VARCHAR(255),
    designation VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Skills Metadata Catalog
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    difficulty VARCHAR(30) DEFAULT 'MEDIUM',
    duration_weeks INT DEFAULT 4,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_skills_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Student Skills
CREATE TABLE IF NOT EXISTS student_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level VARCHAR(30) DEFAULT 'INTERMEDIATE',
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_skills_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_skill (student_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Student Projects
CREATE TABLE IF NOT EXISTS student_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    repo_url VARCHAR(255),
    live_url VARCHAR(255),
    technologies VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Certifications
CREATE TABLE IF NOT EXISTS certifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    issuing_organization VARCHAR(150),
    issue_date DATE,
    credential_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_certifications_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Achievements
CREATE TABLE IF NOT EXISTS achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    date_achieved DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_achievements_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Jobs
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(150),
    job_type VARCHAR(50) DEFAULT 'FULL_TIME',
    salary_range VARCHAR(100),
    min_cgpa DOUBLE DEFAULT 0.0,
    experience_years INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'OPEN',
    deadline DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jobs_recruiter FOREIGN KEY (recruiter_id) REFERENCES recruiters(id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_jobs_status (status),
    INDEX idx_jobs_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Job Skills (Weighted)
CREATE TABLE IF NOT EXISTS job_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    skill_type VARCHAR(30) DEFAULT 'REQUIRED', -- REQUIRED, PREFERRED
    weight INT DEFAULT 3, -- 3 for REQUIRED, 1 for PREFERRED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE KEY uk_job_skill (job_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Applications
CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    status VARCHAR(30) DEFAULT 'APPLIED', -- APPLIED, SHORTLISTED, INTERVIEW, SELECTED, REJECTED
    match_score DOUBLE DEFAULT 0.0,
    match_details_json JSON,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_apps_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_apps_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_job_app (student_id, job_id),
    INDEX idx_apps_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Application Status History
CREATE TABLE IF NOT EXISTS application_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    comment TEXT,
    changed_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_history_app FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Interview Schedules (Real Recruiter Interviews)
CREATE TABLE IF NOT EXISTS interview_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    interview_date TIMESTAMP NOT NULL,
    mode VARCHAR(50) DEFAULT 'ONLINE',
    link_or_location VARCHAR(255),
    notes TEXT,
    status VARCHAR(30) DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interviews_app FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Resumes
CREATE TABLE IF NOT EXISTS resumes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    extracted_text LONGTEXT,
    parsed_data_json JSON,
    resume_score DOUBLE DEFAULT 0.0,
    ats_score DOUBLE DEFAULT 0.0,
    tech_score DOUBLE DEFAULT 0.0,
    project_score DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resumes_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. AI Analysis Persisted Cache
CREATE TABLE IF NOT EXISTS ai_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    analysis_type VARCHAR(50) NOT NULL,
    input_reference VARCHAR(255),
    result_json JSON NOT NULL,
    model VARCHAR(50),
    tokens_used INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_analysis_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. Assessments
CREATE TABLE IF NOT EXISTS assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    topic VARCHAR(100),
    duration_minutes INT DEFAULT 30,
    total_marks INT DEFAULT 100,
    created_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. Assessment Questions
CREATE TABLE IF NOT EXISTS assessment_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_option CHAR(1) NOT NULL,
    marks INT DEFAULT 10,
    CONSTRAINT fk_questions_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. Assessment Results
CREATE TABLE IF NOT EXISTS assessment_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    score INT NOT NULL,
    total_marks INT NOT NULL,
    percentage DOUBLE NOT NULL,
    status VARCHAR(30) DEFAULT 'COMPLETED',
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_results_assessment FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23. Learning Roadmaps
CREATE TABLE IF NOT EXISTS learning_roadmaps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    target_role VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_roadmaps_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. Learning Roadmap Items
CREATE TABLE IF NOT EXISTS learning_roadmap_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    roadmap_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    topic VARCHAR(150) NOT NULL,
    description TEXT,
    priority VARCHAR(30) DEFAULT 'MEDIUM', -- HIGH, MEDIUM, LOW
    duration_weeks INT DEFAULT 1,
    resource_link VARCHAR(255),
    status VARCHAR(30) DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, COMPLETED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_roadmap_items_roadmap FOREIGN KEY (roadmap_id) REFERENCES learning_roadmaps(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. Learning Recommendations
CREATE TABLE IF NOT EXISTS learning_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    priority VARCHAR(30) DEFAULT 'HIGH',
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recs_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_recs_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. Mock Interviews
CREATE TABLE IF NOT EXISTS mock_interviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    target_role VARCHAR(150) NOT NULL,
    interview_type VARCHAR(50) DEFAULT 'TECHNICAL', -- TECHNICAL, HR, BEHAVIORAL, MIXED
    difficulty VARCHAR(30) DEFAULT 'INTERMEDIATE', -- BEGINNER, INTERMEDIATE, ADVANCED
    overall_score DOUBLE DEFAULT 0.0,
    feedback TEXT,
    status VARCHAR(30) DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mock_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. Mock Interview Questions
CREATE TABLE IF NOT EXISTS mock_interview_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mock_interview_id BIGINT NOT NULL,
    question_number INT NOT NULL,
    question_text TEXT NOT NULL,
    user_answer TEXT,
    evaluation_feedback TEXT,
    score DOUBLE DEFAULT 0.0,
    technical_score DOUBLE DEFAULT 0.0,
    communication_score DOUBLE DEFAULT 0.0,
    CONSTRAINT fk_mock_q_interview FOREIGN KEY (mock_interview_id) REFERENCES mock_interviews(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. Coding Problems
CREATE TABLE IF NOT EXISTS coding_problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    topic VARCHAR(100) NOT NULL,
    difficulty VARCHAR(30) DEFAULT 'EASY', -- EASY, MEDIUM, HARD
    hints_json JSON,
    test_cases_json JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 29. Coding Submissions
CREATE TABLE IF NOT EXISTS coding_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    code_content TEXT NOT NULL,
    language VARCHAR(30) DEFAULT 'java',
    score DOUBLE DEFAULT 0.0,
    feedback TEXT,
    status VARCHAR(30) DEFAULT 'PASSED',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_code_sub_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_code_sub_problem FOREIGN KEY (problem_id) REFERENCES coding_problems(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 30. Placement Drives
CREATE TABLE IF NOT EXISTS placement_drives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    job_id BIGINT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    drive_date DATE NOT NULL,
    location VARCHAR(150),
    min_cgpa DOUBLE DEFAULT 0.0,
    status VARCHAR(30) DEFAULT 'UPCOMING', -- UPCOMING, ONGOING, COMPLETED, CANCELLED
    deadline DATE,
    created_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drives_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_drives_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 31. Placement Drive Departments
CREATE TABLE IF NOT EXISTS placement_drive_departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    CONSTRAINT fk_drive_dept_drive FOREIGN KEY (drive_id) REFERENCES placement_drives(id) ON DELETE CASCADE,
    CONSTRAINT fk_drive_dept_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    UNIQUE KEY uk_drive_dept (drive_id, department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 32. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'GENERAL',
    is_read BOOLEAN DEFAULT FALSE,
    link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 33. Career Paths
CREATE TABLE IF NOT EXISTS career_paths (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    target_role VARCHAR(150) NOT NULL,
    stages_json JSON NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_career_path_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 34. Daily Tasks
CREATE TABLE IF NOT EXISTS daily_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, COMPLETED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_task_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_daily_tasks_date (student_id, task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 35. Feedbacks (Faculty/Mentor feedback to student)
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    comment TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedbacks_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedbacks_author FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 36. Faculty Assignments
CREATE TABLE IF NOT EXISTS faculty_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fac_assign_fac FOREIGN KEY (faculty_id) REFERENCES faculty(id) ON DELETE CASCADE,
    CONSTRAINT fk_fac_assign_stu FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    UNIQUE KEY uk_faculty_student (faculty_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 37. Mentor Assignments
CREATE TABLE IF NOT EXISTS mentor_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mentor_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_men_assign_men FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    CONSTRAINT fk_men_assign_stu FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    UNIQUE KEY uk_mentor_student (mentor_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 38. Badges Catalog
CREATE TABLE IF NOT EXISTS badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    icon VARCHAR(100),
    code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 39. Student Badges
CREATE TABLE IF NOT EXISTS student_badges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    awarded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stu_badges_stu FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_stu_badges_badge FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_badge (student_id, badge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 40. Streaks
CREATE TABLE IF NOT EXISTS streaks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE,
    learning_streak_days INT DEFAULT 0,
    coding_streak_days INT DEFAULT 0,
    last_activity_date DATE,
    CONSTRAINT fk_streaks_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 41. AI Usage Log
CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    feature_name VARCHAR(100) NOT NULL,
    model VARCHAR(50),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_logs_feature (feature_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 42. Activity Log
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_act_logs_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
