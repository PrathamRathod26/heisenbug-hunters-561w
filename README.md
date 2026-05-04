# AI-Powered Insurance Claims Processing Platform

Hackathon submission for **Amnex Infotechnologies — Sarjan (April 2026)** by team **Heisenbug Hunters**.

Behavior contract: [`docs/FRS.md`](docs/FRS.md) (v0.2.2). Realisation: [`docs/TSD.md`](docs/TSD.md) (v1.3). Review/rationale: [`docs/FRS-Gap-Analysis.md`](docs/FRS-Gap-Analysis.md). AI-agent guidance: [`CLAUDE.md`](CLAUDE.md).

---

## Repository layout

```
.
├── backend/           Spring Boot 4.0.5 domain services (Java 21)
│   ├── pom.xml
│   ├── uploads/                    Phase-1 evidence byte store (.gitignored; TSD ADR-026)
│   └── src/main/
│       ├── java/com/heisenbug/claims/
│       │   ├── claim/        Claims aggregate (FNOL, state machine, evidence, analysis)
│       │   ├── rbac/         Users, roles, permissions, DOA matrix
│       │   ├── common/       Error model, global exception handler
│       │   └── config/       OpenAPI, CORS, Liquibase runner
│       └── resources/
│           ├── application{,-dev,-qa,-uat,-prod}.yml   Per-env config
│           ├── logback-spring.xml                       Profile-aware Logback
│           └── db/changelog/                            Liquibase seed data
├── frontend/          Angular 19 admin + adjuster SPA
│   ├── package.json
│   ├── proxy.conf.json           /api → backend:8080
│   └── src/app/
│       ├── core/{models,services,interceptors,auth}
│       ├── shared/
│       └── features/{dashboard,claims,login,forbidden,rbac/*}
├── ml-services/       MVP consolidated AI service (Python 3.12 + FastAPI)
│   ├── app/
│   │   ├── main.py            POST /api/v1/analyze (multipart)
│   │   ├── detector.py        YOLOv8-class damage detector
│   │   ├── cost.py            Parts-catalog cost estimator
│   │   ├── gpt_assessment.py  GPT-4o surveyor-style assessment
│   │   └── schemas.py         Pydantic response models (snake_case)
│   └── scripts/       Model download + inference smoke tests
├── database/          PostgreSQL bootstrap for the Docker deploy (see database/README.md)
│   ├── init/          First-boot SQL (extensions, roles) — runs once on empty volume
│   └── conf.d/        postgresql.conf overrides, layered via include_dir on every start
├── docker-compose.yml        Local / manual single-VM compose (uses ${FRONTEND_PORT})
├── docker-compose.prod.yml   Deployment v2 compose (uses ${PORT} — platform port contract)
├── docs/              FRS, TSD, gap analysis, original brief
└── CLAUDE.md          Project conventions & non-negotiables (for humans and agents)
```

`ml-services/` is the **phase-1 consolidated AI service**: one FastAPI process that fulfils FR-3 (detection), FR-4 (cost estimation), and FR-7 (surveyor assessment) behind a single multipart endpoint. Per-worker decomposition and the Kafka DAG sketched in [`docs/TSD.md`](docs/TSD.md) §4.1 / §6.3 remain the phase-2 target — see TSD ADR-025 for rationale. The Fraud Engine (FR-5), Forensics (FR-6), and Routing Engine (FR-8) are **not in this repo yet**.

Evidence bytes submitted via the FNOL form are persisted to `backend/uploads/` (local filesystem, TSD ADR-026) keyed by `{evidenceId}` with their original MIME type on the `claim_evidence` row. AI analysis for each claim is persisted to the `claim_analysis` table (TSD ADR-027) and served back from `GET /api/v1/claims/{id}/analysis` — see the Backend URL table below for both endpoints.

---

## Prerequisites

| Tool | Version | Why |
|---|---|---|
| **JDK** | 21 | Spring Boot 4.0 requires Java 21 |
| **Maven** | 3.8+ | Build tool (3.6.3 works but logs a warning) |
| **Node.js** | 20.11+ or 22.x | Angular 19 build |
| **npm** | 10+ | Comes with Node |
| **Python** | 3.10+ | `ml-services/` FastAPI process (uvicorn, torch/ultralytics, opencv, openai) |
| **PostgreSQL** | 14+ (tested on 17) | Transactional store |
| **Docker** (optional) | — | Quickest way to run Postgres locally |

Angular CLI is not required globally; `npx @angular/cli@19 …` is used where needed.

---

## PostgreSQL setup

Default dev config connects to `postgres://postgres:postgres@localhost:5432/claims_dev`. Fastest path:

```bash
# Docker one-liner
docker run --name claims-pg -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:17

# Create the dev database
docker exec -it claims-pg psql -U postgres -c "CREATE DATABASE claims_dev;"
```

Native install: create a database called `claims_dev`; ensure the `postgres` user can connect with password `postgres`. To use different creds, edit `backend/src/main/resources/application-dev.yml`.

---

## Backend

From `backend/`:

```bash
# Build (compile + annotation processors, no tests)
mvn -B clean compile

# Run tests (none authored yet, but surefire discovers cleanly)
mvn -B test

# Run the app on the dev profile
mvn -B spring-boot:run
# Or, after a package:
mvn -B package -DskipTests && java -jar target/claims-backend-0.0.1-SNAPSHOT.jar
```

First boot does two things automatically:

1. **Hibernate `ddl-auto: update`** creates/updates tables on first access.
2. **`LiquibaseDataInitializer`** runs `db/changelog/db.changelog-master.yaml` immediately after. Changesets with preconditions skip cleanly if tables already hold data, so this is safe to re-run.

### Backend URLs (default port 8080)

| URL | Purpose |
|---|---|
| `http://localhost:8080/actuator/health` | Liveness / readiness |
| `http://localhost:8080/actuator/circuitbreakers` | Resilience4j state for the PAS client |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON (full spec) |
| `http://localhost:8080/v3/api-docs/claims` | OpenAPI JSON (claims group only) |
| `http://localhost:8080/v3/api-docs/rbac` | OpenAPI JSON (RBAC group only) |
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI |
| `http://localhost:8080/api/v1/claims` | Claims API |
| `http://localhost:8080/api/v1/rbac/**` | RBAC APIs |

### Profiles

| Profile | Source | Schema | Logs | Notes |
|---|---|---|---|---|
| `dev` (default) | `application-dev.yml` | `ddl-auto: update`, SQL echo | Human console, `DEBUG` for app | Uses `postgres/postgres` |
| `qa` | `application-qa.yml` | `ddl-auto: validate` | JSON stdout + rolling JSON file | `DB_USER`/`DB_PASSWORD` env required |
| `uat` | `application-uat.yml` | `ddl-auto: validate` | JSON stdout + rolling JSON file | `DB_USER`/`DB_PASSWORD` env required |
| `prod` | `application-prod.yml` | `ddl-auto: validate` | JSON stdout only | `DB_URL`, `PAS_URL`, `CORS_ALLOWED_ORIGINS` env required |

Activate with `SPRING_PROFILES_ACTIVE=qa` or `--spring.profiles.active=qa`.

---

## Frontend

From `frontend/`:

```bash
# Install once (~1-2 min)
npm install

# Run the dev server at http://localhost:4200 (auto-proxies /api → backend:8080)
npm start

# Production build (outputs dist/frontend/)
npm run build
```

The dev server reads `proxy.conf.json`, which forwards `/api`, `/actuator`, `/v3/api-docs`, and `/swagger-ui` to the backend. The backend also has CORS allowlisting `http://localhost:4200` if you'd rather call it directly.

**Key libraries:** Angular Material 19 (UI), `ng2-charts` 6 + `chart.js` 4 (dashboard graphs, bundled only into the lazy-loaded dashboard chunk — initial bundle is unaffected).

### Frontend routes

All feature routes are guarded by `authGuard` (redirects to `/login` with `?redirectTo=…`); `/rbac/**` additionally requires `personaGuard(['ADMIN'])`. Cross-persona access attempts land on `/forbidden`. See [FRS §3.3](docs/FRS.md#33-web-portal-access--phase-1-scope) for the persona gate.

| Path | Component | Access |
|---|---|---|
| `/login` | `LoginComponent` | **Public** — seeded-user picker; rejects roles outside the 3 portal personas |
| `/forbidden` | `ForbiddenComponent` | **Public** — shown when a signed-in user hits a route outside their persona |
| `/dashboard` | `DashboardComponent` | All 3 personas — 4 stat tiles + 3 Chart.js graphs: **Claims by status** (doughnut), **Users per role** (horizontal bar), **Permissions by scope** (doughnut). Per FRS §4 / FR-10 AC-10.1.5. |
| `/claims` | `ClaimListComponent` | All 3 personas (filter by status) |
| `/claims/new` | `ClaimFormComponent` | **Policyholder only** — FR-2 FNOL. Multipart upload of 3–`claims.max-evidences` photos + optional narrative. Backend orchestrates PAS verify → ML analyze → persist; the form swaps into an analysis card showing detections, paise-denominated cost range, surveyor assessment, and severity verdict. |
| `/claims/:id` | `ClaimDetailComponent` | All 3 personas — evidence master-detail, status transitions |
| `/rbac/users` | `UserListComponent` | **Administrator only** |
| `/rbac/users/new` | `UserFormComponent` | **Administrator only** |
| `/rbac/users/:id` | `UserDetailComponent` | **Administrator only** — assignments + effective-permissions (3-level master-detail) |
| `/rbac/roles`, `/new`, `/:id` | `Role*Component` | **Administrator only** — permission replace on detail |
| `/rbac/permissions` | `PermissionListComponent` | **Administrator only** — read-only capability × scope catalog |
| `/rbac/doa-matrix`, `/new`, `/:id` | `Doa*Component` | **Administrator only** — payout limit matrix |

---

## Docker deployment (single VM)

Ship everything behind one `docker compose` file. Four services on one user-defined bridge network; only the frontend port is published to the host.

Two compose files, same topology:

| File | Use for | Port binding |
|---|---|---|
| `docker-compose.yml` | Local dev / manual single-VM runs | `${FRONTEND_PORT:-80}:80` |
| `docker-compose.prod.yml` | Deployment v2 pipeline (quality-gated) | `${PORT:-80}:80` (port contract) |

```
host:${FRONTEND_PORT:-80}         (local compose)
host:${PORT}                      (deployment v2 — platform-injected)
        │
        ▼
   [ frontend ]  nginx — serves Angular bundle, reverse-proxies /api, /actuator, /v3/api-docs, /swagger-ui
        │
        ▼  (internal)
   [ backend ]  Spring Boot 4.0.5 — /app/uploads (volume), /api/v1/**
        │  ┌────────────────────┐
        ▼  ▼                    │
   [ postgres ]           [ ml-services ]  FastAPI + YOLOv11 + GPT-4o
   /var/lib/postgresql    /app/models (volume)
   + ./database/init      (first-boot SQL)
   + ./database/conf.d    (postgresql.conf overrides)
```

### Prerequisites

- Docker 24+ and Docker Compose v2 (`docker compose …`, not legacy `docker-compose`).
- VM size: **≥ 4 vCPU / 6 GB RAM / 20 GB disk** recommended. The `ml-services` image carries `ultralytics` + Torch CPU wheels (~2 GB); the Spring Boot JVM takes ~1 GB resident; Postgres + nginx are small.
- Outbound internet on first boot (Hugging Face model pull + Python/Maven dependency fetch during build).

### First run

```bash
cp .env.example .env
# edit .env — set DB_PASSWORD, OPENAI_API_KEY (optional), HF_TOKEN (optional)

docker compose up -d --build
docker compose logs -f
```

First build takes ~5–10 minutes (Maven + npm + pip). Subsequent rebuilds hit BuildKit's cache mounts (Maven `~/.m2`, npm cache, pip cache) and complete in under a minute for code-only changes.

Open `http://<vm-ip>/` — Angular SPA. `http://<vm-ip>/swagger-ui.html` — proxied Swagger UI. `http://<vm-ip>/actuator/health` — liveness.

### Pipeline deploy (Deployment v2)

The quality-gated pipeline validates `docker-compose.prod.yml` and requires the frontend to publish on the platform-injected `${PORT}`. To smoke-test locally the same way the pipeline does:

```bash
PORT=8080 docker compose -f docker-compose.prod.yml config --quiet   # folderStructure + portCompliance gate
PORT=8080 docker compose -f docker-compose.prod.yml up -d --build    # full stack on :8080
```

The prod compose file mirrors `docker-compose.yml` one-for-one (same services, volumes, bind-mounts into `./database/`) — only the frontend publish differs. Keep them in sync when adding services or env vars.

### Volumes (data persistence across `up`/`down`)

| Volume | Mounted at | Contents |
|---|---|---|
| `postgres-data` | `/var/lib/postgresql/data` in `postgres` | Claims + RBAC + analysis tables |
| `backend-uploads` | `/app/uploads` in `backend` | Evidence byte store (TSD ADR-026) |
| `ml-models` | `/app/models` in `ml-services` | Cached YOLOv11 damage-model.pt (~10 MB) + Torch Hub cache |

```bash
docker compose down          # stop, keep volumes + data
docker compose down -v       # stop AND wipe data (destructive)
```

### Configuration overrides

All backend tunables flow through `.env` → `docker-compose.yml` env → `application-docker.yml`:

| `.env` variable | Effect |
|---|---|
| `DB_USER` / `DB_PASSWORD` / `DB_NAME` | Postgres credentials (shared by both services) |
| `FRONTEND_PORT` | Host port that maps to nginx :80 (local `docker-compose.yml`) |
| `PORT` | Host port that maps to nginx :80 (pipeline `docker-compose.prod.yml` — platform-injected) |
| `CORS_ALLOWED_ORIGINS` | Backend CORS allow-list (comma-separated) |
| `OPENAI_API_KEY` | GPT-4o surveyor-assessment — leave blank for degraded-mode analysis (null `surveyorAssessment`) |
| `HF_TOKEN` | Hugging Face token for the model download, only needed if the model repo is private |

For backend-specific properties not in `.env.example`, add them directly as env vars on the `backend` service in `docker-compose.yml` **and** `docker-compose.prod.yml` (Spring Boot relaxed binding — `claims.max-evidences` ↔ `CLAIMS_MAX_EVIDENCES`, etc.).

### Lifecycle operations

```bash
docker compose ps                         # container + healthcheck state
docker compose logs -f backend             # tail one service
docker compose restart backend             # restart without rebuild
docker compose build --no-cache backend    # rebuild after pom.xml / src changes
docker compose up -d --build --force-recreate
docker compose exec postgres psql -U postgres claims  # jump into DB
docker compose exec backend sh             # shell into backend
```

### Production notes

- The `docker` Spring profile (see `backend/src/main/resources/application-docker.yml`) uses **JSON structured logging** (TSD §5.17); collect with `docker logs | jq` or ship with Loki/Fluent Bit.
- **TLS termination is NOT included** — front the whole stack with Caddy / Traefik / a cloud load balancer if you need HTTPS. nginx here listens on :80 only.
- **Auth is still unwired** (TSD ADR-001 / FR-1 roadmap) — don't put this on a public IP before the OIDC integration lands.
- `claims.upload-dir` points at the `backend-uploads` volume. Migrating to S3 is a one-line swap in `application-docker.yml` once `EvidenceStore` gets an S3 implementation (TSD ADR-026).

---

## Running the full stack

Three terminals — the ML service is optional; when it's down the claim form still works but falls back to an empty analysis (`modelVersion = "ml-service-unavailable"`).

```bash
# Terminal 1 — ml-services (FastAPI on :8000)
cd ml-services && uvicorn app.main:app --host 0.0.0.0 --port 8000
# (requires Python 3.10+, OpenAI API key in ml-services/.env, YOLOv8 weights per scripts/download_model.py)

# Terminal 2 — backend (Spring Boot on :8080)
cd backend && mvn -B spring-boot:run

# Terminal 3 — frontend (Angular on :4200, proxying /api to :8080)
cd frontend && npm start
```

Then open `http://localhost:4200`.

### ML service contract

| Property | Value |
|---|---|
| URL | `POST http://localhost:8000/api/v1/analyze` |
| Content-Type | `multipart/form-data` |
| Parts | `images[]` (3–15 JPEG/PNG), `video?` (single MP4), `narrative?` (≤4000 chars) |
| Response | `AnalyzeResponse` — model version, per-image detections, cost estimate (paise), GPT-4o surveyor assessment, `processing_time_ms` |
| Caller | `MlAnalyzeClient` in the Spring Boot backend — Resilience4j instance `ml-analyze` (20-call window, 60% failure threshold, 30s slow-call threshold, 60s open duration, 2-attempt retry, 60s read-timeout) |
| Fallback | Empty analysis object (`modelVersion: "ml-service-unavailable"`) if the call trips the breaker — the claim still persists with status `FNOL_RECEIVED` |

Configurable via the backend:

| Property | Default | Effect |
|---|---|---|
| `claims.max-evidences` | `10` | Max photos accepted on `/api/v1/claims/with-analysis`; exposed to the frontend via `GET /api/v1/claims/config` (FRS AC-2.1.10) |
| `claims.min-evidences-for-analysis` | `3` | Min photos before the ML pipeline is called (aligns with Python `MIN_IMAGES = 3` in `ml-services/app/main.py`) |
| `integrations.ml-service.base-url` | `http://localhost:8000` | ML service host |
| `integrations.ml-service.connect-timeout` | `3s` | TCP connect timeout |
| `integrations.ml-service.read-timeout` | `60s` | Response read timeout (GPT-4o assessment dominates) |
| `spring.servlet.multipart.max-file-size` | `15MB` | Per-image cap |
| `spring.servlet.multipart.max-request-size` | `120MB` | Total-payload cap (matches FRS §4 FR-2 "≤ 100 MB" envelope + headroom) |

---

## Seeded data (Liquibase, dev profile)

First boot populates via `backend/src/main/resources/db/changelog/changesets/`:

| Table | Rows | Source |
|---|---|---|
| `permission` | 32 | FRS §3.2 capability × scope matrix |
| `role` | 11 | FRS §3.1 |
| `role_permission` | 57 | Matrix cells expanded |
| `app_user` | 11 | One representative user per role + a policyholder |
| `user_role_assignment` | 12 | One primary grant per user + one revoked grant to demo audit trail |

Seeded users (all emails fake, all statuses `ACTIVE`):

| Email | Type | Role | Web-portal persona (FRS §3.3) |
|---|---|---|---|
| `admin@amnex.com` | INTERNAL | ADMIN | **Administrator** |
| `sr.adjuster@amnex.com` | INTERNAL | SR_ADJUSTER | **Claims Adjuster** |
| `adjuster@amnex.com` | INTERNAL | ADJUSTER | **Claims Adjuster** |
| `jane.doe@example.com` | POLICYHOLDER | POLICYHOLDER (+ 1 revoked ADJUSTER grant) | **Policyholder** |
| `auditor@amnex.com` | INTERNAL | AUDITOR | — (out of portal phase 1) |
| `fraud@amnex.com` | INTERNAL | FRAUD_INVESTIGATOR | — |
| `aml@amnex.com` | INTERNAL | AML_OFFICER | — |
| `finance@amnex.com` | INTERNAL | FINANCE_APPROVER | — |
| `grievance@amnex.com` | INTERNAL | GRIEVANCE_OFFICER | — |
| `surveyor.001@amnex.com` | SURVEYOR | SURVEYOR (licence `SLA-IRDAI-MH-00142`) | — |
| `system@amnex.com` | SYSTEM | SYSTEM | — |

---

## Personas & sign-in

The web portal is gated to three business personas per [FRS §3.3](docs/FRS.md#33-web-portal-access--phase-1-scope). Phase 1 uses **dev-mode impersonation**: `/login` lists the seeded users and the `AuthService` checks whether the selected user's active roles include at least one portal-eligible role. Real OIDC (Keycloak + OTP per TSD §5.12 / FR-1) is the roadmap target.

| Persona | Allowed RBAC role(s) | Responsibilities on the web portal |
|---|---|---|
| Policyholder | `POLICYHOLDER` | Submits FNOL + formal claims; views own claim; initiates grievance |
| Claims Adjuster | `ADJUSTER`, `SR_ADJUSTER` | Reviews AI assessments (FR-3..FR-8); approves/escalates within DOA; updates claim status |
| Insurance Administrator | `ADMIN` | Configures role ↔ permission matrix, DOA (role × LOB × geo), reviews audit; per FR-10 |

Users with any of `FRAUD_INVESTIGATOR`, `SURVEYOR`, `GRIEVANCE_OFFICER`, `AML_OFFICER`, `FINANCE_APPROVER`, `AUDITOR`, or `SYSTEM` are rejected at `/login` with HTTP 403 `E-AUTHZ-PORTAL-001` per **FR-1 AF-1.6 / AC-1.1.7**. Their API authorisation per §3.2 is unchanged — only the web-portal channel is denied in phase 1.

**Precedence for multi-role users** (per §3.3): `ADMIN > ADJUSTER > POLICYHOLDER`. Jane Doe (`jane.doe@example.com`), for example, holds `POLICYHOLDER` active + `ADJUSTER` revoked and lands as **Policyholder**.

Session persistence is `localStorage` under key `claims.session.v1`. The `authInterceptor` attaches `X-User-Id: <uuid>` on every `/api/**` call — the backend currently ignores it; when JWT lands, swap the header for `Authorization: Bearer …`.

---

## Smoke tests (copy-paste)

These hit the backend directly — no portal persona gate (that's a frontend concern; the backend has no auth yet).

```bash
# Health
curl -s http://localhost:8080/actuator/health

# Permission catalog (expect 32)
curl -s http://localhost:8080/api/v1/rbac/permissions | jq 'length'

# Admin's effective permissions (expect 6: VIEW_ANY_CLAIM, MANAGE_USER_ROLES, MANAGE_DOA_MATRIX, PROPOSE_THRESHOLD_CHANGE, ACCESS_FORENSIC_ARTIFACTS (read-only), EXECUTE_RIGHT_TO_ERASURE (four-eye))
curl -s http://localhost:8080/api/v1/rbac/users/30000000-0000-0000-0000-000000000001/effective-permissions | jq '.permissions[].capability'

# Dashboard stats
curl -s http://localhost:8080/api/v1/claims/stats/status-counts
curl -s http://localhost:8080/api/v1/rbac/users/stats/active-users-per-role
```

---

## Branching

Per-track branches feed `development`, which merges into `main`:

- `backend-dev` — backend work
- `frontend-dev` — web frontend
- `mobile-dev` — mobile (not started)
- `development` — integration branch
- `main` — release target

Cross-track changes (shared API schemas, contracts) go on `development`.

---

## Troubleshooting

- **`BUILD FAILURE … Port 8080 was already in use`** — a previous Java process is still holding the port. On Windows PowerShell: `Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }`. `mvn spring-boot:run` orphans the child JVM when Ctrl-C doesn't reach it cleanly.
- **`PSQLException: Connection refused`** — Postgres isn't running or credentials differ. Verify with `docker ps` or by connecting with `psql -h localhost -U postgres claims_dev`.
- **`E-AUTHZ-001` 403 on endpoints** — reserved for API-layer RBAC denial (not wired yet — backend has no auth filter). For now, any 403 on `/api/**` is from CORS. The separate `E-AUTHZ-PORTAL-001` is a *frontend* 403 emitted by the login flow when a user's active roles don't intersect the FRS §3.3 persona set.
- **`/login` rejects my user** — the selected user has no role in `{POLICYHOLDER, ADJUSTER, SR_ADJUSTER, ADMIN}`. Phase 1 web-portal scope (FRS §3.3). Use one of `admin@amnex.com`, `sr.adjuster@amnex.com`, `adjuster@amnex.com`, or `jane.doe@example.com`.
- **Signed-in user lands on `/forbidden`** — deep-linking to a route outside their persona (e.g., a Policyholder navigating to `/rbac/users`). Sign out and back in as an Administrator, or follow a persona-appropriate link.
- **CORS errors in the browser console** — check that the origin matches `cors.allowed-origins` in the active profile's YAML. Dev allows `http://localhost:4200`.
- **MapStruct "unmapped target" build failure** — mappers are configured with `ReportingPolicy.ERROR`. Add the missing `@Mapping(target = …, ignore = true)` or `source = …` to the mapper interface.
- **Liquibase `MARK_RAN despite precondition failure`** — not an error; the changeset's preCondition (`count = 0`) fell through because the table already has rows. Expected behavior on re-runs or DBs seeded by the prior Java runner.

---

## Roadmap / intentionally-deferred

- **Auth.**
  - *Done:* Phase-1 web-portal login via dev-mode impersonation (`/login` + `AuthService` + route guards). Persona gate per FRS §3.3 — only `POLICYHOLDER`, `ADJUSTER`/`SR_ADJUSTER`, `ADMIN` pass.
  - *Still TODO:* Real OIDC + OTP (TSD §5.12, Keycloak). Backend JWT verification filter (currently accepts every request; the `X-User-Id` header the SPA sends is a no-op server-side). `E-AUTHZ-001` API-layer enforcement per FRS §3.2.
- **Per-policyholder data scoping.** The portal currently shows all claims to every signed-in user; Policyholders should only see their own. Needs a `Claim.claimantUserId` column + a `?mine=true` query on the claims list (or a runtime filter once auth is live).
- **Assignment-restricted Adjuster view** per FRS `ASSIGNMENT_RESTRICTED` scope — Adjusters see only their queue. Needs `Claim.assignedAdjusterId` + a repo filter.
- **AI workers.**
  - *Done:* Phase-1 consolidated `ml-services/` FastAPI process covering FR-3 (YOLOv8 detection), FR-4 (cost estimation in paise), and FR-7 (GPT-4o surveyor assessment) behind one multipart `POST /api/v1/analyze` — see TSD ADR-025.
  - *Still TODO:* Per-worker decomposition onto Kafka DAG (TSD §6.3). Fraud Engine L1/L2/L3 (FR-5), Forensics / C2PA verify (FR-6), Routing Engine (FR-8). Persistent `claim_analysis` aggregate on the Java side so analyses are searchable. Phase-2 migration path is documented in TSD ADR-025.
- **Mobile.** Flutter 3 app per TSD §5.2. Not started.
- **Tests.** No unit or integration tests yet — the scaffolds compile and were smoke-tested via HTTP only.
- **Prod deploy.** `application-prod.yml` reads `DB_URL`, `PAS_URL`, `CORS_ALLOWED_ORIGINS` from env; Terraform/Helm charts are out of scope for the hackathon scaffold.

---

## Contact

Team lead: `monark@amnex.com`. See [`CLAUDE.md`](CLAUDE.md) for project conventions and non-negotiable invariants (fail-closed audit, change-controlled risk surfaces, separation of duties, data residency).
