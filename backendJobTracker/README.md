<!--
Місце: <repo-root>/README.md
-->

# Job Tracker

AI-powered job application tracker built for the Polish job market. Tracks applications across NoFluffJobs, JustJoin.it, and Pracuj.pl with AI-driven gap analysis, tailored CV generation, and Interview Prep Knowledge Base with RAG for cross-interview learning.

Personal project — built and used while preparing for Java Backend Junior positions.

## Status

🚧 Under active development. Week 1 of 12.

## Tech Stack

**Backend:** Java 21 · Spring Boot 3.5 · Spring AI · PostgreSQL 16 + pgvector · Redis 7 · Flyway · Resilience4j · Bucket4j  
**Frontend:** React 18 · TypeScript · Vite · Tailwind CSS · shadcn/ui · TanStack Query  
**Infrastructure:** Docker Compose · GitHub Actions · Prometheus + Grafana

## Architecture Highlights

- AI extraction from Polish job boards via JSoup + LLM fallback
- Master CV → per-application tailored versions with versioning and LaTeX/PDF export
- pgvector-backed semantic skill matching for gap analysis
- Interview Prep KB with RAG retrieval across past notes
- B2B / UoP / UZ tax calculator for accurate Polish take-home math
- Circuit breakers around LLM calls, distributed rate limiting via Redis

## Quick Start

### Prerequisites

- Java 21 (Temurin recommended)
- Node.js 20+
- Docker + Docker Compose
- Maven 3.9+ (or use bundled `./mvnw`)

### Run infrastructure

```bash
docker compose up -d
```

Starts:
- PostgreSQL with pgvector → `localhost:5432`
- Redis → `localhost:6379`
- Mailpit (email testing) → SMTP `localhost:1025`, Web UI `http://localhost:8025`

### Run backend

```bash
cd backend
export ANTHROPIC_API_KEY=sk-ant-...
export OPENAI_API_KEY=sk-...
./mvnw spring-boot:run
```

Backend lives at `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`

### Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend at `http://localhost:5173`.

## Documentation

- [Domain Model](docs/domain-model.md)
- [API Conventions](docs/api-conventions.md)
- [Architecture Decision Records](docs/adr/)

## License

Private project. All rights reserved.