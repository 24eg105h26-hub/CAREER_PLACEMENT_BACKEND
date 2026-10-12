# SmartPlacement AI — Production Deployment Guide

This guide details environment variables, architectural containerization, cloud deployment workflows, and pre-launch checklists for SmartPlacement AI.

---

## 1. Environment Variable Reference Table

### Backend Variables (`TT/backend/.env`)

| Variable Name | Location / Service | Required in Prod? | Default (Dev) | Production Example | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Spring Boot | **Yes** | `dev` | `prod` | Active profile. When set to `prod`, seeders and swagger are disabled, validation rules apply. |
| `PORT` | Embedded Tomcat | No | `8080` | `8080` | Server HTTP listening port (configured automatically by Cloud Run, Render, Railway). |
| `DB_URL` / `MYSQL_URL` | Spring Data JPA / Flyway | **Yes** | H2 in-memory | `jdbc:mysql://db.host.com:3306/smartplacement?useSSL=true&requireSSL=true` | Primary JDBC database connection URL. |
| `DB_USERNAME` | Spring Data JPA | **Yes** | `sa` | `prod_user` | Database username. |
| `DB_PASSWORD` | Spring Data JPA | **Yes** | `` | `A8b9!zK#194m` | Database user password. |
| `DB_POOL_MAX_SIZE` | HikariCP | No | `10` | `20` | Maximum Hikari connection pool size. |
| `JWT_SECRET` | JwtTokenProvider | **Yes** (min 32 chars) | Dev test key | `6f890c2a8f...32chars` | HMAC-SHA256 signature key. Must be >= 32 characters in `prod`. |
| `JWT_EXPIRATION_MINUTES`| JwtTokenProvider | No | `1440` | `1440` | Token lifespan in minutes (1440 = 24 hours). |
| `CORS_ALLOWED_ORIGINS` | SecurityConfig | **Yes** | `http://localhost:5173` | `https://smartplacement.app` | Comma-separated list of allowed frontend origins (No wildcards in prod). |
| `FRONTEND_URL` | SecurityConfig | No | `http://localhost:5173` | `https://smartplacement.app` | Single primary frontend URL fallback. |
| `GEMINI_API_KEY` | GeminiAiService | **Yes** (if AI active)| `mock-key` | `AIzaSy...YourKey` | Google Gemini API key from Google AI Studio. |
| `GEMINI_MODEL` | GeminiAiService | No | `gemini-2.5-flash` | `gemini-2.5-flash` | Gemini model identifier. |
| `AI_MODE` | AiFeatureService | No | `live` | `live` | AI operation mode: `live` or `dev-fallback`. |
| `FILE_STORAGE_PATH` | ResumeService | No | `uploads/resumes` | `/mnt/volume/resumes` | Directory for resume file uploads. Persistent disk or volume mount required. |
| `MAX_UPLOAD_SIZE` | MultipartResolver | No | `5MB` | `10MB` | Maximum upload size for PDF resumes. |
| `SEED_DEFAULT_PASSWORD`| DataSeeder | No | `Password123!` | `StrongPass!99` | Default password for seeded dev accounts. |
| `SEED_ENABLED` | DataSeeder | No | `true` in dev | `false` | Disables mock seeding on startup. |
| `SWAGGER_ENABLED` | SpringDoc | No | `true` in dev | `false` | Disables OpenAPI docs endpoints in production. |

---

### Frontend Variables (`TT/frontend/.env`)

| Variable Name | Location / Service | Required in Prod? | Default | Production Example | Description |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `VITE_API_BASE_URL` | Axios API Client | **Yes** | `/api` | `https://api.smartplacement.app/api` | Base endpoint for all REST API calls. Embedded at build time. |
| `VITE_BACKEND_URL` | Vite Dev Proxy | No | `http://localhost:8080`| `http://localhost:8080` | Used exclusively by `npm run dev` proxy. |
| `VITE_APP_NAME` | UI Components | No | `SmartPlacement AI` | `SmartPlacement AI` | Public product title. |

> **IMPORTANT**: Vite environment variables (`VITE_*`) are injected into client JS bundles during `npm run build`. If you update `VITE_API_BASE_URL`, you must trigger a re-build.

---

## 2. Local Development Workflow

### Running Backend Locally
```bash
cd TT/backend
# 1. Ensure .env exists with your GEMINI_API_KEY
# 2. Run with Maven (defaults to dev profile with in-memory DB)
mvn spring-boot:run
# Backend will be available at http://localhost:8080 (or your configured PORT)
```

### Running Frontend Locally
```bash
cd TT/frontend
npm install
npm run dev
# Frontend will be available at http://localhost:5173
```

---

## 3. Docker Compose Stack (Local or VPS)

A complete container stack with MySQL 8.0, Spring Boot, and Nginx:

```bash
cd TT/backend
# Create and verify .env configuration
docker compose up --build -d

# Check service health
docker compose ps
# View application logs
docker compose logs -f backend
```

---

## 4. Cloud Deployment Architecture Guide

### Option A: Separate Frontend (Vercel / Netlify) + Backend (Railway / Render / AWS)

#### Step 1: Deploy Database
- Provision a managed MySQL instance on **AWS RDS**, **Railway**, **PlanetScale**, or **Aiven**.
- Obtain the JDBC connection URL, username, and password.

#### Step 2: Deploy Backend (Render / Railway / AWS ECS)
1. Set the root directory / context to `backend/`.
2. Configure Environment Variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `PORT=8080`
   - `DB_URL=jdbc:mysql://db.host.com:3306/smartplacement?useSSL=true&requireSSL=true`
   - `DB_USERNAME=your_db_user`
   - `DB_PASSWORD=your_db_password`
   - `JWT_SECRET=` *(generate with `openssl rand -hex 32`)*
   - `GEMINI_API_KEY=` *(your Google AI key)*
   - `CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app`
   - `SEED_ENABLED=false`
   - `SWAGGER_ENABLED=false`
3. Deploy the backend and obtain the public URL (e.g. `https://smartplacement-api.onrender.com`).
4. Verify health probe: `GET https://smartplacement-api.onrender.com/actuator/health` returns `{"status":"UP"}`.

#### Step 3: Deploy Frontend (Vercel / Netlify)
1. Point repository to `frontend/`.
2. Configure Environment Variable:
   - `VITE_API_BASE_URL=https://smartplacement-api.onrender.com/api`
3. Deploy frontend. Deep links (`/student/jobs`, `/admin/users`) work out of the box via `vercel.json` and `_redirects`.
4. Return to your Backend dashboard and update `CORS_ALLOWED_ORIGINS` to your exact frontend domain if it changed.

---

### Option B: Unified Single-Server / VPS (Ubuntu + Docker)
1. Clone repository to server.
2. Put production variables in `TT/backend/.env`.
3. In `TT/backend/docker-compose.yml`, run `docker compose up --build -d`.
4. Point a reverse proxy (e.g. Caddy, Traefik, or Certbot Nginx) to port 80 for SSL termination.

---

## 5. Ephemeral Disks & File Storage Note
Cloud platforms like Heroku, Render, and AWS Fargate utilize ephemeral filesystems. Any resumes saved to the local container disk will disappear on redeployment or restart.
- In production, either:
  1. Mount a persistent network volume (e.g. Railway volume or AWS EFS) and point `FILE_STORAGE_PATH` to the mount path.
  2. Or configure S3 / Cloud Storage bucket storage for binary PDF blobs.

---

## 6. Pre-Launch Production Checklist

- [ ] **HTTPS Enforced**: All HTTP traffic is redirected to HTTPS (via Cloudflare, AWS ALB, or Nginx).
- [ ] **Strong JWT Secret**: `JWT_SECRET` has at least 32 cryptographically random characters (`openssl rand -hex 32`).
- [ ] **Restricted CORS**: `CORS_ALLOWED_ORIGINS` contains only the exact production frontend domain(s); no `*` wildcard.
- [ ] **Production Profile**: `SPRING_PROFILES_ACTIVE=prod` is set so the app validates configurations on boot.
- [ ] **Seeding Disabled**: `SEED_ENABLED=false` to prevent test accounts from being generated.
- [ ] **Swagger Disabled**: `SWAGGER_ENABLED=false` so internal schema docs are not exposed publicly.
- [ ] **Stack Traces Hidden**: `server.error.include-stacktrace=never` is active in production.
- [ ] **Database Backups**: Automated backups and snapshots are enabled on the managed MySQL service.
- [ ] **Secrets Stored Securely**: No `.env` file is committed to Git; secrets are populated via host environment variables.
