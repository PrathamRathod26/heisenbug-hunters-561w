# Functional Requirements Specification (FRS)

| Field | Value |
|---|---|
| Project | AI-Powered Insurance Claims Processing Platform |
| Team | Heisenbug Hunters |
| Hackathon | Amnex Infotechnologies — Sarjan (April 2026) |
| Document Version | **0.2.2 (Draft)** — amends v0.2.1 with evidence-storage + analysis-persistence behaviour |
| Date | 2026-04-24 |
| Status | **DRAFT — absorbs accepted items from `FRS-Gap-Analysis.md` v1.0 (2026-04-24). v0.2.1 added §3.3 web-portal scope + FR-1 persona gate. v0.2.2 adds FR-2 AC-2.1.12 (evidence storage + retrieval contract) and AC-2.1.13 (analysis persistence + read-back). M-/F-/U- IDs preserved for traceability.** |
| Change Summary | See §15 Changelog. See §16 Traceability Matrix for where each M-/F-/U- landed. Deltas for each minor version are noted inline with `[NEW in v0.2.x]`. |

> **Input-gap notice (unchanged from v0.1).** The source prompt's `INPUT USE CASE:` field was empty. The use case was inferred from the requested output structure (YOLOv8, GPT-4o Vision, fraud detection, image forensics, Policyholder/Adjuster roles) as an **AI-powered motor-insurance claims platform, India, IRDAI-regulated**. Inferred decisions are tagged `[AMBIG-n]` and collected in §14. This document must be validated against the actual business brief before engineering commits.

---

## 1. Introduction

### 1.1 Purpose
Defines the functional and non-functional behavior of the AI-powered insurance claims platform ("the System"). The System enables policyholders to file claims from a mobile device using photographs, supports multi-channel intake for non-digital customers, leverages computer vision and a multimodal LLM to triage damage and estimate repair cost, flags likely fraud with a three-layer rules + ML + graph engine, and routes low-confidence or suspicious claims to human adjusters and IRDAI-licensed surveyors. This is the authoritative source for engineering, QA, security, and operations, traceable from top-level functional requirements (`FR-n`) through sub-items (`FR-n.m`) to acceptance criteria (`AC-n.m.k`). Every item added in v0.2 carries its originating gap-analysis ID (`[M-nn]`, `[F-nn]`, or `[U-nn]`) for audit.

### 1.2 Scope

**In scope**
- **Multi-channel intake** — mobile app (primary), assisted agent intake, IVR, WhatsApp/RCS. `[M-32, U-15]`
- **FNOL (First Notice of Loss) capture distinct from formal claim submission.** `[M-01]`
- **Claim lifecycle** end-to-end: FNOL → formal claim → AI triage → human review → surveyor (when mandated) → cashless or reimbursement settlement → closure → optional re-open. `[M-01, M-02, M-03, M-06]`
- **Third-party liability (TP) and own-damage (OD) tracks** with distinct workflows. `[M-08]`
- **Subrogation, recovery, and salvage management** for claims where the insurer recovers value post-settlement. `[M-04, M-05]`
- **Grievance redressal** aligned with IRDAI IGMS. `[M-09]`
- **AML / sanctions screening and regulatory reporting** (STR to FIU-IND, IRDAI cycle-time disclosures). `[M-14, M-15, M-11]`
- **Consent management and data-principal rights** under DPDP. `[M-18]`
- **Enterprise integrations** — Policy Administration System (PAS), VAHAN/SARATHI, Insurance Information Bureau of India (IIB), OCR for statutory documents, financial ledger, penny-drop bank verification, Consent Manager. `[M-19, M-20, M-21, M-22, M-23, M-24]`
- **AI/ML services** — YOLOv8 damage detection, cost estimation with salvage/coinsurance/TDS handling, three-layer fraud engine (rules + ML + prioritisation), extended image forensics with C2PA capture-provenance and deepfake detection, GPT-4o-class multimodal explanation generation with model-agnostic abstraction. `[F-01 through F-22, U-12, U-13]`
- **Adjuster/Admin web dashboard** with decomposed fraud reasons, claim-history reopening, DOA-gated approvals. `[F-20, M-06, M-26]`
- **Tamper-evident audit** with mandatory external anchoring and true-erasure vs redaction distinction. `[U-09, U-18]`

**Out of scope** (unless `[AMBIG-01]` resolves otherwise)
- Policy sales / underwriting, premium collection, reinsurance treaty accounting.
- Direct dispatch of parts orders from repair shops (the System *recommends and authorises*; execution is the shop's).
- Payment execution rails (UPI/NEFT/RTGS). The System produces payment instructions to the ledger and monitors settlement events; the actual rail is owned by Finance/Treasury.
- Lines of business other than motor/auto (`[AMBIG-02]`).

### 1.3 Definitions, Acronyms, and Abbreviations

| Term | Expansion / Definition |
|---|---|
| **AI** | Artificial Intelligence. |
| **API** | Application Programming Interface. |
| **AML** | Anti-Money-Laundering. |
| **C2PA** | Coalition for Content Provenance and Authenticity — standard for signing media at capture time. `[F-11]` |
| **CIBIL / IIB** | Credit-bureau / Insurance Information Bureau of India (cross-insurer claims-history registry). `[M-21]` |
| **DOA** | Delegation of Authority — approval-limit matrix by role, LOB, geography. `[M-26]` |
| **DPDP** | Digital Personal Data Protection Act, 2023 (India). |
| **ELA** | Error-Level Analysis. |
| **EXIF** | Exchangeable Image File format — metadata embedded in photographs. |
| **FNOL** | First Notice of Loss — first statutory notification that an insured event has occurred. `[M-01]` |
| **FIU-IND** | Financial Intelligence Unit – India. |
| **GNN** | Graph Neural Network. `[F-05]` |
| **GPT-4o-class** | OpenAI's GPT-4o (or equivalent capability-level multimodal LLM); provider- and version-agnostic per abstraction in §8.2. `[U-13]` |
| **IBNR** | Incurred But Not Reported — claims liability reserve for events occurred but not yet notified. `[M-25]` |
| **IDV** | Insured Declared Value (motor). |
| **IGMS** | Integrated Grievance Management System (IRDAI). `[M-09]` |
| **IRDAI** | Insurance Regulatory and Development Authority of India. |
| **JWT** | JSON Web Token. |
| **KYC** | Know Your Customer. |
| **LLM** | Large Language Model. |
| **mAP** | mean Average Precision — object-detection accuracy metric. |
| **MACT** | Motor Accident Claims Tribunal. `[M-08]` |
| **ML / MRM** | Machine Learning / Model Risk Management. `[M-28]` |
| **mTLS** | Mutual TLS. |
| **NCB** | No Claim Bonus. `[M-17]` |
| **NLI** | Natural Language Inference (entailment/contradiction model). `[F-09]` |
| **OTP / OWASP** | One-Time Password / Open Worldwide Application Security Project. |
| **PAS** | Policy Administration System (core insurance system). `[M-19]` |
| **PII** | Personally Identifiable Information. |
| **PRNU** | Photo-Response Non-Uniformity — sensor-level image fingerprint. `[F-13]` |
| **PSI** | Population Stability Index — distribution-drift metric. |
| **PEP** | Politically Exposed Person. `[M-15]` |
| **RBAC** | Role-Based Access Control. |
| **RPO / RTO** | Recovery Point / Time Objective. |
| **SLA / SLO** | Service Level Agreement / Objective. |
| **SSIM / SHAP** | Structural Similarity / SHapley Additive exPlanations. |
| **STR** | Suspicious Transaction Report (to FIU-IND). `[M-14]` |
| **TDS** | Tax Deducted at Source. `[M-16]` |
| **TP / OD** | Third-Party / Own-Damage (motor-claim tracks). `[M-08]` |
| **VAHAN / SARATHI** | Indian national RTO registries (vehicle / licence). `[M-20]` |
| **VIN** | Vehicle Identification Number. |
| **WCAG** | Web Content Accessibility Guidelines. |
| **WORM** | Write-Once, Read-Many storage. |
| **YOLOv8** | "You Only Look Once, v8" — single-stage object detector by Ultralytics. |

---

## 2. System Overview

### 2.1 High-Level Architecture

```
┌──────────────────────────────────────────────┐    ┌──────────────────────┐
│      Multi-Channel Intake  [M-32, U-15]      │    │ Adjuster / Admin /   │
│  ┌────────┐ ┌────────┐ ┌─────┐ ┌───────────┐ │    │ Surveyor / Grievance │
│  │ Mobile │ │ Agent- │ │ IVR │ │ WhatsApp  │ │    │ Web Dashboard (SPA)  │
│  │ App    │ │ assist │ │     │ │  / RCS    │ │    └──────────┬───────────┘
│  └────────┘ └────────┘ └─────┘ └───────────┘ │               │
└────────────────────┬─────────────────────────┘               │
         HTTPS (JWT + device attest)                   HTTPS (JWT + SSO)
                     ▼                                         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                             API Gateway                                  │
│               (WAF, rate-limit, OAuth2 introspection)                    │
└────┬───────────────┬──────────────────┬───────────────────┬──────────────┘
     ▼               ▼                  ▼                   ▼
┌──────────┐  ┌─────────────┐   ┌──────────────┐   ┌──────────────────┐
│ Claims   │  │ Identity,   │   │ Admin &      │   │ Surveyor / TP /  │
│ Service  │  │ Policy,     │   │ Reporting    │   │ Grievance /      │
│ (FNOL +  │  │ Consent Svc │   │              │   │ AML Services     │
│  Formal) │  └──────┬──────┘   └──────┬───────┘   └───────┬──────────┘
└────┬─────┘         │                 │                   │
     │               │                 │                   │
     └──────────┬────┴────────────┬────┴──────────────┬────┘
                ▼                 ▼                   ▼
┌──────────────────────────────────────────────────────────────┐
│                  Message Bus (Kafka / NATS)                  │
└─┬────────┬──────────┬───────────┬──────────┬────────┬────────┘
  ▼        ▼          ▼           ▼          ▼        ▼
┌────┐ ┌──────┐ ┌───────────┐ ┌────────┐ ┌──────┐ ┌──────────┐
│YOLO│ │Fraud │ │ Forensics │ │GPT-4o- │ │Cost  │ │Consent / │
│ v8 │ │3-Lyr │ │ (ELA/QTab/│ │class   │ │Estim.│ │DSR / AML │
│Det.│ │Engine│ │  PRNU/DF/ │ │LLM Svc │ │(Sal- │ │workers   │
│[U-1│ │[F-01 │ │  C2PA/NR) │ │[U-03,  │ │vage, │ │[M-14/15/ │
│ 01]│ │..F-22│ │  [F-11..] │ │ U-13]  │ │ TDS) │ │ 18]      │
└──┬─┘ └───┬──┘ └──────┬────┘ └───┬────┘ └──┬───┘ └────┬─────┘
   │       │           │          │         │          │
   └───────┴───────┬───┴──────────┴─────────┴──────────┘
                   ▼
           ┌─────────────────────┐
           │ Confidence &        │
           │ Routing Engine      │
           │ (change-controlled  │
           │  thresholds, U-11)  │
           └──────────┬──────────┘
                      ▼
 AUTO_APPROVE│HUMAN_REVIEW│FRAUD_INVEST.│NEEDS_MORE_INFO│AUTO_REJECT│SURVEYOR_REQUIRED
                      │                                                  │
                      ▼                                                  ▼
            ┌───────────────────┐                         ┌──────────────────────┐
            │ Adjuster / TP /   │                         │ IRDAI-licensed       │
            │ Grievance queues  │                         │ Surveyor workflow    │
            │ [F-20 reasons]    │                         │ [M-02]               │
            └──────────┬────────┘                         └──────────┬───────────┘
                       ▼                                              ▼
     ┌──────────────────────────────┐              ┌─────────────────────────────┐
     │ Subrogation, Recovery,       │              │ Cashless / Reimbursement    │
     │ Salvage workers [M-04, M-05] │              │ Settlement + Penny-Drop     │
     └──────────────────────────────┘              │ [M-03, M-24]                │
                                                   └─────────────────────────────┘
                       │                                              │
                       └─────────────────────┬────────────────────────┘
                                             ▼
           ┌──────────────────┐     ┌────────────────────┐     ┌────────────────┐
           │ PostgreSQL       │     │ Object Storage     │     │ Feature Store  │
           │ (transactional,  │     │ (S3/MinIO,         │     │ (Redis +       │
           │  partitioned)    │     │  WORM for audit)   │     │  offline OLAP) │
           └──────────────────┘     └────────────────────┘     └────────────────┘
                                             │
                                             ▼
                              ┌──────────────────────────┐
                              │ Tamper-evident Audit     │
                              │ (hash-chained + MANDATORY│
                              │  external anchor, U-18)  │
                              └──────────────────────────┘
```

### 2.2 Stakeholders

| Stakeholder | Interest |
|---|---|
| Policyholder | File claims quickly across any channel; transparent status; fair payout. |
| Claims Adjuster / Sr. Adjuster | Triage, defensible override trail, fraud signals, DOA-bounded authority. |
| **IRDAI-licensed Surveyor** `[M-02]` | Independent assessment for claims above regulator-notified thresholds. |
| **Grievance Officer** `[M-09]` | IGMS-aligned complaint handling, SLA compliance. |
| **Finance / Reserving Officer** `[M-25]` | Case reserve, IBNR, payment leakage management. |
| **AML Officer / Compliance** `[M-14, M-15]` | STR filing, sanctions screening, regulator disclosures. |
| Claims Manager | Throughput, SLA adherence, escalations. |
| Compliance Officer | DPDP, IRDAI, audit trail, model explainability. |
| Fraud Investigator | Prioritised suspicious-claim queue; evidence chain; ring analytics. |
| Data Scientist / MRM | Model performance, bias, drift, MRM framework. `[M-28, M-29]` |
| Platform SRE | Availability, cost, observability. |
| Executive Sponsor | Cycle time, cost per claim, fraud leakage. |
| External Regulator | IRDAI (assumed, `[AMBIG-03]`); FIU-IND for STRs. |

### 2.3 External Integrations `[NEW in v0.2 — absorbs M-19, M-20, M-21, M-22, M-23, M-24]`

All outbound integrations are behind a dedicated **Integration Gateway** that provides: retries with exponential backoff, circuit breakers, per-integration rate limits, SSRF protection, outbound mTLS, a per-integration credential scope, and contract tests run daily. Failures produce `INTEG-n` error codes and do not block core claim intake; they degrade gracefully.

| Integration | Purpose | FR touched | Source |
|---|---|---|---|
| **Policy Administration System (PAS)** | Bi-directional sync of policy, endorsement, coverage, premium status; reserve write-back. | FR-1, FR-4, FR-9 | `[M-19]` |
| **VAHAN / SARATHI (RTO)** | Validate vehicle registration, ownership, fitness certificate, driving licence status; stolen-vehicle flag. | FR-2, FR-5 | `[M-20]` |
| **Insurance Information Bureau of India (IIB)** | Cross-insurer claim history for the policyholder and vehicle. | FR-5 | `[M-21]` |
| **OCR service (RC book, DL, FIR, invoices)** | Extract structured data from statutory documents; GSTN match on bills. | FR-2 | `[M-22]` |
| **Financial Ledger (SAP / Oracle / Tally)** | Post payable on approval; reserve movement; TDS/GST journal entries. | FR-4, FR-9 | `[M-16, M-23, M-25]` |
| **Bank Account Verification (NPCI penny-drop)** | Verify payee account before first payout to a new account. | FR-9 | `[M-24]` |
| **Consent Manager (DPDP)** | Record and retrieve policyholder consent artefacts. | FR-18 | `[M-18]` |
| **Weather / Event Data** | Validate "natural event" claims against actual storm/flood data by region and date. | FR-5 | `[F-06]` |
| **Sanctions / PEP provider** | Screen claimant and payee against UN/OFAC/MEA lists. | FR-17 | `[M-15]` |
| **Device-intelligence / fingerprint provider** | Root/jailbreak/emulator/VPN signals, device reputation. | FR-5 | `[F-08]` |
| **Content Authenticity (C2PA) verification service** | Verify capture-time signatures on media. | FR-2, FR-6 | `[F-11]` |
| **IRDAI IGMS** | Grievance submission/escalation. | FR-16 | `[M-09]` |

---

## 3. User Roles & Permissions (RBAC)

### 3.1 Roles (v0.2 adds four new internal roles)
- **Policyholder**
- **Adjuster** / **Sr. Adjuster**
- **Fraud Investigator**
- **Surveyor (IRDAI-licensed)** — external licensed principal; scoped access to assigned claims only. `[M-02]`
- **Grievance Officer** `[M-09]`
- **AML / Compliance Officer** `[M-14, M-15]`
- **Finance Approver** — gates payout above DOA ceiling and handles ledger exceptions. `[M-23, M-26]`
- **Admin** — configuration (but NOT runtime-mutable risk thresholds; see FR-10). `[U-11]`
- **Auditor** — read-only.
- **System** — non-human principal.

### 3.2 Permissions Matrix

Legend: ✅ allowed · ❌ denied · 👁️ read-only · 🔒 assignment-restricted · 🗝️ requires second-approver (four-eye).

| Capability | Policyholder | Adjuster | Sr. Adjuster | Fraud Inv. | Surveyor | Grievance | AML | Finance | Admin | Auditor | System |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Submit / update own claim | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| View own claim | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| View any claim | ❌ | 🔒 | ✅ | ✅ | 🔒 | 🔒 | ✅ | 👁️ | ✅ | 👁️ | ✅ |
| Request more info from policyholder | ❌ | 🔒 | ✅ | ✅ | 🔒 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Override AI decision | ❌ | 🔒 | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Approve claim ≤ Adjuster DOA `[M-26]` | ❌ | 🔒 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Approve claim Adjuster DOA < x ≤ Sr DOA | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Approve claim > Sr DOA (four-eye) | ❌ | ❌ | 🗝️ | ❌ | ❌ | ❌ | ❌ | 🗝️ | ❌ | ❌ | ❌ |
| Submit surveyor report | ❌ | ❌ | ❌ | ❌ | 🔒 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| File STR to FIU-IND | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Initiate grievance case | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Resolve grievance | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Reopen a closed claim `[M-06]` | ❌ | 🔒 | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Authorise payout to new bank account | ❌ | ❌ | 🗝️ | ❌ | ❌ | ❌ | ❌ | 🗝️ | ❌ | ❌ | ✅ |
| Access raw forensic artifacts | ❌ | ❌ | 👁️ | ✅ | 👁️ | ❌ | 👁️ | ❌ | 👁️ | 👁️ | ✅ |
| **Change confidence thresholds via UI `[U-11]`** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Propose threshold change via change-request `[U-11]` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Approve threshold change (gated) `[U-11]` | ❌ | ❌ | 🗝️ | ❌ | ❌ | ❌ | 🗝️ | ❌ | ❌ | ❌ | ❌ |
| Manage user roles / DOA matrix | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| Execute right-to-erasure (true delete) `[U-09]` | request | request | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 🗝️ | ❌ | execute |
| Invoke AI pipelines | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

**Negative test (unchanged):** Any permission attempt without the corresponding right MUST return HTTP 403 with `E-AUTHZ-001` and be recorded in audit.

### 3.3 Web Portal Access — Phase 1 Scope `[NEW in v0.2.1]`

The **web portal** (Angular SPA in `frontend/`) is the interface for three operational personas. Other roles interact with the platform via different channels (mobile app, IGMS portal, AML console, surveyor offline app, audit UI — see §1.2 and §2.3). Phase 1 restricts web-portal sign-in to the three personas below; the permission matrix in §3.2 remains authoritative at the **API layer** across all channels, and the portal gate is an *additional, channel-specific* filter, not a replacement.

| Persona | Mapped RBAC roles (§3.1) | Web-portal responsibilities |
|---|---|---|
| **Policyholder** | `POLICYHOLDER` | Submits FNOL + formal claims; views own claim status; initiates grievance. |
| **Claims Adjuster** | `ADJUSTER`, `SR_ADJUSTER` | Reviews AI-generated claim assessment (FR-3..FR-8), approves or escalates claims within DOA (§3.2, FR-10), updates claim status. |
| **Insurance Administrator** | `ADMIN` | Configures policy and platform rules (role ↔ permission matrix, DOA by role × LOB × geo per FR-10), reviews audit logs, manages payout limits. |

**Out-of-portal roles in phase 1** (enforced by AF-1.6): `FRAUD_INVESTIGATOR`, `SURVEYOR`, `GRIEVANCE_OFFICER`, `AML_OFFICER`, `FINANCE_APPROVER`, `AUDITOR`, `SYSTEM`. These principals remain valid API consumers and continue to receive RBAC rights per §3.2; only the web-portal channel rejects them in phase 1, with the denial audited per AC-1.1.7.

**Persona precedence (multi-role users).** Where a user holds multiple portal-eligible roles, the portal computes an effective persona by precedence `ADMIN > ADJUSTER (Sr / regular) > POLICYHOLDER`. The underlying role set is unchanged; the precedence rule only governs landing experience and navigation defaults.

**Per-persona UI affordances** `[NEW in v0.2.2]`. The portal hides controls a persona can't exercise under §3.2, so users aren't shown actions they'd be refused at the API layer:

| Control | Policyholder | Claims Adjuster | Administrator |
|---|---|---|---|
| Claim detail — "Change status" dropdown + Apply | **Hidden** (lock-icon notice shown) | Visible | Visible |
| Claim list — "New claim (FNOL)" CTA | Visible | Hidden | Hidden |
| Side-nav — Users / Roles / Permissions / Payout limits | Hidden | Hidden | Visible |

These affordance rules are UI presentation only; they do not substitute for the API-layer `E-AUTHZ-001` enforcement mandated by §3.2. An API caller attempting a denied action still returns 403 regardless of UI visibility.

---

## 4. Functional Requirements

Schema unchanged: **Description · Preconditions · Postconditions · Main Flow · Alternate Flow(s) · Error Handling · Validation Rules · Acceptance Criteria**.

---

### FR-1 — User Registration & Authentication `[revised: U-07]`

**Description.** Policyholders self-register; authenticate via OTP. Internal users authenticate via enterprise SSO (OIDC/PKCE). **Surveyors authenticate via a licensed-principal OIDC provider issued by the surveyor empanelment service** (not standard corporate SSO) `[M-02]`. Sessions issue short-lived JWT access tokens and a rotating refresh token.

**Policyholder identity linking — now multi-signal.** `[U-07]`
1. **Hard match** — mobile + policy number both supplied and agree on PAS → `status=linked`.
2. **Soft match** — mobile alone matches; DOB confirmed as 2FA during registration → `status=linked` after one-shot verification.
3. **Unresolved** — no match within 3 attempts → `status=unlinked`; claim submission disabled (FR-2 blocks); user routed to Grievance Officer for manual linking (FR-16).

**Preconditions.**
- Network connectivity.
- For internal users: identity exists in the corporate IdP. For surveyors: active licence on the surveyor directory.

**Postconditions.**
- Session with JWT (≤15 min TTL) and refresh token (≤30 days).
- User record persists; policyholders linked (hard/soft) to ≥1 policy, or flagged unresolved.
- Login event written to audit log.

**Main Flow — Policyholder (unchanged apart from linking ladder above).** Steps 1–6 as v0.1.

**Main Flow — Internal user / Surveyor.** Steps 1–4 as v0.1 with IdP selection by role; Surveyor tokens additionally carry a `licence_id` claim, validated against a daily-refreshed directory.

**Alternate Flows.**
- AF-1.1 / AF-1.2 / AF-1.3 — as v0.1.
- **AF-1.4** `[U-07]` — Soft-match DOB confirmation failure 3×: account remains `unlinked`, Grievance case opened.
- **AF-1.5** `[M-02]` — Surveyor licence expired during session: token refresh rejected; session revoked; audited.
- **AF-1.6** `[NEW in v0.2.1 — §3.3]` — Principal authenticates to the IdP successfully but carries no active role in `{POLICYHOLDER, ADJUSTER, SR_ADJUSTER, ADMIN}` on the **web** channel: portal denies with `E-AUTHZ-PORTAL-001`, login-denied event is audited with `channel=web` + `principal_id` + `active_roles`, principal is shown "role not authorised for this channel; use your designated portal" with a contact link. Other channels (mobile, IGMS, AML console, surveyor app) remain unaffected for the same principal.

**Error Handling.** v0.1 codes retained; new:
| Code | Condition | HTTP |
|---|---|---|
| `E-AUTH-008` | Soft-match DOB failed | 401 |
| `E-AUTH-009` | Surveyor licence expired/invalid | 401 |
| `E-AUTHZ-PORTAL-001` | Principal authenticated but has no portal-eligible role for the current channel | 403 |

**Validation Rules.** v0.1 retained. Additions:
- Soft-match ladder enforced exactly as specified; `status=linked` never set without documented match type.
- Surveyor principal must carry a verifiable `licence_id` claim on every authenticated call.
- **Portal channel gate** `[§3.3]` — the web portal MUST reject any session whose active role set does not intersect `{POLICYHOLDER, ADJUSTER, SR_ADJUSTER, ADMIN}`. The gate runs after IdP authentication succeeds and before any protected route resolves.

**Acceptance Criteria.** AC-1.1.1…1.1.4 retained.
- **AC-1.1.5** `[U-07]` — In a test population with 5% policy-number data-entry errors, soft-match correctly links ≥ 90% of genuine policyholders; zero false links in an adversarial test of 100 mismatched identities.
- **AC-1.1.6** `[M-02]` — A surveyor whose licence is revoked during an active session loses access within one token refresh cycle (≤ 15 min).
- **AC-1.1.7** `[§3.3]` — A user whose only active role is `AUDITOR` (or any other out-of-portal role) authenticates to the IdP but is denied by the web portal with HTTP 403 `E-AUTHZ-PORTAL-001`; an audit entry is written with `channel=web`, `principal_id`, and the full `active_roles` list; the same principal's API access via their designated channel (e.g., audit UI) is unaffected.
- **AC-1.1.8** `[§3.3]` — A user with roles `{ADMIN, AUDITOR}` lands on the Administrator persona (per §3.3 precedence). A user with `{SR_ADJUSTER, POLICYHOLDER}` lands on Claims Adjuster.

---

### FR-2 — Claim Intake `[substantially revised: M-01, M-08, M-17, M-22, U-14, U-17, F-11]`

**Description.** Claim intake is now a **two-stage flow**: FNOL (fast, statutory notification — seconds to a minute) followed by a formal claim (full evidence, narrative, documents). Capture is **offline-first**; uploads are deferred and resumable. All capture-time media is signed in-app using a device-bound key for C2PA verification. `[M-01, U-17, F-11]`

**2.a — FNOL capture** `[M-01]`
Minimum data: policy id (or soft-match identifiers), incident type, occurred-at, brief narrative (≤500 chars), approximate location. No photos required for FNOL; NCB impact is disclosed here. `[M-17]`

**2.b — Formal claim** — adds photos/video, structured fields, statutory documents, declaration acceptance. Tiered photo minimum per `[U-14]`:
- `collision` / `third_party` — **minimum 6** photos (front, rear, both sides, two damage close-ups).
- `theft` — **minimum 2** photos (of surroundings/where parked) + FIR doc required.
- `vandalism` — **minimum 4** photos.
- `natural_event` — **minimum 4** photos + environmental context shots.
- `fire_damage` — **minimum 6** photos + fire-brigade report required if available.
Maximum 20 photos; total payload ≤ 100 MB; 25s video (≤ 40 MB).

**Configurable per-deployment cap** `[NEW in v0.2.1]` — The hard limit above is the FRS ceiling. Each deployment exposes a **tighter `claims.max-evidences`** knob (backend property) and a **`claims.min-evidences-for-analysis`** knob that gates the synchronous AI pipeline (FR-3/4/7). Phase-1 web portal ships with `max=10`, `min=3` — below the FRS ceiling but above the AI pipeline's minimum image count. The frontend reads both values from the backend at page load (no hardcoded UI caps) so raising the limit is a config-only change.

**Third-party liability (TP) track** `[M-08]` — When `incident_type` involves injury or property damage to a third party, FNOL additionally captures: third-party name(s), contact, injury severity category, FIR number (if filed), MACT case number (if initiated). TP claims route to the dedicated TP workflow (FR-15), not the OD workflow.

**Document uploads with OCR** `[M-22]` — RC book, driving licence, FIR, repair invoice, hospital bill. OCR runs server-side; extracted fields are shown to the user for confirmation before submission. GSTN cross-check on repair invoices.

**Offline-first** `[U-17]` — The app may capture a complete claim fully offline (FNOL + photos + documents); local store is AES-256 encrypted with a key bound to the OS keystore; deferred upload resumes when connectivity returns; a 72-hour local retention cap applies.

**Capture-time signing (C2PA)** `[F-11]` — Each photo/video is signed by the app at capture with a device-bound key; the signature manifest is uploaded alongside bytes and verified by FR-6. A missing signature is a flag but not automatic rejection (accounts for gallery uploads).

**Preconditions.**
- Authenticated, policy status linked or soft-linked.
- Incident date within coverage window and within the configurable reporting limit (`[AMBIG-05]`).

**Postconditions.**
- FNOL created with `fnol_id` and state `FNOL_RECEIVED`; statutory clock begins.
- On formal submission: claim in `SUBMITTED`; id `CLM-YYYYMMDD-NNNNNN` (date-embedded format retained pending opaque-ID decision, see `AMBIG-26`).
- Media uploaded (or queued offline) with C2PA manifests; EXIF sidecar secured; malware scan clean.
- `claim.submitted` event published; AI pipeline enqueued.
- NCB-impact disclosure acknowledged and recorded. `[M-17]`
- Confirmation SMS/push delivered; for assisted intake (FR-19), the agent receives a copy.

**Main Flow.** (new)
1. Stage 2.a: User provides policy link + incident type + occurred-at + brief narrative + approximate location.
2. NCB impact disclosed (computed from PAS); user acknowledges. `[M-17]`
3. FNOL saved; `fnol_id` issued; statutory timer starts.
4. Stage 2.b: User progresses to detailed capture per incident-type-specific checklist.
5. Photos captured with C2PA signing; uploaded (online) or queued (offline).
6. Documents scanned via OCR; user confirms extracted fields.
7. Declaration accepted; formal submit → `SUBMITTED`.
8. Claim id returned; tracker shown.

**Alternate Flows.** v0.1 AF-2.1…AF-2.5 retained with amendments; new:
- **AF-2.6** `[M-08]` — TP track selected: after FNOL, user is routed to TP-specific capture (injury details, witness info); claim enters FR-15 workflow.
- **AF-2.7** `[U-17]` — Deferred upload: user resumes in online mode; server de-dupes by offline payload hash.
- **AF-2.8** `[F-11]` — C2PA signature invalid or missing: image accepted with `capture_provenance = unverified`; FR-6 weights adjust.
- **AF-2.9** `[M-22]` — OCR extracts a GSTN that does not validate: user is asked to re-scan or enter manually; claim proceeds with `invoice_gstn_status = unverified`.

**Error Handling.** v0.1 retained, amended:
| Code | Condition | HTTP |
|---|---|---|
| `E-CLM-002` | Fewer than the tiered photo minimum for incident type | 422 |
| `E-CLM-012` | FNOL missing mandatory fields | 422 |
| `E-CLM-013` | Deferred-upload payload older than 72h | 410 |
| `E-CLM-014` | OCR failure on statutory document (retry allowed) | 422 |

**Validation Rules (deltas).**
- Photo minimums per the tiered table above. `[U-14]`
- NCB acknowledgement is a hard gate on FNOL completion. `[M-17]`
- GSTN on invoices is validated against GSTN API where available. `[M-22]`
- Offline payload integrity verified by client-computed SHA-256 re-checked server-side before acceptance.
- PII redaction on Claim-Service log lines (unchanged).

**Acceptance Criteria (deltas).**
- **AC-2.1.1 (revised)** — FNOL is persisted and `fnol_id` returned in ≤ 1 s after user confirms, *regardless of upload state* (i.e. FNOL does not block on image upload). `[M-01]`
- **AC-2.1.6** `[U-17]` — A claim fully captured offline uploads successfully when connectivity returns, within the 72h cap.
- **AC-2.1.7** `[F-11]` — 100 % of online-captured photos carry a valid C2PA manifest; tampered manifest is detected and flagged at FR-6 in 100 % of a 50-case synthetic corpus.
- **AC-2.1.8** `[U-14]` — `collision` with 5 photos is rejected; with 6 is accepted.
- **AC-2.1.9** `[M-17]` — Submitting without NCB acknowledgement yields `E-CLM-012`.
- **AC-2.1.10** `[NEW in v0.2.1]` — The per-deployment photo cap is advertised by the backend via `GET /api/v1/claims/config` (`maxEvidences`, `minEvidencesForAnalysis`), and the web portal MUST read these values at mount time rather than hardcoding them. A reduction of the cap from 10 → 5 in `application.yml` takes effect on the next page reload without any frontend code change.
- **AC-2.1.11** `[NEW in v0.2.1]` — A formal-claim submission with ≥ `claims.min-evidences-for-analysis` photos synchronously invokes the AI pipeline (FR-3, FR-4, FR-7). If the AI service is unavailable, the claim is still persisted with `status = FNOL_RECEIVED`; the fallback analysis response has `modelVersion = "ml-service-unavailable"` and `totalDetections = 0`, and the claim routes to `HUMAN_REVIEW` per FR-8 AF-8.1.
- **AC-2.1.12** `[NEW in v0.2.2]` — **Evidence bytes are retrievable after submission.** Each uploaded photo / video is persisted (phase-1: local filesystem at `{claims.upload-dir}/{evidenceId}`; phase-2 migrates to S3 per TSD ADR-005) with its original `contentType` and `sizeBytes` recorded on the `claim_evidence` row. A subsequent `GET /api/v1/claims/{claimId}/evidence/{evidenceId}` returns the bytes with the stored MIME type and `Content-Disposition: inline`. 404 when the byte stream is missing (pre-v0.2.2 rows or failed writes); 200 otherwise. (API-layer RBAC per §3.2 is a cross-cutting concern wired separately.)
- **AC-2.1.13** `[NEW in v0.2.2]` — **AI analysis is persisted and re-readable.** On formal-claim submission, the full AI response (FR-3 detections, FR-4 cost estimate, FR-7 surveyor assessment, `model_version`, `processing_time_ms`) is written to a `claim_analysis` row keyed by `claim_id` with denormalised scalars (`model_version`, `total_detections`, `processing_time_ms`, `severity_verdict`, `repair_recommendation`, `cost_total_paise`, `assessment_confidence`) plus the full payload as JSON for forensic recall. A subsequent `GET /api/v1/claims/{id}/analysis` returns the deserialised response (200) or `204 No Content` when the claim has no analysis yet; the claim's own identity is independent of analysis availability (see AC-2.1.11 failure mode).

---

### FR-3 — AI Damage Detection (YOLOv8) `[revised AC: U-01, licensing: U-12; v0.2.1 invocation contract]`

**Description.** Unchanged in architecture. Licensing path selected to avoid Ultralytics AGPL exposure (see §12 C-09). `[U-12]`

**Invocation contract (phase 1)** `[NEW in v0.2.1]`. The claims backend calls the AI pipeline synchronously during formal-claim submission using a single consolidated multipart endpoint (TSD §6.3 "MVP consolidated" variant); detection (FR-3), cost estimation (FR-4), and surveyor-style explanation (FR-7) are computed in one call and returned together. Decomposition into the per-worker DAG sketched in the §2.1 high-level architecture (YOLOv8 → Forensics ∥ Cost → Fraud → Explanation) is deferred to phase 2 when forensics and fraud layers come online; the behavioural contract (AC-3.1.1…3.1.4) is unchanged.

**Acceptance Criteria (revised).** `[U-01]`
- **AC-3.1.1 (revised)** — On an **insurer-specific held-out evaluation set** (not a synthetic public corpus), model shall achieve:
  - v1 go-live: mAP@0.5 ≥ **0.60** on the three dominant classes (dent, scratch, broken_glass).
  - Month 6: mAP@0.5 ≥ **0.70** on those classes.
  - Confidence thresholds are calibrated to measured accuracy; if measured mAP is below target, claim routing shifts more aggressively to HUMAN_REVIEW until retraining.
- AC-3.1.2…3.1.4 retained.

Everything else in FR-3 per v0.1.

---

### FR-4 — AI Cost Estimation `[extended: M-05, M-10, M-16, U-02, U-06]`

**Description.** As v0.1, plus: salvage, coinsurance, and TDS handling; parts-catalog sourcing treated as a named workstream, not an assumption.

**Additions to Main Flow.**
- **Step 4a — TDS/withholding** `[M-16]`: Applicable tax rules resolved from a versioned rule table (Sec 194N / 194DA triggers). TDS reduces the net payable and produces a ledger line item.
- **Step 4b — Coinsurance apportionment** `[M-10]`: If PAS indicates coinsurance, payable is apportioned per share; inter-insurer recoveries tracked against a separate ledger.
- **Step 4c — Salvage path** `[M-05]`: For `total_loss_candidate`, salvage workflow (FR-14.3) is triggered in parallel; estimated salvage value is integrated into net payable as `IDV − salvage − deductible`.

**Validation Rules (deltas).**
- Net payable = gross − deductible − TDS − salvage (where applicable) − coinsurance share due from other insurers; asserted.
- TDS rule version recorded on every estimate.
- Parts-catalog sourcing is multi-source (§12 C-10); every line item records `catalog_source ∈ {OEM_FEED, NEGOTIATED, HUMAN_CORRECTED, FALLBACK}`. `[U-06]`

**Acceptance Criteria (revised and added).**
- **AC-4.1.1 (revised)** `[U-02]` — Measured against **settled** claim amounts (not initial-adjuster estimates), the model-vs-settled distribution shall show:
  - ≥ 70 % of claims within ±15 % of settlement,
  - median absolute percentage error ≤ 12 %,
  - 95th-percentile APE ≤ 40 %.
  Acceptance is distributional; no single-claim hard bound is set.
- AC-4.1.2, AC-4.1.3 retained.
- **AC-4.1.4** `[M-16]` — A claim subject to Sec 194DA trigger produces a ledger line with correct TDS amount on the same transaction.
- **AC-4.1.5** `[M-10]` — A coinsurance 60/40 claim produces two ledger lines: own share (60 %) payable now, recovery receivable (40 %) from coinsurer.
- **AC-4.1.6** `[M-05]` — `total_loss_candidate` triggers the salvage workflow and the estimate displays gross, salvage-deduction, and net-of-salvage.

---

### FR-5 — Fraud Detection Engine (Three-Layer) `[substantially rewritten: F-01..F-10, F-16..F-22, U-11]`

**Description.** A three-layer architecture that separates legally-defensible deterministic blocks from probabilistic scores and from human-prioritisation signals. `[F-01]`

- **Layer 1 — Deterministic Rules.** Fast, explainable; produces hard flags and hard blocks. Defensible grounds for rejection (e.g., EXIF timestamp > incident date by > 24 h; vehicle reported stolen on VAHAN; policy not in force on incident date).
- **Layer 2 — Calibrated ML.** Gradient-boosted tree with isotonic head; produces calibrated probability. **Conformal prediction band** yields an `UNCERTAIN` abstain option when prediction is not defensibly in either class. `[F-03]`
- **Layer 3 — Prioritisation / Graph.** GNN over (claimant ↔ policy ↔ vehicle ↔ garage ↔ vendor ↔ bank account ↔ adjuster); produces a ring-risk feature and a human-review priority, but does not itself drive reject. `[F-05]`

**Feature classes** (expanded).
- Behavioural (v0.1) + **behavioural biometrics at submission** (typing cadence, scroll patterns, swipe pressure via device SDK). `[F-07]`
- Network (v0.1) + **graph/GNN features** from Layer 3. `[F-05]`
- Metadata (v0.1) + **EXIF coherence across the claim** (unchanged).
- Forensic — from FR-6 (now includes C2PA, JPEG-QTab, PRNU, deepfake, noise-residual). `[F-11..F-15]`
- NLP (v0.1) + **dedicated narrative ↔ evidence NLI model** that explicitly checks whether the narrative is entailed by the photos. `[F-09]`
- **Device intelligence** — root/jailbreak, emulator, VPN/Tor, device fingerprint reputation. `[F-08]`
- **Garage score** — per-repair-shop fraud propensity (inflated quotes, repeat-at-same-garage, part-substitution rate). `[F-10]`
- **External enrichment** — IIB claim history, VAHAN stolen/ownership status, weather-event validation, sanctions/PEP hits. `[F-06, M-20, M-21]`

**Ring cascade action** `[F-04]`. When a claim is confirmed fraud (human decision in FR-9), the graph service re-scores all open claims connected within radius 2 and re-routes accordingly; action itself is recorded in audit.

**Layer 1 hard rules (partial, versioned in registry).**
- `HR-01` EXIF timestamp > incident date by > 24 h AND C2PA manifest absent/invalid.
- `HR-02` Vehicle marked stolen on VAHAN as of incident date.
- `HR-03` Policy not in force on incident date.
- `HR-04` Claimant on active sanctions or PEP list. `[M-15]`
- `HR-05` Submitted bank account matches a known-fraud-account block list.
- `HR-06` Same incident hash already settled under another claim id.

**Main Flow.**
1. Evaluate Layer 1; any hit → `FRAUD_HARD_FLAG` surfaced to routing (FR-8).
2. Extract features; compute Layer 2 calibrated probability with conformal band.
3. Layer 3 GNN: compute ring-risk and priority.
4. Emit `fraud_result` with score, `confidence_band`, `top_features` (decomposed by class), `rule_hits`, `ring_risk`, model & ruleset versions. `[F-20]`
5. On FR-9 final fraud confirmation, trigger ring cascade. `[F-04]`

**Alternate Flows.** v0.1 retained; new:
- **AF-5.4** `[F-03]` — Conformal band straddles decision boundary: engine emits `band = uncertain`; routing forces HUMAN_REVIEW.
- **AF-5.5** `[F-04]` — Ring cascade fires: linked open claims enter re-scoring; newly-breaching claims re-route.

**Validation Rules (deltas).**
- Hard rules are deterministic, versioned, and carry a human-readable legal rationale.
- Layer 2 output includes both point score and conformal band.
- Decomposed reasons persist contribution per feature-class, not a blended blob. `[F-20]`
- Inputs to third-party device-intelligence and external-enrichment providers are minimal-PII and redacted.
- Behavioural-biometrics signal is collected only under explicit consent (FR-18); absence is not treated as suspicious.

**Acceptance Criteria.**
- **AC-5.1.1 (revised)** `[F-01]` — Layer 1 hit (e.g., `HR-01`) produces `FRAUD_HARD_FLAG` deterministically, independently of Layer 2/3.
- **AC-5.1.2** `[F-05]` — A synthetic ring of 10 linked claims yields ring-risk ≥ 0.8 on all ten.
- **AC-5.1.3** `[F-03]` — Engineered borderline claim yields `band = uncertain`; routing forces HUMAN_REVIEW.
- **AC-5.1.4** `[F-04]` — Confirmed fraud on claim A triggers re-scoring of graph-linked claims within 5 min.
- **AC-5.1.5** `[F-20]` — Every `fraud_result` exposes per-feature-class contributions (forensic, behavioural, network, narrative, external) visible to adjusters.
- **AC-5.1.6** `[F-21]` — Weekly bias report publishes false-positive rates by region and vehicle class; alert fires when any subgroup FPR exceeds the overall by > 50 %.

---

### FR-6 — Image Forensics (Extended) `[extended: F-11..F-15]`

**Description.** Extends v0.1 with capture-provenance verification, additional sub-scores, and independent-lifecycle deepfake detector.

**Additions to Main Flow.**
- **Step 1a — C2PA manifest verification.** `[F-11]` Verify signature chain; record `provenance ∈ {verified, unverified, invalid}`.
- **Step 2a — JPEG quantisation-table signature.** `[F-12]` Identify camera/editor fingerprints; detect re-encoding by known editors or messaging-app pipelines.
- **Step 2b — Noise-residual / SRM forensics.** `[F-15]` Detect copy-move and splicing that ELA misses.
- **Step 5 (existing AI-generated detector) promoted to a separately-versioned sub-service** with its own monthly retrain cadence, dataset, and canary. `[F-14]`
- **Step 6 — PRNU (sensor fingerprint)** applied only on claims above a configurable value threshold (compute-intensive, see NFR-P budget). `[F-13]`

**Validation Rules (deltas).**
- Composite weights revised to include the five new sub-scores; sum to 1 after renormalisation.
- Each sub-score model is independently versioned in the registry.
- PRNU invocation is gated by claim value and logged for cost tracking.

**Acceptance Criteria (deltas).**
- **AC-6.1.4** `[F-11]` — Valid C2PA manifest yields `provenance=verified`; tampered manifest yields `provenance=invalid` in 100 % of a 50-case test corpus.
- **AC-6.1.5** `[F-14]` — Dedicated deepfake classifier achieves AUROC ≥ 0.85 on a benchmark mix of GAN/diffusion-generated vehicle images at v1 go-live; regression gate on every retrain.
- **AC-6.1.6** `[F-13]` — PRNU analysis is invoked only on claims where `estimate > PRNU_trigger_value` (configurable via versioned config, not runtime edit).

---

### FR-7 — AI Explanation Generation (Model-Agnostic) `[revised: U-03, U-13]`

**Description.** The LLM provider and model are abstracted behind a capability-oriented interface (`MultimodalExplainer`); the system prompt is pinned by `prompt_version_id` regardless of underlying model. `[U-13]` v0.2 uses GPT-4o-class models; migrations to future models are planned annually and re-validated via the semantic-equivalence regression suite.

**Acceptance Criteria (revised).**
- **AC-7.1.1 (revised)** `[U-03]` — Byte-identical outputs are a desired but soft property. The authoritative gate is a **semantic-equivalence regression suite** over a curated 100-case set:
  - ≥ 95 % pass rate of the reference evaluator between consecutive deployments of the same model/prompt pair.
  - ≥ 90 % pass rate across model migrations (pinned prompt, new model).
  - Any failure below threshold blocks promotion.
- AC-7.1.2 retained (zero policyholder-audience fraud language).
- **AC-7.1.3 (revised)** `[U-04]` — P95 end-to-end explanation latency target is **10 s** in v1; 6 s is a v2 goal after warming and batch optimisation. Latency budget per stage is published in §5.1 table.
- AC-7.1.4 retained.

Everything else retained.

---

### FR-8 — Confidence Scoring & Threshold Handling `[revised: U-11, F-01, M-26]`

**Description.** Routing engine unchanged in shape, extended with:
- **SURVEYOR_REQUIRED** decision for claims above the IRDAI-notified surveyor threshold. `[M-02]`
- **DOA-bounded auto-approval** — claim is auto-approved only if *within the effective DOA ceiling for an automated decision*, which is a configurable subset of the Adjuster DOA. `[M-26]`
- **Thresholds are not runtime-editable in production.** `[U-11]` Proposed changes are pull-requests against the threshold registry, require two-person approval (Admin proposer + Sr. Adjuster or AML reviewer approver), and are promoted through a change-management pipeline; the Admin UI surfaces the diff but does not save.

**Updated rule order (first match wins).**
0. **Layer 1 fraud hard-flag** → `FRAUD_INVESTIGATION`. `[F-01]`
1. Hard reject conditions (out-of-coverage, KYC mismatch) → `AUTO_REJECT`.
2. Conformal `UNCERTAIN` OR fraud score in grey zone → `HUMAN_REVIEW`. `[F-03]`
3. `fraud.score ≥ fraud_high_threshold` (default 80) → `FRAUD_INVESTIGATION`.
4. `estimate > surveyor_threshold` (regulator-notified) → `SURVEYOR_REQUIRED`. `[M-02]`
5. `ai_confidence < low_threshold` OR `estimate > auto_decision_ceiling` → `HUMAN_REVIEW`.
6. `forensic.tampering_score ≥ tamper_threshold` → `HUMAN_REVIEW`.
7. Missing-evidence rule → `NEEDS_MORE_INFO`.
8. Else → `AUTO_APPROVE` (within DOA ceiling).

**Acceptance Criteria (deltas).**
- AC-8.1.1 retained with extended decision set.
- **AC-8.1.2 (revised)** `[U-11]` — Attempting to save a threshold change via the dashboard returns HTTP 405; only the change-request API (reviewed, gated) can alter threshold_version.
- **AC-8.1.4** `[M-02]` — A claim with estimate above the regulator-notified surveyor threshold routes to `SURVEYOR_REQUIRED`, never to `AUTO_APPROVE`.

---

### FR-9 — Fallback Flow (Human Review) `[extended: M-06, M-15, M-24, U-08, U-16]`

**Description.** Adjuster workflow extended with claim-time KYC re-verification, sanctions screening, payee-account verification, and a re-open path.

**Additions.**
- **Claim-time KYC re-check** `[U-08]` — Before any decision (approve/reject), PAS identity is re-verified against supplied claim documents (RC name, policyholder name, address) with a soft/hard match; mismatches route to a sub-queue.
- **Sanctions / PEP screen** `[M-15]` — Claimant and payee screened against UN/OFAC/MEA lists; hit routes to AML Officer (FR-17).
- **Payee-account verification (penny-drop)** `[M-24]` — Before first payout to a new bank account, `pennydrop.verified=true` required; failure blocks approval.
- **Re-open** `[M-06]` — Closed claims can be re-opened by Sr. Adjuster, Fraud Investigator, or Grievance Officer on documented grounds; re-open is a new state transition that preserves the prior terminal state in `history[]`.

**Acceptance Criteria (deltas).**
- AC-9.1.1 retained.
- **AC-9.1.2 (revised)** `[U-16]` — Analytics alert `ANLTC-OVR-001` fires on **override rate > 50 % on individual claim or > 30 % week-over-week shift at aggregate level**, not on an implicit "rare override" baseline.
- AC-9.1.3 retained.
- **AC-9.1.4** `[M-24]` — Payout to a new account without penny-drop verification is rejected deterministically.
- **AC-9.1.5** `[M-06]` — A re-opened claim retains the full prior history, including the previous terminal state, and requires a new reason code.
- **AC-9.1.6** `[U-08]` — Claim-time KYC mismatch routes to the KYC-mismatch sub-queue and blocks approval until resolved.

---

### FR-10 — Admin Dashboard & Controls `[revised: U-11, F-20, M-26; v0.2.1 adds analytical panels]`

**Description.** Unchanged in shape. Key changes:
- Threshold-change UI shows proposed diff, routes as a change request; **no direct save in production**. `[U-11]`
- Claim detail exposes **decomposed fraud reasons** (per-feature-class contributions), not a blended score. `[F-20]`
- **DOA Matrix management** — Admin can edit role-and-limit matrix; changes are four-eye approved. `[M-26]`
- Bulk operations unchanged; model and prompt registries viewable, not editable from UI.
- **Analytical panels on the landing dashboard** `[NEW in v0.2.1]` — at-a-glance visualisations driven by the existing `/api/v1/claims/stats/status-counts`, `/api/v1/rbac/users/stats/active-users-per-role`, and `/api/v1/rbac/permissions` endpoints; no new data contracts.
  - **Claims by status** (doughnut) — distribution over the FR-8 routing outcomes (`FNOL_RECEIVED`, `SUBMITTED`, `TRIAGE`, `HUMAN_REVIEW`, `SURVEYOR_REQUIRED`, `APPROVED`, `REJECTED`, `CLOSED`); click-through to the filtered claim list.
  - **Users per role** (horizontal bar) — count of active `user_role_assignment` rows per `RoleName`; helps administrators confirm the §3.1 role inventory is populated before running access reviews.
  - **Permissions by scope** (doughnut) — `(capability × scope)` distribution across the seeded §3.2 catalog; useful sanity view when tuning the matrix.
  Each panel must render its own empty state (no silent-blank canvases); percentage values accompany raw counts in tooltips; panels refresh on navigation to the dashboard (no real-time push required).

**Acceptance Criteria (deltas).**
- AC-10.1.1, AC-10.1.3 retained.
- **AC-10.1.2 (revised)** `[U-11]` — Threshold save action in production is disabled; only change-request submission is available.
- **AC-10.1.4** `[M-26]` — DOA-matrix change without a second approver cannot be saved.
- **AC-10.1.5** `[NEW in v0.2.1]` — On an Administrator landing load the three analytical panels render within 500 ms of their stats endpoints returning; totals shown on the stat tiles (Total claims, Active users, Roles, Permission atoms) equal the sum of values feeding their respective chart; each panel shows an explicit empty-state message when its data source returns zero rows.

---

### FR-11 — Audit Logging & Compliance `[revised: U-09, U-18]`

**Description.** Append-only tamper-evident log with **mandatory external anchoring**. `[U-18]` Right-to-erasure is executed as a **dual path**: true deletion for data not under lawful retention, redaction for data subject to retention; both paths are audit-logged with legal-basis reference. `[U-09]`

**Changes.**
- **External anchoring mandatory** `[U-18]` — Daily Merkle root is published to an independent notary / public timestamp service (e.g., RFC 3161 TSA, or a permissioned public ledger). Anchoring failure for > 24 h raises a critical incident and fails-closed any new state transitions (degraded mode).
- **Erasure bifurcation** `[U-09]` — Two endpoints: `erasure.delete` (true physical deletion across primary, replicas, backups per a documented schedule), `erasure.redact` (PII replaced with pseudonymous tokens in the transactional store while preserving referential integrity). The choice is determined by legal-basis registry; decision is logged.

**Acceptance Criteria (deltas).**
- AC-11.1.1, AC-11.1.2, AC-11.1.3 retained.
- **AC-11.1.4** `[U-18]` — A 24-h external-anchoring outage triggers degraded mode; resumption on recovery; event and recovery are audit-logged.
- **AC-11.1.5** `[U-09]` — `erasure.delete` removes PII from primary, read replicas, and nearest-line backup within the retention-schedule window; `erasure.redact` leaves stable pseudonyms in place; both emit audit entries; legal-basis reference persisted on each.

---

### FR-12 — Surveyor / Loss Assessor Workflow `[NEW — M-02]`

**Description.** For claims with `estimate > surveyor_threshold` (IRDAI-notified) or `incident_type ∈ {fire_damage, total_loss_candidate, TP}`, an IRDAI-licensed surveyor is assigned, conducts on-site or virtual inspection, and files a report that the System integrates into the estimate.

**Preconditions.** Claim in `SURVEYOR_REQUIRED` (from FR-8); a suitable surveyor is available in the regional panel.

**Postconditions.** `surveyor_report` persisted with line items, photos, and recommendation; claim transitions to adjuster review with the surveyor report as authoritative evidence.

**Main Flow.**
1. System selects the nearest available surveyor with matching specialisation and no conflict-of-interest.
2. Surveyor receives assignment; confirms SLA (default: visit within 48 h of assignment, report within 72 h of visit; configurable `[AMBIG-27]`).
3. Surveyor uses the dashboard (FR-10) to capture evidence, line items, and a recommendation.
4. Report is digitally signed with the surveyor's licence-bound key; persisted; event `surveyor.report_submitted` emitted.
5. Adjuster receives claim with surveyor report; decides per FR-9.

**Alternate Flows.**
- **AF-12.1** — Surveyor unavailable in region: escalated to manual assignment by Claims Manager.
- **AF-12.2** — Surveyor detects fraud indicators: direct route to Fraud Investigator queue.
- **AF-12.3** — Surveyor's recommendation differs from AI estimate by > 25 %: flagged for Sr. Adjuster review irrespective of DOA ceiling.

**Error Handling.** `E-SUR-001` (no surveyor available within SLA), `E-SUR-002` (licence invalid at submission), `E-SUR-003` (report schema validation failure).

**Validation Rules.**
- Surveyor must hold a valid IRDAI licence at submission (re-validated at token-refresh, FR-1).
- Report captures mandatory IRDAI-format fields (claim id, incident description, loss particulars, recommended liability).
- Conflict-of-interest checks (surveyor ↔ policyholder, surveyor ↔ garage) enforced.

**Acceptance Criteria.**
- **AC-12.1.1** — Assignment to the nearest eligible surveyor completes within 15 min of `SURVEYOR_REQUIRED` transition.
- **AC-12.1.2** — Report without a valid licence signature is rejected with `E-SUR-002`.
- **AC-12.1.3** — A ≥ 25 % delta between AI estimate and surveyor estimate triggers Sr. Adjuster review.

---

### FR-13 — Cashless Settlement & Network Garages `[NEW — M-03]`

**Description.** Network-garage path for cashless repair: policyholder selects a network garage, system pre-authorises a repair amount, garage submits bills against the pre-authorisation, system settles directly with the garage.

**Preconditions.** Claim has entered an approval track (either `AUTO_APPROVE` or adjuster/surveyor-approved) and policy permits cashless.

**Postconditions.** Pre-authorisation id issued; on final-bill receipt, variance is adjudicated; payment instruction posted to ledger; garage notified; policyholder notified.

**Main Flow.**
1. Policyholder selects a network garage (or one is recommended based on proximity and garage score, FR-5 `F-10`).
2. System posts pre-authorisation (gross estimate bounded by policy limits) to the garage portal.
3. Garage uploads final bill with GSTN-verified invoice `[M-22]`.
4. System adjudicates variance vs pre-authorisation:
   - Within tolerance (default ±10 %): auto-settle.
   - Outside tolerance: route to Sr. Adjuster with variance reason.
5. Settlement event posted; ledger line created; policyholder notified.

**Alternate Flows.**
- **AF-13.1** — Garage not in network: reimbursement track instead (policyholder pays, submits bills, is reimbursed).
- **AF-13.2** — Garage fraud score above threshold: auto-decline cashless at that garage; policyholder offered alternates.
- **AF-13.3** — Partial disallowance after surveyor review: garage and policyholder informed of disallowed items with reasons.

**Error Handling.** `E-CASH-001` (garage offline), `E-CASH-002` (GSTN mismatch), `E-CASH-003` (variance beyond hard cap).

**Validation Rules.**
- GSTN on final invoice verified; input-tax-credit eligibility recorded.
- Pre-authorisation TTL 45 days unless extended.
- Garage score gate enforced at pre-authorisation.

**Acceptance Criteria.**
- **AC-13.1.1** — Pre-authorisation is issued within 60 s of approval transition.
- **AC-13.1.2** — A final bill within ±10 % auto-settles; > 10 % routes to Sr. Adjuster.
- **AC-13.1.3** — A garage with fraud score ≥ threshold cannot receive pre-authorisations.

---

### FR-14 — Subrogation, Recovery & Salvage `[NEW — M-04, M-05]`

**FR-14.1 Subrogation & Recovery.** When a third party is at fault for an own-damage claim, the System opens a recovery case: demand letter to the at-fault party or their insurer, response tracking, legal escalation hand-off, recovered-amount credit to the ledger.

**FR-14.2 Salvage.** For `total_loss_candidate`, a salvage case is opened: salvage valuation, bidding workflow, realisation, and net-of-salvage payout to policyholder.

**FR-14.3 Ledger integration.** Recovery and salvage movements are mirrored to the financial ledger via the Integration Gateway (§2.3).

**Acceptance Criteria.**
- **AC-14.1.1** — Settlement of a third-party-at-fault claim automatically opens a `recovery_case` referencing the source claim.
- **AC-14.1.2** — Salvage realisation updates the net payout calculation within one settlement cycle.
- **AC-14.1.3** — Ledger lines for recovery and salvage are reconciled against claims monthly with a standing report.

---

### FR-15 — Third-Party Liability (TP) Claims `[NEW — M-08]`

**Description.** TP claims are legally and operationally distinct from own-damage: they involve injury or property damage to third parties and often proceed through MACT. The TP workflow captures victim details, FIR, medical records (where applicable), witness statements, and supports long-tail reserving and legal-case tracking.

**Scope in v0.2.** Intake, initial triage, reserve setup, and legal-case referral. MACT proceedings themselves are out of scope (handled by a legal case-management system; the System tracks status via integration).

**Acceptance Criteria.**
- **AC-15.1.1** — A TP incident-type cannot be AUTO_APPROVED; minimum route is `HUMAN_REVIEW` with TP specialisation.
- **AC-15.1.2** — Victim name, injury severity, FIR number captured as structured fields.
- **AC-15.1.3** — Initial case-reserve is posted to the ledger on TP claim registration.

---

### FR-16 — Grievance Management (IGMS-aligned) `[NEW — M-09]`

**Description.** Policyholders can raise grievances (about claim decision, service, payout, other); Grievance Officer handles per IRDAI IGMS SLAs; escalations to Insurance Ombudsman / IRDAI are surfaced.

**Acceptance Criteria.**
- **AC-16.1.1** — Grievance acknowledgement is issued within 3 working days; resolution SLA per IRDAI norms (default 15 days, configurable).
- **AC-16.1.2** — An unresolved grievance at day 14 is auto-escalated to Claims Manager.
- **AC-16.1.3** — Grievance status is synced with IRDAI IGMS via the Integration Gateway.

---

### FR-17 — AML, Sanctions & Regulatory Reporting `[NEW — M-11, M-14, M-15]`

**Description.** The AML Officer receives an alert queue for suspicious patterns (unusual payout velocity, structuring, sanctions hits); produces STRs to FIU-IND; regulatory reports (IRDAI cycle-time disclosures) are generated on cadence.

**Acceptance Criteria.**
- **AC-17.1.1** — A sanctions/PEP hit generates an AML alert within 1 min of detection.
- **AC-17.1.2** — STR can be produced in FIU-IND's prescribed format from the UI, with supporting evidence packaged.
- **AC-17.1.3** — IRDAI cycle-time public-disclosure report generated monthly on schedule without manual aggregation.

---

### FR-18 — Consent & Data-Principal Rights `[NEW — M-18, U-09]`

**Description.** DPDP-aligned consent and rights management: granular purpose-scoped consent captured at registration and on new processing purposes; rights portal allows a data principal to view, export, correct, and request erasure (invoking FR-11 erasure dual-path); Consent Manager integration where applicable.

**Acceptance Criteria.**
- **AC-18.1.1** — Behavioural-biometrics collection (FR-5 `F-07`) is gated on an explicit, revocable consent artefact.
- **AC-18.1.2** — Consent-revocation propagates to downstream processors within 24 h.
- **AC-18.1.3** — Erasure requests route through FR-11 dual path, with legal-basis reference recorded.

---

### FR-19 — Multi-Channel Intake `[NEW — M-32, U-15]`

**Description.** Assisted intake (agent portal), IVR, WhatsApp/RCS as first-class intake channels; all channels create an FNOL (FR-2.a) with the correct `intake_channel` tag; downstream flow is identical from FNOL onward.

**Acceptance Criteria.**
- **AC-19.1.1** — An FNOL captured via IVR results in the same `fnol_id` contract and statutory timer as mobile.
- **AC-19.1.2** — Assisted intake records both the acting agent and the beneficiary policyholder; conflict-of-interest check applies to agents too.
- **AC-19.1.3** — WhatsApp/RCS intake respects the same consent artefacts (FR-18) before processing PII.

---

## 5. Non-Functional Requirements

### 5.1 Performance — with explicit per-stage budget `[revised: U-04]`

End-to-end pipeline target P95 30 s is retained as an aspirational goal; a documented per-stage budget makes it measurable and tune-able.

| Stage | P95 Budget | Notes |
|---|---|---|
| API submit (no images) | ≤ 300 ms | NFR-P-001 (unchanged) |
| Image upload (per image, 4G) | ≤ 1.5 s/MB | NFR-P-002 (unchanged) |
| YOLOv8 detection (per image, GPU) | ≤ 600 ms | FR-3 AC-3.1.2 |
| Forensics (ELA + pHash + Q-Tab + NR, no PRNU) | ≤ 1.5 s | `[F-12, F-15]` |
| PRNU (when triggered) | ≤ 4 s | `[F-13]` |
| Fraud Layer 1 + Layer 2 | ≤ 500 ms | `[F-01]` |
| Fraud Layer 3 (GNN) | ≤ 2 s | `[F-05]` |
| Cost estimation | ≤ 1 s | |
| Explanation (GPT-4o-class, ≤8 images) | ≤ 10 s (v1), 6 s (v2) | **AC-7.1.3 revised `[U-04]`** |
| Routing Engine | ≤ 200 ms | |
| **Total P95 (parallelised, v1)** | **≤ 45 s** | Revised from v0.1's 30 s per U-04 |
| **Total P95 (parallelised, v2 post-tuning)** | **≤ 30 s** | Goal after 6 months |

NFR-P-004, NFR-P-005 unchanged.

### 5.2 Security — IRDAI Cyber-Security-2023-aligned `[M-12]`

NFR-S-001…NFR-S-011 unchanged. Additions:
- **NFR-S-012** `[M-12]` — Compliance with IRDAI Information and Cyber Security Guidelines, 2023 (specifically: asset inventory, CERT-In 6-hour incident reporting, quarterly VAPT, annual information-security audit by a CERT-In-empanelled auditor).
- **NFR-S-013** `[U-10]` — LLM-provider data handling: contractual zero-retention on inputs (DPA clause), or on-prem deployment for sensitive inference (damage detection, fraud features) with the multimodal explainer as the only cloud-LLM touchpoint.
- **NFR-S-014** `[F-11]` — Mobile app performs capture-time C2PA signing with a device-bound key resident in the OS keystore (Keychain / Android Keystore).
- **NFR-S-015** — Pre-commit secret scanner (gitleaks or equivalent); leaked-credential response runbook.

### 5.3 Scalability — unchanged.

### 5.4 Availability — unchanged; add:
- **NFR-A-006** `[U-18]` — If external audit anchoring is unavailable for > 24 h, the System enters degraded mode: new state transitions are blocked, announced to Ops, and resumed on recovery.

### 5.5 Audit & Compliance `[additions: M-11, M-12]`

NFR-AC-001…NFR-AC-005 unchanged. Additions:
- **NFR-AC-006** `[M-11]` — IRDAI claims-cycle-time public-disclosure reports generated monthly in prescribed format.
- **NFR-AC-007** `[M-28]` — A documented Model Risk Management framework: model inventory, approval gates, monitoring, incident response, kill-switch, annual risk review.

### 5.6 Observability `[addition: F-19]`

NFR-O-001…NFR-O-004 unchanged. Addition:
- **NFR-O-005** `[F-19]` — Input-distribution drift monitoring (per-feature PSI, KS test) on fraud and detection pipelines, not only output drift. Alert thresholds configured per feature.

---

## 6. Data Requirements

### 6.1 Entities (additions in v0.2)

| Entity | Fields (key) | Origin |
|---|---|---|
| **FNOL** `[M-01]` | `fnol_id`, `policy_id`, `incident_type`, `occurred_at`, `approximate_location`, `ncb_acknowledged`, `status`, `statutory_clock_start`. | FR-2.a |
| **SurveyorAssignment / SurveyorReport** `[M-02]` | `assignment_id`, `surveyor_licence_id`, `claim_id`, `sla_visit_due`, `report_due`, `report_fields`, `signature`. | FR-12 |
| **CashlessPreAuth / Settlement** `[M-03]` | `preauth_id`, `garage_id`, `amount_paise`, `gstn`, `variance`. | FR-13 |
| **RecoveryCase / SalvageCase** `[M-04, M-05]` | `case_id`, `source_claim_id`, `counterparty`, `status`, `amount_paise`, `milestones`. | FR-14 |
| **TPClaim** `[M-08]` | `tp_claim_id`, `source_claim_id`, `victim`, `injury_severity`, `fir_no`, `mact_case_no`. | FR-15 |
| **GrievanceCase** `[M-09]` | `grievance_id`, `policyholder_id`, `claim_id?`, `category`, `sla_due`, `status`, `resolution`. | FR-16 |
| **AmlAlert / StrDraft** `[M-14, M-15]` | `alert_id`, `claim_id`, `features`, `assignee`, `outcome`. | FR-17 |
| **ConsentArtefact** `[M-18]` | `artefact_id`, `principal_id`, `purpose`, `lawful_basis`, `granted_at`, `revoked_at?`, `cm_reference?`. | FR-18 |
| **IntakeChannel** `[M-32]` | `channel_id`, `type ∈ {mobile, agent, ivr, whatsapp, rcs}`, `agent_id?`. | FR-19 |
| **CatEventTag** `[M-07]` | `cat_event_id`, `description`, `date_range`, `region`. Claims carry 0..n cat-event tags. | §6.1 |
| **Reserve** `[M-25]` | `reserve_id`, `claim_id`, `case_reserve`, `ibnr_bucket`, `movement_history`. | §6.1 |
| **DoaMatrix** `[M-26]` | `role`, `lob`, `geo`, `approve_up_to_paise`, `four_eye_above_paise`. | FR-10 |
| **GarageScore** `[F-10]` | `garage_id`, `score`, `top_features`, `window`. | FR-5 |
| **CaptureManifest (C2PA)** `[F-11]` | `image_id`, `manifest_bytes`, `signer_cert_chain`, `verification_status`. | FR-6 |

Existing entities gain v0.2 fields: `Claim.fnol_id`, `Claim.intake_channel`, `Claim.cat_event_tags[]`, `Claim.reserve_id`, `Claim.tp_claim_id?`, `Claim.surveyor_assignment_id?`, `Explanation.model_family`, `FraudResult.band`, `FraudResult.layer1_hits[]`, `FraudResult.ring_risk`.

### 6.2 Data Flow (textual) — appended

```
...existing flow up to Routing Engine...
                    Routing Engine → RoutingDecision
                           │
             ┌─────────┬───┴────┬─────────┬───────────┬────────────┐
             ▼         ▼        ▼         ▼           ▼            ▼
        AUTO_APPROVE  HUMAN   FRAUD    NEEDS_MORE   AUTO_REJECT  SURVEYOR_REQUIRED  [M-02]
                     REVIEW  INVEST.    INFO                        │
             │         │        │          │            │           ▼
             │         │        │          │            │    Surveyor assigns, reports
             │         │        │          │            │           │
             └─────────┴───┬────┴──────────┴────────────┘           │
                           ▼                                         │
                    Adjuster decision  ◄──────────────────────────── ┘
                           │
                ┌──────────┼──────────────┐
                ▼          ▼              ▼
        Cashless [M-03]  Reimburse    Reject / Escalate
                │          │              │
                ▼          ▼              ▼
        Pre-auth,    Payout (penny-   Grievance  [M-09]
        settle @     drop [M-24])     Recovery/Salvage [M-04/05]
        garage                        AML/STR [M-14/15]
```

### 6.3 Storage — unchanged. Retention additions per `[M-11]`: cycle-time data retained separately for regulator-disclosure aggregation.

---

## 7. API Specifications

### 7.1 Endpoint list — v0.1 table retained; additions below.

| Method | Path | Auth | Description | Source |
|---|---|---|---|---|
| POST | `/v1/fnol` | JWT (policyholder) / system (IVR/WA/agent) | Create FNOL | `[M-01]` |
| POST | `/v1/fnol/{id}/convert` | JWT | Convert FNOL to formal claim | `[M-01]` |
| POST | `/v1/claims/{id}/tp-details` | JWT | Capture third-party details | `[M-08]` |
| POST | `/v1/surveyor/assignments/{id}/report` | JWT (surveyor) | Submit surveyor report | `[M-02]` |
| POST | `/v1/cashless/preauth` | system | Issue pre-authorisation | `[M-03]` |
| POST | `/v1/cashless/bills` | JWT (garage) | Submit final invoice | `[M-03]` |
| POST | `/v1/recovery/cases` | system | Open recovery case | `[M-04]` |
| POST | `/v1/salvage/cases` | system | Open salvage case | `[M-05]` |
| POST | `/v1/grievances` | JWT (policyholder/grievance) | Raise grievance | `[M-09]` |
| POST | `/v1/aml/alerts/{id}/str` | JWT (AML) | File STR | `[M-14]` |
| GET | `/v1/me/consents` | JWT | View consents | `[M-18]` |
| POST | `/v1/me/consents/{id}/revoke` | JWT | Revoke consent | `[M-18]` |
| POST | `/v1/me/data-rights/erasure` | JWT | Request erasure (dual-path) | `[U-09]` |
| POST | `/v1/admin/thresholds/change-requests` | JWT (admin) | Propose threshold change (PR-style) | `[U-11]` |
| POST | `/v1/admin/thresholds/change-requests/{id}/approve` | JWT (sr adj / AML) | Approve change request | `[U-11]` |
| POST | `/v1/claims/{id}/reopen` | JWT (sr adj / fraud / grievance) | Reopen a closed claim | `[M-06]` |
| POST | `/v1/integrations/vahan/lookup` | system | VAHAN query | `[M-20]` |
| POST | `/v1/integrations/iib/history` | system | IIB claim history | `[M-21]` |
| POST | `/v1/integrations/pennydrop/verify` | system | Bank penny-drop verification | `[M-24]` |
| POST | `/v1/ocr/extract` | system | OCR statutory documents | `[M-22]` |
| POST | `/v1/audit/anchor/publish` | system | Daily Merkle anchor to external TSA | `[U-18]` |

### 7.2 Example — FNOL creation `[M-01]`

**Request** `POST /v1/fnol`
```json
{
  "policy_identifiers": { "mobile": "+91XXXXXXXXXX", "policy_number": "MOT-2025-ABC-0042" },
  "incident_type": "collision",
  "occurred_at": "2026-04-20T17:42:00+05:30",
  "approximate_location": { "lat": 23.02, "lng": 72.57 },
  "brief_narrative": "Rear-ended at signal.",
  "ncb_acknowledged": true,
  "intake_channel": "mobile"
}
```

**201 Response**
```json
{
  "fnol_id": "FNOL-20260424-000877",
  "statutory_clock_start": "2026-04-24T11:02:00+05:30",
  "next_step": "convert-to-claim",
  "self_url": "/v1/fnol/FNOL-20260424-000877"
}
```

### 7.3 Error Code Families (additions)

`INTEG-0xx` integration · `E-SUR-0xx` surveyor · `E-CASH-0xx` cashless · `E-REC-0xx` recovery · `E-SAL-0xx` salvage · `E-TP-0xx` TP · `E-GRV-0xx` grievance · `E-AML-0xx` AML · `E-CNSNT-0xx` consent · `E-FNOL-0xx` FNOL · `E-OCR-0xx` OCR.

Envelope unchanged.

---

## 8. AI/ML Module Specifications

### 8.1 YOLOv8 — `[U-01, U-12]`
- Licensing path decided (§12 C-09): procure Ultralytics Enterprise licence **or** replace with RT-DETR / YOLO-NAS with compatible licence. Decision due before any code commits. `[U-12]`
- Targets revised per AC-3.1.1 (phased 0.60 → 0.70). `[U-01]`
- Other sections retained.

### 8.2 GPT-4o-class — `[U-03, U-13]`
- Provider- and model-agnostic interface `MultimodalExplainer`. `[U-13]`
- Determinism reframed as semantic equivalence, AC-7.1.1. `[U-03]`
- Annual migration budget to next-generation models; migrations pass the regression suite as a gate.

### 8.3 Fraud — `[F-01, F-02, F-03, F-04, F-05, F-16, F-18]`
- Three-layer architecture (FR-5).
- Champion-challenger shadow: challenger runs on 100 % traffic; daily comparison report. `[F-02]`
- Conformal-prediction calibration; abstain output. `[F-03]`
- Ring-cascade action wired. `[F-04]`
- GNN pipeline with weekly graph-rebuild. `[F-05]`
- Active-learning queue for borderline claims. `[F-16]`
- Monthly isotonic recalibration independent of retrain. `[F-18]`

### 8.4 Forensics — `[F-11..F-15]`
- C2PA verifier, Q-Tab analyser, noise-residual analyser, deepfake classifier (independent lifecycle, monthly retrain), PRNU (cost-gated).

### 8.5 Model & Prompt Versioning — `[M-30, F-17]`
- Registry unchanged in schema; additions:
  - Labelling SLA, inter-annotator agreement target (Cohen's κ ≥ 0.7), golden-set regression gate before promotion. `[M-30]`
  - Synthetic-data augmentation for rare-class balancing permitted, recorded with dataset provenance flag `synthetic=true`. `[F-17]`

### 8.6 Confidence Scoring — unchanged formula; weights live in versioned config; change-controlled. `[U-11]`

### 8.7 AI Governance — `[NEW — M-28, M-29, F-21, F-22, M-31]`
- **Model Risk Management framework** `[M-28]` — model inventory; approval gates (Data Science + Risk + Compliance sign-off); monitoring plan per model; kill-switch runbook (rollback registry pointer within 15 min); annual model risk review.
- **Bias / fairness protocol** `[M-29, F-21]` — disparate-impact testing pre-promotion on geography, vehicle class, language; FPR/FNR parity thresholds; weekly subgroup dashboards.
- **Red-team programme** `[F-22]` — adversarial corpus maintained by a red-team; every model change re-runs against it; regression gate.
- **Feedback loop pipeline** `[M-31]` — adjuster overrides, dispute outcomes, and grievance reversals are automatically labelled and flow into the next training round with provenance and quality scoring.

---

## 9. UI/UX Functional Flow

### 9.1 Mobile — Policyholder — extensions
- FNOL is a separate, short flow shown before detailed capture. `[M-01]`
- Offline mode explicitly indicated; queued uploads visible. `[U-17]`
- NCB impact disclosed on the FNOL confirmation screen. `[M-17]`
- Language picker includes at least `en | hi | gu`; WCAG 2.1 AA target per AMBIG-25.

### 9.2 Assisted / IVR / WhatsApp — new flows `[M-32, U-15]`
- Agent portal: search policy, capture FNOL on behalf of caller, generate OTP-confirmed consent.
- IVR: structured prompts to collect policy, incident type, location; produces FNOL; SMS deep-link for photo capture when the policyholder returns online.
- WhatsApp/RCS: button-driven FNOL; media attachments accepted with C2PA verification attempted (usually unverified due to platform compression — flag not fatal).

### 9.3 Web — Adjuster — additions
- Decomposed fraud reasons panel (per-feature-class contributions). `[F-20]`
- Claim-time KYC panel. `[U-08]`
- Surveyor report panel. `[M-02]`
- Cashless pre-auth and variance panel. `[M-03]`
- Reopen button (with reason) for closed claims. `[M-06]`
- Threshold change UI shows diff and submits a change request; does not save. `[U-11]`

### 9.4 Web — Surveyor, Grievance, AML, Finance — new focused consoles.

---

## 10. Reporting & Analytics

RPT-001…RPT-006 retained. Additions:

- **RPT-007 Leakage analytics** `[M-27]` — payment leakage by vendor, region, incident type, garage.
- **RPT-008 Subgroup fairness** `[F-21]` — FPR/FNR per region, vehicle class, language.
- **RPT-009 IRDAI cycle-time disclosure** `[M-11]` — monthly, prescribed format.
- **RPT-010 Feedback-loop quality** `[M-31]` — override-to-training-sample conversion rate, label disagreement.

---

## 11. Assumptions (revised)

Assumptions updated where the gap analysis retired an unrealistic one.

| # | Assumption | v0.2 status |
|---|---|---|
| A-01 | LOB is motor/auto; subject is 4-wheeler. | retained (pending AMBIG-00) |
| A-02 | Jurisdiction is India; INR; IRDAI and DPDP apply. | retained |
| A-03 | SSO exists for internal users (OIDC). | retained |
| A-04 (revised `[U-06]`) | Parts-catalog sourcing is a funded multi-source workstream, not a drop-in procurement. | revised |
| A-05 (revised `[U-10]`) | Third-party LLM is used under a DPA with contractual zero-retention, or sensitive inference is on-prem. | revised |
| A-06 (revised `[U-05]`) | A supervised damage dataset is a funded workstream with labelling operations; cold-start uses public + synthetic + progressive labelling. | revised |
| A-07 (revised `[U-08]`) | Upstream KYC may be stale; claim-time re-verification is required. | revised |
| A-08 | Payout execution integrates with an external payment rail. | retained |
| A-09 | Push notifications (FCM/APNs) available. | retained |
| A-10 | Internal users on MDM-managed devices (surveyors on their own devices with licence-bound tokens). | revised |
| A-11 `[NEW]` | IRDAI-empanelled surveyor directory is consumable via API or scheduled feed. | new |
| A-12 `[NEW]` | IIB / VAHAN / Consent Manager are integrable for the pilot geographies. | new |

---

## 12. Constraints (additions)

C-01…C-07 retained. Additions:
- **C-08 `[M-13]`** IRDAI Outsourcing Guidelines — cloud/LLM usage for claims data is a material outsourcing; notification and governance obligations apply.
- **C-09 `[U-12]`** Detector licensing — Ultralytics YOLOv8 AGPL-3.0 is not acceptable for commercial SaaS without an Enterprise licence; a compliant licence or alternative detector must be in place before code commits.
- **C-10 `[U-06]`** Parts catalog is a funded multi-source workstream, not a single-vendor procurement assumption.
- **C-11 `[F-11]`** Mobile platform must support capture-time cryptographic signing using device-bound keys.
- **C-12 `[U-18]`** External audit anchoring is mandatory in production; provider must be legally and technically independent of the System's operator.

---

## 13. Risks & Mitigations (refreshed)

v0.1 risks retained; additions:
- **R-13** Surveyor-panel availability in remote regions is a cycle-time risk → geographic panel planning; virtual-survey fallback where policy permits. `[M-02]`
- **R-14** Garage fraud collusion despite scoring → rotating audits of network garages; independent surveyor for high-variance cases. `[F-10]`
- **R-15** External-anchoring TSA outage blocks state transitions → redundant anchoring to two independent TSAs. `[U-18]`
- **R-16** LLM provider migration breaks explanation quality → abstraction + regression suite gate. `[U-13]`
- **R-17** C2PA adoption is uneven across platforms → signature absence is flagged, not fatal; provider-side compression is accommodated. `[F-11]`

---

## 14. Open Questions / Ambiguities

v0.1 AMBIG-00 through AMBIG-25 retained (unchanged status — these still require stakeholder sign-off). New ambiguities surfaced by v0.2:

| ID | Priority | Question | Affected | Owner |
|---|---|---|---|---|
| AMBIG-26 | P1 | Claim ID format — keep date-embedded `CLM-YYYYMMDD-NNNNNN` or move to opaque ID (to avoid incident-date leakage)? | FR-2 | Security |
| AMBIG-27 | P1 | Surveyor SLA targets (48h visit / 72h report assumed); regulator-mandated values may differ by state/LOB. | FR-12 | Compliance |
| AMBIG-28 | P1 | Cashless-settlement variance tolerance (±10 % assumed); contractual norms with garage network may differ. | FR-13 | Ops |
| AMBIG-29 | P0 | Auto-decision ceiling — the subset of Adjuster DOA for full automation (distinct from, and below, Adjuster DOA). | FR-8 | Claims |
| AMBIG-30 | P0 | Permitted LLM providers and deployment topology (on-prem vs cloud DPA) under IRDAI Outsourcing Guidelines. | NFR-S-013, C-08 | Compliance + Security |
| AMBIG-31 | P1 | Which TSA / anchoring provider(s) are acceptable for external audit anchor. | FR-11 | Compliance + SRE |
| AMBIG-32 | P1 | Consent Manager integration path — native DPDP Consent Manager vs insurer-internal. | FR-18 | Compliance |

---

## 15. Changelog — v0.1 → v0.2

All edits in v0.2 absorb items from `FRS-Gap-Analysis.md` v1.0.

### New functional requirements
FR-12 Surveyor Workflow `[M-02]` · FR-13 Cashless Settlement `[M-03]` · FR-14 Subrogation/Recovery/Salvage `[M-04, M-05]` · FR-15 Third-Party Liability `[M-08]` · FR-16 Grievance Management `[M-09]` · FR-17 AML / Sanctions / Regulatory Reporting `[M-11, M-14, M-15]` · FR-18 Consent & Data-Principal Rights `[M-18]` · FR-19 Multi-Channel Intake `[M-32, U-15]`.

### Substantive revisions
FR-2 split into FNOL + formal claim, tiered photo minimums, offline-first, C2PA signing, OCR, NCB disclosure, TP routing `[M-01, M-08, M-17, M-22, U-14, U-17, F-11]` · FR-5 rewritten to three-layer + graph + new features `[F-01..F-10, F-16..F-22]` · FR-6 extended with C2PA/Q-Tab/PRNU/deepfake/NR `[F-11..F-15]` · FR-8 adds SURVEYOR_REQUIRED, DOA-bounded auto-approval, non-runtime thresholds `[M-02, M-26, U-11]` · FR-9 adds claim-time KYC, sanctions, penny-drop, reopen `[M-06, M-15, M-24, U-08]` · FR-11 adds mandatory external anchoring and erasure dual path `[U-09, U-18]` · FR-1 multi-signal identity linking `[U-07]`.

### Acceptance criteria rewritten
AC-3.1.1 (YOLOv8 phased targets on insurer-specific set) `[U-01]` · AC-4.1.1 (distributional vs settled amounts) `[U-02]` · AC-7.1.1 (semantic equivalence, not byte-identical) `[U-03]` · AC-7.1.3 (10 s v1 / 6 s v2) and NFR-P-003 (45 s v1 / 30 s v2) with per-stage latency budget `[U-04]` · AC-9.1.2 (realistic override thresholds) `[U-16]`.

### NFR additions
NFR-S-012 IRDAI Cyber-Security-2023 `[M-12]` · NFR-S-013 LLM-provider data handling `[U-10]` · NFR-S-014 C2PA capture signing `[F-11]` · NFR-S-015 secret scanner · NFR-A-006 anchoring-outage degraded mode `[U-18]` · NFR-AC-006 cycle-time disclosures `[M-11]` · NFR-AC-007 MRM framework `[M-28]` · NFR-O-005 input drift `[F-19]`.

### Reports added
RPT-007 Leakage `[M-27]` · RPT-008 Subgroup fairness `[F-21]` · RPT-009 IRDAI cycle-time `[M-11]` · RPT-010 Feedback-loop quality `[M-31]`.

### Assumptions / Constraints refreshed
A-04, A-05, A-06, A-07, A-10 revised; A-11, A-12 added · C-08…C-12 added.

### RBAC additions
Surveyor, Grievance Officer, AML Officer, Finance Approver roles + four-eye / assignment-restricted semantics.

### New AMBIGs
AMBIG-26…AMBIG-32.

---

## 16. Traceability Matrix — Gap-Analysis → FRS v0.2

### Missing requirements (M)

| ID | Title | Absorbed into |
|---|---|---|
| M-01 | FNOL vs formal claim | FR-2.a, §6.1 FNOL entity, §7.1 `/v1/fnol`, §6.2 data flow |
| M-02 | Surveyor workflow | FR-12, FR-8 `SURVEYOR_REQUIRED`, §3 Surveyor role, §6.1 Surveyor entities, §7.1 surveyor endpoints, NFR-A, AMBIG-27 |
| M-03 | Cashless settlement | FR-13, §6.1 Cashless entities, §7.1 cashless endpoints, AMBIG-28 |
| M-04 | Subrogation / recovery | FR-14.1, §6.1 RecoveryCase, §7.1 recovery endpoint |
| M-05 | Salvage | FR-4 step 4c, FR-14.2, §6.1 SalvageCase, §7.1 salvage endpoint |
| M-06 | Re-open closed claim | FR-9 (reopen), §7.1 `/reopen`, RBAC additions |
| M-07 | Cat-event tagging | §6.1 CatEventTag, Claim extension |
| M-08 | Third-party liability | FR-15, FR-2.b TP capture, §6.1 TPClaim |
| M-09 | Grievance / IGMS | FR-16, §3 Grievance Officer role, §7.1 `/grievances`, §2.3 IGMS integration |
| M-10 | Coinsurance | FR-4 step 4b, AC-4.1.5 |
| M-11 | IRDAI cycle-time disclosure | NFR-AC-006, RPT-009 |
| M-12 | IRDAI Cyber-Security 2023 | NFR-S-012, §12 C-08 |
| M-13 | IRDAI Outsourcing Guidelines | §12 C-08, AMBIG-30 |
| M-14 | AML / STR | FR-17, §3 AML role, §7.1 AML endpoints |
| M-15 | Sanctions / PEP | FR-9 (screen), FR-17, FR-5 HR-04 |
| M-16 | TDS / withholding | FR-4 step 4a, AC-4.1.4 |
| M-17 | NCB disclosure | FR-2.a, AC-2.1.9 |
| M-18 | DPDP consent artefacts | FR-18, §6.1 ConsentArtefact, §7.1 consent endpoints |
| M-19 | PAS integration | §2.3 Integration Gateway |
| M-20 | VAHAN/SARATHI | §2.3, FR-5 external enrichment, §7.1 `/vahan/lookup` |
| M-21 | IIB | §2.3, FR-5 external enrichment, §7.1 `/iib/history` |
| M-22 | OCR | FR-2 Step 6, §2.3, §7.1 `/ocr/extract` |
| M-23 | Financial ledger | §2.3, FR-4, FR-14.3 |
| M-24 | Penny-drop | FR-9 validation, §2.3, §7.1 `/pennydrop/verify` |
| M-25 | Reserves / IBNR | §6.1 Reserve entity, FR-15 initial reserves |
| M-26 | DOA matrix | FR-8, FR-10, §6.1 DoaMatrix, §3 four-eye semantics |
| M-27 | Leakage analytics | RPT-007 |
| M-28 | MRM framework | §8.7, NFR-AC-007 |
| M-29 | Bias / fairness | §8.7, RPT-008 |
| M-30 | Labelling ops | §8.5 additions |
| M-31 | Feedback loop | §8.7, RPT-010 |
| M-32 | Multi-channel intake | FR-19, §9.2 |

### Fraud improvements (F)

| ID | Title | Absorbed into |
|---|---|---|
| F-01 | Three-layer architecture | FR-5 Description, Main Flow, AC-5.1.1; FR-8 rule 0 |
| F-02 | Champion-challenger shadow | §8.3 |
| F-03 | Conformal abstain | FR-5 Layer 2, AF-5.4, AC-5.1.3, FR-8 rule 2 |
| F-04 | Ring-cascade action | FR-5 Main Flow §5, AF-5.5, AC-5.1.4 |
| F-05 | GNN graph scoring | FR-5 Layer 3, AC-5.1.2 |
| F-06 | External enrichment | FR-5 features, §2.3 |
| F-07 | Behavioural biometrics | FR-5 features, FR-18 consent gate |
| F-08 | Device intelligence | FR-5 features, §2.3 |
| F-09 | Narrative↔evidence NLI | FR-5 features |
| F-10 | Garage score | FR-5 features, §6.1 GarageScore, FR-13 gate |
| F-11 | C2PA capture-time signing | FR-2, FR-6 Step 1a, NFR-S-014, AC-6.1.4 |
| F-12 | JPEG Q-Tab signatures | FR-6 Step 2a |
| F-13 | PRNU | FR-6 Step 6, AC-6.1.6 |
| F-14 | Deepfake (separate lifecycle) | FR-6 Step 5, AC-6.1.5 |
| F-15 | Noise-residual | FR-6 Step 2b |
| F-16 | Active learning | §8.3 |
| F-17 | Synthetic augmentation | §8.5 |
| F-18 | Rolling recalibration | §8.3 |
| F-19 | Input drift monitoring | NFR-O-005 |
| F-20 | Decomposed adjuster reasons | FR-5 Main Flow §4, AC-5.1.5, §9.3 |
| F-21 | Subgroup/bias monitoring | §8.7, RPT-008, AC-5.1.6 |
| F-22 | Red-team programme | §8.7 |

### Unrealistic-assumption replacements (U)

| ID | Replacement landed in |
|---|---|
| U-01 | AC-3.1.1 revised (phased 0.60 → 0.70 on insurer-specific set) |
| U-02 | AC-4.1.1 revised (distributional vs settled amounts) |
| U-03 | AC-7.1.1 revised (semantic equivalence regression suite) |
| U-04 | AC-7.1.3 revised + §5.1 per-stage latency budget (45 s v1 / 30 s v2) |
| U-05 | A-06 revised (funded dataset workstream) |
| U-06 | A-04 revised + C-10 + FR-4 catalog_source field |
| U-07 | FR-1 multi-signal linking ladder, AF-1.4, AC-1.1.5 |
| U-08 | FR-9 claim-time KYC, AC-9.1.6; A-07 revised |
| U-09 | FR-11 erasure dual path, AC-11.1.5, FR-18 |
| U-10 | NFR-S-013 |
| U-11 | FR-8 thresholds non-runtime, FR-10 AC-10.1.2, §7.1 change-request endpoints |
| U-12 | C-09; §8.1 licensing path |
| U-13 | §8.2 model-agnostic abstraction |
| U-14 | FR-2.b tiered minimums, AC-2.1.8 |
| U-15 | FR-19, §9.2 |
| U-16 | AC-9.1.2 revised |
| U-17 | FR-2 offline-first, AC-2.1.6 |
| U-18 | FR-11 mandatory external anchor, NFR-A-006, C-12, AC-11.1.4 |

---

*— Document end —*
