# Menu and Page Audit Matrix

Audited across all 6 platform roles with seeded database accounts.

| Role | Menu Item | Route | Page Exists? | API Called | API Authorized? | Loads Real Data? | Actions Work? | Status |
| :--- | :--- | :--- | :---: | :--- | :---: | :---: | :--- | :---: |
| MENTOR | Dashboard | `/mentor/dashboard` | Yes | `GET /api/mentor/dashboard` | Yes (200) | Yes (DB) | View assigned metrics & sessions | **OK** |
| MENTOR | My Mentees | `/mentor/mentees` | Yes | `GET /api/mentor/mentees` | Yes (200) | Yes (DB) | Search, sort, filter, click mentee | **OK** |
| MENTOR | Student Detail (6 Tabs) | `/mentor/students/:id` | Yes | `GET /api/mentor/students/1` | Yes (200) | Yes (DB) | Overview, Skills, Roadmap, Applications, Assessments, Feedback write | **OK** |
| MENTOR | Progress & Roadmaps | `/mentor/progress` | Yes | `GET /api/mentor/progress` | Yes (200) | Yes (DB) | Cohort readiness & milestones | **OK** |
| MENTOR | AI Recommendations | `/mentor/recommendations` | Yes | `GET /api/mentor/recommendations` | Yes (200) | Yes (DB) | Review & generate AI advice | **OK** |
| MENTOR | Mentee Feedback | `/mentor/feedback` | Yes | `GET /api/mentor/feedback` | Yes (200) | Yes (DB) | Create & review student feedback | **OK** |
| MENTOR | Mentoring Sessions | `/mentor/activities` | Yes | `GET /api/mentor/activities` | Yes (200) | Yes (DB) | Log & manage 1-on-1 sessions | **OK** |
| FACULTY | Dashboard | `/faculty/dashboard` | Yes | `GET /api/faculty/dashboard` | Yes (200) | Yes (DB) | Cohort CGPA & readiness view | **OK** |
| FACULTY | Assigned Students | `/faculty/students` | Yes | `GET /api/faculty/students` | Yes (200) | Yes (DB) | Search, filter, view profile | **OK** |
| FACULTY | Student Detail | `/faculty/students/:id` | Yes | `GET /api/faculty/students/1` | Yes (200) | Yes (DB) | Academic & skill review | **OK** |
| FACULTY | Early Warnings | `/faculty/warnings` | Yes | `GET /api/faculty/warnings` | Yes (200) | Yes (DB) | Inspect at-risk diagnostic reasons | **OK** |
| FACULTY | Assessments | `/faculty/assessments` | Yes | `GET /api/faculty/assessments` | Yes (200) | Yes (DB) | Create & view test results | **OK** |
| TPO | Dashboard | `/tpo/dashboard` | Yes | `GET /api/tpo/dashboard` | Yes (200) | Yes (DB) | Placement stats & drive overview | **OK** |
| TPO | Placement Drives | `/tpo/drives` | Yes | `GET /api/tpo/drives` | Yes (200) | Yes (DB) | Create & close placement drives | **OK** |
| TPO | Companies | `/tpo/companies` | Yes | `GET /api/tpo/companies` | Yes (200) | Yes (DB) | Add & manage corporate partners | **OK** |
| TPO | Manage Jobs | `/tpo/jobs` | Yes | `GET /api/tpo/jobs` | Yes (200) | Yes (DB) | Toggle listing status & criteria | **OK** |
| TPO | Eligible Students | `/tpo/students` | Yes | `GET /api/tpo/eligible-students` | Yes (200) | Yes (DB) | Filter by CGPA & department | **OK** |
| TPO | Placement Analytics | `/tpo/analytics` | Yes | `GET /api/tpo/analytics` | Yes (200) | Yes (DB) | View branch & salary breakdown | **OK** |
| RECRUITER | Dashboard | `/recruiter/dashboard` | Yes | `GET /api/recruiter/dashboard` | Yes (200) | Yes (DB) | Review hiring funnel & applicants | **OK** |
| RECRUITER | Company Profile | `/recruiter/company` | Yes | `GET /api/recruiter/company` | Yes (200) | Yes (DB) | Update company bio & website | **OK** |
| RECRUITER | Job Postings | `/recruiter/jobs` | Yes | `GET /api/jobs` | Yes (200) | Yes (DB) | Create, edit & close jobs | **OK** |
| RECRUITER | Applicants | `/recruiter/applicants` | Yes | `GET /api/recruiter/applicants` | Yes (200) | Yes (DB) | Filter, shortlist, interview, hire | **OK** |
| RECRUITER | Candidate Compare | `/recruiter/compare` | Yes | `GET /api/recruiter/compare` | Yes (200) | Yes (DB) | Side-by-side skill match | **OK** |
| STUDENT | Dashboard | `/student/dashboard` | Yes | `GET /api/students/dashboard` | Yes (200) | Yes (DB) | View readiness & daily planner | **OK** |
| STUDENT | My Profile | `/student/profile` | Yes | `GET /api/students/profile` | Yes (200) | Yes (DB) | Update skills & achievements | **OK** |
| STUDENT | Resume & ATS | `/student/resume` | Yes | `GET /api/students/profile` | Yes (200) | Yes (DB) | Upload & analyze ATS score | **OK** |
| STUDENT | Browse Jobs | `/student/jobs` | Yes | `GET /api/jobs` | Yes (200) | Yes (DB) | Search, filter, view job detail | **OK** |
| STUDENT | My Applications | `/student/applications` | Yes | `GET /api/applications/my` | Yes (200) | Yes (DB) | Track stage in hiring pipeline | **OK** |
| STUDENT | Skill Gap & Roadmap | `/student/roadmap` | Yes | `GET /api/students/roadmap` | Yes (200) | Yes (DB) | Mark milestones complete | **OK** |
| STUDENT | AI Mock Interview | `/student/interview` | Yes | `GET /api/students/readiness` | Yes (200) | Yes (DB) | Answer technical questions & get feedback | **OK** |
| STUDENT | Coding Practice | `/student/coding` | Yes | `GET /api/students/skills` | Yes (200) | Yes (DB) | Execute code challenges | **OK** |
| STUDENT | Career Intelligence | `/student/career` | Yes | `GET /api/students/recommendations` | Yes (200) | Yes (DB) | Explore career paths & skills | **OK** |
| ADMIN | Dashboard | `/admin/dashboard` | Yes | `GET /api/admin/dashboard` | Yes (200) | Yes (DB) | Overview platform metrics & activity | **OK** |
| ADMIN | User Management | `/admin/users` | Yes | `GET /api/admin/users` | Yes (200) | Yes (DB) | Add, toggle active, change role, delete | **OK** |
| ADMIN | Recruiter Approvals | `/admin/approvals` | Yes | `GET /api/admin/users` | Yes (200) | Yes (DB) | Approve & reject corporate recruiters | **OK** |
| ADMIN | Student Assignments | `/admin/assignments` | Yes | `GET /api/admin/assignments` | Yes (200) | Yes (DB) | Assign students to mentors & faculty | **OK** |
| ADMIN | Academic Departments | `/admin/departments` | Yes | `GET /api/admin/departments` | Yes (200) | Yes (DB) | Add & delete departments | **OK** |
| ADMIN | Partner Companies | `/admin/companies` | Yes | `GET /api/admin/companies` | Yes (200) | Yes (DB) | Add & delete partner companies | **OK** |
| ADMIN | AI Usage & Tokens | `/admin/ai-logs` | Yes | `GET /api/admin/ai-logs` | Yes (200) | Yes (DB) | Audit LLM latency & token usage | **OK** |
| ADMIN | Audit Activity Log | `/admin/activities` | Yes | `GET /api/admin/activities` | Yes (200) | Yes (DB) | Review system security audit logs | **OK** |

## Audit Summary
- **Total Routes & Menus Audited**: 40
- **Passed (OK)**: 40
- **Broken**: 0
- **Missing**: 0
- **Ownership Enforcement**: Verified 403 Forbidden when accessing unassigned students.
- **Role-Based Guards**: Verified 403 Forbidden when unauthorized roles access protected endpoints.
- **Database Seeder**: 100% real database entity relationships (no mock/canned data).
