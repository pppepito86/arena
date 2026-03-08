#!/bin/bash
set -e

# Configuration
DEV_DB="arena_dev"
DUMP_PATH="src/test/resources/e2e/dump.sql"
DB_USER="root"
DB_PASS="password"

echo "==========================================="
echo " Creating E2E dump from $DEV_DB"
echo "==========================================="

mkdir -p src/test/resources/e2e

# Perform the dump
echo "Dumping $DEV_DB to $DUMP_PATH..."
mysqldump -u "$DB_USER" -p"$DB_PASS" --databases "$DEV_DB" > "$DUMP_PATH"

echo "Success! Dump created at $DUMP_PATH"
echo "You can now run 'npm run e2e' to use this data."
