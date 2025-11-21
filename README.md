# Trailglass Backend

Server-side companion for the Trailglass mobile apps.
Implements the public `/api/v1` contract (see mobile repo docs) using Kotlin + Ktor with a tiny user base in mind (≈3–5 accounts).

## Current Scope
- Authentication endpoints (register/login/logout/refresh, password reset placeholders)
- Delta sync endpoints for locations, place visits, trips, photos, and settings
- Conflict resolution + encrypted payload passthrough
- Device tracking and export jobs

## Tech Stack
- Kotlin 2.x + Gradle
- Ktor 3 (server, JSON, JWT auth, rate limiting)
- Exposed ORM + PostgreSQL 15 managed via Flyway migrations
- Optional MinIO/S3 storage for photo and export blobs (fallback to Postgres `bytea` for MVP)
- Argon2 password hashing, kotlinx.serialization for shared DTOs
- Dockerized deployment (single VM/Fly.io/Raspberry Pi)

## Running locally
- Copy the sample env files into place and adjust secrets:
  ```bash
  cp env/app.env.sample env/app.env
  cp env/db.env.sample env/db.env
  cp env/minio.env.sample env/minio.env
  ```
- Build and start the stack (Flyway migrations run before the app boots):
  ```bash
  docker compose up -d --build
  ```
- Health/ops checks:
  - App: `curl http://localhost:8080/health`
  - Metrics (optional, Cloudflare Access service token recommended): `curl -H "CF-Access-Client-Id: $ID" -H "CF-Access-Client-Secret: $SECRET" http://localhost:8080/metrics`
  - Postgres: `docker compose exec postgres pg_isready`
  - MinIO: `curl http://localhost:9000/minio/health/live`

## Deployment & operations
- The app enforces HTTPS by honoring `X-Forwarded-Proto` and rejecting plain HTTP when `ALLOW_PLAIN_HTTP` is not set to `true` (default).
- Flyway migrations are executed automatically during startup and via the dedicated `flyway` service in `compose.yaml`.
- CI builds/pushes multi-arch images to GHCR on pushes/tags; see `.github/workflows/ci.yaml`.
- Backups: `scripts/backup.sh` dumps Postgres and mirrors MinIO buckets; `scripts/restore.sh` rehydrates both.
- Observability: enable `METRICS_ENABLED=true` and front `/metrics` with Cloudflare Access service tokens (`CLOUDFLARE_ACCESS_CLIENT_ID/SECRET`).

## Documentation
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) – module breakdown, data model, deployment guidance
- [`docs/SELF_HOSTING.md`](docs/SELF_HOSTING.md) – Raspberry Pi + Cloudflare Tunnel instructions
- Refer to the Trailglass mobile repository for the canonical API specification and product requirements.

> **Status:** Deployment scaffolding, health, migrations, and ops tooling are in place; feature endpoints are still forthcoming.
