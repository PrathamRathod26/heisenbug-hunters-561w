# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

Hackathon submission for **Amnex Infotechnologies — Sarjan (April 2026)**, by team **Heisenbug Hunters**.

## Current State

No source code, build config, dependency manifest, or tests exist yet. What is tracked:

- `README.md` — one-liner.
- `docs/UC - Hackathon.pdf` — original hackathon brief.
- `docs/FRS.md` (v0.2) — Functional Requirements Spec. Authoritative on **behavior**.
- `docs/FRS-Gap-Analysis.md` — review commentary: 32 missing requirements (`M-nn`), 22 fraud-logic improvements (`F-nn`), 18 unrealistic assumptions (`U-nn`). v0.2 of the FRS absorbs accepted items; the `M-/F-/U-` tags in the FRS trace back here.
- `docs/TSD.md` (v1.0) — Technical Spec. Authoritative on **realisation**. Contains the ADR index (§5, §25) and FRS→TSD traceability (§26).
- `.idea/` — IntelliJ scaffold only; the `JAVA_MODULE` label is the IDE default and does not reflect the chosen stack (see ADR-001 below).

Picking up work here still means **establishing structure** (build tooling, source layout, test harness) rather than navigating existing code. Update this file with build/test/run commands and a per-service layout as soon as those exist — future Claude instances will rely on it.

## Target Architecture (per TSD v1.1)

The product is an **AI-powered motor-insurance claims platform** for the Indian market (IRDAI-regulated). Read the FRS and TSD before making non-trivial decisions; key ADRs that will shape the repo layout:

- **Backend is explicitly bimodal** (TSD v1.1 §5.1, ADR-001 revised 2026-04-24):
  - **Domain services — Java 21 + Spring Boot 4.0.5.** Jakarta Persistence 3.2 + Hibernate, PostgreSQL, MapStruct, Jakarta Bean Validation on DTOs, Resilience4j for remote-call resilience. Covers: Claims, Intake Orchestrator, Identity & Auth, Policy & PAS Gateway, Surveyor, Cashless, Recovery & Salvage, TP, Grievance, AML, Consent, Audit, **Routing Engine**, **Integration Gateway**. Reference scaffold lives at `backend/`.
  - **AI/ML workers — Python 3.12.** Covers: AI Orchestrator, Fraud Engine (L1 rules + L2 ML + L3 GNN), Forensics Worker, Explanation Worker. Kept on Python because Triton clients, PyTorch/ONNX, DGL, `cv2`, `c2pa-python`, and LLM tooling are Python-native with no Java equivalent.
  - **Contracts across the language boundary are Kafka (Protobuf) or OpenAPI/REST** — never shared in-process code. Stateless services; Kafka is the integration backbone.
- **Web:** React 18 + TypeScript + Vite, TanStack Query, Zustand, shadcn/ui + Tailwind. Single SPA serves Adjuster / Admin / Surveyor / Grievance / AML / Finance roles — gate by role, not by bundle.
- **Mobile:** Flutter 3.x with Isar (offline), tus_client (resumable upload), platform channels for camera + C2PA signing. Offline FNOL queue with 72h TTL and dedupe hash (`[U-17]`).
- **Data:** Postgres 16 (monthly-partitioned `claims`, `events`, `audit_entries`), S3 in `ap-south-1` (hot/cold/WORM audit bucket with Object Lock Compliance mode), Redis 7, ClickHouse for analytics/offline features, MLflow registry.
- **AI serving:** Triton (vision, ONNX/TensorRT) + BentoML (tabular LightGBM). LLM behind a `MultimodalExplainer` abstraction — Azure OpenAI (India, zero-retention DPA) primary, self-hosted vLLM fallback. **Damage detector: RT-DETR (Apache-2.0), not YOLOv8** — ADR-013 switches away from Ultralytics AGPL; do not reintroduce YOLOv8 without revisiting `[U-12]`.
- **Platform:** EKS (Kubernetes 1.30) with `system` / `apps` / `ai-cpu` / `ai-gpu` node pools; KEDA for Kafka-lag-driven AI workers; Istio service mesh (mTLS); Kong API gateway; OPA sidecar for authz; Vault + External Secrets Operator.
- **Observability & release:** OpenTelemetry → Prometheus/Loki/Tempo/Grafana. Terraform + Helm + Argo CD (GitOps). GitHub Actions for CI. Event serialisation: Protobuf internal, JSON for external webhooks.

## Non-Negotiable Behaviors

These are load-bearing rules from the FRS/TSD — violate them and the design breaks:

- **Fail-closed on audit (`[U-18]`).** If the audit-spool SLA degrades, state transitions stop. Every state change writes an append-only hash-chained entry; nightly Merkle root is anchored by **two** RFC 3161 TSAs (DigiCert + FreeTSA).
- **Change-controlled risk surfaces (`[U-11]`).** Thresholds, prompts, model artefacts, DOA matrix, and fraud rules are versioned, promoted via CI gates, and **immutable once live** — never runtime-mutable. The Routing Engine pins a threshold snapshot at decision time.
- **Separation of duties (`[M-26, U-11]`).** Approvals above DOA, payouts to new bank accounts, and threshold/model changes require two-principal approval by construction.
- **Data residency & minimisation (`[NFR-AC-005, M-13, U-10]`).** PII stays in India-region stores (or insurer on-prem) and is minimised before any egress. The LLM provider choice is an outsourcing-notifiable decision.
- **FNOL is legally distinct from formal claim submission (`[M-01]`).** They start different statutory clocks — do not collapse them into one endpoint or one aggregate state.
- **Asynchronous AI, synchronous decisions.** The AI DAG (Detection → Forensics ∥ Cost Estimate → Fraud L1+L2+L3 → Explanation → Routing) runs over Kafka topics as edges. The routing decision is synchronous against whatever AI state exists at the moment routing fires.
- **AI is evidence, adjuster is the authority.** Every AI output carries model version, prompt version, input hash, output hash, and decomposed reasons. Never design a path where an AI score auto-approves without a defensible override trail.

## Traceability

FRS items use stable IDs that thread through both design docs:

- `FR-n`, `FR-n.m`, `AC-n.m.k`, `NFR-*` — FRS-native requirement IDs.
- `[M-nn]` — Missing-requirement from the gap analysis (lifecycle/regulatory/integration/financial/AI-gov/CX/security).
- `[F-nn]` — Fraud-logic improvement.
- `[U-nn]` — Unrealistic-assumption correction.
- `[AMBIG-n]` — Open ambiguity from the input brief (the hackathon `INPUT USE CASE` was empty; the domain was inferred).

When writing code, commits, or PRs, reference the relevant IDs so the audit trail from brief → spec → design → implementation stays intact. TSD §26 is the forward traceability matrix.

## Branching

Work is split across per-track branches that feed `development`, which in turn merges into `main`:

- `backend-dev` — backend work
- `frontend-dev` — web frontend work
- `mobile-dev` — mobile work
- `development` — integration branch for the three tracks above
- `main` — PR target; release-quality integration

When starting a task, check out the track branch that matches the work. Cross-track changes (shared contracts, API schemas, etc.) should be coordinated on `development` rather than one track branch.
