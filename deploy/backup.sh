#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Nightly PostgreSQL Backup with zstd Compression
# Database: matchmate_db
# Retention: 7 daily backups, 4 weekly backups
# Executed via the 'postgres' user's crontab:
#   0 2 * * * /usr/local/bin/backup-matchmate-db.sh >> /var/log/postgresql/backup.log 2>&1
# ==============================================================================

DB_NAME="matchmate_db"
BACKUP_BASE="/var/backups/postgresql"
DAILY_DIR="${BACKUP_BASE}/daily"
WEEKLY_DIR="${BACKUP_BASE}/weekly"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DAY_OF_WEEK=$(date +"%u") # 1 (Mon) .. 7 (Sun)

mkdir -p "${DAILY_DIR}" "${WEEKLY_DIR}"
chmod 700 "${BACKUP_BASE}" "${DAILY_DIR}" "${WEEKLY_DIR}"

DAILY_FILE="${DAILY_DIR}/${DB_NAME}-daily-${TIMESTAMP}.dump.zst"
echo "[$(date -Iseconds)] Starting backup for database '${DB_NAME}'..."

# Dump and compress using zstd (level 3: fast and high compression ratio)
pg_dump -Fc "${DB_NAME}" | zstd -q -3 -o "${DAILY_FILE}"
chmod 600 "${DAILY_FILE}"
echo "[$(date -Iseconds)] Daily backup created: ${DAILY_FILE} ($(du -h "${DAILY_FILE}" | cut -f1))"

# On Sunday (day 7), preserve as weekly backup
if [ "${DAY_OF_WEEK}" -eq 7 ]; then
    WEEKLY_FILE="${WEEKLY_DIR}/${DB_NAME}-weekly-${TIMESTAMP}.dump.zst"
    cp -p "${DAILY_FILE}" "${WEEKLY_FILE}"
    chmod 600 "${WEEKLY_FILE}"
    echo "[$(date -Iseconds)] Weekly backup created: ${WEEKLY_FILE}"
fi

# Prune daily backups older than 7 days
echo "[$(date -Iseconds)] Pruning daily backups older than 7 days..."
find "${DAILY_DIR}" -type f -name "${DB_NAME}-daily-*.dump.zst" -mtime +7 -delete

# Prune weekly backups older than 28 days (4 weeks)
echo "[$(date -Iseconds)] Pruning weekly backups older than 28 days..."
find "${WEEKLY_DIR}" -type f -name "${DB_NAME}-weekly-*.dump.zst" -mtime +28 -delete

echo "[$(date -Iseconds)] Backup job completed successfully."
