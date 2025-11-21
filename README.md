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

## Documentation
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) – module breakdown, data model, deployment guidance
- [`docs/SELF_HOSTING.md`](docs/SELF_HOSTING.md) – Raspberry Pi + Cloudflare Tunnel instructions
- Refer to the Trailglass mobile repository for the canonical API specification and product requirements.

> **Status:** Architecture and deployment plans documented; implementation scaffolding will be added next.
