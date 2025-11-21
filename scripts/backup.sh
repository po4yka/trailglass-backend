#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR=${BACKUP_DIR:-"$(pwd)/backups"}
APP_ENV_FILE=${APP_ENV_FILE:-"env/app.env"}
DB_ENV_FILE=${DB_ENV_FILE:-"env/db.env"}
MINIO_ENV_FILE=${MINIO_ENV_FILE:-"env/minio.env"}

mkdir -p "$BACKUP_DIR/postgres" "$BACKUP_DIR/minio"

if [[ -f "$DB_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$DB_ENV_FILE"
fi
if [[ -f "$MINIO_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$MINIO_ENV_FILE"
fi

TIMESTAMP=$(date +%Y%m%dT%H%M%S)
PG_BACKUP="$BACKUP_DIR/postgres/postgres-$TIMESTAMP.sql"

echo "[+] Dumping Postgres to $PG_BACKUP"
docker compose exec -T postgres pg_dump -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-trailglass}" > "$PG_BACKUP"

echo "[+] Syncing MinIO bucket to $BACKUP_DIR/minio"
docker compose --profile ops run --rm \
  -v "$BACKUP_DIR/minio:/backup" \
  mc sh -c "mc alias set minio http://minio:9000 ${MINIO_ROOT_USER:-minioadmin} ${MINIO_ROOT_PASSWORD:-minioadmin} >/dev/null && mc mirror minio/${MINIO_BUCKET:-trailglass} /backup/${MINIO_BUCKET:-trailglass}"

echo "[✓] Backup complete"
