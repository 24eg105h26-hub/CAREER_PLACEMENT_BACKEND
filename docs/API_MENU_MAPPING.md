# API Menu Mapping (API_MENU_MAPPING.md)

## API Service Method Mapping

### Auth APIs (`authService`)
- `POST /api/auth/register` -> `authService.register()`
- `POST /api/auth/login` -> `authService.login()`
- `GET /api/auth/me` -> `authService.getCurrentUser()`

### Student APIs (`studentService`)
- `GET /api/students/profile` -> `studentService.getProfile()`
- `PUT /api/students/profile` -> `studentService.updateProfile()`
- `GET /api/students/skills` -> `studentService.getSkills()`
- `POST /api/students/skills` -> `studentService.addSkill()`
- `GET /api/students/readiness` -> `studentService.getReadiness()`
- `GET /api/students/recommendations` -> `studentService.getRecommendations()`

### Job APIs (`jobService`)
- `GET /api/jobs` -> `jobService.getJobs()`
- `GET /api/jobs/{id}` -> `jobService.getJobById()`
- `POST /api/jobs` -> `jobService.createJob()`

### Application APIs (`applicationService`)
- `POST /api/applications/{jobId}` -> `applicationService.apply()`
- `GET /api/applications/my` -> `applicationService.getMyApplications()`
- `PUT /api/applications/{id}/status` -> `applicationService.updateStatus()`

### AI APIs (`aiService`)
- `POST /api/ai/analyze-resume` -> `aiService.analyzeResume()`
- `GET /api/ai/skill-gap/{studentId}/{jobId}` -> `aiService.getSkillGap()`
- `POST /api/ai/interview/start` -> `aiService.startInterview()`
- `POST /api/ai/interview/answer` -> `aiService.submitInterviewAnswer()`
- `POST /api/ai/code-review` -> `aiService.reviewCode()`
- `POST /api/ai/career-path` -> `aiService.getCareerPath()`

### Recruiter APIs (`recruiterService`)
- `GET/PUT /api/recruiter/company` -> `recruiterService.getCompany() / updateCompany()`
- `GET /api/recruiter/applicants` -> `recruiterService.getApplicants()`
- `GET /api/recruiter/compare` -> `recruiterService.compareCandidates()`

### TPO APIs (`tpoService`)
- `GET/POST /api/tpo/drives` -> `tpoService.getDrives() / createDrive()`
- `GET /api/tpo/companies` -> `tpoService.getCompanies()`
- `GET /api/tpo/eligible-students` -> `tpoService.getEligibleStudents()`
- `GET /api/tpo/analytics` -> `tpoService.getAnalytics()`

### Faculty & Mentor APIs (`facultyService`, `mentorService`)
- `GET /api/faculty/students` -> `facultyService.getAssignedStudents()`
- `GET /api/faculty/warnings` -> `facultyService.getEarlyWarnings()`
- `GET /api/mentor/mentees` -> `mentorService.getMentees()`
- `GET /api/mentor/progress` -> `mentorService.getProgress()`

### Admin APIs (`adminService`)
- `GET/POST /api/admin/users` -> `adminService.getUsers() / createUser()`
- `PUT /api/admin/users/{id}/approval` -> `adminService.updateApprovalStatus()`
- `GET/POST /api/admin/departments` -> `adminService.getDepartments() / createDepartment()`
- `GET /api/admin/ai-logs` -> `adminService.getAiLogs()`

