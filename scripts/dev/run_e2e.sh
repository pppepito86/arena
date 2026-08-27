#!/bin/bash
set -e

# Setup environment variables
export SPRING_PROFILES_ACTIVE=dev,e2e
# arena and worker both enforce JDK 1.8 (see maven-enforcer-plugin in pom.xml),
# so pin it explicitly rather than depending on the shell's default `java`.
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"
# The webpack 4 / webpack-dev-server 3 toolchain here predates modern Node and
# silently dies partway through a build on Node 24 (the machine default), while
# Cypress 15's own engines field requires >=20.1. Node 22 satisfies both. Uses
# `n` (https://github.com/tj/n) if available; falls back to the shell's default
# `node` otherwise, matching the "n" tool's own PATH-prepending convention.
if command -v n >/dev/null 2>&1; then
  export PATH="$(dirname "$(n which 22 2>/dev/null || n which 22.23.2)")":"$PATH"
fi
E2E_WORK_DIR="/tmp/arena_e2e_workdir"
WORKER_WORK_DIR="/tmp/arena_e2e_worker_workdir"
WORKER_PORT=8189
DUMP_FILE="src/test/resources/e2e/dump.sql"
PROBLEM_FIXTURES_DIR="src/test/resources/e2e/fixtures/problems"

# Handle --open flag
CY_CMD="npm run cypress:run"
if [[ "$1" == "--open" ]]; then
  CY_CMD="npm run cypress:open"
fi

echo "==========================================="
echo " Setting up E2E environment"
echo "==========================================="

# Clean and setup work directories
echo "Cleaning work directories: $E2E_WORK_DIR, $WORKER_WORK_DIR"
rm -rf "$E2E_WORK_DIR" "$WORKER_WORK_DIR"
mkdir -p "$E2E_WORK_DIR"

# Seed problem archive fixtures (checked into the repo) into arena's work dir,
# since real problem grading needs the actual problem.zip on disk, not just DB rows.
echo "Seeding problem archive fixtures into $E2E_WORK_DIR"
for problem_dir in "$PROBLEM_FIXTURES_DIR"/*/; do
  problem_id="$(basename "$problem_dir")"
  mkdir -p "$E2E_WORK_DIR/problems/$problem_id"
  cp "$problem_dir/problem.zip" "$E2E_WORK_DIR/problems/$problem_id/problem.zip"
done

# Reset Database from Dump
echo "Resetting E2E database (arena_e2e) from dump..."
mysql -u root -ppassword -e "DROP DATABASE IF EXISTS arena_e2e; CREATE DATABASE arena_e2e;"
# Load dump and replace arena_dev with arena_e2e if needed
sed 's/arena_dev/arena_e2e/g' "$DUMP_FILE" | mysql -u root -ppassword arena_e2e

# The dump is a production snapshot and carries thousands of old submissions
# still marked waiting/judging. Once a real worker is running (below), the
# scheduled GraderTask would immediately try to re-grade all of them, which
# just fails noisily (their source files were never part of the dump) and
# floods the logs. Mark them as a distinct terminal verdict so they're left
# alone and only the submissions the e2e specs actually create get graded.
echo "Marking stale waiting/judging submissions from the dump as skipped..."
mysql -u root -ppassword arena_e2e -e "UPDATE submission SET verdict = 'SKIPPED' WHERE verdict IN ('waiting', 'judging');"

# The submission list pages are per-user for non-admins (see
# CompetitionResource.getCompetitionSubmissions / SubmissionResource.getAllSubmissions),
# and the dump's testuser has almost none, so there would be nothing to page
# through. Seed enough submissions spread across the root competition's subtree
# that /submission and /catalog/1/submissions both have several pages. These are
# metadata-only rows (no source file on disk) — the specs list them, never open
# them.
echo "Seeding paginated submissions for testuser..."
mysql -u root -ppassword arena_e2e -e "
SET @testuser := (SELECT id FROM jhi_user WHERE login = 'testuser');
INSERT INTO submission (verdict, points, time_in_millis, upload_date, user_id, competition_problem_id)
WITH RECURSIVE subtree AS (
    SELECT id FROM competition WHERE id = 1
    UNION ALL
    SELECT c.id FROM competition c JOIN subtree s ON c.parent_id = s.id
),
picked AS (
    SELECT cp.id AS cp_id, ROW_NUMBER() OVER (ORDER BY cp.id) AS rn
    FROM competition_problem cp JOIN subtree s ON cp.competition_id = s.id
)
SELECT CASE WHEN rn % 3 = 0 THEN 'OK' WHEN rn % 3 = 1 THEN 'WA' ELSE 'TL' END,
       CASE WHEN rn % 3 = 0 THEN 100 ELSE 0 END,
       rn * 10,
       DATE_SUB(NOW(), INTERVAL rn DAY),
       @testuser, cp_id
FROM picked WHERE rn <= 45;"

# The admin audit log spec needs to log in as an admin. The dump's real admins
# carry production password hashes, and testuser has to stay a plain ROLE_USER
# for the other specs, so add a dedicated admin that reuses testuser's password
# ('testpass'), plus a throwaway user it can activate/deactivate without
# disturbing testuser.
echo "Seeding an admin and an audit target user..."
mysql -u root -ppassword arena_e2e -e "
INSERT INTO jhi_user (login, password_hash, first_name, last_name, email, activated, lang_key, created_by)
SELECT 'testadmin', password_hash, 'Test', 'Admin', 'testadmin@test.com', b'1', 'en', 'system'
FROM jhi_user WHERE login = 'testuser';

INSERT INTO jhi_user_authority (user_id, authority_name)
SELECT u.id, a.name
FROM jhi_user u CROSS JOIN (SELECT 'ROLE_ADMIN' AS name UNION SELECT 'ROLE_USER') a
WHERE u.login = 'testadmin';

INSERT INTO jhi_user (login, password_hash, first_name, last_name, email, activated, lang_key, created_by)
SELECT 'audittarget', password_hash, 'Audit', 'Target', 'audittarget@test.com', b'1', 'en', 'system'
FROM jhi_user WHERE login = 'testuser';

INSERT INTO jhi_user_authority (user_id, authority_name)
SELECT id, 'ROLE_USER' FROM jhi_user WHERE login = 'audittarget';"

# Run Liquibase to ensure any recent changes are applied
echo "Running Liquibase updates on e2e database..."
./mvnw liquibase:update -Pe2e

echo "==========================================="
echo " Starting the grading worker (port $WORKER_PORT)"
echo "==========================================="

# Run the sibling worker/ service out-of-process, configured entirely via
# spring-boot:run program arguments so nothing is written into worker/'s checkout.
# Note: worker is on the Spring Boot 1.x plugin, which reads program arguments
# from the comma-separated `-Drun.arguments` system property (the 2.x-style
# `-Dspring-boot.run.arguments` is silently ignored by this plugin version).
(
  cd ../worker && mvn -q spring-boot:run "-Drun.arguments=--server.port=$WORKER_PORT,--work.dir=$WORKER_WORK_DIR,--piper.dir=piper,--spring.http.multipart.max-file-size=512MB,--spring.http.multipart.max-request-size=512MB"
) &
WORKER_JOB=$!

cleanup() {
  echo "Stopping e2e worker..."
  kill "$WORKER_JOB" 2>/dev/null || true
  # spring-boot:run forks a child JVM; fall back to killing whatever is bound
  # to the worker port in case the job PID above isn't the JVM itself.
  fuser -k "${WORKER_PORT}/tcp" 2>/dev/null || true
}
trap cleanup EXIT

echo "Waiting for worker health-check on port $WORKER_PORT..."
worker_ready=false
for i in $(seq 1 90); do
  if curl -sf "http://localhost:$WORKER_PORT/api/v1/health-check" 2>/dev/null | grep -q "ok"; then
    echo "Worker is up."
    worker_ready=true
    break
  fi
  sleep 2
done
if [[ "$worker_ready" != "true" ]]; then
  echo "Worker did not become healthy in time." >&2
  exit 1
fi

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
