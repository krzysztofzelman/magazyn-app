#!/bin/bash
# Database backup script for magazyn-app
# Runs pg_dump inside the postgres container and writes to /backups on the host
set -euo pipefail

BACKUP_DIR="/backups"
DB_NAME="magazyn_db"
RETENTION_DAYS=7

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="${BACKUP_DIR}/magazyn_${TIMESTAMP}.sql"

pg_dump -h postgres -U "${PGUSER:-magazyn}" -d "$DB_NAME" > "$FILENAME"

# compress with gzip
gzip "$FILENAME"
echo "Backup created: ${FILENAME}.gz"

# remove backups older than RETENTION_DAYS
find "$BACKUP_DIR" -name "magazyn_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "Old backups cleaned (retention: ${RETENTION_DAYS} days)"
