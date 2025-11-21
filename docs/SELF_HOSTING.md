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
 │   └── db.env
 └── data/
     ├── postgres/
     └── minio/
```
- Ktor container should use an ARM base image (`eclipse-temurin:21-jre-jammy`).
- Sample `compose.yaml`:
```yaml
services:
  app:
    image: ghcr.io/po4yka/trailglass-backend:latest
    env_file: env/app.env
    depends_on: [postgres, minio]
    ports:
      - "8080:8080"  # local only
  postgres:
    image: postgres:15
    env_file: env/db.env
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
  minio:
    image: minio/minio
    command: server /data
    env_file: env/minio.env
    volumes:
      - ./data/minio:/data
    ports:
      - "9000:9000"
```

## 4. Cloudflare Tunnel
1. Create Cloudflare Zero Trust account + add your domain.
2. Install cloudflared:
   ```bash
   curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb -o cloudflared.deb
   sudo dpkg -i cloudflared.deb
   ```
3. Login: `cloudflared tunnel login`.
4. Create tunnel: `cloudflared tunnel create trailglass-backend`.
5. Config `~/.cloudflared/config.yml`:
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
4. Run Flyway migrations (via app startup or a dedicated container).
5. Smoke test: `curl -H "Host: api.example.com" http://localhost:8080/health`.

## 8. Maintenance
- Regularly `apt upgrade` and refresh container images.
- Monitor disk + logs: `df -h`, `docker compose logs -f app`.
- For observability, optionally expose Prometheus metrics behind Cloudflare Access.
- Document recovery steps (restore DB + MinIO, reconfigure tunnel) for disaster recovery.

Cloudflare Tunnel provides HTTPS and DDoS shielding while keeping your residential IP hidden—ideal for Trailglass' low-user backend.
