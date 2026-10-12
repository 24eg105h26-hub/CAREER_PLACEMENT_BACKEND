# SmartPlacement AI - Development Plan

## Overview
SmartPlacement AI is an AI-powered student placement and career intelligence platform built with Java 17, Spring Boot 3.x, MySQL 8, Flyway, React 18 (Vite), plain CSS design system, and AI integration.

## Architectural Principles
1. **Vertical Slice Development**: Implement end-to-end features phase by phase.
2. **Deterministic Logic in Java**: Job-match score, skill-gap priority, eligibility, readiness score, and analytics formulas are strictly computed in code.
3. **Structured AI Integration**: AI output requested in strict JSON, validated with schemas, with dev fallback capability.
4. **Security & Ownership**: Role-based access (ADMIN, TPO, FACULTY, STUDENT, RECRUITER, MENTOR) with service-layer data ownership checks.
5. **Real Data Integrity**: All UI metrics and charts are generated from actual backend data.

## Execution Phases

### Phase 1: Setup & Infrastructure (Current)
- Workspace structure: `backend/` and `frontend/`.
- MySQL database `smartplacement` setup and Flyway initialization.
- Spring Boot 3 dependencies & application configuration.
- Vite + React 18 scaffold with Lucide icons, Recharts, @fontsource/inter.
- Base Docker Compose configuration.

### Phase 2: Auth & Security Engine
- User entity, exact 6 roles, status handling (e.g., RECRUITER pending approval).
- BCrypt password hashing, JWT provider (access token with claims), JwtAuthenticationFilter.
- Method security (`@PreAuthorize`) + service-level ownership validation.
- Standardized API envelope (`success`, `message`, `data`, `timestamp`, `errors`).
- Auth endpoints: `/api/auth/register`, `/api/auth/login`, `/api/auth/me`.
- Auth unit & integration tests.

### Phase 3: Design System & Application Shell
- Custom CSS design system (`tokens.css`, `base.css`, `layout.css`, `components.css`, `utilities.css`).
- Inter font, strict Slate/Indigo palette, light & dark theme toggle.
- UI components: StatCard, ProgressRing, DataTable, Modal, Toast, Pipeline, AIAssistant, etc.
- Component preview page (`/dev/design-system`).
- SaaS Landing page (`/`), Login & Register forms.
- App Shell: Sidebar navigation, Navbar header, Role-based route guards.

### Phase 4: Core Placement Lifecycle
- Flyway migrations for core domain tables (User, Student, Recruiter, Faculty, Mentor, Company, Department, Skill, Job, JobSkill, Application, ApplicationStatusHistory, Drive, Notification).
- Student Profile CRUD, Resume upload metadata.
- Recruiter Company profile, Job posting with weighted skills (3:1 required/preferred).
- Application pipeline (Apply -> Shortlist -> Interview -> Select/Reject).
- TPO Placement Drives with eligibility calculation.
- Faculty/Mentor student assignments.

### Phase 5: AI Engine Architecture & Core Intelligence
- Provider-neutral `AiService` supporting Live, Dev Fallback, Disabled modes.
- Explainable formulas:
  - Job Match Score: `Σ weight(matched) / Σ weight(all) * 100` (Required 3x, Preferred 1x).
  - Skill-gap priority (HIGH for missing required, MEDIUM for missing preferred).
  - Placement Readiness score formula with missing data indicators.
- AI Resume Analyzer (PDFBox text extraction -> AI parse -> skills sync option).
- Job Description Analyzer & AI Skill-Gap Analyzer.
- AI Learning Roadmap Generator (week-by-week actionable items).

### Phase 6: Career Prep Suite & AI Tools
- AI Mock Interviewer (Questions & structured evaluation).
- AI Coding Practice (Problem generator & AI code reviewer).
- Career Path Generator & Daily Task Planner.
- Resume Builder & ATS Simulator (with disclaimer).
- Cover Letter Generator, Communication Analyzer, GitHub Analyzer.
- AI Study Buddy (context-aware assistant).
- Streak tracking & Badge gamification.

### Phase 7: Role Analytics & Early-Warning System
- Admin: System metrics, user growth, AI token usage log.
- TPO: Placement rates, department breakdown, skill demand trends.
- Recruiter: Hiring funnel, applicant comparison matrix.
- Faculty & Mentor: Explainable Early-Warning flags (low readiness, inactive, missing resume, low CGPA).

### Phase 8: UI Polish, Dark Mode & Responsiveness Audit
- Mobile & Desktop viewports (1440px, 1024px, 768px, 375px).
- Skeletons, empty states, error boundaries.
- Visual self-review documented in `docs/UI_REVIEW.md`.

### Phase 9: Testing & Build Verification
- Backend tests (Auth, Security, Domain logic, AI Mock).
- Frontend Vitest components tests.
- Production build verification (`mvn package` & `npm run build`).
- Docker Compose smoke test.

### Phase 10: Final QA & Documentation
- End-to-end walk-through verification.
- Final documentation, screenshots, and summary report.
