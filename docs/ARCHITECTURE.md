# Architecture

Backend design for the Trailglass travel journal apps.
Optimized for a self-hosted/small-scale deployment (≈3–5 users) while aligning with the documented mobile API.

## Goals
1. Mirror the `/api/v1` REST contract defined in the mobile repo.
2. Keep operational overhead low (single server) without compromising auth/data integrity.
3. Enable incremental delivery: auth + sync first, then feature CRUD, photos, exports.

## System Overview
- **Ktor Server (JVM, Kotlin 2.x)** – coroutines-first web server.
- **HTTP Plugins** – ContentNegotiation (kotlinx.serialization), StatusPages, CallLogging, CallId, RateLimit, DoubleReceive, Authentication (JWT + optional Basic/admin).
- **DI** – Koin (or lightweight manual wiring) for service composition.
- **Persistence** – PostgreSQL 15 with Exposed ORM and Flyway migrations.
- **Object Storage** – MinIO/S3 for photo/export blobs (fallback to Postgres `bytea` for MVP).
- **Background Jobs** – Coroutine-based scheduler within the app for exports, cleanup, email notifications.

```
Clients → Ktor HTTP Layer → Application Services → Postgres / MinIO / Email
```

## Modules
### HTTP Layer
- Routes anchored at `/api/v1`.
- Enforces `X-Device-ID` + `X-App-Version` headers.
- Uniform JSON error responses (`code`, `message`, `details`).
- Simple in-memory rate limiting (upgrade to Redis later if needed).

### Auth & Identity
- Endpoints: `/auth/register|login|logout|refresh|reset-password-request|reset-password`.
- Argon2id password hashing, JWT access tokens (~15 min), refresh tokens (~30 days) bound to device IDs.
- Tables: `users`, `user_credentials`, `devices`, `refresh_tokens`, `password_reset_tokens`.
- Logout invalidates refresh token + device session record.

### Sync
- Entity tables include `server_version`, `updated_at`, `deleted_at`, `device_id`, `user_id`.
- `sync_versions` table issues monotonically increasing versions.
- `/sync/status` returns per-device info.
- `/sync/delta` writes local changes transactionally and streams remote deltas; conflicts triggered when incoming version < stored `server_version`.
- `/sync/resolve-conflict` applies choice and bumps version.
- `encrypted_sync_data` table stores opaque blobs when `isEncrypted=true`.

### Domain Feature Modules
- **Locations** – `/locations/batch`, `/locations`, `/locations/{id}` with dedupe + pagination.
- **Place Visits**, **Trips**, **Photos**, **Settings**, **User Profile/Devices**, **Exports** – CRUD + filtering per spec, returning `serverVersion`.
- Photos: metadata in Postgres, binary via storage service with presigned URLs.
- Exports: async job writes ZIP to storage; `/export/{id}/status` polls.

### Supporting Services
- Email sender abstraction (SendGrid/SES/SMTP) for password resets & export notifications.
- Cleanup job removes expired tokens + orphaned blobs.
- Optional Micrometer/Prometheus metrics endpoint.

## Data Model Notes
- Mirror SQLDelight schema from mobile repo (`location_samples`, `place_visits`, `route_segments`, `trips`, `trip_days`, `memories`, `journal_entries`, `photo_attachments`, `memorabilia`, `country_visits`).
- Columns per table: `id UUID`, `user_id UUID`, `device_id UUID`, `created_at`, `updated_at`, `deleted_at`, `server_version BIGINT`.
- Indexes on `(user_id, updated_at)` and `(user_id, server_version)` for sync queries.
- Start without PostGIS; add later if spatial queries grow.

## Security & Compliance
- HTTPS enforced via reverse proxy (Cloudflare Tunnel, Nginx, etc.); validate `X-Forwarded-Proto`.
- HSTS + TLS handled by hosting platform.
- Structured logging with request IDs; avoid logging sensitive data.
- Scheduled backups for Postgres + MinIO.
- Optional Cloudflare Access rules for admin endpoints.

## Deployment Plan
1. Local dev: Docker Compose (Ktor + Postgres + MinIO). Run Flyway migrations automatically.
2. CI: GitHub Actions builds/tests Docker image & validates migrations.
3. Prod: Single host (Fly.io, Render, Raspberry Pi) running docker compose stack with Cloudflare Tunnel for HTTPS.
4. Observability: start with platform logs; add Prometheus/Grafana later.

## Roadmap
1. Scaffold Gradle + Ktor project, health check route.
2. Define Flyway migrations for auth + sync tables.
3. Implement Auth module.
4. Implement Sync module (status/delta/conflict).
5. Add Locations/Place Visits/Trips/Settings endpoints.
6. Layer Photos + storage abstraction.
7. Implement Exports + background jobs.
8. Harden security + finalize deployment automation/self-hosting docs.
