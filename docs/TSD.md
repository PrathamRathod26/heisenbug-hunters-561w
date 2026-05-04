# Technical Specification Document (TSD)

| Field | Value |
|---|---|
| Project | AI-Powered Insurance Claims Processing Platform |
| Team | Heisenbug Hunters |
| Hackathon | Amnex Infotechnologies — Sarjan (April 2026) |
| Version | 1.3 (Draft) |
| Date | 2026-04-24 |
| Status | **DRAFT — derived from `FRS.md` v0.2.2 (2026-04-24). v1.1 supersedes ADR-001 (Java domain / Python AI). v1.2 lands the MVP consolidated `ml-services` endpoint. v1.3 adds evidence byte persistence (local FS, phase-1) + `ClaimAnalysis` entity for AI-response persistence. See §5.1, §6.1, §6.3, §25 ADR-025 / ADR-026.** |
| Audience | Engineering, SRE, Security, Data Science, QA |
| Pair-reads | `FRS.md` v0.2.2 (functional contract), `FRS-Gap-Analysis.md` v1.0 (rationale for absorbed items) |
| Change Log | **v1.1 (2026-04-24)** — ADR-001 overridden (Python/FastAPI → Java 21 + Spring Boot 4.0.5 for domain services); AI/ML workers retain Python. Touched: §2.3, §4.1 container diagram, §5.1, §6.7, §10.1, §18.1, §25 ADR-001. Reference scaffold added at repo-root `backend/`. **v1.2 (2026-04-24)** — MVP consolidated AI service landed at repo-root `ml-services/` (single FastAPI process exposes `POST /api/v1/analyze` that currently fulfils FR-3 + FR-4 + FR-7 in one call). Claims Service gains `POST /api/v1/claims/with-analysis` (multipart); new `MlAnalyzeClient` (Resilience4j-protected RestClient) and `claims.max-evidences` configuration property. Touched: §2.3, §6.1, §6.3, §10.1, §25 new ADR-025. **v1.3 (2026-04-24)** — Evidence byte persistence (phase-1 local FS at `{claims.upload-dir}/{evidenceId}`) + retrieval endpoint; `claim_evidence` gains `content_type`/`size_bytes` columns. `ClaimAnalysis` entity persists the full AI response (denormalised scalars + `payload_json`); new `GET /api/v1/claims/{id}/analysis` and `GET /api/v1/claims/{claimId}/evidence/{evidenceId}` endpoints. PasClient invocation dropped from Claims Service pending Phase-2 PAS wiring. Touched: §6.1, §6.5 (new — Evidence & Analysis Storage), §25 ADR-026 (evidence storage), ADR-027 (analysis persistence). |

> This document answers **how** the system is built. The FRS answers **what** it does. Every section below references the FRS items (`FR-n`, `AC-n.m.k`, `NFR-*`, `[M-nn]`, `[F-nn]`, `[U-nn]`) that drive the decision. §26 provides a full FRS → TSD traceability matrix.

---

## 1. Introduction

### 1.1 Purpose
Specifies the technical design, technology stack, and implementation approach for the System as defined in FRS v0.2. Concrete enough to estimate and build from; abstract enough to tolerate reasonable team choices. Where a decision is still open, it is captured as an **ADR** (§25) with the status `Proposed` and a clear owner.

### 1.2 Scope
- Backend services (claims lifecycle, AI orchestration, surveyor, cashless, recovery/salvage, TP, grievance, AML, consent, intake orchestrator, audit, admin/reporting).
- AI/ML serving stack (YOLOv8-class damage detection, three-layer fraud engine, extended forensics, multimodal explanation, routing engine).
- Data stores (transactional, object, cache, feature, analytics, audit).
- Integration platform (PAS, VAHAN, IIB, OCR, ledger, penny-drop, Consent Manager, IGMS, weather, sanctions, device intelligence, C2PA verifier, TSA anchoring).
- Mobile app (policyholder) and web dashboard (adjuster/admin/surveyor/grievance/AML/finance).
- Cross-cutting concerns: security, observability, CI/CD, testing, deployment, DR, compliance controls.

### 1.3 Relationship to the FRS

The FRS is authoritative on **behavior**. This TSD is authoritative on **realisation**. Where they disagree on behavior, the FRS wins and this document is amended; where they disagree on realisation, this document wins and the FRS is not touched.

### 1.4 How to read this document
- §2–§4 set the architectural frame (principles, context, container view).
- §5 records the technology stack decisions as ADRs.
- §6–§20 detail components, data, APIs, events, integrations, security, AI/ML, mobile, web, deployment, observability, CI/CD, testing, NFR approach, and audit.
- §21–§24 cover compliance mapping, rollout, runbooks, and open decisions.
- §25 is the ADR index; §26 is the FRS → TSD traceability matrix.

---

## 2. Architectural Approach & Principles

### 2.1 Guiding Principles

1. **FRS-first.** Every component exists to satisfy one or more `FR-n` or `NFR-*`. Any component without a traceable FRS anchor is a code smell.
2. **Boring technology at the edges, bold only where warranted.** Postgres, Kafka, Redis, S3, Kubernetes. Bold is reserved for the AI/ML path, which is where the product's value lives.
3. **Services stateless; data in managed stores.** No in-process queues, no sticky sessions, no disk dependencies in request paths (audit spool excepted, §20).
4. **Events as the integration backbone.** State changes emit domain events; consumers subscribe. No cross-service synchronous RPC chains deeper than one hop. Supports replay, audit, and scale.
5. **Asynchronous AI, synchronous decisions.** The AI pipeline is async; the routing decision is synchronous against the latest AI state at the moment routing fires (FR-8).
6. **Change-controlled risk surfaces.** Thresholds, prompts, model artefacts, DOA matrix, rules: all versioned, promoted via CI gates, immutable once live. Never runtime-mutable. `[U-11]`
7. **Fail-closed on audit.** If audit writes degrade beyond SLA, state transitions stop. `[U-18]`
8. **Data residency and minimisation.** All PII lives in India-region stores (or insurer's own on-prem) and is minimised before egress. `[NFR-AC-005, M-13, U-10]`
9. **Defensible AI.** Every AI decision carries model version, prompt version, input hash, output hash, and decomposed reasons. Adjuster is the authority; AI is evidence.
10. **Separation of duties baked in.** Approvals above a DOA ceiling, bank-account payouts to new accounts, and threshold/model changes require two-principal approval by construction. `[M-26, U-11]`

### 2.2 C4 Model Levels Used
- **L1 System Context** — §3.
- **L2 Container** — §4.
- **L3 Component** — §6, per service.
- **L4 Code** — deferred; captured in each service's repo during implementation.

### 2.3 Key Architectural Decisions (ADR Index — expanded in §25)

| ADR | Decision | Status |
|---|---|---|
| ADR-001 | Domain services: **Java 21 + Spring Boot 4.0.5**; AI/ML workers: Python 3.12 | Accepted (revised 2026-04-24 — supersedes prior Python-only decision) |
| ADR-002 | Primary mobile framework: **Flutter 3.x** | Proposed |
| ADR-003 | Primary web framework: **React 18 + Vite + TypeScript** | Accepted |
| ADR-004 | Transactional DB: **PostgreSQL 16**, monthly partitioning | Accepted |
| ADR-005 | Object storage: **S3-compatible** (AWS S3 in `ap-south-1`), WORM for audit bucket | Accepted |
| ADR-006 | Message bus: **Apache Kafka** + Schema Registry | Accepted |
| ADR-007 | Cache / online feature store: **Redis 7** | Accepted |
| ADR-008 | Analytics store: **ClickHouse** (self-hosted in VPC) | Accepted |
| ADR-009 | Orchestration: **Kubernetes (EKS)** with GPU node pool | Accepted |
| ADR-010 | Model registry: **MLflow** (self-hosted, S3 artefact store) | Accepted |
| ADR-011 | Model serving: **NVIDIA Triton** (vision) + **BentoML** (tabular) | Accepted |
| ADR-012 | LLM provider: **Azure OpenAI in India region** with DPA zero-retention + self-hosted **vLLM** fallback | Proposed |
| ADR-013 | Damage detector license: **switch to RT-DETR** (Apache-2.0) to avoid Ultralytics AGPL | **Proposed (P0 decision — FRS U-12 / C-09)** |
| ADR-014 | Authorization: **Open Policy Agent (OPA)** sidecar | Accepted |
| ADR-015 | Secrets: **HashiCorp Vault** + K8s External Secrets Operator | Accepted |
| ADR-016 | Audit anchoring: **dual RFC 3161 TSA** (DigiCert + FreeTSA) | Proposed |
| ADR-017 | IaC: **Terraform** + **Helm** | Accepted |
| ADR-018 | Observability: **OpenTelemetry** → **Prometheus + Loki + Tempo + Grafana** | Accepted |
| ADR-019 | CI/CD: **GitHub Actions** with Argo CD for K8s | Proposed |
| ADR-020 | Policyholder OTP provider: **MSG91** (primary) + **Twilio** (failover) | Proposed |
| ADR-021 | Feature store: **Feast** with Redis online + ClickHouse offline | Accepted |
| ADR-022 | Encryption: **AES-256-GCM** with envelope encryption via AWS KMS (BYOK production) | Accepted |
| ADR-023 | Resumable uploads: **tus protocol** (tusd) behind API gateway | Accepted |
| ADR-024 | Event serialization: **Protobuf** on internal Kafka topics; JSON for external webhooks | Accepted |
| ADR-025 | **Phase-1 MVP: consolidated `ml-services` FastAPI process** for FR-3 + FR-4 + FR-7; single synchronous multipart call from the Java Claims Service. Phase-2 decomposes per §6.3 Kafka DAG. | Accepted (2026-04-24) |
| ADR-026 | **Phase-1 evidence-byte storage: local filesystem** at `{claims.upload-dir}/{evidenceId}`. Phase-2 swaps to S3 (ADR-005) without changing the HTTP contract. | Accepted (2026-04-24) |
| ADR-027 | **`ClaimAnalysis` entity owns the persisted AI response** — denormalised scalars + `payload_json` (TEXT, MVP; JSONB + GIN in phase-2). One-to-one with `Claim`; re-runs overwrite; history deferred. | Accepted (2026-04-24) |

---

## 3. System Context (C4 L1)

```
                            ┌──────────────────────┐
                            │    Policyholder       │
                            │ (mobile / IVR /       │
                            │  WhatsApp / agent)    │
                            └──────────┬───────────┘
                                       │
                              ┌────────▼────────┐
                              │                 │
 ┌─────────┐  ┌──────────┐   │    AI-Claims    │  ┌────────────┐  ┌─────────────┐
 │  PAS    │◄─►│  VAHAN/  │◄──►│   Platform    │◄─►  IIB       │  │ Consent     │
 │ (core)  │   │ SARATHI  │   │                 │   Claim Hist.│  │ Manager     │
 └─────────┘  └──────────┘   │                 │  └────────────┘  │ (DPDP)      │
                              │                 │                  └─────────────┘
 ┌─────────┐  ┌──────────┐   │                 │  ┌────────────┐  ┌─────────────┐
 │ Financial│◄►│ Penny-   │◄──►                 ◄─►│ IGMS       │  │ Azure       │
 │ Ledger  │   │ Drop     │   │                 │  │ Grievance  │  │ OpenAI /    │
 │(SAP/ORA)│   │(NPCI)    │   │                 │  └────────────┘  │ self-hosted │
 └─────────┘  └──────────┘   │                 │                  │ vLLM        │
                              │                 │                  └─────────────┘
 ┌─────────┐  ┌──────────┐   │                 │  ┌────────────┐  ┌─────────────┐
 │Sanctions│◄─►│ Weather/ │◄──►                 ◄─►│ Garage     │  │ RFC-3161    │
 │ / PEP   │   │ Event    │   │                 │  │ Network    │  │ TSAs (audit │
 │ Provider│   │ Data     │   │                 │  │ (cashless) │  │ anchors)    │
 └─────────┘  └──────────┘   └────────┬────────┘  └────────────┘  └─────────────┘
                                       │
                        ┌──────────────┴──────────────┐
                        ▼                             ▼
                ┌───────────────┐             ┌───────────────────┐
                │ Internal Users │             │ FIU-IND / IRDAI   │
                │ (Adjuster, Sr, │             │ (regulator        │
                │ Fraud, Surveyor│             │  reports, STR     │
                │ Grievance, AML,│             │  filings)         │
                │ Finance, Admin)│             └───────────────────┘
                └───────────────┘
```

---

## 4. Container Architecture (C4 L2)

### 4.1 Container view

```
                          ┌──────────────────────────────┐
                          │       API Gateway (Kong)     │
                          │  WAF • TLS term • mTLS out • │
                          │  OAuth2 introspect • RL      │
                          └──────┬──────────────┬────────┘
                                 │              │
                 ┌───────────────┘              └─────────────┐
                 ▼                                            ▼
   ┌───────────────────────┐                     ┌──────────────────────┐
   │  Intake Orchestrator  │                     │  Admin / Reporting   │
   │  (FR-2, FR-19)        │                     │  SPA (React)         │
   │  Java 21 + Spring Boot│                     └──────────┬───────────┘
   └──────────┬────────────┘                                │
              │                                             │
   ┌──────────▼───────────────────────────────────────────────┐
   │                  Service Mesh (Istio / Linkerd)          │
   │            mTLS • L7 telemetry • circuit breaking        │
   └──┬────────┬────────┬────────┬────────┬────────┬────────┬─┘
      ▼        ▼        ▼        ▼        ▼        ▼        ▼
 ┌────────┐┌───────┐┌────────┐┌────────┐┌──────┐┌───────┐┌────────────┐
 │Claims  ││Identity││Policy ││Surveyor││Cash- ││Recovery││Third-Party │
 │Svc     ││& Auth ││& PAS  ││Svc     ││less  ││& Salv- ││Liability   │
 │(FR-1/2/││Svc    ││Gateway││(FR-12) ││Svc   ││age Svc ││Svc (FR-15) │
 │8/9/11) ││(FR-1/ ││(M-19) ││        ││(FR-13│(FR-14) ││            │
 │        ││18)    ││        ││        ││)     ││        ││            │
 └────────┘└───────┘└────────┘└────────┘└──────┘└───────┘└────────────┘
 ┌────────┐┌───────┐┌────────┐┌────────┐
 │Grievan-││AML    ││Consent ││Audit   │
 │ce Svc  ││Svc    ││Svc     ││Svc     │
 │(FR-16) ││(FR-17)││(FR-18) ││(FR-11) │
 └────────┘└───────┘└────────┘└────────┘

                       │    internal events
                       ▼
       ┌───────────────────────────────┐
       │  Apache Kafka (MSK managed)   │
       │  Schema Registry • DLQ topics │
       └───────────────┬───────────────┘
                       │
   ┌───────────────────┴──────────────────────────────────────┐
   │                                                          │
   ▼                                                          ▼
┌──────────────────┐              ┌─────────────────────────────┐
│ AI Orchestrator  │              │ Integration Gateway         │
│ (DAG runner —    │              │ (Kong or dedicated edge     │
│ orchestrates     │              │  svc); hosts VAHAN, IIB,    │
│ FR-3..FR-8)      │              │  Penny-Drop, OCR, Weather,  │
└──────┬───────────┘              │  Sanctions, IGMS, Consent   │
       │                          │  Manager, TSA clients)      │
       ▼                          └─────────────────────────────┘
 ┌──────────────────────────────────────────────────────┐
 │        AI Worker Pool (K8s deployment)               │
 │ ┌────────┐┌────────┐┌────────┐┌────────┐┌─────────┐  │
 │ │YOLOv8/ ││Forens- ││Fraud L1││Fraud L2││Explana- │  │
 │ │RT-DETR ││ics     ││Rules   ││ML+GNN  ││tion LLM │  │
 │ │(Triton)││(Triton ││(Python)││(Triton ││(vLLM or │  │
 │ │        ││+CPU)   ││        ││+BentoML││Azure    │  │
 │ │        ││        ││        ││)       ││OpenAI)  │  │
 │ └────────┘└────────┘└────────┘└────────┘└─────────┘  │
 │   each scales on Kafka lag via KEDA                  │
 └──────────────────────────────────────────────────────┘

 Data Plane:
  Postgres 16 (primary + read replicas, monthly partitions)
  S3 (hot • cold • WORM audit bucket)
  Redis 7 (cache + online feature store)
  ClickHouse (analytics + offline features)
  MLflow (model registry; artefacts in S3)
  Vault (secrets, signing keys)
```

### 4.2 Container responsibilities

| Container | Responsibility | FRS anchor |
|---|---|---|
| API Gateway (Kong) | TLS termination, OAuth2 introspection, rate limits, WAF, routing | NFR-S |
| Service Mesh (Istio) | mTLS, retries, circuit-breakers, per-hop telemetry | NFR-S-002 |
| Intake Orchestrator | FNOL + formal-claim intake from all channels; OCR dispatch | FR-2, FR-19 |
| Claims Service | Claim aggregate, state machine, decisions, reopen | FR-2, FR-8, FR-9, FR-11 |
| Identity & Auth | OTP flow, OIDC for internal + surveyor, JWT minting via KMS | FR-1 |
| Policy & PAS Gateway | Coverage, policy lifecycle, endorsement, PAS sync | FR-1, FR-4, M-19 |
| Surveyor Service | Panel mgmt, assignment, surveyor reports | FR-12 |
| Cashless Service | Pre-auth, final bills, variance adjudication | FR-13 |
| Recovery & Salvage Service | Recovery cases, salvage valuation/bidding | FR-14 |
| TP Service | Third-party intake, MACT hand-off | FR-15 |
| Grievance Service | IGMS-aligned case management | FR-16 |
| AML Service | Sanctions/PEP screening; STR drafting | FR-17 |
| Consent Service | DPDP consent artefacts; data-principal rights portal | FR-18 |
| Audit Service | Append-only hash-chained log + external anchoring | FR-11 |
| AI Orchestrator | DAG runner (YOLOv8 → Forensics → Fraud L1/L2/L3 → Explanation → Routing) | FR-3..FR-8 |
| Integration Gateway | Typed clients to external systems with CBs, retries, rate-limits | §2.3 of FRS |
| Admin/Reporting SPA | Adjuster/Admin/Surveyor/Grievance/AML/Finance UIs | FR-10 |

---

## 5. Technology Stack Decisions

### 5.1 Backend language — ADR-001 `[FR-*, NFR-P]`

> **Revised 2026-04-24.** This section supersedes the prior Python-only decision. The stack is now explicitly bimodal.

- **Domain services: Java 21 + Spring Boot 4.0.5.** Jakarta Persistence 3.2 + Hibernate, Jakarta Bean Validation on DTOs, MapStruct for DTO↔Entity mapping, Resilience4j for remote-call resilience (circuit breaker + retry + time-limiter), springdoc for OpenAPI, Spring Data JPA repositories with `@EntityGraph` for eager-path control. Applies to: Claims, Intake Orchestrator, Identity & Auth, Policy & PAS Gateway, Surveyor, Cashless, Recovery & Salvage, Third-Party Liability, Grievance, AML, Consent, Audit, Routing Engine (§6.7), Integration Gateway (§10.1).
- **AI/ML workers: Python 3.12.** Applies to: AI Orchestrator (§6.3), Fraud Engine all three layers (§6.4), Forensics Worker (§6.5), Explanation Worker (§6.6). Kept on Python because model serving (Triton clients, PyTorch/ONNX runtimes, DGL for GNNs), image forensics (`numpy`/`scipy`/`cv2`/`c2pa-python`), conformal prediction (MAPIE), and LLM/prompt tooling are Python-native and have no competitive Java equivalents. Kafka + Protobuf remains the inter-service contract, so the language split is invisible to callers.
- **Rationale for the override.** Team skill mix favours the JVM for domain-services work. Spring Boot's transactional data-access, Bean Validation, auditing, scheduled jobs, typed HTTP clients, and enterprise ecosystem are a better fit for the claims lifecycle than FastAPI. The AI/ML stack has no equivalent leverage on the JVM, so the bimodal split is accepted explicitly rather than forced to one side.
- **Trade-offs accepted.**
  - Two CI pipelines (Maven for Java; `uv`/Poetry for Python).
  - Two dependency-scanning chains (Trivy + OWASP Dependency-Check for Java; `pip-audit` for Python).
  - Two test runners (JUnit 5 + Mockito + Testcontainers-Java for Java services; `pytest` + `testcontainers` for Python workers).
  - No shared in-process code across languages; inter-service contracts are Protobuf on Kafka or OpenAPI/REST.
- **Reference scaffold.** `backend/` at repo root is the canonical Spring Boot module. `pom.xml` pins the dependency set; per-env configuration lives in `application{.yml,-dev.yml,-qa.yml,-uat.yml,-prod.yml}`. Key defaults: `spring.jpa.open-in-view=false`, `default_batch_fetch_size=25`, Resilience4j instance `pas` pre-configured for the PAS integration, `@EntityGraph` on repository fetch paths.

### 5.2 Mobile — ADR-002 `[FR-2, FR-19, U-17, F-11]`
- **Flutter 3.x** (Dart) with **Isar** for offline DB, **image_picker** + platform channels for camera + C2PA signing, **tus_client** for resumable uploads, **firebase_messaging** for push.
- **Rationale:** mature camera plugins, excellent offline persistence, single-codebase iOS + Android, easy to embed native C2PA libraries via platform channels.
- **Alternative:** React Native with Expo (acceptable; slightly weaker on offline-first tooling).

### 5.3 Web (Admin / Adjuster / Surveyor / Grievance / AML / Finance) — ADR-003 `[FR-10, FR-12, FR-16, FR-17]`
- **React 18 + TypeScript + Vite** with **TanStack Query** (server state), **Zustand** (UI state), **shadcn/ui + Tailwind** (components), **zod** (validation), **react-hook-form**.
- Charting: **Recharts** (simple dashboards) + **Apache ECharts** (analytics).
- Real-time queue updates: **Server-Sent Events** from gateway.

### 5.4 Transactional datastore — ADR-004 `[§6 of FRS]`
- **PostgreSQL 16** managed (AWS RDS or Aurora PG). Monthly partitioning on `claims`, `events`, `audit_entries` to keep hot indexes small.
- Extensions: `pgcrypto` (encryption functions), `pg_stat_statements` (observability), `pgvector` (future: embedding search on narratives).
- Read replicas for analytics reads; write only to primary.

### 5.5 Object storage — ADR-005 `[FR-2, FR-6, FR-11]`
- **AWS S3** in `ap-south-1` (Mumbai) with three buckets per environment:
  - `media-hot` (claim images/video, lifecycle → `media-cold` at 90 days)
  - `artefacts` (forensic sidecars, ELA heatmaps, C2PA manifests, LLM prompt/input/output snapshots)
  - `audit` — **Object Lock (Compliance mode)** for WORM `[FR-11]`
- On-prem alternative: MinIO with Object Lock enabled.

### 5.6 Message bus — ADR-006 `[§4.2, FR-11]`
- **AWS MSK (Kafka 3.6)** with **Confluent Schema Registry** (self-managed).
- **Protobuf** internal events (performance, schema evolution).
- Topic naming: `domain.aggregate.event_v{n}` (e.g., `claims.claim.submitted_v1`).
- Partitions keyed by aggregate id.
- DLQ topic per consumer group (`*.dlq.v1`).

### 5.7 Cache / online feature store — ADR-007 `[FR-5]`
- **Redis 7** (ElastiCache). TTLs tuned per cache namespace. Used for:
  - Session revocation lists.
  - Rate-limit counters.
  - Online fraud features (latest claim count per policy, graph-neighbour stats).
  - Idempotency keys for webhooks.

### 5.8 Analytics / offline features — ADR-008 `[§10, FR-5]`
- **ClickHouse** cluster (self-hosted, 3-node) for RPT-001…RPT-010 and offline feature computation.
- Why not BigQuery: cost-efficiency at expected volumes, data-residency clarity, tight SQL semantics for analytics-heavy workload.

### 5.9 Orchestration — ADR-009
- **Amazon EKS** (Kubernetes 1.30). Node pools:
  - `system` (2× m5.large)
  - `apps` (HPA, t3.xlarge baseline)
  - `ai-cpu` (c6i.2xlarge, for forensics + fraud L1 + cost estimation)
  - `ai-gpu` (g5.xlarge, NVIDIA A10G, for YOLOv8/RT-DETR + deepfake + PRNU)
- Scaling: HPA for CPU-bound services; **KEDA** for Kafka-lag-driven AI workers.

### 5.10 Model serving — ADR-010, ADR-011
- **MLflow** (self-hosted) for model registry + experiment tracking; artefacts in S3.
- **NVIDIA Triton** for vision models (ONNX/TensorRT); **BentoML** for tabular fraud models (LightGBM).
- **vLLM** for self-hosted LLM fallback; **Azure OpenAI** (India region) as primary under a DPA with zero-retention; both behind a `MultimodalExplainer` abstraction (FRS §8.2, `[U-13]`).

### 5.11 Damage-detector license — ADR-013 `[U-12, C-09]`
- **Recommended: RT-DETR (Apache-2.0)** to eliminate AGPL exposure. Accuracy is competitive and export-to-ONNX + Triton is clean.
- **Alternative:** YOLO-NAS (Apache-2.0) or licensed Ultralytics Enterprise.
- **Decision owner:** Engineering Lead. Must be resolved before any detector training starts.

### 5.12 Authentication stack — ADR-020 `[FR-1]`
- Policyholder OTP: **MSG91** primary, **Twilio** failover. DLT-templated SMS (TRAI).
- Internal SSO: **Keycloak 24** (self-hosted) or customer's **Azure AD** — configurable.
- Surveyor IdP: separate Keycloak realm `surveyors`, tokens carry `licence_id` claim.
- JWT: **RS256** signed by **AWS KMS**; public JWKS published; access TTL 15m, refresh TTL 30d with rotation + reuse detection.

### 5.13 Authorization — ADR-014 `[§3 FRS]`
- **Open Policy Agent (OPA)** sidecar; policies written in Rego and version-controlled.
- RBAC + ABAC in one policy layer: role → capability mapping plus assignment-based restrictions (🔒) and four-eye (🗝️) gates.
- Policies loaded from a bundle server; changes go through PR + canary.

### 5.14 Secrets — ADR-015 `[NFR-S-005]`
- **HashiCorp Vault** (KV v2, PKI, Transit engines) + Kubernetes **External Secrets Operator**.
- No secret in env files, images, or Git. Pre-commit `gitleaks`. `[NFR-S-015]`

### 5.15 Audit anchoring — ADR-016 `[U-18, R-15]`
- **Dual RFC 3161 TSA**: DigiCert + FreeTSA (or equivalent commercial second TSA). Nightly Merkle root is stamped by both. Failure of both for > 24 h triggers degraded mode.

### 5.16 IaC & release — ADR-017, ADR-019
- **Terraform** for AWS, VPC, RDS, MSK, ElastiCache, S3, KMS, IAM, EKS.
- **Helm** charts per service. **Argo CD** for GitOps.
- **GitHub Actions** for CI (build/test/scan); Argo CD for CD. Branch-to-env mapping in §17.

### 5.17 Observability — ADR-018
- **OpenTelemetry** SDKs in every service → OTLP → **Grafana Tempo** (traces).
- Metrics: **Prometheus** + **Grafana**; alerting via **Alertmanager** → **PagerDuty**.
- Logs: **Loki** (structured JSON, retention 30d hot + 1y cold in S3).

### 5.18 Encryption — ADR-022 `[NFR-S-006]`
- At rest: **AES-256-GCM**; envelope encryption with **AWS KMS CMK** (BYOK in prod).
- Column-level encryption (via `pgcrypto`) for PII columns in Postgres.
- In transit: **TLS 1.3** externally; **mTLS** via Istio internally.
- Key rotation: automatic annual CMK rotation; data-key rotation monthly.

---

## 6. Component Designs (C4 L3, per service)

Each section below follows: **Responsibilities · Public API (summary) · Consumed events · Emitted events · Data owned · Failure modes · FRS anchors**. Sequence-diagram sketches are included where the interaction is non-obvious.

### 6.1 Claims Service `[FR-1.link, FR-2, FR-8, FR-9, FR-11]`
- **Responsibilities.** Own the `Claim` aggregate. FNOL create/convert, formal submission, state transitions, reopen, adjuster decisions, override history, claim-time KYC re-check delegation.
- **Public API.** `/v1/fnol/*`, `/v1/claims/*`, `/v1/claims/{id}/submit`, `/v1/claims/{id}/decision`, `/v1/claims/{id}/reopen`, **`/v1/claims/with-analysis` (multipart; see §6.3 MVP variant), `/v1/claims/config` (advertises `claims.max-evidences` / `claims.min-evidences-for-analysis` to clients per FRS AC-2.1.10)** `[v1.2]`, **`GET /v1/claims/{id}/analysis` (serves the persisted `claim_analysis` row; 204 when no analysis exists), `GET /v1/claims/{claimId}/evidence/{evidenceId}` (streams the persisted bytes; 404 on missing)** `[v1.3 — FRS AC-2.1.12 / 2.1.13]`.
- **Consumed events.** `detection.completed`, `forensics.completed`, `fraud.scored`, `estimate.completed`, `explanation.completed`, `routing.decided`, `surveyor.report_submitted`, `cashless.settled`, `pennydrop.verified`.
- **Emitted events.** `fnol.received`, `claim.submitted`, `claim.decision_made`, `claim.reopened`, `claim.state_changed`.
- **Data owned.** `fnol`, `claim`, `claim_history`, `decision`, `reopen_reason`.
- **Failure modes.** Optimistic-concurrency rejects; audit spool backpressure (fails closed per `[U-18]`). ML-service unavailability is handled by the `MlAnalyzeClient` fallback (empty analysis, `modelVersion="ml-service-unavailable"`); the claim still persists and routes to `HUMAN_REVIEW` per FRS AC-2.1.11.

### 6.2 Intake Orchestrator `[FR-2, FR-19, FR-2 offline U-17, F-11]`
- **Responsibilities.** Normalise FNOL/formal-claim creation across channels (mobile/agent/IVR/WhatsApp/RCS); OCR dispatch; C2PA manifest verification pre-persist; resumable-upload coordination.
- **Key flows.**
  1. Mobile online submit → Intake → Claims Service.
  2. Mobile offline submit → local queue → deferred upload → Intake verifies 72h TTL and dedupe hash `[U-17]`.
  3. IVR FNOL → Intake → Claims Service → SMS deep-link returned for photo capture later.
- **OCR dispatch.** Invokes Integration Gateway's OCR client; persists extracted structured fields; flags `invoice_gstn_status`.

### 6.3 AI Orchestrator `[FR-3..FR-8]`
- **Responsibilities.** Execute the DAG below per claim. Enforces per-stage latency budget (§5.1 NFR table).

```
                  ┌────────────┐
                  │ Detection  │ YOLOv8/RT-DETR on each image (parallel)
                  └─────┬──────┘
                        │
         ┌──────────────┴────────────────┐
         ▼                                 ▼
  ┌──────────────┐                 ┌───────────────┐
  │  Forensics   │   parallel      │ Cost Estimate │
  └──────────────┘                 └───────────────┘
         │                                  │
         └───────────────┬──────────────────┘
                         ▼
                ┌──────────────────┐
                │  Fraud L1+L2+L3  │
                └────────┬─────────┘
                         ▼
                ┌──────────────────┐
                │   Explanation    │
                └────────┬─────────┘
                         ▼
                ┌──────────────────┐
                │  Routing Engine  │
                └──────────────────┘
```
- **Implementation.** Python service; uses **Kafka topics as the DAG edges** (no in-process state). DAG status persisted in `ai_run` table keyed by `claim_id`.
- **Back-pressure & retries.** KEDA scales each worker on consumer-group lag; retries with exponential backoff; dead-letter after 3 attempts; failure routed to `HUMAN_REVIEW` by FR-8 AF-8.1.

#### 6.3.MVP — Consolidated `ml-services` process `[NEW in v1.2 — ADR-025]`

For the hackathon / phase-1 deployment the DAG above is collapsed into **a single FastAPI process** at repo-root `ml-services/` (`app/main.py`). One synchronous multipart endpoint fulfils FR-3, FR-4, and FR-7 together — the decomposed per-worker + Kafka design remains the phase-2 target.

- **Endpoint.** `POST http://ml-services:8000/api/v1/analyze` (multipart/form-data).
  - `images[]` — 3 ≤ n ≤ 15 JPEG/PNG files (the Python `MIN_IMAGES=3`, `MAX_IMAGES=15` hard-coded in `app/main.py`; the Spring Boot `claims.max-evidences` must stay ≤ `MAX_IMAGES`).
  - `video?` — single optional MP4 (frames extracted server-side).
  - `narrative?` — ≤ 4000-char free-text surveyor context.
- **Response.** `AnalyzeResponse` (`app/schemas.py`) — `model_version`, per-image detections with bboxes + severity, `cost_estimate` in paise with low/high band, GPT-4o `surveyor_assessment`, `processing_time_ms`.
- **Caller.** `MlAnalyzeClient` in the Java Claims Service (Spring `RestClient`, Resilience4j instance `ml-analyze`: COUNT_BASED sliding window of 20, ≥ 5 calls to trigger, 60 % failure-rate threshold, 30 s slow-call threshold, 60 s open duration; 2-attempt retry, 60 s read-timeout). Fallback returns `{modelVersion: "ml-service-unavailable", totalDetections: 0, images: [], costEstimate: null, surveyorAssessment: null}` — see FRS AC-2.1.11.
- **JSON naming.** Python emits snake_case; Java DTOs (`MlAnalyzeResponse` and nested records) use `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy)` + `@JsonIgnoreProperties(ignoreUnknown = true)` so field additions on the Python side are backward-compatible.
- **Persistence.** No dedicated `claim_analysis` row yet — the response is returned transiently with the `ClaimResponse` inside `ClaimWithAnalysisResponse`. Persistent storage is deferred to phase 2 (see Roadmap in `README.md`).
- **Phase-2 migration path.** When Kafka + per-worker deployment lands: swap `MlAnalyzeClient` for an `AiOrchestratorDispatcher` that publishes `ai.analyze.requested.v1`; the Java Claims Service stops waiting synchronously and subscribes to `routing.decided`. Existing `/with-analysis` endpoint is deprecated in favour of `/submit` + event stream.

### 6.4 Fraud Engine (three-layer) `[F-01..F-10, F-16..F-22]`
- **Layer 1 — Deterministic Rules.** Python service; rules loaded from a content-addressed YAML registry; each rule has a `legal_rationale` field. Evaluation in-process; latency budget ≤ 200ms.
- **Layer 2 — Calibrated ML.** LightGBM + isotonic head + **MAPIE** for conformal prediction bands; served via BentoML; features from Feast online + ClickHouse offline.
- **Layer 3 — Graph (GNN).** DGL-based GNN; graph is rebuilt nightly from `graph_edges` materialised view in ClickHouse; online lookups served from Redis nearest-neighbour cache. Embeddings updated nightly; ring-risk is a real-time lookup.
- **Cascade action.** On FR-9 confirmed-fraud event, the engine publishes `fraud.confirmed`; a cascade worker enumerates graph-linked open claims and re-evaluates routing per FR-8.

### 6.4.MVP — Evidence & Analysis storage `[NEW in v1.3 — ADR-026, ADR-027; FRS AC-2.1.12, AC-2.1.13]`

**Evidence byte storage (phase 1).** Claim Service writes each uploaded file's bytes to the local filesystem at `{claims.upload-dir}/{evidenceId}` immediately after the `ClaimEvidence` row is persisted (so the generated UUID is available as the storage key). Content type and size are captured on the `claim_evidence` row (`content_type`, `size_bytes` columns) — no on-disk metadata beyond the bytes themselves, to keep move-to-S3 trivial.

- **Configuration.** `claims.upload-dir` (default `uploads`, overridable per profile; prod expects an env-driven absolute path pointing at an EBS / EFS volume or — phase-2 — `s3://…` prefix after ADR-005 wiring lands).
- **Retrieval.** `GET /api/v1/claims/{claimId}/evidence/{evidenceId}` validates that the evidence belongs to the named claim, reads the bytes, returns `Content-Type` from `claim_evidence.content_type`, `Content-Disposition: inline`, `Cache-Control: private, max-age=3600`.
- **Absences.** Legacy `claim_evidence` rows created before v1.3 have no on-disk bytes → endpoint returns 404; the frontend thumbnail renders a dashed "?" placeholder.
- **Phase-2 migration path.** Replace `ClaimService.writeEvidenceToDisk(...)` + `loadEvidenceContent(...)` with an `EvidenceStore` interface backed by S3 (see ADR-005 `media-hot` bucket with WORM audit). The endpoint contract is stable — only the backing store changes. The `content_type` / `size_bytes` columns become canonical for metadata either way.

**AI analysis persistence.** The `ClaimAnalysis` aggregate is one-to-one with `Claim` (unique `claim_id`). On every `POST /with-analysis` the service writes / upserts a row with:
- Denormalised scalars for reporting / queryability: `model_version`, `total_detections`, `processing_time_ms`, `severity_verdict`, `repair_recommendation`, `cost_total_paise`, `assessment_confidence`.
- `payload_json` (TEXT) — the full `MlAnalyzeResponse` serialised via a locally-instantiated `ObjectMapper` (camelCase, matches what the frontend consumes).
- Fallback responses (`modelVersion="ml-service-unavailable"`) are also persisted — audit trail of "we called, it failed".

`GET /api/v1/claims/{id}/analysis` reads the row, deserialises `payload_json`, returns the `MlAnalyzeResponse`. No analysis row → `204 No Content`; no claim → `404`.

**What's intentionally deferred**: proper JSONB column with a GIN index (phase-2, migrating to Liquibase-owned DDL); versioning to keep history of re-runs; a `claim_analysis_history` append-only log for fraud investigation. For now the row is the current-truth overwrite-on-rerun view.

### 6.5 Forensics Worker `[F-11..F-15]`
- Python service. Components:
  - **C2PA verifier** (`c2pa-python`).
  - **ELA** + **noise-residual/SRM** + **JPEG Q-Tab** analyser.
  - **Deepfake CNN** (Triton).
  - **PRNU** — triggered only for high-value claims; runs on `ai-cpu` pool.
- Outputs written to `forensic_result` table and `artefacts` bucket.

### 6.6 Explanation Worker `[FR-7, U-03, U-13]`
- Python service implementing `MultimodalExplainer`:

```python
class MultimodalExplainer(Protocol):
    async def explain(self, *, prompt_version_id: str,
                      inputs: ExplainInputs,
                      images: list[ImageRef],
                      audience: Literal["policyholder", "adjuster"]
                     ) -> ExplainOutput: ...
```
- Primary backend: Azure OpenAI (GPT-4o-class, India region, zero-retention DPA).
- Fallback backend: self-hosted vLLM running a Vision-capable open model (Llama-4-Vision or Qwen-VL-class).
- JSON schema validation on output; repair retry once; template fallback.
- Prompt registry: Postgres table `prompt_registry` with content-addressed storage of immutable prompt text.

### 6.7 Routing Engine `[FR-8, U-11, M-26]`
- **Java (Spring Boot) domain service** per ADR-001 revised. Pure function over AI outputs + thresholds snapshot + DOA matrix — no ML dependency.
- Thresholds loaded from `threshold_version` table; the engine pins the snapshot at decision time.
- Exposes `/v1/internal/routing/evaluate` for dry-runs used by Admin change-request preview.
- Called by Claims Service (§6.1) either in-process (same JVM module) or over internal REST; the synchronous-decision invariant from §2.1 principle 5 holds either way.

### 6.8 Audit Service `[FR-11, U-09, U-18]`
- Append-only writer:
  - Canonical JSON → SHA-256 → `prev_hash || canonical → curr_hash`.
  - Writes: Postgres `audit_entries` (partitioned by month) **and** S3 WORM bucket (object per entry keyed by id).
- Nightly Merkle-root job: computes root of the day's entries, stamps with dual TSAs, publishes root to an internal notary topic.
- **Erasure dual-path API** `[U-09]`: `erasure.delete` performs cascaded physical delete across primary, replicas, and nearest-line backups on the retention window; `erasure.redact` replaces PII with pseudonyms while preserving referential integrity; both emit immutable audit entries referencing the legal basis.

### 6.9 Surveyor Service `[FR-12]`
- Panel directory (synced daily from IRDAI-empanelled feed).
- Assignment algorithm: nearest-available eligible surveyor with no conflict-of-interest; exponential back-off across surveyors if SLA missed.
- Digital signing: surveyor's licence-bound key (OS keystore).

### 6.10 Cashless Service `[FR-13]`
- Garage-portal integration (REST + webhooks).
- Variance adjudication: auto-settle within ±10 % (configurable); Sr. Adjuster review otherwise.
- Garage-score gate invokes Fraud Engine L3 (graph).

### 6.11 Recovery & Salvage Service `[FR-14]`
- Separate aggregates `recovery_case`, `salvage_case`; lifecycle events to the ledger via Integration Gateway.
- Salvage bidding interface is out-of-scope for v1; for MVP, salvage value is entered by a Sr. Adjuster with audit.

### 6.12 Third-Party Liability Service `[FR-15]`
- Aggregate `tp_claim` linked to source `claim`. MACT case tracking via external legal-case system integration; intake + initial reserving only in scope.

### 6.13 Grievance Service `[FR-16]`
- IGMS-aligned case model; SLA clocks per IRDAI norms; auto-escalation day 14; sync with IRDAI IGMS via Integration Gateway.

### 6.14 AML Service `[FR-17]`
- Alert queue fed by sanctions-provider webhooks + fraud-engine suspicious-pattern signals.
- STR draft composer (FIU-IND format); evidence package bundler.
- Cycle-time disclosure report generator on monthly schedule.

### 6.15 Consent Service `[FR-18]`
- DPDP consent artefact store (`consent_artefact`); granular purpose codes; revocation propagation (24h SLA) via a `consent.revoked` event consumed by downstream processors.
- Data-principal rights portal: view/export/correct/erase flows.

### 6.16 Identity & Auth Service `[FR-1]`
- OTP issue/verify; OIDC callbacks; soft-match + hard-match linking ladder; session management; surveyor licence verification.

### 6.17 Policy & PAS Gateway `[M-19]`
- Adapter to the Policy Administration System. Bi-directional sync: reads coverage/endorsement; writes reserve/settlement back via event-driven CDC (Outbox pattern) or REST depending on PAS contract.

### 6.18 Admin/Reporting (backend)
- Serves queue views, claim-detail composition (stitches data from multiple services via a read-model), and analytics proxying to ClickHouse.

---

## 7. Data Architecture

### 7.1 Logical ER (textual)

Entities already specified in FRS §6.1 (v0.2). Their relationships:

```
User 1──n Policy 1──n Claim 1──n Image
                             1──n DetectionResult (via Image)
                             1──n ForensicResult (via Image)
                             1──1 CostEstimate
                             1──1 FraudResult
                             1──n Explanation (per audience)
                             1──1 RoutingDecision
                             1──n SurveyorAssignment
                             1──n SurveyorReport
                             1──n CashlessPreAuth
                             1──n RecoveryCase
                             1──1 SalvageCase (when total-loss)
                             1──1 TPClaim (when TP)
                             1──n GrievanceCase
                             1──n AmlAlert
                             1──n ConsentArtefact (indirectly via User)
                             1──n CaptureManifest (via Image)
                             1──n AuditEntry

Claim n──n CatEventTag
Claim 1──1 Reserve
Role/DoaMatrix — separate lookup tables
```

### 7.2 PostgreSQL schema (abridged DDL)

```sql
-- Partitioned parent: claims
CREATE TABLE claim (
  claim_id            TEXT PRIMARY KEY,
  fnol_id             TEXT,
  policy_id           TEXT NOT NULL,
  incident_type       TEXT NOT NULL,
  incident_at         TIMESTAMPTZ NOT NULL,
  incident_location   GEOGRAPHY(Point, 4326),
  narrative           TEXT,
  intake_channel      TEXT NOT NULL,           -- [M-32]
  status              TEXT NOT NULL,
  ai_confidence       NUMERIC(4,3),
  fraud_score         NUMERIC(5,2),
  fraud_band          TEXT,                    -- [F-03]
  routing_decision    TEXT,
  surveyor_assignment_id TEXT,                 -- [M-02]
  tp_claim_id         TEXT,                    -- [M-08]
  reserve_id          TEXT,                    -- [M-25]
  cat_event_tags      TEXT[],                  -- [M-07]
  estimate_total_paise BIGINT,
  submitted_at        TIMESTAMPTZ NOT NULL,
  sla_deadline        TIMESTAMPTZ,
  prev_hash           BYTEA,
  curr_hash           BYTEA,
  created_at          TIMESTAMPTZ DEFAULT now(),
  updated_at          TIMESTAMPTZ DEFAULT now(),
  PARTITION BY RANGE (submitted_at)
);

CREATE TABLE claim_2026_04 PARTITION OF claim
  FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
-- one per month; created by a pg_partman job

CREATE INDEX idx_claim_policy ON claim(policy_id);
CREATE INDEX idx_claim_status ON claim(status) WHERE status IN ('SUBMITTED','AI_PENDING','HUMAN_REVIEW','FRAUD_INVESTIGATION','SURVEYOR_REQUIRED');
CREATE INDEX idx_claim_incident_at ON claim(incident_at);

-- PII stored separately, encrypted at rest
CREATE TABLE user_pii (
  user_id             TEXT PRIMARY KEY,
  name_enc            BYTEA,                   -- pgp_sym_encrypt
  mobile_enc          BYTEA,
  email_enc           BYTEA,
  address_enc         BYTEA,
  pseudonym           TEXT UNIQUE              -- stable token used in analytics/audit
);

-- Audit: append-only, partitioned, hash-chained
CREATE TABLE audit_entry (
  id                  BIGSERIAL,
  entry_hash          BYTEA NOT NULL,
  prev_hash           BYTEA,
  actor               JSONB NOT NULL,
  action              TEXT NOT NULL,
  resource            JSONB NOT NULL,
  body                JSONB NOT NULL,
  signing_key_id      TEXT NOT NULL,
  ts                  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (ts, id)
) PARTITION BY RANGE (ts);
-- Database role CLAIM_SERVICE has INSERT only, no UPDATE/DELETE grants on audit_entry [FR-11]

-- Consent (DPDP)
CREATE TABLE consent_artefact (
  artefact_id         TEXT PRIMARY KEY,
  principal_id        TEXT NOT NULL,
  purpose             TEXT NOT NULL,
  lawful_basis        TEXT NOT NULL,
  granted_at          TIMESTAMPTZ NOT NULL,
  revoked_at          TIMESTAMPTZ,
  cm_reference        TEXT,
  created_by          TEXT NOT NULL
);
```

### 7.3 Partitioning & lifecycle
- `claim`, `audit_entry`, `event_outbox` — monthly partitions via `pg_partman`.
- Old partitions detached after 13 months, archived to S3 (`cold`) as parquet.
- Right-to-erasure honoured in cold tier by rewriting parquet partitions.

### 7.4 S3 bucket layout

| Bucket | Prefix pattern | Lifecycle |
|---|---|---|
| `media-hot` | `claims/{yyyy-mm}/{claim_id}/img/{image_id}.jpg` | → `media-cold` @ 90d |
| `media-cold` | same | → Glacier Deep Archive @ 2y |
| `artefacts` | `ai/{yyyy-mm}/{claim_id}/{kind}/{file}` — ELA, C2PA manifest, LLM IO | Delete @ 7y |
| `audit` | `audit/{yyyy-mm-dd}/{seq}.json` (Object Lock: Compliance, 7y) | WORM 7y |

### 7.5 Redis keys (naming + TTL)
```
session:{jti}                  value: revocation flag         TTL: token exp
rl:otp:mobile:{msisdn}        value: counter                  TTL: 15m
rl:otp:ip:{ip}                value: counter                  TTL: 1h
feat:fraud:policy:{id}:v1     value: hash                     TTL: 24h
idem:{endpoint}:{key}         value: response                 TTL: 24h
graph:ring_risk:{claim_id}    value: float                    TTL: 1h
```

### 7.6 ClickHouse tables (analytics + features)
- `claim_fact` (denormalised, daily ingest from Kafka via ClickPipes/Vector).
- `fraud_features_offline` (offline materialised features for backtesting).
- `model_metrics` (per-prediction scores, latency, outcomes).
- `bias_subgroup` (for RPT-008 `[F-21]`).

---

## 8. API Design Standards

### 8.1 Conventions
- REST over HTTPS; JSON; UTF-8; RFC-3339 timestamps.
- Base path: `/v1/...`. Major-version only URL-versioning; breaking changes land on `/v2`.
- Resource-oriented naming; verbs only for actions (`/submit`, `/decision`, `/reopen`).
- Money: integer paise + `currency: "INR"`.

### 8.2 Pagination
- Cursor-based: `?cursor=…&limit=…`, `limit` max 100; responses include `next_cursor`.
- No offset pagination for large collections (performance).

### 8.3 Idempotency
- All `POST` that mutate state require `Idempotency-Key` header (UUID v4).
- Server stores `(key, endpoint)` → first response for 24h; replays return the cached response.

### 8.4 Rate limiting
- Per-principal: 600 req/min default; burst 60.
- Per-IP: 1200 req/min. Surveyor and garage endpoints have separate quotas.
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

### 8.5 Error envelope
Exactly as FRS §7.2. Every response carries `X-Request-Id`; every 5xx additionally carries `X-Trace-Id`.

### 8.6 OpenAPI
- Each service publishes OpenAPI 3.1 at `/v1/openapi.json`.
- A workspace-level bundled spec is generated nightly for the portal.
- Breaking-change detection in CI (`openapi-diff`).

### 8.7 Sample OpenAPI fragment (FNOL)

```yaml
paths:
  /v1/fnol:
    post:
      summary: Create FNOL
      operationId: createFnol
      parameters:
        - in: header
          name: Idempotency-Key
          required: true
          schema: { type: string, format: uuid }
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/FnolCreate' }
      responses:
        '201':
          description: FNOL created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/FnolCreated' }
        '422':
          $ref: '#/components/responses/ValidationError'
components:
  schemas:
    FnolCreate:
      type: object
      required: [policy_identifiers, incident_type, occurred_at, ncb_acknowledged, intake_channel]
      properties:
        policy_identifiers:
          type: object
          properties:
            mobile: { type: string, pattern: '^\\+91[0-9]{10}$' }
            policy_number: { type: string }
        incident_type:
          type: string
          enum: [collision, theft, vandalism, natural_event, third_party, fire_damage, other]
        occurred_at: { type: string, format: date-time }
        approximate_location:
          type: object
          properties: { lat: {type: number}, lng: {type: number} }
        brief_narrative: { type: string, maxLength: 500 }
        ncb_acknowledged: { type: boolean, enum: [true] }
        intake_channel: { type: string, enum: [mobile, agent, ivr, whatsapp, rcs] }
```

---

## 9. Event / Message Schemas

### 9.1 Event catalog (excerpt)

| Topic | Key | Producer | Consumers | Schema |
|---|---|---|---|---|
| `claims.fnol.received_v1` | `fnol_id` | Claims Svc | AI Orchestrator (skip), Audit, Analytics | `FnolReceived` |
| `claims.claim.submitted_v1` | `claim_id` | Claims Svc | AI Orchestrator, Audit, Analytics | `ClaimSubmitted` |
| `ai.detection.completed_v1` | `claim_id` | YOLO Worker | AI Orchestrator, Audit | `DetectionCompleted` |
| `ai.forensics.completed_v1` | `claim_id` | Forensics Worker | AI Orch, Audit | — |
| `ai.fraud.scored_v1` | `claim_id` | Fraud Svc | AI Orch, Audit, Analytics | — |
| `ai.routing.decided_v1` | `claim_id` | Routing Engine | Claims Svc, Audit, Notifications | — |
| `claims.decision.made_v1` | `claim_id` | Claims Svc | Cashless, Ledger, Notifications, Audit, Analytics | — |
| `fraud.confirmed_v1` | `claim_id` | Claims Svc | Fraud Ring-Cascade Worker | — |
| `cashless.settled_v1` | `preauth_id` | Cashless Svc | Ledger, Claims, Audit | — |
| `audit.anchor.published_v1` | `yyyy-mm-dd` | Audit Svc | Notary log topic | — |

### 9.2 Schema example (Protobuf)

```proto
syntax = "proto3";
package claims.v1;

message ClaimSubmitted {
  string claim_id = 1;
  string fnol_id = 2;
  string policy_id = 3;
  string intake_channel = 4;
  string incident_type = 5;
  int64  incident_at_unix_ms = 6;
  repeated string image_ids = 7;
  string correlation_id = 8;
  int64  submitted_at_unix_ms = 9;
  // PII is NEVER on the bus; names/phones/addresses stay behind PII-vault tokens.
}
```

### 9.3 Delivery semantics
- Producers use **idempotent Kafka producers** and the **transactional outbox pattern** (Postgres `event_outbox` table + Debezium CDC → Kafka) so a DB commit and event emission are atomic.
- Consumers are at-least-once; idempotency keys on downstream effects prevent duplicate side-effects.
- DLQ: 3 attempts with exponential backoff, then route to `*.dlq.v1`; on-call alert fires.

### 9.4 Schema evolution
- Backward-compatible additions only on `_v1` topics; breaking changes get `_v2` with dual-running period.
- Schema Registry enforces compatibility on publish.

---

## 10. Integration Architecture

### 10.1 Integration Gateway
- Typed clients per external system — **Java + Spring `RestClient` + Resilience4j** per ADR-001 revised.
- Shared middleware:
  - **Circuit breaker** (Resilience4j `CircuitBreaker`, per-integration instance; sliding-window COUNT_BASED; fallback on open).
  - **Retry** (Resilience4j `Retry`) with exponential backoff and jitter (max 3 attempts, respecting `Retry-After`).
  - **Time limiter** (Resilience4j `TimeLimiter`) per integration (p95-based, documented in `integration_slos.md`).
  - **Rate limit** per integration (Resilience4j `RateLimiter` or Bucket4j).
  - **SSRF guard** (resolve + CIDR allowlist via a shared filter).
  - **mTLS** where the counterparty supports it (configured on the `RestClient`'s underlying `HttpClient`).
  - **Outbound audit entry** per call with request/response hashes (Spring interceptor emits to the audit topic).

### 10.2 Per-integration notes

| Integration | Auth | Timeout (P95) | Rate | Failure path |
|---|---|---|---|---|
| PAS | mTLS + OAuth2 client-credentials | 2.0s | per-tenant | Read-cached coverage; block writes + alert |
| VAHAN / SARATHI | API key | 3.0s | 60/min | Feature unavailable flag; fraud sub-score renormalised |
| IIB | mTLS | 5.0s | low | Feature unavailable flag |
| OCR (Textract or Azure Form Recognizer) | Signed URL | 10s | burstable | User re-scan or manual entry path |
| Penny-drop (Decentro/Cashfree) | API key + HMAC | 6.0s | per-tenant | Payout blocked until resolved |
| Ledger (SAP/Oracle) | iDoc or REST | 4.0s | queued | Outbox; retry; on breach → Finance Ops alert |
| IGMS | mTLS or signed POST | 5.0s | low | Internal grievance still progresses; external sync retried |
| Consent Manager | OAuth2 | 2.0s | steady | Block new consents; existing honoured |
| Weather data | API key | 3.0s | cached 1h | Feature unavailable |
| Sanctions/PEP | streaming webhook + daily full load | n/a (async) | — | Stale-list alert if feed > 24h old |
| TSA (RFC 3161) | mTLS | 5.0s | nightly | Failover to second TSA; alert if both fail |
| Azure OpenAI / vLLM | API key / mTLS | 10s | per-key | Templated fallback explanation |
| C2PA verifier | in-process | 100ms | — | `provenance=unverified` |

### 10.3 Contract testing
- **Pact** contracts for integrations we own both sides of; **replay-based** tests using stored real responses for external systems we do not control.
- Contract-test suite runs nightly and on every PR touching integration code.

---

## 11. Security Architecture

### 11.1 Authentication flows

**Policyholder (OTP)**
```
 mobile → /otp/request {msisdn}
        ← 202 + otp_tx_id
 SMS provider → user
 mobile → /otp/verify {otp_tx_id, otp}
        ← 200 {access_token (RS256), refresh_token, user}
```

**Internal / Surveyor (OIDC)** — authorization code with PKCE; backend exchanges code for tokens; issues internal JWT; surveyor tokens include `licence_id` verified against a daily-refreshed directory.

**Refresh-token rotation + reuse detection.** Stored as hashed values; reuse (same token presented twice) revokes the entire session tree.

**Device attestation.** Play Integrity on Android, App Attest on iOS — token minted at app launch, verified at claim submission. `[NFR-S-010]`

### 11.2 Authorization model
- Every request carries a JWT; API Gateway introspects and injects `x-principal` headers.
- Each service calls OPA sidecar with `(principal, action, resource)`; OPA returns allow/deny + reason. OPA decisions are logged.
- Assignment (🔒) and four-eye (🗝️) semantics are in Rego.

### 11.3 Encryption at rest
- EBS volumes: AES-256, KMS CMK.
- RDS: AES-256, KMS CMK.
- S3: SSE-KMS with CMK.
- App-level column encryption for PII (envelope via KMS).
- Backups encrypted; restores audited.

### 11.4 Network segmentation
- 3-tier VPC: public (ALB/NLB only), private-apps, private-data.
- Data-tier security groups accept only from apps-tier.
- Egress NAT through an egress-filtering proxy (Squid or mitmproxy) with an allowlist.

### 11.5 Threat model (STRIDE excerpt)

| Component | Threat | Mitigation |
|---|---|---|
| Mobile app | Spoofing (cloned APK / repackaged) | Play Integrity / App Attest (`NFR-S-010`); certificate pinning with backup pin |
| Mobile upload | Tampering with images in flight | C2PA signing at capture (`F-11`) + TLS |
| Claims Service | Elevation of privilege via IDOR | OPA sidecar enforces `policy_id` ∈ JWT `policy_ids` on every read |
| Audit Service | Repudiation via log manipulation | Hash chain + dual-TSA external anchor (`U-18`) |
| LLM Worker | Information disclosure via provider retention | DPA zero-retention + face-mask + PII redaction pre-call (`U-10`) |
| Fraud Engine | Adversarial evasion of L2 | Red-team corpus, champion-challenger shadow, conformal abstain (`F-22, F-02, F-03`) |
| Integration Gateway | SSRF on outbound fetchers | CIDR-allowlist resolver; egress proxy |
| Admin UI | CSRF | SameSite cookies + CSRF tokens; strict CORS |
| Any service | DoS | Kong rate-limits + KEDA scale-out + per-tenant caps |

### 11.6 OWASP Top-10 controls mapping
A01 BAC → OPA + IDOR CI tests; A02 Crypto → AES-256-GCM, TLS 1.3, KMS; A03 Injection → parameterised queries (SQLAlchemy Core), template-escaped rendering; A04 Insecure Design → this TSD, threat models, ADRs; A05 Misconfig → IaC scanning (tfsec, trivy); A06 Vulnerable/Outdated → dependency scans gate (pip-audit, npm audit); A07 Authn Failures → JWT with rotation + reuse-detect; A08 Data Integrity → hash chain, image C2PA, signed artefacts; A09 Logging/Monitoring → OTel + audit chain; A10 SSRF → egress allowlist + resolver guard.

### 11.7 IRDAI Cyber-Security 2023 controls mapping `[M-12]`
- Asset inventory: Kubernetes label conventions + an automated generator writing to the CMDB.
- CERT-In 6-hour incident reporting: runbook wired into PagerDuty incident workflow.
- Quarterly VAPT + annual CERT-In-empanelled audit: scheduled.
- Logs retention: audit log 7y; security event log 180d hot + 5y cold.
- Encryption at rest and in transit: per §11.3 / §11.4.

---

## 12. AI/ML Technical Design

### 12.1 Model serving topology
- **Vision (YOLOv8-class / RT-DETR, deepfake, PRNU optional).** ONNX → TensorRT; Triton Inference Server; A10G GPU node pool. Ensemble scheduler in Triton for chained detection+classification.
- **Fraud (LightGBM + isotonic + conformal).** BentoML containers, CPU node pool.
- **Graph (GNN).** DGL; nightly re-training on ClickHouse feature dump; online scoring via Python service with cached embeddings in Redis.
- **LLM.** Azure OpenAI (primary) or vLLM (fallback) behind `MultimodalExplainer`.

### 12.2 Feature store (Feast)
- Offline store: ClickHouse.
- Online store: Redis.
- Features defined in `features/` repo with Git-based promotion; materialisation via Airflow DAGs.
- Feature lineage: every feature recorded with source table, transformation SHA, effective-from timestamp.

### 12.3 Training pipelines
- **Airflow** DAGs in a dedicated `ml-train` namespace.
- Each DAG: extract → validate (great_expectations) → train → evaluate → register (MLflow).
- Artefacts written to `mlflow-artefacts` S3 bucket with content-addressed keys.
- Training data lineage recorded (`dataset_version` pinned at registry).

### 12.4 Promotion & rollout
- Stage: `staging` → `shadow` → `canary (5%→25%→100%)` → `prod`.
- Gates:
  1. Eval metrics ≥ baseline on golden set.
  2. Bias thresholds met (`[F-21]`).
  3. Red-team corpus pass (`[F-22]`).
  4. Security scan on model file (e.g., `pickle-scan`).
  5. Dataset lineage recorded.
- Promotion is a registry-pointer update; rollback is instantaneous (pointer swap); prior version kept warm 7 days.

### 12.5 Prompt registry
- Postgres `prompt_registry(prompt_version_id, sha256, text_ref_s3, model_family_hint, created_by, created_at, status)`.
- Text is content-addressed; storage immutable once promoted.
- Semantic-equivalence regression suite in `ml-eval/prompt-suite`; runs on every new `prompt_version_id`.

### 12.6 Drift & observability
- **Input drift (PSI/KS)** per feature — `[F-19]`.
- **Output drift** per class distribution — v0.1 baseline.
- **Outcome drift** — model vs adjuster alignment (RPT-005).
- **Bias drift** — subgroup FPR/FNR (RPT-008).
- Alerts into Prometheus → Alertmanager → PagerDuty.

### 12.7 Kill-switch
- A feature flag per model (`ff:model:yolov8.enabled`) evaluated at orchestrator entry. Disabling routes claims directly to `HUMAN_REVIEW` with reason `MODEL_DISABLED`.

---

## 13. Mobile App Technical Design

### 13.1 Stack
- **Flutter 3.x**, Dart 3.4.
- State: **BLoC**. Offline DB: **Isar** (encrypted with Cipher).
- Camera: `camera` + `image` packages; C2PA signing via native module (`c2pa-rs` bridged with FFI).
- Upload: **tus_client** for resumable uploads.
- Push: `firebase_messaging` (Android + iOS).
- Localisation: `intl` + ARB files (`en`, `hi`, `gu`, extensible per `[AMBIG-22]`).

### 13.2 Offline-first architecture (`[U-17]`)
```
 ┌──────────────────────┐
 │ UI (BLoC)            │
 └──────────┬───────────┘
            │ command
            ▼
 ┌──────────────────────┐
 │ Claim Draft Repository│
 └──────────┬───────────┘
            ▼
 ┌──────────────────────┐   offline queue
 │ Isar encrypted store  │  →→→ SyncWorker (when online) →→→ tus upload + /v1/fnol + /v1/claims/submit
 └──────────────────────┘
```
- All writes go to Isar first (72h retention cap, deterministic hash so the server can dedupe).
- `SyncWorker` reconciles on connectivity (Android `WorkManager`, iOS BGTaskScheduler).

### 13.3 Capture-time signing (`[F-11]`)
- On shutter: raw bytes → EXIF strip (server side) → C2PA manifest with (device key → attestation) → stored alongside bytes in Isar.
- Device key is generated at first-run, stored in OS keystore (Keychain/Android Keystore), never exported.

### 13.4 Biometric unlock + token binding
- Biometric unlock to access saved sessions.
- Access token bound to device key via a `dpop`-style proof-of-possession (RFC 9449) header on sensitive requests.

### 13.5 Security
- Certificate pinning (primary + backup pin).
- Jailbreak/root detection (flutter_jailbreak_detection); high-risk status attenuates auto-approval path (routes to human review regardless of score).
- Deep links: App Links (Android) + Universal Links (iOS) with signed payloads.

---

## 14. Web Dashboard Technical Design

### 14.1 Stack
- Vite + React 18 + TypeScript + Tailwind + shadcn/ui.
- Routing: **React Router 6**.
- Server state: **TanStack Query** with suspense; pagination via cursors.
- Forms: react-hook-form + zod (shared zod schemas generated from OpenAPI).
- Charts: Recharts + ECharts for analytics dashboards.
- Real-time updates: **SSE** from gateway (preferred over WebSocket for simplicity); topic per queue.

### 14.2 Role-aware routing
- A single SPA, authz-driven shell. On login, the server returns `capabilities[]`; the SPA maps capabilities to modules:
  - Adjuster Queue, Claim Detail, Bulk Ops (Sr.)
  - Surveyor Console
  - Grievance Console
  - AML Console
  - Finance Approver Console
  - Admin (Thresholds change-request, DOA matrix, model/prompt registry viewer, Users)
  - Analytics (ClickHouse-backed)

### 14.3 Accessibility
- Target: WCAG 2.1 AA.
- Contrast, focus rings, keyboard navigation, screen-reader landmarks on every route; automated axe checks in CI.

---

## 15. Deployment Architecture

### 15.1 Environments

| Env | Purpose | Region | Data class |
|---|---|---|---|
| dev | Ephemeral per-PR (optional) | ap-south-1 | Synthetic only |
| staging | Integration, load, UAT | ap-south-1 | Masked copy of prod |
| prod | Live | ap-south-1 (primary), ap-south-2 (warm standby) | Real PII |
| on-prem (optional) | Regulated insurer deployments | Insurer DC | Real PII |

### 15.2 EKS topology (prod, primary region)

```
VPC (3 AZs)
├── public subnets → ALB (ingress), NLB (Kafka)
├── private-apps  → EKS node pools (system, apps, ai-cpu, ai-gpu)
├── private-data  → RDS (PG), MSK, ElastiCache, ClickHouse
└── egress proxy  → allowlist NAT
```

### 15.3 DR posture
- **Primary:** ap-south-1 (Mumbai). **Warm standby:** ap-south-2 (Hyderabad).
- RPO ≤ 15 min (PG + S3 cross-region replication + MSK cross-region replicator).
- RTO ≤ 4h (infra pre-provisioned via Terraform in standby; runbook-driven activation; DNS failover via Route 53 health checks).
- Quarterly DR drills with documented findings.

### 15.4 On-prem alternative
- Kubernetes on bare-metal or VMware; MinIO for S3; self-managed Kafka + RDS-equivalent (EDB or Patroni); HashiCorp Vault; on-prem Azure OpenAI equivalent is **not available** so the LLM path must be vLLM + self-hosted vision models; forensics + detection fully in-premises.

---

## 16. Observability Architecture

### 16.1 Pillars
- **Traces:** OTel SDK in every service; exporter → Tempo via OTLP. Claim-id propagated via baggage across services and Kafka headers.
- **Metrics:** Prometheus scrape; service-level RED + custom (e.g., `ai_pipeline_duration_seconds{stage=…}`); exported via `/metrics`.
- **Logs:** Loki; JSON schema enforced; PII redaction middleware (central regex + list-based scrubber).

### 16.2 SLOs (must match FRS NFR-A)
- `policyholder_submit_availability` 99.5%.
- `ai_pipeline_completion_within_slo` 99.0% (P95 ≤ 45s v1, ≤ 30s v2).
- `admin_dashboard_availability` 99.5% business hours.
- Alerts on burn rate (fast + slow) using multi-window, multi-burn-rate formulas.

### 16.3 Dashboards (essential)
- Claim lifecycle funnel (FNOL → Submitted → AI pipeline done → Decision).
- AI per-stage latency distributions.
- Fraud score distribution; bias subgroup FPR/FNR.
- Integration SLOs per external system.
- Audit anchor health (TSA status, spool depth).
- Queue backlog per adjuster role.

---

## 17. CI/CD & Release Management

### 17.1 Pipeline

```
PR opened (any *-dev branch)
   ├── unit tests
   ├── lint (ruff / eslint) + types (mypy / tsc)
   ├── SAST (semgrep) + secret scan (gitleaks)
   ├── dependency scan (pip-audit, npm audit)
   ├── build + SBOM (syft)
   ├── container scan (trivy)
   ├── contract tests (Pact)
   ├── ephemeral integration tests (docker-compose / testcontainers)
   └── preview env (optional)

merge to development
   ├── full integration suite (K8s namespace)
   ├── load smoke (k6) — short
   ├── model regression (eval on golden set)
   └── deploy to staging (Argo CD sync)

release tag on main
   ├── image signed (cosign)
   ├── Argo CD promotes to prod via canary (5% → 25% → 100%)
   ├── post-deploy smoke
   └── rollback ready (Argo CD sync-wave previous)
```

### 17.2 Branch-to-env mapping (matches `CLAUDE.md` v0.2)
- `backend-dev`, `frontend-dev`, `mobile-dev` → per-PR preview envs.
- `development` → staging.
- `main` → prod (via tag).

### 17.3 Release gates
- Security scans pass.
- Contract tests pass.
- For any model change: eval, bias, red-team all pass.
- Manual approval required for prod promotion (two-person for model changes).

---

## 18. Testing Strategy

### 18.1 Test pyramid

| Layer | Tooling | Owner |
|---|---|---|
| Unit | **JUnit 5 + Mockito** (Java domain services) / pytest (Python AI workers) / Jest (web) / flutter_test (mobile) | Each service |
| Component | **Spring Boot Test + Testcontainers-Java** (Java services, DB/Kafka/Redis) / pytest + testcontainers (Python workers) | Each service |
| Contract | Pact | Service + Integration |
| End-to-end | Playwright (web), Detox (mobile) | QA |
| Load | k6 against staging | SRE |
| Chaos | Litmus (K8s) | SRE |
| ML eval | MLflow + deepchecks + custom harnesses | Data Science |
| Security | Semgrep (SAST), OWASP ZAP (DAST), pip-audit/npm audit, trivy | Security |
| Accessibility | axe-core in CI | Web team |

### 18.2 Test data
- Synthetic generator for claims, images, policies; deterministic seeds; PII-free.
- Curated benchmark sets:
  - **Damage-eval**: labelled held-out set for FR-3.
  - **Forensics-eval**: known-tampered + known-clean corpus.
  - **Deepfake-eval**: GAN + diffusion mix.
  - **Fraud-eval**: labelled backtest for FR-5.
  - **Prompt-regression**: 100-case set for FR-7 (AC-7.1.1).
  - **Red-team adversarial corpus**: FR-5 / FR-6.

### 18.3 Performance test methodology
- k6 scripts simulate:
  - 200 claim submissions/min sustained; 600/min peak burst for 10 min.
  - Admin queue filter at 500 rpm over 10k claims.
- Budget matches NFR-P-003 / 004 / 005.

### 18.4 Chaos drills
- Quarterly: kill AI GPU node pool mid-claim; verify FR-3 AF-3.3 CPU fallback.
- Kafka AZ loss; verify exactly-once outbox semantics.
- LLM provider outage; verify templated-fallback explanations (FR-7 AF-7.1).
- TSA outage; verify degraded-mode entry (FR-11 AF-11.1 / NFR-A-006).

---

## 19. Non-Functional Design Approach

### 19.1 Performance budget (mirrors FRS §5.1)
- Each service publishes p50/p95/p99 histograms; SRE tracks against budget weekly.
- Cache layers: Redis for hot reads on policy coverage and policyholder pseudonym.
- Query patterns: read models for claim detail; keep writes against normalised tables.

### 19.2 Scalability
- Stateless services with HPA on CPU + RPS; AI workers with KEDA on Kafka lag.
- DB hot-spot mitigation: partitioning; avoid sequential PKs on hot tables (use ULIDs).
- MSK partition count sized to 2× current peak to allow lateral growth.

### 19.3 Availability & DR
- Multi-AZ for all data stores; cross-region async replication to warm standby.
- Critical ingest paths (FNOL, image upload) designed to survive any one AZ loss.

### 19.4 Graceful degradation
- External LLM down → templated explanations, rest of pipeline unaffected.
- OCR down → user prompts manual entry.
- VAHAN/IIB down → fraud sub-scores unavailable, composite renormalised.
- TSA both down > 24h → state transitions blocked; user intake still accepted (claim stays in `AI_PENDING`).

---

## 20. Audit Log Technical Design

### 20.1 Canonical form
- Every entry serialised with JCS (RFC 8785) canonical JSON — `canonical_json(entry_body)`.
- `curr_hash = SHA-256(prev_hash || canonical_json(entry))`, where `prev_hash` is the most recent `curr_hash` in the chain (seeded with zeroes for the genesis entry per environment).

### 20.2 Writer
- Single-writer pattern per partition; Postgres `SERIALIZABLE` transaction writes to `audit_entry` and a companion S3 object (Object Lock Compliance mode) in a two-phase commit via transactional outbox; reconciler verifies both.
- On Postgres or S3 outage: spool to local encrypted disk (bounded 15 min per NFR-A-006); beyond that, service fails-closed.

### 20.3 Nightly anchor
- Cron at 00:15 IST collects the day's entries, computes Merkle root, calls both TSAs in parallel, persists the two RFC-3161 tokens, publishes a `audit.anchor.published_v1` event.
- On TSA failure: retry with backoff; alert after 2h; degraded mode at 24h `[NFR-A-006]`.

### 20.4 Erasure dual path (`[U-09]`)
```
/v1/me/data-rights/erasure
   ├── decide legal_basis via basis_registry
   ├── if retention_required == true  → path REDACT
   │    ├── update user_pii SET fields = pseudonym_tokens
   │    ├── update derived tables preserving FKs
   │    └── emit audit entry: erasure.redacted
   └── if retention_required == false → path DELETE
        ├── physical delete across primary, replicas, nearest backup (retention-window aware)
        ├── rewrite cold-parquet partitions
        └── emit audit entry: erasure.deleted
```

### 20.5 Verification endpoint
- `GET /v1/admin/audit/verify?from&to` recomputes the hash chain over the range and compares the day's anchored Merkle root; discrepancy → `E-AUDIT-002`.

---

## 21. Compliance Controls Mapping

### 21.1 DPDP
- Lawful basis registry keyed on `purpose`; every PII column declares its purpose.
- Consent Service (FR-18) stores artefacts; revocation propagation within 24h.
- Erasure dual-path (§20.4) honours right-to-erasure.
- Grievance → Data Protection Officer (DPO) workflow documented.

### 21.2 IRDAI Cyber-Security Guidelines, 2023 (§NFR-S-012 / M-12)
- Asset inventory (auto-generated from K8s labels).
- CERT-In 6-hour incident reporting runbook.
- Quarterly VAPT; annual empanelled audit.
- Encryption at rest/in transit (per §11).
- Logs retention aligned to regulator expectations.

### 21.3 IRDAI Outsourcing Guidelines (§C-08 / M-13)
- Cloud usage and LLM provider usage declared as material outsourcing.
- DPA with LLM provider mandated; zero-retention contractual clause (NFR-S-013).
- Exit strategy documented (data extraction, destruction).

### 21.4 Future: SOC 2 Type II
- Controls mapped to Trust Services Criteria; tracked in a separate compliance tracker.

---

## 22. Migration & Rollout Strategy

### 22.1 Phase 1 — Hackathon MVP (target April 2026)
- Happy-path motor own-damage only.
- FR-1 (policyholder auth), FR-2 (FNOL + formal), FR-3, FR-4 (simplified catalog), FR-5 Layer 1 + Layer 2 (no GNN), FR-6 (ELA + pHash + C2PA), FR-7 (cloud LLM with DPA), FR-8, FR-9 (adjuster only), FR-10 (minimal admin), FR-11 (chain only; external anchor optional).
- Single-region AWS; no DR; on-prem path deferred.
- Open ADR-013 must be closed before Phase 1 code-cut.

### 22.2 Phase 2 — Enterprise (target +6 months)
- FR-12 Surveyor, FR-13 Cashless, FR-14 Recovery/Salvage, FR-16 Grievance, FR-18 Consent.
- Fraud Layer 3 (GNN) + external enrichments.
- Forensics Q-Tab + deepfake + noise-residual.
- DR posture (warm standby).
- Audit external anchoring mandatory.

### 22.3 Phase 3 — Scale (target +12 months)
- FR-15 TP + MACT legal hand-off.
- FR-17 AML/STR.
- Multi-tenancy (see `AMBIG-23`).
- On-prem deployment option.
- Model migration rehearsed (see `U-13`).

### 22.4 Legacy PAS data migration
- One-shot historical migration via CDC snapshot; ongoing sync via outbox/CDC.
- PII mapped to `user_pii` vault and referenced by pseudonym in analytics.

---

## 23. Operational Runbooks (Outline)

Each runbook lives in `ops/runbooks/` in its own repo; this section names the minimum set required for Phase 1 go-live.

- RB-01 LLM provider outage.
- RB-02 TSA outage (single vs dual).
- RB-03 AI GPU node pool loss.
- RB-04 Kafka AZ loss.
- RB-05 PAS integration incident.
- RB-06 Model rollback (registry pointer swap).
- RB-07 Threshold emergency freeze (promote a "conservative" threshold set).
- RB-08 Audit-spool backpressure recovery.
- RB-09 Mass-incident event (catastrophe) capacity surge.
- RB-10 Incident response aligned to CERT-In 6-hour reporting.

On-call: Platform SRE primary; Data Science on-call for model/ML; AML Officer on-call for sanctions-related incidents.

---

## 24. Open Technical Decisions

| # | Topic | Reference | Owner | Due |
|---|---|---|---|---|
| OTD-01 | Detector license (RT-DETR vs Ultralytics Enterprise vs YOLO-NAS) | ADR-013, U-12, C-09 | Eng Lead | Before Phase 1 code-cut |
| OTD-02 | LLM provider + topology under IRDAI Outsourcing | ADR-012, U-10, M-13, AMBIG-30 | Security + Compliance | Before Phase 1 code-cut |
| OTD-03 | TSA selection | ADR-016, U-18, AMBIG-31 | Compliance + SRE | Before Phase 2 |
| OTD-04 | Consent Manager integration (native DPDP vs internal) | AMBIG-32 | Compliance | Before Phase 2 |
| OTD-05 | Claim-ID opacity (date-embedded vs ULID) | AMBIG-26 | Security | Before Phase 1 schema freeze |
| OTD-06 | Surveyor SLA parameters | AMBIG-27 | Compliance | Before Phase 2 |
| OTD-07 | Cashless variance tolerance (±10% default) | AMBIG-28 | Ops | Before Phase 2 |
| OTD-08 | Auto-decision ceiling subset of Adjuster DOA | AMBIG-29 | Claims | Before Phase 1 code-cut |
| OTD-09 | Single-tenant vs multi-tenant | AMBIG-23 | Product | Before Phase 3 |
| OTD-10 | On-prem option vs cloud-only | §15.4 | Product + Compliance | Before Phase 2 |

---

## 25. Architecture Decision Records

Each ADR uses the format **Context · Decision · Status · Consequences**. The list below is the authoritative index; full ADR bodies live in `docs/adr/` in the implementation repo(s).

### ADR-001: Java 21 + Spring Boot 4.0.5 (domain services) + Python 3.12 (AI/ML workers) — SUPERSEDES prior Python/FastAPI decision
- **Context.** Hackathon team; team skill mix favours the JVM for domain work; AI workload remains heavy but is structurally isolated from domain services via Kafka/Protobuf. The prior "single-language for domain + ML" framing has lost its force now that the bimodal split is accepted as a feature, not a cost.
- **Decision.**
  - **Domain services on Java 21 + Spring Boot 4.0.5** with Jakarta Persistence 3.2 + Hibernate, MapStruct, Jakarta Bean Validation, Resilience4j. Covers: Claims, Intake Orchestrator, Identity & Auth, Policy & PAS Gateway, Surveyor, Cashless, Recovery & Salvage, Third-Party Liability, Grievance, AML, Consent, Audit, Routing Engine, Integration Gateway.
  - **AI/ML workers remain on Python 3.12** (Triton clients, PyTorch/ONNX, DGL, `cv2`, `c2pa-python`, LLM/prompt tooling). Covers: AI Orchestrator, Fraud Engine (L1/L2/L3), Forensics Worker, Explanation Worker.
- **Status.** Accepted (2026-04-24). **Supersedes** the prior "Python 3.12 + FastAPI for all services" decision.
- **Consequences.**
  - Bimodal stack is now intentional; CI, dependency scanning, and on-call run-books double. Mitigated by language-isolated service boundaries and shared contracts (Kafka/Protobuf, OpenAPI/REST).
  - Domain services gain mature transactional data-access, validation, typed HTTP clients, and enterprise ergonomics; AI services retain ML ecosystem fit.
  - Language split is invisible across service boundaries because no in-process code is shared.
  - Fraud Engine stays end-to-end Python to keep L1/L2/L3 co-located with the ML runtime (L1 rules co-locate for deployment simplicity, not ML necessity).
  - Routing Engine moves to Java (pure function, no ML dependency; called in-process by Claims Service or over internal REST).
  - Integration Gateway moves to Java with Spring `RestClient` + Resilience4j (replacing the previously-planned `httpx + Tenacity + pybreaker` stack).
- **Revisit trigger.** If the AI stack gains competitive JVM alternatives (unlikely on the 12-month horizon), or if the Fraud L1 rules evolve into a domain-owned policy engine decoupled from L2/L3, reconsider the placement.

### ADR-013: Switch damage detector from YOLOv8 (AGPL) to RT-DETR (Apache-2.0)
- **Context.** `[U-12, C-09]` — Ultralytics YOLOv8 is AGPL-3.0; commercial use exposes derivative works to copyleft. SaaS deployment is incompatible.
- **Decision.** Adopt RT-DETR (Apache-2.0) as the primary detector; retain YOLOv8 API naming in FRS §8.1 as "YOLOv8-class" for brevity. Procure Ultralytics Enterprise only if RT-DETR's measured accuracy underperforms on the insurer-specific set by > 5 mAP.
- **Status.** Proposed (P0).
- **Consequences.** Slightly different hyper-parameters, export path, Triton config. Licensing risk eliminated.

### ADR-025: Consolidated `ml-services` MVP (single FastAPI process behind `/api/v1/analyze`)
- **Context.** FRS §2.1 paints a per-worker AI DAG (Detection → Forensics ∥ Cost Estimate → Fraud → Explanation → Routing) connected by Kafka. For the hackathon window, building that many workers + the Kafka plumbing is disproportionate to the demo value, and the Forensics / Fraud / Routing layers carry no FRS-mandated AC on the v1 cut (FR-5, FR-6 timelines stretch into phase 2).
- **Decision.** Ship a **single Python FastAPI process** at repo-root `ml-services/` that internally composes YOLOv8 detection, a deterministic parts-catalog cost estimator, and a GPT-4o-class surveyor-assessment generator behind one multipart endpoint: `POST /api/v1/analyze`. The Java Claims Service calls it synchronously via `MlAnalyzeClient` (Resilience4j-protected) inside `POST /api/v1/claims/with-analysis`. The FRS-level behaviour for FR-3, FR-4, and FR-7 is satisfied by this consolidation; only the internal topology differs from §2.1.
- **Status.** Accepted (2026-04-24, v1.2).
- **Consequences.**
  - Net-positive: one process to run locally, one contract to version, smaller surface for the hackathon judges to review.
  - Trade-off: loses the per-stage latency budgets from §5.1 (p95 ≤ X ms per stage) — the consolidated call is a single 5–30 s operation.
  - Trade-off: Fraud (FR-5), Forensics (FR-6), and Routing Engine (FR-8) are not yet wired — claims land in `FNOL_RECEIVED` and stay there until the adjuster transitions them manually.
  - Deprecation note: when the Kafka DAG lands in phase 2, `/claims/with-analysis` becomes a thin shim that publishes `ai.analyze.requested.v1` and the response is served asynchronously via SSE or poll.
- **Revisit trigger.** When any of the following become true: (a) Fraud Engine L1 rules are authored, (b) Forensics (C2PA verify + ELA) becomes a release-blocker for a production-grade AC, (c) ML-service latency p95 pushes past 20 s (users abandon the submit flow).

### ADR-026: Phase-1 evidence storage on local filesystem
- **Context.** FNOL submission uploads 3–10 photos + optional video. Those bytes need to be (a) forwarded to the ML service synchronously, (b) retrievable later via the claim-detail page for adjuster review and (c) preserved for audit. The TSD's long-term target is S3 `media-hot` (ADR-005) with WORM on the `audit` bucket, but S3 requires AWS credentials, bucket provisioning, and KMS — disproportionate to the hackathon loop.
- **Decision.** Phase-1: write bytes to the local filesystem at `{claims.upload-dir}/{evidenceId}`. Content type and size are captured on the `claim_evidence` row (`content_type`, `size_bytes`) — no on-disk metadata. Retrieval via `GET /api/v1/claims/{claimId}/evidence/{evidenceId}` streams the bytes back with the stored `Content-Type`. Path is configurable per profile.
- **Status.** Accepted (2026-04-24, v1.3).
- **Consequences.**
  - Works on a single node; **no multi-replica support** — horizontal scaling requires shared storage (NFS / EFS) or Phase-2 S3 migration.
  - No WORM, no object-level encryption, no lifecycle rules. For demo / hackathon only.
  - `uploads/` is `.gitignore`-d; nothing about the storage leaks into the repo.
- **Revisit trigger.** Before any non-local deployment: replace the `writeEvidenceToDisk(...)` + `loadEvidenceContent(...)` methods with an `EvidenceStore` interface, provide an `S3EvidenceStore` implementation, and point `claims.upload-dir` at `s3://…`. The `GET /evidence/{id}` endpoint contract stays identical.

### ADR-027: `ClaimAnalysis` persistence with denormalised scalars + JSON payload
- **Context.** AI analysis (FR-3 / FR-4 / FR-7) runs once on FNOL submission. Without persistence the adjuster would re-run the ML pipeline every time they open a claim, which costs GPT-4o tokens and 10–30 s of latency each. We also need historical recall for audit.
- **Decision.** Store the full `MlAnalyzeResponse` on a `claim_analysis` row keyed by `claim_id` (one-to-one), with denormalised scalars (`model_version`, `total_detections`, `processing_time_ms`, `severity_verdict`, `repair_recommendation`, `cost_total_paise`, `assessment_confidence`) for filter-able queries + `payload_json` (TEXT) for forensic read-back. Re-runs upsert the same row — no history table yet.
- **Status.** Accepted (2026-04-24, v1.3).
- **Consequences.**
  - `GET /api/v1/claims/{id}/analysis` is a cheap indexed lookup; no ML token costs.
  - Denormalised scalars enable dashboards ("count of severe-verdict claims in the last 7 days") without parsing `payload_json`.
  - No history: a re-run overwrites the prior analysis. For a model-version bump or adjuster-requested re-run, the previous analysis is lost — phase-2 introduces `claim_analysis_history`.
  - Phase-1 uses `TEXT` not `JSONB` — Postgres JSONB + GIN indexing comes when Liquibase owns the DDL.
- **Revisit trigger.** When fraud investigators need to diff analyses over time, or when dashboards need indexed JSONB predicates.

### ADR-016: Dual TSA anchoring
- **Context.** `[U-18, R-15]` — single TSA is a single point of failure; FRS mandates mandatory external anchoring.
- **Decision.** Stamp nightly Merkle root at two independent TSAs (DigiCert + FreeTSA). Degraded mode triggers only if both fail > 24h.
- **Status.** Proposed.
- **Consequences.** Additional commercial TSA cost; minor integration complexity.

*(Other ADRs exist in the index table at §2.3; bodies are out of scope for this document revision.)*

---

## 26. Traceability — FRS v0.2 → TSD v1.0

### 26.1 Functional requirements

| FRS item | Realisation section(s) |
|---|---|
| FR-1 Auth / linking ladder | §5.12, §6.16, §11.1, §11.2 |
| FR-2 FNOL + formal claim | §6.1, §6.2, §8.7, §13 (offline-first, C2PA) |
| FR-3 Damage detection | §6.3, §12.1, ADR-013 |
| FR-4 Cost estimation | §6.3, §7.2 (schema), §10.2 (catalog & ledger) |
| FR-5 Fraud engine (L1/L2/L3) | §6.4, §12.1–§12.2 |
| FR-6 Forensics extended | §6.5, §12.1, §13.3 |
| FR-7 Explanation (model-agnostic) | §6.6, §12.5 |
| FR-8 Routing (non-runtime thresholds) | §6.7, §14.2 |
| FR-9 Human review (KYC, sanctions, penny-drop, reopen) | §6.1, §6.14, §10.2, §14.2 |
| FR-10 Admin dashboard | §14.2, §6.18 |
| FR-11 Audit (mandatory external anchor, erasure dual-path) | §6.8, §20 |
| FR-12 Surveyor workflow | §6.9 |
| FR-13 Cashless settlement | §6.10, §10.2 |
| FR-14 Recovery / Salvage | §6.11 |
| FR-15 TP liability | §6.12 |
| FR-16 Grievance (IGMS) | §6.13, §10.2 |
| FR-17 AML/sanctions/regulatory | §6.14, §10.2 |
| FR-18 Consent & DSR | §6.15, §7.2 (schema), §21.1 |
| FR-19 Multi-channel intake | §6.2, §13, §10.2 (WhatsApp/RCS) |

### 26.2 Non-functional requirements

| FRS item | Realisation |
|---|---|
| NFR-P (per-stage latency budget) | §19.1, §16.2, §6.3 DAG |
| NFR-S-001..011 (Security) | §11, §17 (CI scans) |
| NFR-S-012 IRDAI Cyber-Security 2023 | §11.7, §21.2 |
| NFR-S-013 LLM data handling | §12.1, ADR-012 |
| NFR-S-014 C2PA capture signing | §13.3 |
| NFR-S-015 Secret scanner | §11 (gitleaks), §17 (CI) |
| NFR-A-001..005 Availability | §15.3, §16.2 |
| NFR-A-006 Anchoring-outage degraded mode | §20.3, RB-02 |
| NFR-AC (compliance) | §21 |
| NFR-AC-007 MRM framework | §12.4 |
| NFR-O (Observability) | §16 |
| NFR-O-005 Input drift | §12.6 |
| NFR-SC (Scalability) | §19.2, §5.9 |

### 26.3 Gap-analysis items (M / F / U)

| Item | Realisation |
|---|---|
| M-01 FNOL | §6.2, §9.1 event |
| M-02 Surveyor | §6.9, §14.2, RBAC §11.2 |
| M-03 Cashless | §6.10, §10.2 |
| M-04/05 Recovery/Salvage | §6.11, §7.2 schema |
| M-06 Reopen | §6.1 |
| M-07 Cat-event tag | §7.2 schema |
| M-08 TP | §6.12 |
| M-09 Grievance (IGMS) | §6.13, §10.2 |
| M-11 IRDAI cycle-time | §6.14 |
| M-12 IRDAI Cyber-Security | §11.7, §21.2 |
| M-13 Outsourcing | §21.3, OTD-02 |
| M-14/15 AML/Sanctions/PEP | §6.14, §10.2, §11.5 |
| M-16 TDS | §6.1/§7.2, §10.2 ledger |
| M-17 NCB | §6.1, §8.7 schema example |
| M-18 Consent artefacts | §6.15, §7.2, §21.1 |
| M-19 PAS integration | §6.17, §10.1, §10.2 |
| M-20 VAHAN/SARATHI | §10.2 |
| M-21 IIB | §10.2 |
| M-22 OCR | §6.2, §10.2 |
| M-23 Ledger | §10.2 |
| M-24 Penny-drop | §6.1, §10.2, §11.2 |
| M-25 Reserves | §7.2 |
| M-26 DOA matrix | §6.7, §11.2, §14.2 |
| M-27 Leakage analytics | §12 (ClickHouse), §14.2 |
| M-28/29 MRM & bias | §12.4, §12.6 |
| M-30 Labelling ops | §12.3 |
| M-31 Feedback loop | §12.3 |
| M-32 Multi-channel | §6.2, §13, §10.2 |
| F-01 Three-layer fraud | §6.4 |
| F-02 Champion-challenger | §12.4 |
| F-03 Conformal abstain | §6.4 |
| F-04 Ring cascade | §6.4, §9.1 `fraud.confirmed` |
| F-05 GNN | §6.4, §12.1 |
| F-06 External enrichment | §10.2 |
| F-07 Behavioural biometrics | §13.1, §6.15 (consent gate) |
| F-08 Device intelligence | §13.5, §10.2 |
| F-09 NLI | §6.4, §12.1 |
| F-10 Garage scoring | §6.4, §6.10 |
| F-11 C2PA | §6.5, §13.3, §10.2 |
| F-12 Q-Tab | §6.5 |
| F-13 PRNU | §6.5, §5.9 |
| F-14 Deepfake (independent lifecycle) | §6.5, §12.4 |
| F-15 Noise residual | §6.5 |
| F-16 Active learning | §12.3 |
| F-17 Synthetic augmentation | §12.3 |
| F-18 Rolling recalibration | §12.4 |
| F-19 Input drift | §12.6, §16.1 |
| F-20 Decomposed reasons | §6.4, §14.2 |
| F-21 Subgroup bias | §12.6, §14.2 |
| F-22 Red-team | §12.4 |
| U-01 Phased detection targets | §12.1 (acceptance), §22.1 |
| U-02 Distributional cost accuracy | §12 (eval pipelines) |
| U-03 Semantic-equivalence regression | §12.5 |
| U-04 Latency budget (45s v1 / 30s v2) | §19.1, §16.2 |
| U-05 Dataset workstream | §12.3 |
| U-06 Multi-source parts catalog | §6.1, §7.2 `catalog_source` |
| U-07 Multi-signal linking | §6.16 |
| U-08 Claim-time KYC re-check | §6.1, §10.2 |
| U-09 Erasure dual-path | §20.4 |
| U-10 LLM zero-retention DPA | §12.1, ADR-012 |
| U-11 Non-runtime thresholds | §6.7, §14.2, §11.2 |
| U-12 Detector licensing | ADR-013 |
| U-13 Model-agnostic abstraction | §6.6, §12.1 |
| U-14 Tiered photo minimums | §13.1 UI + §6.2 validation |
| U-15 Multi-channel intake | §6.2, §13 |
| U-16 Realistic override alert | §14.2, §12.6 |
| U-17 Offline-first | §13.2 |
| U-18 Mandatory external anchor | §20.3, ADR-016 |

---

*— Document end —*
