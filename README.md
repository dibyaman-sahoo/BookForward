# BookForward — *Give Every Book Another Chapter.*

An educational book reuse, discovery and resale marketplace. Users list school, college and exam-prep books with three mandatory evidence photos, discover them through search/filter/sort, save favourites, send purchase requests, move orders through a validated lifecycle, chat in real time, receive notifications and review sellers. Moderators and admins handle reports, listing moderation, users and audit logs.

Implementation of the *BookForward Finalized B.Tech Major Project Blueprint*.

> **Honest status:** this repository was generated in an environment without network access, so the **Java backend has not been compiled or run**, and the UI has not been exercised in a browser. Only the frontend JavaScript syntax was checked (`scripts/check-frontend.sh`). Expect to fix small compile/runtime issues on first build — see [Verification status](#verification-status).

## Tech stack
| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation, WebSocket/STOMP, Actuator), Flyway, jjwt, Lombok |
| Database | PostgreSQL 16 (UUID keys, versioned migrations) |
| Frontend | Framework-free HTML5 / CSS3 / ES modules, hash-routed SPA, CSS-3D hero, `@stomp/stompjs` (CDN, pinned) |
| Ops | Docker, Docker Compose, GitHub Actions, nginx (frontend image) |

## Architecture
Modular monolith (`backend/src/main/java/com/bookforward`): `config`, `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `security`, `websocket`, `payment`, `storage`, `search`, `notification`, `admin`, `exception`, `util`. REST for durable resources; STOMP over WebSocket (`/ws`) for messaging, typing, presence and live notifications. PostgreSQL is the source of truth.

Provider-neutral boundaries: `StorageProvider` (local disk adapter), `PaymentProvider` (offline adapter; payment is disabled by default), `PresenceService` and `RateLimiter` (in-memory adapters, Redis-ready), `SearchService` (PostgreSQL specifications; swap for OpenSearch later).

### Key rules implemented
- JWT auth (BCrypt-12 passwords); `logout` bumps a per-user token version so old tokens die. Roles: `USER`, `MODERATOR`, `ADMIN` (stored in `user_roles`).
- Ownership enforced in services; other users' listings/orders return 404 (no IDOR leakage).
- Publishing requires front-cover, details-page and index-page images; uploads validated by size, extension, declared type **and magic bytes**; keys are random UUIDs.
- Requests: `PENDING → ACCEPTED | REJECTED | CANCELLED`. Accepting reserves the listing (row-locked), creates an order and rejects rival requests.
- Orders: `CONFIRMED → HANDOVER → COMPLETED` (or `CANCELLED`), seller marks handover, buyer confirms receipt, history recorded with actor/reason. Completion marks the listing `SOLD`; cancellation re-opens it.
- Reviews: buyer only, completed orders only, one per order, rating 1–5.
- Moderation: reports, hide/reject/approve listings, suspend users, hide reviews; all audited. Admin-only role/status/audit endpoints.
- STOMP: token required on CONNECT; subscriptions limited to `/user/queue/**` and `/topic/presence`; sends limited to `/app/chat.send|typing`; membership checked per message; presence tracks sessions per user and broadcasts the full online set.
- Rate limits on login/register, uploads and messages; CORS restricted to `FRONTEND_ORIGIN`; correlation-id logging; uniform JSON errors.

## Project structure
```
backend/            Spring Boot app (pom.xml, src/main, src/test, db/migration)
frontend/           Static SPA (index.html, css/, js/, js/pages/, Dockerfile, nginx.conf)
database/           Schema notes + seed guidance
docs/ scripts/ tests/ docker/ .github/
Dockerfile          Backend image   |   docker-compose.yml   Postgres + backend + frontend (+ optional redis profile)
.env.example
```

## Quick start (Docker)
```bash
cp .env.example .env        # set DB_PASSWORD, JWT_SECRET (32+ chars), optionally ADMIN_EMAIL/ADMIN_PASSWORD
docker compose up --build   # frontend http://localhost:5173 · API http://localhost:8080
```

## Local development
Prerequisites: JDK 21, Maven 3.9+, PostgreSQL 14+ (needs `gen_random_uuid()`), Node 18+ (syntax check only).
```bash
createdb bookforward && createuser bookforward   # then set a password
export DB_PASSWORD=... JWT_SECRET=$(openssl rand -base64 48) ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=ChangeMe123
cd backend && mvn spring-boot:run          # Flyway migrates on startup
./scripts/dev-frontend.sh                  # serves frontend on :5173
```
Edit `frontend/env.js` (`apiBase`) if the API is not on `http://localhost:8080`.

## Environment variables
See `.env.example`. Required: `DB_PASSWORD`, `JWT_SECRET`. Others: `DB_URL`, `DB_USERNAME`, `FRONTEND_ORIGIN`, `WS_ALLOWED_ORIGINS`, `STORAGE_PROVIDER`/`STORAGE_LOCAL_DIR`, `PAYMENT_ENABLED`/`PAYMENT_PROVIDER`, `ADMIN_EMAIL`/`ADMIN_PASSWORD`, `LOG_LEVEL`, `HIBERNATE_DDL_AUTO` (default `validate`; if a mapping mismatch blocks startup set `none` temporarily and report it).

## API overview
Auth `POST /api/auth/{register,login,logout}`, `GET /api/auth/me` · Listings `GET/POST /api/listings`, `GET/PUT/PATCH/DELETE /api/listings/{id}`, `POST …/publish|unpublish`, `POST …/images?type=FRONT_COVER|DETAILS_PAGE|INDEX_PAGE|EXTRA`, `GET /api/me/listings` · Search `GET /api/search/listings?query=&category=&level=&board=&ncert=&condition=&minPrice=&maxPrice=&availability=&sort=&page=` · Saved `GET /api/saved`, `POST/DELETE /api/saved/{id}` · Requests/Orders `POST /api/requests`, `GET /api/requests/mine?role=`, `PATCH /api/requests/{id}/status`, `GET /api/orders`, `PATCH /api/orders/{id}/status`, `POST /api/orders/{id}/payments` (503 unless enabled) · Chat `GET/POST /api/conversations`, `GET/POST …/{id}/messages`, `GET /api/presence` · Notifications `GET /api/notifications`, `PATCH …/{id}/read`, `POST …/read-all` · Reviews `POST /api/reviews`, `GET /api/listings/{id}/reviews` · Reports/Admin `POST /api/reports`, `/api/admin/{reports,listings,users,audit}` · Health `GET /actuator/health` (+ `/liveness`, `/readiness`).
STOMP: connect to `/ws` with header `Authorization: Bearer <jwt>`; subscribe `/user/queue/{messages,notifications,typing,errors}` and `/topic/presence`; send to `/app/chat.send` `{conversationId, content}` and `/app/chat.typing`.

## Testing
`cd backend && mvn verify` runs unit tests (order state machine, JWT service). **Not yet written** (blueprint targets): repository/integration tests with Testcontainers, WebSocket tests, security tests for access control and uploads, and the E2E flow. See `tests/README.md`.

## Verification status
| Item | Status |
|---|---|
| Frontend JS syntax | checked with Node |
| Backend compile / unit tests | **not run** (no Maven/network in build environment) |
| Flyway migrations vs. JPA mappings | **not run** — most likely source of first-start errors |
| Browser flows, responsive layout, console | **not exercised** |
| Docker builds | **not run** |

## Security notes
Secrets come from environment only. The JWT is kept in `localStorage` (simple, XSS-sensitive): all dynamic UI output goes through an escaping `html` template; add a strict CSP at your reverse proxy for production. Use HTTPS in production; set `forward-headers` appropriately behind a proxy. Presence broadcasts the online user IDs to every authenticated user (acceptable for this scope; restrict to conversation partners if privacy requires).

## Deployment
Same images everywhere: backend container + managed PostgreSQL, frontend container (or any static host with `env.js` pointing at the API). Render/Railway/AWS/Azure/GCP/VPS patterns are in `docs/`. Back up PostgreSQL (`pg_dump`) and the uploads volume; roll back by redeploying the previous image tag (Flyway migrations are forward-only — write compensating migrations).

## Screenshots
_Add screenshots here after running the app._

## Known gaps / future work
Redis adapters, OpenSearch, background workers, email/push notifications, real payment gateway adapter, object-storage adapter, integration/E2E tests, report-a-user/review buttons in UI, separate `roles` master table (currently `user_roles` enum values), `presence` table (presence is in-memory with `users.last_seen_at`).

## Contributing
Branch from `main`, keep migrations additive, add tests with behaviour changes, keep controllers thin.
