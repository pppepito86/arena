#!/bin/bash
set -e

# Setup environment variables
export SPRING_PROFILES_ACTIVE=dev,e2e
E2E_WORK_DIR="/tmp/arena_e2e_workdir"
DUMP_FILE="src/test/resources/e2e/dump.sql"

# Handle --open flag
CY_CMD="npm run cypress:run"
if [[ "$1" == "--open" ]]; then
  CY_CMD="npm run cypress:open"
fi

echo "==========================================="
echo " Setting up E2E environment"
echo "==========================================="

# Clean and setup work directory
echo "Cleaning work directory: $E2E_WORK_DIR"
rm -rf "$E2E_WORK_DIR"
mkdir -p "$E2E_WORK_DIR"

# Reset Database from Dump
echo "Resetting E2E database (arena_e2e) from dump..."
mysql -u root -ppassword -e "DROP DATABASE IF EXISTS arena_e2e; CREATE DATABASE arena_e2e;"
# Load dump and replace arena_dev with arena_e2e if needed
sed 's/arena_dev/arena_e2e/g' "$DUMP_FILE" | mysql -u root -ppassword arena_e2e

# Run Liquibase to ensure any recent changes are applied
echo "Running Liquibase updates on e2e database..."
./mvnw liquibase:update -Pe2e

echo "==========================================="
echo " Starting servers and Cypress ($CY_CMD)"
echo "==========================================="

# Orchestrate:
# 1. Start Spring Boot Backend (port 8080)
# 2. Start Webpack Dev Server (port 9060)
# 3. Wait for both and run Cypress command
npx start-server-and-test \
  "./mvnw -Pe2e" http://localhost:8080 \
  "npm start" http://localhost:9060 \
  "$CY_CMD"
