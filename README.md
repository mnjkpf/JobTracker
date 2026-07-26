JobTracker

AI-powered job application tracker with cross-interview learning through RAG. Built as a Junior Java Backend portfolio project targeting the Polish tech market.

Live demo: job-tracker-six-flame.vercel.app

Demo

(video coming soon)

What makes it different

Most job trackers treat applications as static records — you log them, drag them between statuses, and that's it.

JobTracker learns from your interview experience. You take notes during and after interviews. When preparing for a similar role at a different company, the system uses Retrieval-Augmented Generation to find relevant notes from your past interviews and tailor the new prep guide accordingly — questions you struggled with before resurface as focused practice for the next one.

Features
AI-powered gap analysis between CV skills and job requirements, via embeddings and pgvector similarity search
Tailored CV generation per application, with ATS keyword scoring and DOCX export
Cover letter generation with adjustable tone and a refine flow
Interview prep guides with RAG-powered context from past interview notes
Kanban board with drag-and-drop status transitions, backed by an explicit state machine
Statistics dashboard — response rate, status breakdown, applications per month
Tech stack

Backend: Java 21, Spring Boot 3.5, PostgreSQL with pgvector, Redis, Spring AI (Anthropic Claude + OpenAI embeddings), Flyway, JWT auth. 372 tests, 96% coverage on the RAG module.

Frontend: React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack Query, React Router, @dnd-kit.

Infrastructure: Railway (backend), Vercel (frontend), Neon (Postgres + pgvector).
