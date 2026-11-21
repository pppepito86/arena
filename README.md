# Arena

Online judge and competitive programming platform for Bulgarian Olympiad in Informatics (NOI) and national contests.

## Architecture & Required Repositories

Arena consists of four repositories that must be cloned as **sibling directories** under the same parent folder (e.g. `~/dev/arena`, `~/dev/worker`, `~/dev/grader`, `~/dev/sandbox`), because `GraderIntegrationTest` and `scripts/dev/run_e2e.sh` invoke `../worker` directly:

| Repository | Branch | Role |
| :--- | :--- | :--- |
| [`pppepito86/arena`](https://github.com/pppepito86/arena) | `master` | Angular 7 frontend + Spring Boot 2 / JHipster backend |
| [`pppepito86/worker`](https://github.com/pppepito86/worker) | **`noi`** | Grading worker server (Spring Boot 2.7, port `8089`) |
| [`pppepito86/grader`](https://github.com/pppepito86/grader) | **`noi`** | Core grading library (Maven dependency of `worker`) |
| [`pppepito86/sandbox`](https://github.com/pppepito86/sandbox) | **`noi`** | IOI `isolate` wrapper (Maven dependency of `grader`) |

> [!IMPORTANT]
> Use the **`noi`** branch for `sandbox`, `grader`, and `worker`. `worker`'s `master` branch does not compile against `grader`'s `master` branch, and `worker/install/provision.sh` installs from `noi`.

---

## Prerequisites

1. **JDK 8 (`openjdk-8-jdk`)** — Required across all four repositories (enforced by `maven-enforcer-plugin` in `arena`, `java.version=1.8` in `arena` and `worker`, and `maven.compiler.source`/`target=1.8` in `grader` and `sandbox`). Ensure `JAVA_HOME` points to JDK 8 (e.g. `/usr/lib/jvm/java-8-openjdk-amd64`) if your system defaults to a newer JDK.
2. **Node.js 22** — Webpack 4 / `webpack-dev-server` 3 hangs or exits silently on Node 24, while Cypress 15 requires Node `>=20.1`. Node 22 satisfies both.
3. **MySQL 8.0** — Used in dev (`arena_dev`) and E2E (`arena_e2e`). MySQL 8 is required for E2E seeding queries that use `WITH RECURSIVE` CTEs.
4. **IOI [`isolate`](https://github.com/ioi/isolate)** (with cgroups) — Required by `worker` for `/api/v1/health-check` and sandboxed execution.
5. **C++ compiler (`g++`) & `psmisc` (`fuser`)** — `g++` is used to compile submissions/checkers; `fuser` is used by integration and E2E scripts to manage worker ports.

### Installing `isolate` on Ubuntu/Debian

```bash
sudo apt-get update
sudo apt-get install -y libcap-dev libseccomp-dev libsystemd-dev pkg-config asciidoc-base

git clone https://github.com/ioi/isolate.git /tmp/isolate
make -C /tmp/isolate isolate
sudo make -C /tmp/isolate install

# Configure sandbox UID/GID mapping and start cgroup keeper daemon
sudo useradd -r -s /usr/sbin/nologin isolate 2>/dev/null || true
sudo usermod --add-subuids 60000-65535 --add-subgids 60000-65535 isolate
sudo cp /usr/local/lib/systemd/system/isolate.service /usr/local/lib/systemd/system/isolate.slice /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now isolate.service
```

Verify `isolate` works with cgroups:

```bash
isolate --box-id=0 --cg --init && isolate --box-id=0 --cg --run -- /bin/echo ok && isolate --box-id=0 --cg --cleanup
```

---

## First-Time Setup

### 1. Clone all repositories

```bash
mkdir -p arena-workspace && cd arena-workspace
git clone https://github.com/pppepito86/arena.git
git clone -b noi https://github.com/pppepito86/worker.git
git clone -b noi https://github.com/pppepito86/grader.git
git clone -b noi https://github.com/pppepito86/sandbox.git
```

### 2. Build & install Java dependencies (`sandbox` -> `grader` -> `worker`)

With `JAVA_HOME` pointing to JDK 8:

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

mvn -f sandbox/pom.xml install
mvn -f grader/pom.xml install
# Worker's ProblemsTest loads test.properties without piper.dir, so skip tests during local install
mvn -f worker/pom.xml install -DskipTests
```

### 3. Configure MySQL

[`src/main/resources/config/application-dev.yml`](src/main/resources/config/application-dev.yml) connects to `jdbc:mysql://localhost:3306/arena_dev` as `root` with password `password`:

```bash
sudo mysql -e "
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'password';
CREATE DATABASE IF NOT EXISTS arena_dev;
FLUSH PRIVILEGES;"
```

*(Note: [`src/main/docker/mysql.yml`](src/main/docker/mysql.yml) defines MySQL 5.7 with an empty root password and database `arena`, which does not match `application-dev.yml` or `run_e2e.sh` without editing credentials/version.)*

### 4. Install frontend dependencies

In `arena/`:

```bash
npm install --legacy-peer-deps
```

- `--legacy-peer-deps` is required on npm 7+ due to an `@angular/animations@7.2.16` vs `@angular/core@7.1.0` peer dependency mismatch (and `npm ci` fails because `package-lock.json` is out of sync).
- On npm 10+/12+, lifecycle scripts may be blocked by default; if you plan to run Cypress E2E tests, also run:
  ```bash
  npx cypress install
  ```

---

## Running Locally (Development)

Run the three processes in separate terminals (with JDK 8 active for the Java processes):

1. **Worker (port `8089`)** — `worker` (`noi`) uses Spring Boot 2.7, so pass arguments via `-Dspring-boot.run.arguments` (`piper.dir` is resolved relative to `work.dir`):
   ```bash
   mkdir -p /tmp/worker_workdir
   mvn -f ../worker/pom.xml spring-boot:run \
     "-Dspring-boot.run.arguments=--server.port=8089 --work.dir=/tmp/worker_workdir --piper.dir=piper"
   ```
   Verify with `curl http://localhost:8089/api/v1/health-check` (should return `ok`).

2. **Arena Backend (port `8080`)** — Override the hardcoded `work-dir` and empty `worker-url` in [`application-dev.yml`](src/main/resources/config/application-dev.yml):
   ```bash
   mkdir -p /tmp/arena_dev_workdir
   ./mvnw "-Dspring-boot.run.arguments=--application.work-dir=/tmp/arena_dev_workdir,--application.worker-url=localhost:8089"
   ```

3. **Arena Frontend (port `9060`)**:
   ```bash
   npm start
   ```
   Open [http://localhost:9060](http://localhost:9060) (API calls proxy to `localhost:8080`).

To run a standalone frontend Webpack build, pass `--openssl-legacy-provider`:

```bash
NODE_OPTIONS=--openssl-legacy-provider npm run webpack:build
```

---

## Building for Production

```bash
./mvnw -Pprod clean package
java -jar target/*.war
```

Then navigate to [http://localhost:8080](http://localhost:8080).

---

## Testing

### Server & Grader Integration Tests

```bash
./mvnw clean test
```

Runs backend unit/integration tests including `GraderIntegrationTest`, which automatically starts `../worker` on port `8089` and grades real C++ problem fixtures through `isolate`. Requires JDK 8, `../worker` built/installed, `isolate`, `g++`, and `fuser`.

### End-to-End (E2E) Tests (Cypress)

E2E tests run via [`scripts/dev/run_e2e.sh`](scripts/dev/run_e2e.sh) against an isolated `arena_e2e` database, `/tmp/arena_e2e_workdir`, and a dedicated worker on port `8189`.

```bash
# Headless run
npm run e2e

# Interactive Cypress UI
npm run e2e:open
```

#### E2E Database Dump (`dump.sql`)

The suite expects a MySQL dump at `src/test/resources/e2e/dump.sql` (git-ignored). You can create one from your local `arena_dev` database with:

```bash
npm run e2e:dump
```

The dump must include:
- User `testuser` with password `testpass` (and Bulgarian locale `lang_key = 'bg'` or matching UI setup)
- Competition catalog and problems from [`src/main/resources/config/liquibase/`](src/main/resources/config/liquibase/) (`competitions.csv`, `problems.csv`, `competitionproblems.csv`, `tags.csv`)
- At least 10 users with recent submissions across Groups `A`–`E` (so `standings.cy.js` finds ≥10 ranked rows across "All times", "1 year", and "1 month")

### Frontend Linting

```bash
npm run lint
```

*(Note: `npm test` runs `npm run lint` followed by Jest pointing at `src/test/javascript/jest.conf.js`, though no Jest test suite is currently present in the repository.)*

---

## Notes for AI Agents & Automated Environments

- **Sibling repos & branches:** Always check out `noi` for `sandbox`, `grader`, and `worker`, and build them to `~/.m2` in that order before testing `arena`.
- **JDK & Node pinning:** If the environment defaults to Java 11/17/21 or Node 24, `JAVA_HOME` must be JDK 8 for Maven and Node must be 22 for Webpack 4 + Cypress 15. (`scripts/dev/run_e2e.sh` forces JDK 8 automatically if `/usr/lib/jvm/java-8-openjdk-amd64` exists.)
- **Spring Boot version split:** `arena` is on Spring Boot 2.0 (`-Dspring-boot.run.arguments=...` comma-separated, or space-separated on `worker` 2.7), whereas older `worker` `master` used Spring Boot 1.5 (`-Drun.arguments=...`). Both `GraderIntegrationTest` and `scripts/dev/run_e2e.sh` inspect `../worker/pom.xml` to pick the matching flag automatically.
- **Worker `mvnw` permissions:** `worker/mvnw` is not marked executable in git; invoke it via `sh ./mvnw` or `mvn`.
- **Port cleanup:** `GraderIntegrationTest` uses port `8089`; `run_e2e.sh` uses `8189` (worker), `8080` (backend), and `9060` (Webpack dev server). Make sure no stale dev servers are bound to `8080`, `8089`, or `9060` before running tests.
