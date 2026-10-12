# SmartPlacement AI - Backend Service

Spring Boot 3 + Java 17 enterprise backend powering algorithmic placement readiness, ATS resume parsing, and Gemini AI career coaching.

## Structure
- `src/main/java/`: Controllers, Services, Repositories, Security, and AI integration.
- `src/main/resources/`: Flyway migrations, seed configurations, and YAML application profiles.
- `docs/`: Project documentation, menu mapping, and decision architecture logs.
- `sample_resumes/`: Reference PDFs for automated ATS parsing verification.
- `uploads/`: Local storage directory for student resume uploads.
- `docker-compose.yml`: Multi-container stack (MySQL, Spring Boot Backend, Vite Frontend).

## Quickstart
```bash
# Run locally
cd backend
mvn spring-boot:run

# Build and package
mvn clean package

# Run with Docker
docker-compose up -d
```
"# CAREER_PLACEMENT_BACKEND" 
