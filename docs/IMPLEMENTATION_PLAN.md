# Implementation Preparation Notes

This document summarizes the existing backend requirements and architectural decisions to guide the upcoming Kotlin/Ktor implementation.

## Product and API Scope
- Implement the `/api/v1` contract for a small self-hosted user base (~3–5 accounts) focused on authentication, sync, and travel data CRUD with conflict resolution and encrypted payload passthrough.【F:README.md†L3-L10】
- Follow the published REST specification for auth, sync, locations, place visits, trips, photos, settings, and exports, including required headers (`Authorization`, `X-Device-ID`, `X-App-Version`) and JWT-based auth flows.【F:docs/backend-api-specification.md†L17-L128】

## Core Architecture Targets
- Kotlin 2.x + Ktor 3 server with coroutine-first handling, JSON serialization, rate limiting, and JWT authentication plugins, wired via Koin or minimal DI.【F:docs/ARCHITECTURE.md†L12-L29】
- Persistence on PostgreSQL 15 using Exposed ORM with Flyway migrations; plan for MinIO/S3 object storage but support Postgres `bytea` for MVP blobs.【F:docs/ARCHITECTURE.md†L15-L18】【F:README.md†L13-L18】
- Background coroutine jobs for exports, cleanup, and email notifications, plus optional metrics endpoint.【F:docs/ARCHITECTURE.md†L16-L17】【F:docs/ARCHITECTURE.md†L50-L53】

## Module Responsibilities
- **Auth & Identity:** Argon2id password hashing; endpoints for register/login/logout/refresh/password reset; refresh tokens bound to device IDs with session invalidation on logout.【F:docs/ARCHITECTURE.md†L30-L35】
- **Sync:** Maintain per-entity `server_version` with monotonic `sync_versions` table; `/sync/status`, `/sync/delta`, and `/sync/resolve-conflict` endpoints with encrypted blob support when `isEncrypted=true`.【F:docs/ARCHITECTURE.md†L36-L43】【F:docs/backend-api-specification.md†L132-L239】
- **Domain Features:** CRUD endpoints for locations (including batch), place visits, trips, settings, photos (metadata in DB, binary in storage), exports with async job + status polling.【F:docs/ARCHITECTURE.md†L44-L49】
- **Supporting Services:** Email sender abstraction, cleanup of expired tokens/blobs, optional metrics and rate limiting per module as specified in the API doc.【F:docs/ARCHITECTURE.md†L50-L53】【F:docs/backend-api-specification.md†L202-L239】

## Data Modeling Notes
- Mirror the mobile SQLDelight schema with UUID primary keys and audit columns (`created_at`, `updated_at`, `deleted_at`, `server_version`, `user_id`, `device_id`).【F:docs/ARCHITECTURE.md†L55-L59】
- Index on `(user_id, updated_at)` and `(user_id, server_version)` to support sync queries and conflict detection; start without PostGIS extensions.【F:docs/ARCHITECTURE.md†L58-L59】

## Deployment & Ops Checklist
- Target Dockerized deployment (Fly.io, Render, or single VM) with Cloudflare Tunnel providing TLS termination; enforce `X-Forwarded-Proto` and keep app bound to localhost behind the tunnel.【F:README.md†L18-L19】【F:docs/SELF_HOSTING.md†L55-L79】
- Compose stack: app + Postgres + MinIO with env-file secrets; ARM-friendly base image recommended for Raspberry Pi deployments.【F:docs/SELF_HOSTING.md†L18-L53】
- CI goals: build/test Docker image and validate Flyway migrations; local dev via docker compose with automatic migrations.【F:docs/ARCHITECTURE.md†L68-L73】
- Backups: nightly `pg_dump` plus MinIO mirroring; restore and disaster recovery steps documented for self-hosters.【F:docs/SELF_HOSTING.md†L80-L98】

## Initial Delivery Milestones
1. Scaffold Gradle/Ktor project with health check route and baseline middleware (content negotiation, status pages, logging, rate limiting, auth wiring).【F:docs/ARCHITECTURE.md†L12-L29】【F:docs/ARCHITECTURE.md†L74-L78】
2. Add Flyway migrations for auth and sync tables, then implement auth flows with Argon2 hashing and JWT issuance/refresh per device.【F:docs/ARCHITECTURE.md†L30-L35】【F:docs/ARCHITECTURE.md†L74-L78】
3. Implement sync status/delta/conflict endpoints using server versions and encrypted blob storage support; include rate limiting from API spec.【F:docs/ARCHITECTURE.md†L36-L43】【F:docs/backend-api-specification.md†L132-L239】
4. Layer feature CRUD (locations, place visits, trips, settings) followed by photos and exports with background job scheduler and presigned URLs where applicable.【F:docs/ARCHITECTURE.md†L44-L49】【F:docs/ARCHITECTURE.md†L74-L81】
5. Harden security (HTTPS enforcement, CORS, logging hygiene), add metrics, and finalize deployment automation/self-hosting guides.【F:docs/ARCHITECTURE.md†L61-L73】【F:docs/SELF_HOSTING.md†L55-L98】

These notes should keep the implementation aligned with the documented API scope, architecture, and deployment expectations.
