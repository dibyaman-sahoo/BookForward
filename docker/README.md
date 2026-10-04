# Docker

Root `Dockerfile` builds the backend; `frontend/Dockerfile` serves the SPA with nginx. `docker compose up --build` starts Postgres, backend, frontend (`--profile redis` adds an unused-by-default Redis). Ports: 5173 web, 8080 API. Volumes: pgdata, uploads. Troubleshooting: check `docker compose logs backend`; ensure .env sets DB_PASSWORD and JWT_SECRET; CORS errors mean FRONTEND_ORIGIN doesn't match the browser origin.
