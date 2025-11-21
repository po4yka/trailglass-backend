# Self-Hosting on Raspberry Pi + Cloudflare Tunnel

Run the Trailglass backend from home without a public IP by pairing a Raspberry Pi with Cloudflare Tunnel.

## 1. Hardware & OS
- Raspberry Pi 4 (4–8 GB RAM) or newer.
- 64-bit OS (Raspberry Pi OS Lite or Ubuntu Server 24.04 arm64).
- Prefer USB SSD over SD card for Postgres durability.

## 2. Base Setup
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl ufw docker docker-compose-plugin
```
- Create non-root deploy user, enable SSH keys, disable password auth.
- Configure UFW to allow only SSH; all web traffic will flow through the tunnel.

## 3. Docker Stack
Layout example:
```
/opt/trailglass-backend/
 ├── compose.yaml
 ├── env/
 │   ├── app.env
 │   ├── db.env
 │   └── minio.env
 ├── backups/
 │   ├── postgres/
 │   └── minio/
 └── data/
     ├── postgres/
     └── minio/
```

Copy the provided samples and edit secrets:
```bash
cp env/app.env.sample env/app.env
cp env/db.env.sample env/db.env
cp env/minio.env.sample env/minio.env
chmod 600 env/*.env
```

`compose.yaml` (in repo root) builds an ARM-friendly image, runs Flyway before the app, and wires in healthchecks:
```yaml
services:
  app:
    image: ghcr.io/po4yka/trailglass-backend:latest
    build: .
    env_file: env/app.env
    depends_on:
      flyway:
        condition: service_completed_successfully
      postgres:
        condition: service_healthy
      minio:
        condition: service_healthy
    ports:
      - "8080:8080"  # keep localhost-only
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

  flyway:
    image: flyway/flyway:10-alpine
    command: >-
      -connectRetries=10
      -url=jdbc:postgresql://postgres:5432/${POSTGRES_DB}?sslmode=prefer
      -user=${POSTGRES_USER}
      -password=${POSTGRES_PASSWORD}
      -locations=filesystem:/flyway/sql
      migrate
    env_file: env/db.env
    volumes:
      - ./db/migration:/flyway/sql:ro
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:15-alpine
    env_file: env/db.env
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 5

  minio:
    image: minio/minio:RELEASE.2024-09-22T00-33-43Z
    command: server /data --console-address :9001
    env_file: env/minio.env
    volumes:
      - ./data/minio:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9000/minio/health/live || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  mc:
    image: minio/mc:RELEASE.2024-09-18T17-19-38Z
    profiles: ["ops"]
    entrypoint: ["/bin/sh", "-c", "sleep 3600"]
    depends_on:
      minio:
        condition: service_healthy
    env_file: env/minio.env
    volumes:
      - ./backups/minio:/backup
```
Flyway migration SQL lives in `db/migration` and is consumed both by the dedicated `flyway` job and by the app on startup.

## 4. Cloudflare Tunnel
1. Create Cloudflare Zero Trust account + add your domain.
2. Install cloudflared:
   ```bash
   curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb -o cloudflared.deb
   sudo dpkg -i cloudflared.deb
   ```
3. Login: `cloudflared tunnel login`.
4. Create tunnel: `cloudflared tunnel create trailglass-backend`.
5. Config `~/.cloudflared/config.yml` (add a Cloudflare Access policy if exposing `/metrics`):
   ```yaml
   tunnel: trailglass-backend
   credentials-file: /home/pi/.cloudflared/trailglass-backend.json
   ingress:
     - hostname: api.example.com
       service: http://localhost:8080
     - service: http_status:404
   ```
6. Install service: `sudo cloudflared service install && sudo systemctl enable --now cloudflared`.
7. Add DNS CNAME for `api.example.com` → tunnel target in Cloudflare dashboard.

## 5. TLS & App Config
- Cloudflare terminates TLS; ensure Ktor trusts `X-Forwarded-Proto` and rejects non-HTTPS when behind the tunnel.
- Keep port 8080 bound to localhost; only cloudflared should access it.

## 6. Secrets & Storage
- Store secrets in `env/*.env` with `chmod 600`.
- Nightly `pg_dump` backups to `/opt/trailglass-backend/backups`, optionally sync to Cloudflare R2 or another machine.
- Mirror MinIO data using `mc mirror` or periodic rsync.

## 7. Deployment Workflow
1. Build multi-arch image: `docker buildx build --platform linux/amd64,linux/arm64 -t ghcr.io/... .`.
2. Push image to registry.
3. On Pi: `docker compose pull && docker compose up -d`.
4. Smoke test health, DB, MinIO, and metrics (if enabled):
   ```bash
   curl -H "Host: api.example.com" http://localhost:8080/health
   docker compose exec postgres pg_isready
   curl http://localhost:9000/minio/health/live
   curl -H "CF-Access-Client-Id: $ID" -H "CF-Access-Client-Secret: $SECRET" http://localhost:8080/metrics  # if METRICS_ENABLED=true
   ```

## 8. Maintenance
- Regularly `apt upgrade` and refresh container images.
- Monitor disk + logs: `df -h`, `docker compose logs -f app`.
- For observability, set `METRICS_ENABLED=true` and secure `/metrics` with a Cloudflare Access service token (`CLOUDFLARE_ACCESS_CLIENT_ID/SECRET`).
- Backups & restores:
  - `./scripts/backup.sh` → dumps Postgres and mirrors MinIO buckets into `./backups`.
  - `./scripts/restore.sh` → restores the newest Postgres dump and MinIO snapshot.
- Keep `ALLOW_PLAIN_HTTP=false` (default) so the app rejects non-HTTPS traffic behind the tunnel.
- Document recovery steps (restore DB + MinIO, reconfigure tunnel) for disaster recovery.

Cloudflare Tunnel provides HTTPS and DDoS shielding while keeping your residential IP hidden—ideal for Trailglass' low-user backend.
