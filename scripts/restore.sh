#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR=${BACKUP_DIR:-"$(pwd)/backups"}
DB_ENV_FILE=${DB_ENV_FILE:-"env/db.env"}
MINIO_ENV_FILE=${MINIO_ENV_FILE:-"env/minio.env"}

if [[ -f "$DB_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$DB_ENV_FILE"
fi
if [[ -f "$MINIO_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$MINIO_ENV_FILE"
fi

LATEST_PG=$(ls -1 "$BACKUP_DIR/postgres"/postgres-*.sql 2>/dev/null | sort | tail -n 1)
if [[ -z "${LATEST_PG:-}" ]]; then
  echo "No Postgres backup found in $BACKUP_DIR/postgres" >&2
  exit 1
fi

echo "[+] Restoring Postgres from $LATEST_PG"
cat "$LATEST_PG" | docker compose exec -T postgres psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-trailglass}"

echo "[+] Restoring MinIO bucket data"
docker compose --profile ops run --rm \
  -v "$BACKUP_DIR/minio:/backup" \
  mc sh -c "mc alias set minio http://minio:9000 ${MINIO_ROOT_USER:-minioadmin} ${MINIO_ROOT_PASSWORD:-minioadmin} >/dev/null && mc mirror /backup/${MINIO_BUCKET:-trailglass} minio/${MINIO_BUCKET:-trailglass}"

echo "[✓] Restore complete"
