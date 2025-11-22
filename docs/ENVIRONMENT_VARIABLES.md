# Environment Variables Reference

Comprehensive guide to all environment variables used in the Trailglass backend application.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Server Configuration](#server-configuration)
3. [Database Configuration](#database-configuration)
4. [JWT/Authentication Configuration](#jwtauthentication-configuration)
5. [Storage Configuration (S3/MinIO)](#storage-configuration-s3minio)
6. [Email Configuration](#email-configuration)
7. [Rate Limiting Configuration](#rate-limiting-configuration)
8. [Migration Configuration](#migration-configuration)
9. [Monitoring & Observability Configuration](#monitoring--observability-configuration)
10. [Security Configuration](#security-configuration)
11. [Container-Specific Variables](#container-specific-variables)
12. [Setup Instructions](#setup-instructions)
13. [Production Deployment Checklist](#production-deployment-checklist)
14. [Troubleshooting](#troubleshooting)

---

## Quick Start

The backend uses three environment files located in the `env/` directory:

- **`env/app.env`** - Application configuration (server, database, JWT, storage, monitoring)
- **`env/db.env`** - PostgreSQL container configuration
- **`env/minio.env`** - MinIO/S3 container configuration

To get started locally:

```bash
cp env/app.env.sample env/app.env
cp env/db.env.sample env/db.env
cp env/minio.env.sample env/minio.env
chmod 600 env/*.env
```

Then customize the values for your environment. **Never commit actual `*.env` files to version control.**

---

## Server Configuration

### APP_PORT

- **Description:** Port the Ktor server listens on
- **Required:** No
- **Default:** `8080`
- **Example:** `APP_PORT=8080`
- **Used in:** `AppConfig.kt`

### APP_HOST

- **Description:** Host address the Ktor server binds to
- **Required:** No
- **Default:** `0.0.0.0` (all interfaces)
- **Example:** `APP_HOST=0.0.0.0`
- **Used in:** `AppConfig.kt`
- **Notes:** In production behind a reverse proxy/tunnel, binding to `0.0.0.0` is safe as long as firewall rules restrict access

### PORT

- **Description:** Legacy/alternative port variable (used in `env/app.env.sample`)
- **Required:** No
- **Default:** `8080`
- **Example:** `PORT=8080`
- **Notes:** Prefer `APP_PORT` in application code; `PORT` may be used by platform conventions (e.g., Heroku, Fly.io)

### APP_ENVIRONMENT

- **Description:** Deployment environment (development, staging, production)
- **Required:** No
- **Default:** `development`
- **Example:** `APP_ENVIRONMENT=production`
- **Used in:** `AppConfig.kt`
- **Security Impact:** HIGH - In production mode:
  - Enforces strict JWT_SECRET validation
  - Disables auto-migration by default
  - May enable additional security checks
- **Valid Values:** `development`, `staging`, `production`

### ALLOW_PLAIN_HTTP

- **Description:** Allow HTTP connections without HTTPS (for development only)
- **Required:** No
- **Default:** `false`
- **Example:** `ALLOW_PLAIN_HTTP=false`
- **Security Impact:** CRITICAL
- **Notes:**
  - Must be `false` in production
  - The app honors `X-Forwarded-Proto` header from reverse proxies
  - When `false`, rejects any request not marked as HTTPS by the proxy
  - Cloudflare Tunnel and similar proxies automatically set this header

---

## Database Configuration

The application constructs database connections using either individual components or a single `DATABASE_URL`.

### DATABASE_URL

- **Description:** Full PostgreSQL connection URL (JDBC format)
- **Required:** Yes (or use individual DATABASE_* components)
- **Default:** None
- **Example:** `DATABASE_URL=jdbc:postgresql://postgres:5432/trailglass?sslmode=prefer`
- **Used in:** `AppConfig.kt`
- **Format:** `jdbc:postgresql://[host]:[port]/[database]?[params]`
- **Security Impact:** HIGH - Contains credentials if included in URL
- **Notes:** App will exit with fatal error if not provided

### DATABASE_HOST

- **Description:** PostgreSQL server hostname or IP
- **Required:** Yes (if not using DATABASE_URL)
- **Default:** None
- **Example:** `DATABASE_HOST=postgres` (Docker service name) or `DATABASE_HOST=localhost`

### DATABASE_PORT

- **Description:** PostgreSQL server port
- **Required:** No
- **Default:** `5432`
- **Example:** `DATABASE_PORT=5432`

### DATABASE_NAME

- **Description:** PostgreSQL database name
- **Required:** Yes
- **Default:** None
- **Example:** `DATABASE_NAME=trailglass`

### DATABASE_USER

- **Description:** PostgreSQL username for application connections
- **Required:** Yes
- **Default:** None
- **Example:** `DATABASE_USER=postgres`
- **Used in:** `AppConfig.kt`
- **Security Impact:** HIGH

### DATABASE_PASSWORD

- **Description:** PostgreSQL password for application connections
- **Required:** Yes
- **Default:** None
- **Example:** `DATABASE_PASSWORD=your_secure_password_here`
- **Used in:** `AppConfig.kt`
- **Security Impact:** CRITICAL
- **Production Guidance:**
  - Use strong, randomly generated passwords (32+ characters)
  - Consider using secrets management (e.g., Docker secrets, HashiCorp Vault)
  - Rotate periodically

### DATABASE_SSL_MODE

- **Description:** PostgreSQL SSL/TLS connection mode
- **Required:** No
- **Default:** `prefer`
- **Example:** `DATABASE_SSL_MODE=require`
- **Valid Values:**
  - `disable` - No SSL (development only)
  - `prefer` - Use SSL if available, fallback to plain (default)
  - `require` - Force SSL, fail if unavailable (recommended for production)
  - `verify-ca` - Require SSL and verify CA certificate
  - `verify-full` - Require SSL and verify hostname
- **Security Impact:** HIGH
- **Production Guidance:** Use `require` or stronger in production

### DATABASE_MAX_POOL_SIZE

- **Description:** Maximum number of database connections in the pool
- **Required:** No
- **Default:** `5`
- **Example:** `DATABASE_MAX_POOL_SIZE=10`
- **Notes:**
  - Adjust based on expected concurrent users
  - For small deployments (3-5 users), default is sufficient
  - Monitor connection usage before increasing

---

## JWT/Authentication Configuration

### JWT_SECRET

- **Description:** Secret key used to sign and verify JWT tokens
- **Required:** Yes in production (has development fallback)
- **Default:** `dev-secret-change-me` (development only)
- **Example:** `JWT_SECRET=your-random-256-bit-secret-here`
- **Used in:** `AppConfig.kt`
- **Security Impact:** CRITICAL
- **Production Guidance:**
  - MUST be changed in production (app will exit if using default)
  - Generate using: `openssl rand -base64 32` or similar
  - Never reuse across environments
  - Store securely (secrets manager, encrypted env files)
  - Changing this invalidates all existing tokens
- **Validation:** App performs strict validation in production mode

### JWT_ISSUER

- **Description:** JWT issuer claim (identifies who issued the token)
- **Required:** No
- **Default:** `trailglass`
- **Example:** `JWT_ISSUER=https://api.trailglass.example.com`
- **Used in:** `AppConfig.kt`
- **Format:** Can be a URL or simple string identifier
- **Notes:** Must match between token generation and validation

### JWT_AUDIENCE

- **Description:** JWT audience claim (identifies intended token recipients)
- **Required:** No
- **Default:** `trailglass-clients`
- **Example:** `JWT_AUDIENCE=trailglass-mobile-app`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - Mobile clients should expect this value
  - Prevents token reuse across different applications

---

## Storage Configuration (S3/MinIO)

The application supports two storage backends: S3-compatible storage (MinIO/AWS S3) or PostgreSQL bytea (fallback).

### STORAGE_DRIVER

- **Description:** Storage backend selection (legacy variable from sample)
- **Required:** No
- **Default:** Not used in current code (use STORAGE_BACKEND instead)
- **Example:** `STORAGE_DRIVER=minio`
- **Notes:** Kept in samples for reference; replaced by STORAGE_BACKEND

### STORAGE_BACKEND

- **Description:** Storage backend type
- **Required:** No
- **Default:** `database`
- **Example:** `STORAGE_BACKEND=s3`
- **Used in:** `AppConfig.kt`
- **Valid Values:**
  - `database` or `postgres` - Store blobs in PostgreSQL bytea (default)
  - `s3` or `minio` - Use S3-compatible object storage
- **Notes:** When using S3/MinIO backend, additional STORAGE_* variables are required

### STORAGE_BUCKET

- **Description:** S3/MinIO bucket name for storing photos and exports
- **Required:** Yes (when STORAGE_BACKEND=s3/minio)
- **Default:** `trailglass`
- **Example:** `STORAGE_BUCKET=trailglass-prod`
- **Used in:** `AppConfig.kt`
- **Validation:** App will exit with fatal error if not provided for S3 backend

### STORAGE_REGION

- **Description:** AWS region or MinIO region designation
- **Required:** No
- **Default:** `us-east-1`
- **Example:** `STORAGE_REGION=us-west-2`
- **Used in:** `AppConfig.kt`
- **Notes:** For MinIO, region is mostly cosmetic but should match bucket configuration

### STORAGE_ENDPOINT

- **Description:** Custom S3 endpoint URL (required for MinIO)
- **Required:** Yes for MinIO; No for AWS S3
- **Default:** None
- **Example:** `STORAGE_ENDPOINT=http://minio:9000` (Docker) or `STORAGE_ENDPOINT=https://s3.trailglass.example.com`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - For AWS S3: leave empty (SDK uses default endpoints)
  - For MinIO: must include protocol and port
  - Use internal Docker service names in compose environments

### STORAGE_ACCESS_KEY

- **Description:** S3/MinIO access key ID (equivalent to username)
- **Required:** Yes (when STORAGE_BACKEND=s3/minio)
- **Default:** None
- **Example:** `STORAGE_ACCESS_KEY=minioadmin` (development) or `STORAGE_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE`
- **Used in:** `AppConfig.kt`
- **Security Impact:** HIGH
- **Validation:** App will exit with fatal error if not provided for S3 backend

### STORAGE_SECRET_KEY

- **Description:** S3/MinIO secret access key (equivalent to password)
- **Required:** Yes (when STORAGE_BACKEND=s3/minio)
- **Default:** None
- **Example:** `STORAGE_SECRET_KEY=minioadmin` (development) or `STORAGE_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`
- **Used in:** `AppConfig.kt`
- **Security Impact:** CRITICAL
- **Production Guidance:**
  - Generate strong random keys
  - Rotate periodically
  - Never commit to version control
- **Validation:** App will exit with fatal error if not provided for S3 backend

### STORAGE_PATH_STYLE

- **Description:** Use path-style S3 URLs instead of virtual-hosted style
- **Required:** No
- **Default:** `true`
- **Example:** `STORAGE_PATH_STYLE=true`
- **Used in:** `AppConfig.kt`
- **Valid Values:** `true`, `false`
- **Notes:**
  - `true`: URLs like `https://endpoint/bucket/key` (required for MinIO)
  - `false`: URLs like `https://bucket.endpoint/key` (AWS S3 default)
  - MinIO requires path-style access

### STORAGE_SIGNING_SECRET

- **Description:** Secret for generating presigned URLs
- **Required:** No
- **Default:** None
- **Example:** `STORAGE_SIGNING_SECRET=your-signing-secret-here`
- **Used in:** `AppConfig.kt`
- **Security Impact:** MEDIUM
- **Notes:**
  - Used for temporary URL generation for photo/export downloads
  - If not set, may fall back to storage backend's signing mechanism

### MINIO_ENDPOINT

- **Description:** Legacy MinIO endpoint variable (from sample)
- **Required:** No (use STORAGE_ENDPOINT)
- **Default:** None
- **Example:** `MINIO_ENDPOINT=http://minio:9000`
- **Notes:** Prefer STORAGE_ENDPOINT in application code

### MINIO_BUCKET

- **Description:** Legacy MinIO bucket variable (from sample)
- **Required:** No (use STORAGE_BUCKET)
- **Default:** None
- **Example:** `MINIO_BUCKET=trailglass`
- **Notes:** Prefer STORAGE_BUCKET in application code

### MINIO_ACCESS_KEY

- **Description:** Legacy MinIO access key variable (from sample)
- **Required:** No (use STORAGE_ACCESS_KEY)
- **Default:** None
- **Example:** `MINIO_ACCESS_KEY=minioadmin`
- **Notes:** Prefer STORAGE_ACCESS_KEY in application code

### MINIO_SECRET_KEY

- **Description:** Legacy MinIO secret key variable (from sample)
- **Required:** No (use STORAGE_SECRET_KEY)
- **Default:** None
- **Example:** `MINIO_SECRET_KEY=minioadmin`
- **Notes:** Prefer STORAGE_SECRET_KEY in application code

---

## Email Configuration

The application supports multiple email providers for sending transactional emails (password resets, export notifications, etc.). By default, email sending is disabled and uses console logging.

### EMAIL_ENABLED

- **Description:** Enable or disable email sending functionality
- **Required:** No
- **Default:** `false`
- **Example:** `EMAIL_ENABLED=true`
- **Used in:** `AppConfig.kt`
- **Valid Values:** `true`, `false`
- **Notes:**
  - When `false`, all emails are logged to console instead of being sent
  - Useful for development and testing
  - Should be `true` in production for password reset functionality

### EMAIL_PROVIDER

- **Description:** Email service provider to use
- **Required:** No
- **Default:** `console`
- **Example:** `EMAIL_PROVIDER=smtp`
- **Used in:** `AppConfig.kt`
- **Valid Values:**
  - `console` - Log emails to console (development)
  - `smtp` - Use SMTP server
  - `sendgrid` - Use SendGrid API (not yet implemented)
  - `ses` - Use AWS SES (not yet implemented)
- **Notes:**
  - `console` provider works even if `EMAIL_ENABLED=false`
  - `smtp` requires additional SMTP_* variables
  - SendGrid and SES fall back to console if not implemented

### SMTP_HOST

- **Description:** SMTP server hostname
- **Required:** Yes (when EMAIL_PROVIDER=smtp)
- **Default:** None
- **Example:** `SMTP_HOST=smtp.gmail.com`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - Common providers:
    - Gmail: `smtp.gmail.com`
    - Outlook/Office365: `smtp.office365.com`
    - SendGrid: `smtp.sendgrid.net`
    - Mailgun: `smtp.mailgun.org`

### SMTP_PORT

- **Description:** SMTP server port
- **Required:** No
- **Default:** `587`
- **Example:** `SMTP_PORT=587`
- **Used in:** `AppConfig.kt`
- **Valid Values:**
  - `587` - STARTTLS (recommended)
  - `465` - SSL/TLS
  - `25` - Plain (not recommended)
- **Notes:**
  - Port 587 with STARTTLS is the modern standard
  - Port 465 uses implicit TLS
  - Port 25 is often blocked by ISPs

### SMTP_USERNAME

- **Description:** SMTP authentication username
- **Required:** Yes (when EMAIL_PROVIDER=smtp)
- **Default:** None
- **Example:** `SMTP_USERNAME=your-email@example.com`
- **Used in:** `AppConfig.kt`
- **Security Impact:** MEDIUM
- **Notes:**
  - Often the same as your email address
  - For Gmail, use app-specific password
  - For SendGrid, use "apikey" as username

### SMTP_PASSWORD

- **Description:** SMTP authentication password
- **Required:** Yes (when EMAIL_PROVIDER=smtp)
- **Default:** None
- **Example:** `SMTP_PASSWORD=your-smtp-password`
- **Used in:** `AppConfig.kt`
- **Security Impact:** CRITICAL
- **Production Guidance:**
  - Use app-specific passwords for Gmail/Outlook
  - For SendGrid, use API key as password
  - Store securely (secrets manager)
  - Never commit to version control
- **Notes:**
  - For Gmail, generate at: https://myaccount.google.com/apppasswords
  - For Outlook, enable 2FA and create app password

### SMTP_FROM_EMAIL

- **Description:** Email address to use as sender
- **Required:** Yes (when EMAIL_PROVIDER=smtp)
- **Default:** None
- **Example:** `SMTP_FROM_EMAIL=noreply@trailglass.com`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - Must be authorized to send from SMTP server
  - Use "noreply" or "no-reply" for transactional emails
  - Must match domain verified with provider (for some providers)

### SMTP_FROM_NAME

- **Description:** Display name for email sender
- **Required:** No
- **Default:** `Trailglass`
- **Example:** `SMTP_FROM_NAME=Trailglass`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - Shows as "Trailglass <noreply@trailglass.com>" in email clients
  - Keep it simple and recognizable

### SMTP_TLS_ENABLED

- **Description:** Enable STARTTLS for SMTP connection
- **Required:** No
- **Default:** `true`
- **Example:** `SMTP_TLS_ENABLED=true`
- **Used in:** `AppConfig.kt`
- **Valid Values:** `true`, `false`
- **Security Impact:** HIGH
- **Notes:**
  - Should always be `true` in production
  - Only disable for local testing with unsecured SMTP servers
  - Required by most modern SMTP providers

---

## Rate Limiting Configuration

### GLOBAL_RATE_LIMIT_PER_MINUTE

- **Description:** Maximum number of requests per minute per IP address
- **Required:** No
- **Default:** `120`
- **Example:** `GLOBAL_RATE_LIMIT_PER_MINUTE=100`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - Applies to all endpoints globally
  - Adjust based on expected usage patterns
  - For 3-5 users, default is very permissive
  - Consider lower limits for public-facing deployments
  - Implements simple in-memory rate limiting (upgrade to Redis for multi-instance deployments)

---

## Migration Configuration

### FLYWAY_AUTO_MIGRATE

- **Description:** Enable automatic database migrations on application startup
- **Required:** No
- **Default:** `true` (development), `false` (production)
- **Example:** `FLYWAY_AUTO_MIGRATE=true`
- **Used in:** `AppConfig.kt`
- **Valid Values:** `true`, `false`
- **Notes:**
  - Automatically set based on APP_ENVIRONMENT
  - Development: auto-migrates by default
  - Production: disabled by default for safety (use dedicated Flyway service)
  - The dedicated `flyway` service in `compose.yaml` always runs migrations before app startup
- **Production Guidance:**
  - Use explicit migration service in production
  - Test migrations in staging first
  - Keep backups before major migrations

---

## Monitoring & Observability Configuration

### ENABLE_METRICS

- **Description:** Enable Prometheus/metrics endpoint
- **Required:** No
- **Default:** `false`
- **Example:** `ENABLE_METRICS=true`
- **Used in:** `AppConfig.kt`
- **Notes:**
  - Exposes metrics at `/metrics` endpoint
  - Should be protected with authentication in production

### METRICS_ENABLED

- **Description:** Alternative metrics enable flag (used in sample)
- **Required:** No
- **Default:** `false`
- **Example:** `METRICS_ENABLED=true`
- **Notes:** Prefer ENABLE_METRICS in application code

### METRICS_PATH

- **Description:** Custom path for metrics endpoint
- **Required:** No
- **Default:** `/metrics`
- **Example:** `METRICS_PATH=/metrics`
- **Notes:**
  - Primarily for documentation/reference
  - May not be configurable in current implementation

### CLOUDFLARE_ACCESS_CLIENT_ID

- **Description:** Cloudflare Access service token client ID for protecting metrics
- **Required:** No (recommended for production metrics)
- **Default:** None (empty)
- **Example:** `CLOUDFLARE_ACCESS_CLIENT_ID=your-cf-client-id.access`
- **Security Impact:** MEDIUM
- **Notes:**
  - Used to authenticate metrics scraper with Cloudflare Access
  - Pair with CLOUDFLARE_ACCESS_CLIENT_SECRET
  - Get from Cloudflare Zero Trust dashboard
  - Optional but recommended for production metrics endpoints

### CLOUDFLARE_ACCESS_CLIENT_SECRET

- **Description:** Cloudflare Access service token secret for protecting metrics
- **Required:** No (recommended for production metrics)
- **Default:** None (empty)
- **Example:** `CLOUDFLARE_ACCESS_CLIENT_SECRET=your-secret-here`
- **Security Impact:** HIGH
- **Notes:**
  - Must be used with CLOUDFLARE_ACCESS_CLIENT_ID
  - Used in request headers: `CF-Access-Client-Id` and `CF-Access-Client-Secret`
  - Protects sensitive metrics from unauthorized access
  - Rotate periodically

---

## Security Configuration

These variables are configured primarily in application code or reverse proxy configuration.

### X-Forwarded-Proto Header

- **Type:** HTTP Header (not environment variable)
- **Description:** Set by reverse proxy to indicate original request protocol
- **Required:** Yes (when behind reverse proxy)
- **Example:** `X-Forwarded-Proto: https`
- **Notes:**
  - Set automatically by Cloudflare Tunnel, Nginx, etc.
  - App uses this to enforce HTTPS when ALLOW_PLAIN_HTTP=false
  - Never set this manually from client code (security risk)

---

## Container-Specific Variables

These variables configure the PostgreSQL and MinIO containers and are loaded from `env/db.env` and `env/minio.env`.

### PostgreSQL Container (env/db.env)

#### POSTGRES_DB

- **Description:** Database name to create on first startup
- **Required:** Yes
- **Default:** None
- **Example:** `POSTGRES_DB=trailglass`
- **Notes:**
  - Used by postgres:15-alpine image initialization
  - Should match DATABASE_NAME in app.env

#### POSTGRES_USER

- **Description:** PostgreSQL superuser name to create
- **Required:** Yes
- **Default:** None
- **Example:** `POSTGRES_USER=postgres`
- **Notes:**
  - Used by postgres:15-alpine image initialization
  - Should match DATABASE_USER in app.env
  - Has superuser privileges

#### POSTGRES_PASSWORD

- **Description:** PostgreSQL superuser password
- **Required:** Yes
- **Default:** None
- **Example:** `POSTGRES_PASSWORD=your_secure_password`
- **Security Impact:** CRITICAL
- **Notes:**
  - Used by postgres:15-alpine image initialization
  - Should match DATABASE_PASSWORD in app.env
  - Change from default immediately

### MinIO Container (env/minio.env)

#### MINIO_ROOT_USER

- **Description:** MinIO root user (admin username)
- **Required:** Yes
- **Default:** None
- **Example:** `MINIO_ROOT_USER=minioadmin`
- **Security Impact:** HIGH
- **Notes:**
  - Used by minio/minio container
  - Should match STORAGE_ACCESS_KEY in app.env
  - Change from default in production

#### MINIO_ROOT_PASSWORD

- **Description:** MinIO root password (admin password)
- **Required:** Yes
- **Default:** None
- **Example:** `MINIO_ROOT_PASSWORD=minioadmin`
- **Security Impact:** CRITICAL
- **Notes:**
  - Used by minio/minio container
  - Should match STORAGE_SECRET_KEY in app.env
  - Change from default immediately
  - Minimum 8 characters

#### MINIO_REGION

- **Description:** MinIO server region designation
- **Required:** No
- **Default:** None
- **Example:** `MINIO_REGION=us-east-1`
- **Notes:**
  - Should match STORAGE_REGION in app.env
  - Primarily for compatibility with S3 clients

#### MINIO_BROWSER_REDIRECT_URL

- **Description:** URL for MinIO web console redirect
- **Required:** No
- **Default:** None
- **Example:** `MINIO_BROWSER_REDIRECT_URL=http://localhost:9001`
- **Notes:**
  - Used for accessing MinIO web UI
  - Port 9001 is console, 9000 is API
  - Not used by application backend

---

## Setup Instructions

### Local Development

1. **Copy sample files:**
   ```bash
   cd /Users/npochaev/GitHub/trailglass-backend
   cp env/app.env.sample env/app.env
   cp env/db.env.sample env/db.env
   cp env/minio.env.sample env/minio.env
   ```

2. **Secure the files:**
   ```bash
   chmod 600 env/*.env
   ```

3. **Customize values:**
   Edit each `.env` file and change at minimum:
   - `JWT_SECRET` (even in development, use something unique)
   - Database passwords if exposing ports externally
   - MinIO credentials if exposing console externally

4. **Start the stack:**
   ```bash
   docker compose up -d --build
   ```

5. **Verify health:**
   ```bash
   curl http://localhost:8080/health
   docker compose exec postgres pg_isready
   curl http://localhost:9000/minio/health/live
   ```

### Staging Environment

Follow the same steps as local development, but additionally:

1. Set `APP_ENVIRONMENT=staging`
2. Use strong, unique passwords for all secrets
3. Consider enabling metrics: `ENABLE_METRICS=true`
4. Use `DATABASE_SSL_MODE=require` if your Postgres supports it
5. Test migrations before applying to production

### Production Environment

See the [Production Deployment Checklist](#production-deployment-checklist) below.

---

## Production Deployment Checklist

Before deploying to production, verify all these critical environment variables:

### Critical Security Variables

- [ ] `APP_ENVIRONMENT=production`
- [ ] `JWT_SECRET` - Strong random value (32+ chars), never use default
- [ ] `JWT_ISSUER` - Set to your actual API domain (e.g., `https://api.trailglass.example.com`)
- [ ] `JWT_AUDIENCE` - Set to specific audience identifier
- [ ] `DATABASE_PASSWORD` - Strong password (32+ chars), rotated regularly
- [ ] `POSTGRES_PASSWORD` - Matches DATABASE_PASSWORD, strong password
- [ ] `STORAGE_SECRET_KEY` - Strong password if using S3/MinIO
- [ ] `MINIO_ROOT_PASSWORD` - Strong password if using MinIO (8+ chars minimum)
- [ ] `ALLOW_PLAIN_HTTP=false` - Must be false in production

### Database Configuration

- [ ] `DATABASE_URL` - Correct production database connection string
- [ ] `DATABASE_SSL_MODE=require` - Enforce SSL for database connections
- [ ] `DATABASE_MAX_POOL_SIZE` - Appropriate for your load (start with 10-20)
- [ ] Test database connectivity before deploying app

### Storage Configuration

- [ ] `STORAGE_BACKEND` - Choose `s3`/`minio` or `database` based on scale
- [ ] If using S3/MinIO:
  - [ ] `STORAGE_BUCKET` - Bucket exists and is accessible
  - [ ] `STORAGE_ENDPOINT` - Correct endpoint (HTTPS recommended)
  - [ ] `STORAGE_ACCESS_KEY` - Valid credentials
  - [ ] `STORAGE_SECRET_KEY` - Valid credentials
  - [ ] `STORAGE_PATH_STYLE=true` - If using MinIO
  - [ ] Test storage connectivity before deploying

### Email Configuration

- [ ] `EMAIL_ENABLED=true` - Enable email sending in production
- [ ] `EMAIL_PROVIDER` - Choose provider (smtp recommended for self-hosting)
- [ ] If using SMTP:
  - [ ] `SMTP_HOST` - Valid SMTP server hostname
  - [ ] `SMTP_PORT` - Appropriate port (587 recommended)
  - [ ] `SMTP_USERNAME` - Valid SMTP credentials
  - [ ] `SMTP_PASSWORD` - Valid SMTP credentials (use app-specific password)
  - [ ] `SMTP_FROM_EMAIL` - Authorized sender address
  - [ ] `SMTP_FROM_NAME` - Sender display name
  - [ ] `SMTP_TLS_ENABLED=true` - Enable TLS for security
  - [ ] Test email sending before deploying (send test password reset)

### Operational Variables

- [ ] `FLYWAY_AUTO_MIGRATE=false` - Disable auto-migration in production
- [ ] Run migrations via dedicated Flyway service or manual process
- [ ] `GLOBAL_RATE_LIMIT_PER_MINUTE` - Set appropriate rate limit (60-120 for small deployments)
- [ ] Configure firewall/security groups to restrict database access
- [ ] Configure firewall/security groups to restrict MinIO access

### Monitoring & Observability

- [ ] `ENABLE_METRICS=true` - Recommended for production monitoring
- [ ] `CLOUDFLARE_ACCESS_CLIENT_ID` - Set if using Cloudflare Access for metrics
- [ ] `CLOUDFLARE_ACCESS_CLIENT_SECRET` - Set if using Cloudflare Access for metrics
- [ ] Configure metrics scraping (Prometheus/Grafana)
- [ ] Set up log aggregation and alerting

### Reverse Proxy Configuration

- [ ] Cloudflare Tunnel or equivalent configured with HTTPS
- [ ] `X-Forwarded-Proto` header set by proxy
- [ ] Application bound to localhost (not 0.0.0.0 if possible)
- [ ] Firewall rules prevent direct access to application port
- [ ] Test HTTPS enforcement with `ALLOW_PLAIN_HTTP=false`

### Backup & Recovery

- [ ] Automated database backups configured (scripts/backup.sh)
- [ ] MinIO bucket backups/replication configured
- [ ] Backup restoration tested (scripts/restore.sh)
- [ ] Disaster recovery documentation complete
- [ ] Regular backup testing scheduled

### Documentation & Secrets Management

- [ ] All production secrets stored securely (not in git)
- [ ] Secrets documented in password manager or secrets vault
- [ ] Team has access to secrets in emergency
- [ ] Rotation schedule documented for all secrets
- [ ] `.env` files have `chmod 600` permissions

---

## Troubleshooting

### Application won't start - "JWT_SECRET is required"

**Symptom:** App exits immediately with:
```
[fatal] JWT_SECRET is required in production environments
```

**Solution:**
- Set `JWT_SECRET` to a strong random value in `env/app.env`
- Generate: `openssl rand -base64 32`
- Never use the default value `dev-secret-change-me`

### Application won't start - "DATABASE_URL is required"

**Symptom:** App exits with:
```
[fatal] DATABASE_URL is required for persistence configuration
```

**Solution:**
- Ensure `DATABASE_URL` is set in `env/app.env`
- Format: `jdbc:postgresql://host:port/database?sslmode=prefer`
- Or verify all individual `DATABASE_*` variables are set correctly

### Storage backend errors - "STORAGE_BUCKET is required"

**Symptom:** App exits with:
```
[fatal] STORAGE_BUCKET is required for S3 storage backend
```

**Solution:**
- If using `STORAGE_BACKEND=s3` or `minio`, set all required storage variables:
  - `STORAGE_BUCKET`
  - `STORAGE_ACCESS_KEY`
  - `STORAGE_SECRET_KEY`
  - `STORAGE_ENDPOINT` (for MinIO)
- Or switch to `STORAGE_BACKEND=database` for development/testing

### Database connection fails

**Symptoms:**
- Connection timeouts
- Authentication failures
- SSL/TLS errors

**Solutions:**

1. **Connection timeout:**
   - Check `DATABASE_HOST` is correct (use `postgres` for Docker, `localhost` for local)
   - Verify database container is running: `docker compose ps postgres`
   - Check database health: `docker compose exec postgres pg_isready`

2. **Authentication failure:**
   - Ensure `DATABASE_USER` and `DATABASE_PASSWORD` match `POSTGRES_USER` and `POSTGRES_PASSWORD`
   - Check password doesn't contain special characters that need escaping in JDBC URLs

3. **SSL errors:**
   - Set `DATABASE_SSL_MODE=disable` for local development
   - Use `prefer` or `require` for production
   - Ensure Postgres server has SSL configured if using `require`

### MinIO connection fails

**Symptoms:**
- Cannot connect to MinIO
- Authentication failures
- Bucket not found errors

**Solutions:**

1. **Connection issues:**
   - Check `STORAGE_ENDPOINT` uses correct protocol and port: `http://minio:9000` (Docker) or `https://s3.example.com`
   - Verify MinIO container is running: `docker compose ps minio`
   - Check MinIO health: `curl http://localhost:9000/minio/health/live`

2. **Authentication failures:**
   - Ensure `STORAGE_ACCESS_KEY` matches `MINIO_ROOT_USER`
   - Ensure `STORAGE_SECRET_KEY` matches `MINIO_ROOT_PASSWORD`
   - MinIO password must be at least 8 characters

3. **Bucket not found:**
   - Bucket must be created manually or via init script
   - Access MinIO console at `http://localhost:9001`
   - Or use MinIO client: `docker compose run --rm mc mb minio/trailglass`

### HTTPS enforcement issues

**Symptom:** App rejects requests with "HTTPS required" or similar

**Solution:**
- For local development: Set `ALLOW_PLAIN_HTTP=true` in `env/app.env`
- For production: Ensure reverse proxy sets `X-Forwarded-Proto: https` header
- Test header: `curl -H "X-Forwarded-Proto: https" http://localhost:8080/health`

### Rate limiting too aggressive

**Symptom:** Getting 429 "Too Many Requests" errors

**Solution:**
- Increase `GLOBAL_RATE_LIMIT_PER_MINUTE` in `env/app.env`
- Default is 120 requests/minute
- For development, you can set very high (e.g., 1000)
- For production, monitor actual usage and adjust accordingly

### Migrations not running

**Symptoms:**
- Tables not created
- Schema version mismatch errors

**Solutions:**

1. **Check Flyway service:**
   - View logs: `docker compose logs flyway`
   - Ensure service completed successfully: `docker compose ps flyway`

2. **Force auto-migration (development only):**
   - Set `FLYWAY_AUTO_MIGRATE=true` in `env/app.env`
   - Restart app: `docker compose restart app`

3. **Manual migration:**
   ```bash
   docker compose run --rm flyway \
     -url=jdbc:postgresql://postgres:5432/trailglass?sslmode=prefer \
     -user=postgres \
     -password=postgres \
     migrate
   ```

### Metrics endpoint not accessible

**Symptoms:**
- 404 on `/metrics`
- 403 Forbidden

**Solutions:**

1. **Enable metrics:**
   - Set `ENABLE_METRICS=true` in `env/app.env`
   - Restart app: `docker compose restart app`

2. **Cloudflare Access protection:**
   - Include required headers:
     ```bash
     curl -H "CF-Access-Client-Id: $ID" \
          -H "CF-Access-Client-Secret: $SECRET" \
          http://localhost:8080/metrics
     ```
   - Verify `CLOUDFLARE_ACCESS_CLIENT_ID` and `CLOUDFLARE_ACCESS_CLIENT_SECRET` are correct

### Email sending fails

**Symptoms:**
- Password reset emails not received
- SMTP authentication errors
- Connection timeout to SMTP server

**Solutions:**

1. **Email not enabled:**
   - Check `EMAIL_ENABLED=true` in `env/app.env`
   - Check `EMAIL_PROVIDER` is set correctly (smtp/console)
   - Restart app after changes: `docker compose restart app`

2. **SMTP authentication failure:**
   - Verify `SMTP_USERNAME` and `SMTP_PASSWORD` are correct
   - For Gmail: Use app-specific password (not regular password)
     - Generate at: https://myaccount.google.com/apppasswords
     - Requires 2-factor authentication enabled
   - For Outlook: Enable 2FA and create app password
   - Check if account requires "less secure app access" (older providers)

3. **Connection timeout:**
   - Check `SMTP_HOST` and `SMTP_PORT` are correct
   - Common ports: 587 (STARTTLS), 465 (SSL/TLS), 25 (plain)
   - Verify firewall allows outbound connections on SMTP port
   - Try telnet test: `telnet smtp.example.com 587`

4. **TLS/SSL errors:**
   - Ensure `SMTP_TLS_ENABLED=true` for port 587
   - Set to `false` only for port 465 (implicit TLS)
   - Check SMTP server supports TLS version (1.2+)

5. **Development/Testing:**
   - Use `EMAIL_PROVIDER=console` to log emails to console
   - Check application logs: `docker compose logs app | grep EMAIL`
   - Test with a service like Mailtrap (https://mailtrap.io) for development

6. **Email not received but no errors:**
   - Check spam/junk folder
   - Verify sender email (`SMTP_FROM_EMAIL`) is authorized
   - Check email provider's sender reputation
   - Review SMTP provider logs/dashboard

### Environment variables not loading

**Symptoms:**
- App using default values despite .env files
- "Variable not found" errors

**Solutions:**

1. **Check file locations:**
   - Files must be in `env/` directory relative to `compose.yaml`
   - Verify: `ls -la env/`

2. **Check file permissions:**
   - Should be readable: `chmod 600 env/*.env`
   - Owner should match Docker user

3. **Check file format:**
   - Use `KEY=value` format (no spaces around `=`)
   - No quotes needed unless value contains spaces
   - Comments start with `#`
   - No trailing whitespace

4. **Verify compose file:**
   - Check `env_file` directives in `compose.yaml`
   - Ensure paths are correct

5. **Rebuild containers:**
   ```bash
   docker compose down
   docker compose up -d --build
   ```

---

## Reference Files

This documentation is based on the following source files:

- **Application Config:** `/src/main/kotlin/com/trailglass/backend/config/AppConfig.kt`
- **Sample Files:**
  - `/env/app.env.sample`
  - `/env/db.env.sample`
  - `/env/minio.env.sample`
- **Deployment:**
  - `/compose.yaml`
  - `/docs/SELF_HOSTING.md`
  - `/docs/ARCHITECTURE.md`
- **Documentation:** `/README.md`

For API-specific configuration (headers, authentication), see `/docs/backend-api-specification.md`.

---

## Change Log

When updating environment variables:

1. Update the variable in `AppConfig.kt` or relevant configuration file
2. Update corresponding `.env.sample` file(s)
3. Document the change in this file
4. Update the [Production Deployment Checklist](#production-deployment-checklist) if security-relevant
5. Add migration notes if the change affects existing deployments

---

**Last Updated:** 2025-11-21
**Maintained by:** Trailglass Backend Team
