#!/bin/bash
set -euo pipefail

CONTAINER="solar-postgres"
BACKUP_DIR="$(cd "$(dirname "$0")/.." && pwd)/backups"

# Determine which backup to restore
if [ $# -ge 1 ]; then
  BACKUP_FILE="$1"
else
  BACKUP_FILE=$(ls -1t "$BACKUP_DIR"/solar_backup_*.sql 2>/dev/null | head -1)
  if [ -z "$BACKUP_FILE" ]; then
    echo "Error: No backup files found in ${BACKUP_DIR}"
    exit 1
  fi
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Error: Backup file not found: ${BACKUP_FILE}"
  exit 1
fi

# Check container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "Error: Container '${CONTAINER}' is not running."
  exit 1
fi

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "Backup file: ${BACKUP_FILE} (${FILE_SIZE})"
echo ""
echo "WARNING: This will overwrite data in solar_management and keycloak databases."
read -rp "Are you sure? (y/N): " confirm

if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 0
fi

echo "Restoring databases..."

# Split the backup file and restore each database
# Extract solar_management section
sed -n '/^-- Database: solar_management/,/^-- Database: keycloak/p' "$BACKUP_FILE" | head -n -3 | \
  docker exec -i "$CONTAINER" psql -U postgres -d solar_management

# Extract keycloak section
sed -n '/^-- Database: keycloak/,$p' "$BACKUP_FILE" | \
  docker exec -i "$CONTAINER" psql -U postgres -d keycloak

echo "Restore complete."
