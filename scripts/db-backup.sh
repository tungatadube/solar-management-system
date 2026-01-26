#!/bin/bash
set -euo pipefail

CONTAINER="solar-postgres"
BACKUP_DIR="$(cd "$(dirname "$0")/.." && pwd)/backups"
TIMESTAMP=$(date +"%Y-%m-%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/solar_backup_${TIMESTAMP}.sql"
MAX_BACKUPS=10

# Check container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "Error: Container '${CONTAINER}' is not running."
  exit 1
fi

mkdir -p "$BACKUP_DIR"

echo "Backing up databases..."

# Dump both databases into a single file
{
  echo "-- Solar Management System Database Backup"
  echo "-- Created: $(date)"
  echo ""

  echo "-- ========================================"
  echo "-- Database: solar_management"
  echo "-- ========================================"
  docker exec "$CONTAINER" pg_dump -U postgres --clean --if-exists solar_management

  echo ""
  echo "-- ========================================"
  echo "-- Database: keycloak"
  echo "-- ========================================"
  docker exec "$CONTAINER" pg_dump -U postgres --clean --if-exists keycloak
} > "$BACKUP_FILE"

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "Backup saved: ${BACKUP_FILE} (${FILE_SIZE})"

# Remove old backups beyond MAX_BACKUPS
BACKUP_COUNT=$(ls -1 "$BACKUP_DIR"/solar_backup_*.sql 2>/dev/null | wc -l | tr -d ' ')
if [ "$BACKUP_COUNT" -gt "$MAX_BACKUPS" ]; then
  REMOVE_COUNT=$((BACKUP_COUNT - MAX_BACKUPS))
  ls -1t "$BACKUP_DIR"/solar_backup_*.sql | tail -n "$REMOVE_COUNT" | while read -r old; do
    echo "Removing old backup: $(basename "$old")"
    rm "$old"
  done
fi

echo "Done. Total backups: $(ls -1 "$BACKUP_DIR"/solar_backup_*.sql 2>/dev/null | wc -l | tr -d ' ')"
