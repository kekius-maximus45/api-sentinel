# Deployment Guide

The project supports two modes:

- **Local demo mode:** Spring Boot + embedded H2 using the `local` profile.
- **Production mode:** Spring Boot + PostgreSQL using the default profile.

Do not deploy with H2. Use managed PostgreSQL for hosted environments.

## Recommended Hosting

- Frontend: Vercel or Netlify.
- Backend: Render, Railway, Fly.io, Azure App Service, or a VPS.
- Database: managed PostgreSQL from the same backend host or a dedicated provider.

## Backend Environment

Required:

```text
DATABASE_URL=jdbc:postgresql://host:5432/database
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=at-least-32-random-characters
APP_URL=https://your-frontend-domain
CORS_ALLOWED_ORIGINS=https://your-frontend-domain
AI_PROVIDER=mock
```

Optional real AI:

```text
AI_PROVIDER=openai
AI_OPENAI_BASE_URL=https://api.openai.com/v1
AI_OPENAI_API_KEY=...
AI_OPENAI_MODEL=gpt-4.1-mini
```

## Backend Docker

The backend includes `backend/Dockerfile`.

Build from the repository root:

```powershell
docker build -t api-sentinel-api ./backend
```

Run with environment variables:

```powershell
docker run --rm -p 8080:8080 `
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/api_sentinel `
  -e DATABASE_USERNAME=api_sentinel `
  -e DATABASE_PASSWORD=api_sentinel `
  -e JWT_SECRET=replace-with-a-real-secret `
  -e APP_URL=http://localhost:5173 `
  -e CORS_ALLOWED_ORIGINS=http://localhost:5173 `
  api-sentinel-api
```

## Frontend Deployment

The frontend includes `frontend/vercel.json`.

Set this Vercel environment variable:

```text
VITE_API_BASE_URL=https://your-backend-domain
```

Build command:

```text
npm run build
```

Output directory:

```text
dist
```

## Render Notes

`render.yaml.example` is a template, not a ready-to-apply secret-bearing blueprint. Copy it to `render.yaml`, replace the PostgreSQL JDBC URL and frontend URL, then connect the repository in Render.

Render's managed PostgreSQL connection string is commonly `postgres://...`; Spring Boot needs the JDBC form:

```text
jdbc:postgresql://host:5432/database
```

## Pre-Deploy Checklist

- Generate a strong `JWT_SECRET`.
- Use PostgreSQL, not H2.
- Set `CORS_ALLOWED_ORIGINS` to the frontend domain only.
- Set `APP_URL` to the frontend domain so webhook payloads link to the dashboard.
- Confirm `GET /actuator/health` returns `UP`.
- Confirm Swagger is available only if you want public API docs.
- Confirm public status pages do not expose internal URLs, headers, request bodies, or snippets.
