# Deploy Guide

## Prerequisites

1. Neon account with pgvector-enabled Postgres database
2. Upstash account with Redis database (TLS)
3. Railway account
4. Vercel account
5. Anthropic API key
6. OpenAI API key

## Required environment variables

### Backend (Railway)

Set these as Railway project variables (not in `railway.toml` — Railway's config-as-code doesn't support env vars, only build/deploy settings).

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL` — Neon JDBC URL, e.g. `jdbc:postgresql://<host>/<db>?sslmode=require`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `REDIS_URL` — Upstash URI, must start with `rediss://` for TLS
- `ANTHROPIC_API_KEY`
- `OPENAI_API_KEY`
- `JWT_SECRET` — base64 string decoding to >=32 bytes (HS256). Startup fails fast if unset (`jobtracker.auth.jwt-secret` is `@NotBlank`-validated).
- `CORS_ALLOWED_ORIGINS` — Vercel frontend URL(s), comma-separated
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` — real SMTP relay when available. Without `MAIL_HOST`, the app defaults to `localhost:587`: it will still start, but email verification requests fail at send time instead of at boot.

### Frontend (Vercel)

- `VITE_API_BASE_URL` — Railway backend URL + `/api/v1` (baked in at build time; also update `frontend/.env.production`)

## Generate JWT secret (PowerShell)

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## Setup steps

1. Neon: create project, run `CREATE EXTENSION vector;`
2. Upstash: create Redis database with TLS enabled
3. Railway: import GitHub repo (root: `backendJobTracker`), set env vars above, deploy
4. Update `VITE_API_BASE_URL` in `frontend/.env.production` and in Vercel project settings with the Railway URL
5. Vercel: import GitHub repo (root: `frontend`), deploy
6. Update Railway `CORS_ALLOWED_ORIGINS` with the Vercel URL, redeploy
