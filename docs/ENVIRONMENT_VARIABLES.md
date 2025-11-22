# Environment Variables

Configuration reference for the Trailglass backend application.

## Quick Start

```bash
cp env/app.env.sample env/app.env
cp env/db.env.sample env/db.env
cp env/minio.env.sample env/minio.env
chmod 600 env/*.env
```

Edit the files to customize for your environment. Never commit `*.env` files to version control.

## Application Configuration (env/app.env)

### Server

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_PORT` / `PORT` | `8080` | Server port |
| `APP_HOST` | `0.0.0.0` | Server bind address |
| `APP_ENVIRONMENT` | `development` | Environment: `development`, `staging`, `production` |
| `ALLOW_PLAIN_HTTP` | `false` | Allow HTTP (dev only; MUST be false in production) |

### Database

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | Yes | Full JDBC URL: `jdbc:postgresql://host:port/db?sslmode=prefer` |
| `DATABASE_HOST` | If no URL | Postgres hostname (e.g., `postgres` for Docker) |
| `DATABASE_PORT` | No | Postgres port (default: `5432`) |
| `DATABASE_NAME` | If no URL | Database name |
| `DATABASE_USER` | Yes | Database username |
| `DATABASE_PASSWORD` | Yes | Database password (use strong random value) |
| `DATABASE_SSL_MODE` | No | SSL mode: `disable`, `prefer`, `require` (default: `prefer`) |
| `DATABASE_MAX_POOL_SIZE` | No | Connection pool size (default: `5`) |

### JWT Authentication

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_SECRET` | Yes | Signing key (CRITICAL: generate with `openssl rand -base64 32`) |
| `JWT_ISSUER` | No | Issuer claim (default: `trailglass`) |
| `JWT_AUDIENCE` | No | Audience claim (default: `trailglass-clients`) |

### Storage (S3/MinIO)

| Variable | Default | Description |
|----------|---------|-------------|
| `STORAGE_BACKEND` | `database` | Backend: `database`/`postgres` or `s3`/`minio` |
| `STORAGE_BUCKET` | `trailglass` | S3/MinIO bucket name (required if backend=s3) |
| `STORAGE_REGION` | `us-east-1` | AWS region or MinIO region |
| `STORAGE_ENDPOINT` | - | S3 endpoint (required for MinIO, e.g., `http://minio:9000`) |
| `STORAGE_ACCESS_KEY` | - | Access key (required if backend=s3) |
| `STORAGE_SECRET_KEY` | - | Secret key (required if backend=s3) |
| `STORAGE_PATH_STYLE` | `true` | Use path-style URLs (true for MinIO) |

### Email

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_ENABLED` | `false` | Enable email sending |
| `EMAIL_PROVIDER` | `console` | Provider: `console`, `smtp`, `sendgrid`, `ses` |
| `SMTP_HOST` | - | SMTP hostname (required if provider=smtp) |
| `SMTP_PORT` | `587` | SMTP port (587=STARTTLS, 465=SSL) |
| `SMTP_USERNAME` | - | SMTP username |
| `SMTP_PASSWORD` | - | SMTP password (use app-specific password for Gmail) |
| `SMTP_FROM_EMAIL` | - | Sender email address |
| `SMTP_FROM_NAME` | `Trailglass` | Sender display name |
| `SMTP_TLS_ENABLED` | `true` | Enable STARTTLS (should be true) |

### Other Settings

| Variable | Default | Description |
|----------|---------|-------------|
| `GLOBAL_RATE_LIMIT_PER_MINUTE` | `120` | Global rate limit per IP |
| `FLYWAY_AUTO_MIGRATE` | auto | Auto-migrate database (auto-set based on environment) |
| `ENABLE_METRICS` / `METRICS_ENABLED` | `false` | Enable `/metrics` endpoint |
| `CLOUDFLARE_ACCESS_CLIENT_ID` | - | Cloudflare Access client ID for metrics |
| `CLOUDFLARE_ACCESS_CLIENT_SECRET` | - | Cloudflare Access secret for metrics |

## Database Container (env/db.env)

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_DB` | Yes | Database name (must match `DATABASE_NAME`) |
| `POSTGRES_USER` | Yes | Postgres username (must match `DATABASE_USER`) |
| `POSTGRES_PASSWORD` | Yes | Postgres password (must match `DATABASE_PASSWORD`) |

## MinIO Container (env/minio.env)

| Variable | Required | Description |
|----------|----------|-------------|
| `MINIO_ROOT_USER` | Yes | MinIO admin username (must match `STORAGE_ACCESS_KEY`) |
| `MINIO_ROOT_PASSWORD` | Yes | MinIO admin password (must match `STORAGE_SECRET_KEY`, min 8 chars) |
| `MINIO_REGION` | No | MinIO region (should match `STORAGE_REGION`) |

## Production Checklist

Before deploying to production:

### Critical Security
- [ ] `APP_ENVIRONMENT=production`
- [ ] `ALLOW_PLAIN_HTTP=false`
- [ ] `JWT_SECRET` - Strong random value, never use default
- [ ] `DATABASE_PASSWORD` - Strong password (32+ chars)
- [ ] `DATABASE_SSL_MODE=require` (if supported)
- [ ] `STORAGE_SECRET_KEY` - Strong password if using S3/MinIO
- [ ] All passwords rotated from development defaults

### Database
- [ ] `DATABASE_URL` points to production database
- [ ] `DATABASE_MAX_POOL_SIZE` appropriate for load
- [ ] Database backups configured (`scripts/backup.sh`)

### Storage
- [ ] `STORAGE_BACKEND` selected (`s3` recommended for production)
- [ ] If S3/MinIO: bucket exists, credentials valid, connectivity tested
- [ ] Storage backups configured

### Email
- [ ] `EMAIL_ENABLED=true`
- [ ] `EMAIL_PROVIDER=smtp` (or other)
- [ ] SMTP credentials valid and tested
- [ ] `SMTP_TLS_ENABLED=true`

### Operations
- [ ] `FLYWAY_AUTO_MIGRATE=false` (use dedicated migration service)
- [ ] Cloudflare Tunnel or reverse proxy configured
- [ ] `X-Forwarded-Proto` header set by proxy
- [ ] Firewall rules restrict database/MinIO access
- [ ] Metrics enabled and secured with Cloudflare Access
- [ ] Log aggregation configured
- [ ] Disaster recovery plan documented

### Secrets Management
- [ ] All `.env` files have `chmod 600`
- [ ] Secrets stored in password manager/vault
- [ ] Rotation schedule documented

## Common Issues

**App won't start - "JWT_SECRET required"**
- Set `JWT_SECRET` in `env/app.env`
- Generate: `openssl rand -base64 32`

**App won't start - "DATABASE_URL required"**
- Ensure `DATABASE_URL` is set with correct format
- Or verify all `DATABASE_*` variables are set

**Database connection fails**
- Check `DATABASE_HOST` (use `postgres` for Docker)
- Verify database is running: `docker compose ps postgres`
- Ensure credentials match between `app.env` and `db.env`

**Storage backend errors**
- If using `STORAGE_BACKEND=s3`, set all required variables: `STORAGE_BUCKET`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`, `STORAGE_ENDPOINT`
- Or use `STORAGE_BACKEND=database` for testing

**HTTPS enforcement issues**
- For dev: Set `ALLOW_PLAIN_HTTP=true`
- For prod: Ensure proxy sets `X-Forwarded-Proto: https`

**Email not working**
- Check `EMAIL_ENABLED=true` and `EMAIL_PROVIDER` is set
- For Gmail: Use app-specific password, not regular password
- Test SMTP connectivity: `telnet smtp.example.com 587`
- For testing: Use `EMAIL_PROVIDER=console` to log to console

For detailed configuration and additional troubleshooting, see the sample files in `env/*.env.sample`.
