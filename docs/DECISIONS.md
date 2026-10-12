# Architectural Decisions Log (DECISIONS.md)

## ADR 001: Tech Stack & Tooling Choices
- **Backend Framework**: Java 17, Spring Boot 3.2.3, Maven wrapper, Spring Security 6, JWT, Spring Data JPA, Flyway.
- **Frontend Stack**: React 18, Vite 5, JavaScript/JSX only (per specification), React Router v6, Axios, Recharts, Lucide-React, @fontsource/inter, Plain CSS with tokens.
- **Database**: MySQL 8.0 on localhost / docker-compose.
- **Role Model**: Single Role Enum per user (`ADMIN`, `TPO`, `FACULTY`, `STUDENT`, `RECRUITER`, `MENTOR`).
- **Recruiter Approval Flow**: Recruiters register with `approvalStatus = PENDING`. Admin or TPO can approve them to enable job posting.
- **AI Mode**: Configurable via `AI_MODE` env var (`live`, `dev-fallback`, `disabled`). Dev-fallback explicitly labels mock responses in both API and UI.
- **Deterministic Math**: Match score calculation formula: `Σ weight(matched skills) / Σ weight(all job skills) * 100` where Required skills = weight 3, Preferred skills = weight 1.
