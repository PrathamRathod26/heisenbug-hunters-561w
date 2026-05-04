# FRS Gap Analysis & Review Commentary

**Companion document to** `FRS.md` (v0.1 Draft, 2026-04-24)

| Field | Value |
|---|---|
| Project | AI-Powered Insurance Claims Processing Platform |
| Team | Heisenbug Hunters |
| Hackathon | Amnex Infotechnologies — Sarjan (April 2026) |
| Document Version | 1.0 |
| Date | 2026-04-24 |
| Audience | Product, Engineering, Compliance, Data Science, Claims Ops |
| Status | Client-ready |

---

## Executive Summary

This document supplements the Functional Requirements Specification with three independent analyses:

1. **Missing requirements** — 32 items an enterprise-grade insurance platform is expected to address that the current FRS does not, grouped by theme (Lifecycle, Regulatory, Integration, Financial Controls, AI Governance, Customer Experience, Security).
2. **Fraud-detection logic improvements** — 22 specific enhancements beyond the current baseline (behavioral biometrics, graph analytics, external-data enrichment, evidence provenance, model governance).
3. **Unrealistic assumptions** — 18 assumptions in the FRS that, based on industry evidence, are aspirational or will fail under production conditions; each is paired with a realistic re-statement.

Each item is numbered, prioritised, and tied to the FRS section it extends or contradicts so it can be actioned directly.

**Priority key (applies throughout):** **P0** = must resolve before design freeze · **P1** = must resolve before first production deploy · **P2** = resolve within 90 days of go-live.

---

## 1. Missing Requirements (Enterprise Insurance Perspective)

### 1.1 Claim-Lifecycle Completeness

| # | Gap | Priority | Extends | Note |
|---|---|---|---|---|
| **M-01** | **FNOL vs formal claim separation.** A First Notice of Loss is a legally distinct intake that starts statutory clocks; today the FRS collapses FNOL into "claim submission" and loses that distinction. | P0 | FR-2 | IRDAI timelines and many policy wordings trigger on FNOL, not submission. |
| **M-02** | **Surveyor / Loss Assessor workflow.** For motor claims above IRDAI-notified thresholds, an IRDAI-licensed surveyor is mandatory; the System must assign, track, and capture their report. | P0 | FR-9 | Without this the platform cannot process claims above the threshold legally. |
| **M-03** | **Cashless settlement at network garages.** Integration with the repair-shop network for pre-authorisation, bill submission, and direct settlement (vs reimbursement). | P0 | FR-9, §6 | Cashless is the dominant mode in Indian motor insurance. |
| **M-04** | **Recovery / Subrogation workflow.** When a third party is at fault, recovery tracking, demand letters, and legal handover. | P1 | FR-9 | Material for loss-ratio management; currently absent. |
| **M-05** | **Salvage management.** For `total_loss_candidate` cases: salvage tagging, bidding, realisation. | P1 | FR-4 | Drives ~10–20 % of effective payout reduction. |
| **M-06** | **Re-opening a closed claim.** Late bills, missed injuries, legal escalation after `CLOSED_NO_ACTION` — the current state machine has no re-open path. | P1 | FR-9 | |
| **M-07** | **Catastrophe / event tagging.** All claims from a single flood/storm need a shared `cat_event_id` for reinsurance and ops. | P1 | §6.1 | |
| **M-08** | **Third-party liability (TP) claims.** TP claims are structurally different from Own-Damage: Motor Accident Claims Tribunal handling, long-tail reserving, legal workflow. Current FRS assumes own-damage only. | P0 | FR-2 | |
| **M-09** | **Grievance redressal workflow.** IRDAI mandates a grievance channel with SLAs (Integrated Grievance Management System — IGMS). | P0 | §1.2 | |
| **M-10** | **Coinsurance / contribution clause handling.** Where liability is shared across insurers. | P2 | FR-4 | |

### 1.2 Regulatory & Statutory

| # | Gap | Priority | Extends | Note |
|---|---|---|---|---|
| **M-11** | **IRDAI Claim Settlement Timelines & Public Disclosures.** Mandatory reporting of claim cycle-time buckets per quarter. | P0 | NFR-AC | Regulator file-and-use. |
| **M-12** | **IRDAI Information & Cyber Security Guidelines (2023).** Specific control list goes beyond the generic OWASP/ISO mentions; requires explicit mapping. | P0 | NFR-S | |
| **M-13** | **IRDAI Outsourcing Guidelines.** Use of public-cloud AI/LLM providers for claims data triggers outsourcing-notification and material-outsourcing rules. | P0 | §12 | GPT-4o Vision use case is directly implicated. |
| **M-14** | **AML / KYC re-verification & Suspicious Transaction Reports (STR).** Reporting obligations to FIU-IND for suspicious payouts. | P1 | FR-5 | |
| **M-15** | **Sanctions / PEP screening.** Screen claimant against UN/OFAC/MEA lists before payout. | P1 | FR-9 | |
| **M-16** | **TDS / withholding tax on payouts** (e.g., Sec 194N / 194DA triggers in certain cases). | P1 | FR-4 | |
| **M-17** | **NCB (No Claim Bonus) impact disclosure.** Statutory requirement to inform policyholder of NCB loss before they confirm submission. | P1 | FR-2 | |
| **M-18** | **DPDP Consent Artefacts.** Explicit, purpose-scoped consent records; data-principal-rights portal with verifiable identity; Consent Manager integration under DPDPA 2023. | P0 | NFR-AC | "Consent captured" is too loose today. |

### 1.3 Core-System & Data Integrations

| # | Gap | Priority | Extends | Note |
|---|---|---|---|---|
| **M-19** | **Policy Administration System (PAS) integration contract.** Read-only sync vs bi-directional; reserve write-back; endorsement events. Currently unspecified. | P0 | §2.1 | |
| **M-20** | **VAHAN / SARATHI (RTO database) lookup.** Confirm vehicle registration status, ownership, fitness certificate validity. | P1 | FR-5 | Reduces identity-and-ownership fraud. |
| **M-21** | **Insurance Information Bureau of India (IIB) claim-history check.** Cross-insurer claim history — significant fraud signal missed today. | P0 | FR-5 | |
| **M-22** | **OCR for statutory documents.** RC book, driving licence, FIR, hospital bills, invoice matching with GSTN. | P1 | FR-2 | |
| **M-23** | **Financial-ledger integration (SAP/Oracle/Tally).** Claim approved → payable posted → reserve released — currently hand-waved. | P0 | §6 | |
| **M-24** | **Bank-account verification (penny-drop / NPCI).** Before first payout to a newly-added account. | P0 | FR-9 | Primary payout-fraud vector. |

### 1.4 Financial Controls & Reserving

| # | Gap | Priority | Extends | Note |
|---|---|---|---|---|
| **M-25** | **Claim reserve accounting.** Case reserve, IBNR (Incurred But Not Reported), and movement reports — core to insurance P&L. | P0 | §6, §10 | |
| **M-26** | **Delegation-of-authority (DOA) matrix.** Approval limits by role, LOB, geography; four-eye above ceiling; maker-checker logs. Only partially covered (FR-8/10). | P0 | FR-10 | |
| **M-27** | **Leakage analytics.** Payment leakage by segment (vendor, region, incident type) — a named reporting view. | P1 | §10 | Missing RPT. |

### 1.5 AI Governance (Enterprise-Grade)

| # | Gap | Priority | Extends | Note |
|---|---|---|---|---|
| **M-28** | **AI/Model Risk Management framework.** Model approval board sign-offs (Data Science + Risk + Compliance), AI risk register, kill-switch procedure, an emergency model-rollback SLA, post-incident review template. | P0 | §8 | Regulators now expect this for high-impact AI. |
| **M-29** | **Bias / fairness testing.** Disparate-impact analysis across geography, vehicle class, language of narrative; documented acceptance thresholds. | P0 | §8 | Absent today. |
| **M-30** | **Ground-truth labelling operations.** Labelling SLA, inter-annotator agreement targets, golden set that a new model must match before promotion. | P1 | §8.5 | |
| **M-31** | **Feedback loop as first-class requirement.** Adjuster overrides and customer disputes automatically become labelled training data with provenance. | P1 | FR-9, §8 | Implicit today; needs an explicit pipeline. |

### 1.6 Customer Experience & Inclusion

| # | Gap | Priority | Extends | Note |
|---|---|---|---|---|
| **M-32** | **Multi-channel intake.** IVR, WhatsApp/RCS, agent-initiated (bulk capture by a field agent for low-digital-literacy customers). Mobile-only is exclusionary for large segments of the Indian market. | P0 | §9 | |

---

## 2. Fraud-Detection Logic — Recommended Improvements

The current engine (FR-5/FR-6) is a reasonable v1 but lags adversarial practice. Recommendations below are ordered by expected lift.

### 2.1 Architecture

| # | Improvement | Priority | Expected Lift | Note |
|---|---|---|---|---|
| **F-01** | **Three-layer architecture.** L1 deterministic hard-rules (fast, explainable, legally defensible blocks), L2 calibrated-probability ML, L3 prioritisation for human review (GNN + anomaly). Today's engine conflates L2 and L3. | P0 | High | Separates legal-defensible blocks from probabilistic flags. |
| **F-02** | **Champion-challenger live shadow.** Always run a challenger model on 100 % of traffic; compare precision/recall daily. | P1 | Medium | |
| **F-03** | **Conformal prediction / abstain option.** Model may return `UNCERTAIN` rather than a forced score; routes to human review with a reason. | P1 | Medium | |
| **F-04** | **Fraud-ring cascade action.** When one claim is confirmed fraud, automatically raise fraud scores on graph-linked open claims and re-evaluate routing. | P0 | High | Ring-level recovery, not just per-claim. |

### 2.2 Signals & Features

| # | Improvement | Priority | Expected Lift | Note |
|---|---|---|---|---|
| **F-05** | **Graph/GNN-based scoring.** Build a claimant ↔ policy ↔ garage ↔ vendor ↔ bank-account ↔ adjuster graph; use community detection and personalised PageRank as features. | P0 | High | Catches organised fraud the current feature set misses. |
| **F-06** | **External-data enrichment.** IIB claim history, Vahan stolen-vehicle registry, FIR/police-case database, weather/flood/hail event data (to validate "natural event" claims by region + date). | P0 | High | See M-20/M-21. |
| **F-07** | **Behavioural biometrics at submission.** Typing cadence, scroll patterns, swipe pressure, time-on-screen; fraud rings often batch-submit. | P1 | Medium | Vendor: BioCatch/equivalent. |
| **F-08** | **Device intelligence.** Root/jailbreak detection, emulator detection, VPN/Tor/exit-node, device-fingerprint reputation. Currently only `device_id` is captured. | P0 | Medium | |
| **F-09** | **Narrative ↔ evidence consistency model.** A dedicated NLI-style model that checks whether the narrative is supported by the photos (not just a prompt-side instruction). | P1 | Medium | |
| **F-10** | **Repair-garage scoring.** Per-garage fraud propensity index (inflation of quotes, repeat-at-same-garage ratio, part-substitution rate). | P0 | High | Garages are a top fraud vector. |

### 2.3 Image & Evidence Forensics

| # | Improvement | Priority | Expected Lift | Note |
|---|---|---|---|---|
| **F-11** | **C2PA / Content Authenticity Initiative provenance.** Sign image bytes at capture time with a device-bound key; verify chain at submission. Bypasses the EXIF-stripping problem. | P0 | High | Eliminates a major forensic blind spot. |
| **F-12** | **JPEG quantisation-table signatures.** Identify camera/editor signatures; detect re-encoding by known editors (Photoshop, WhatsApp compression). | P1 | Medium | |
| **F-13** | **PRNU (sensor-fingerprint) analysis.** Link images to the specific sensor that captured them; detects cross-device fabrication. | P2 | Medium | Compute-intensive; reserve for high-value cases. |
| **F-14** | **Dedicated AI-generated-image (deepfake) detector with periodic re-training.** Currently mentioned as a sub-score; needs its own lifecycle (threat evolves monthly). | P0 | High | |
| **F-15** | **Noise-residual / SRM-based forensics.** Complements ELA; catches copy-move forgeries ELA misses. | P1 | Medium | |

### 2.4 Data, Labelling, Drift

| # | Improvement | Priority | Expected Lift | Note |
|---|---|---|---|---|
| **F-16** | **Weak-supervision / active learning.** Prioritise uncertain borderline claims for adjudication; accelerates labelled corpus growth in a domain that starts data-poor. | P1 | Medium | |
| **F-17** | **Synthetic data augmentation for rare classes.** Generative augmentation for tampered/AI-generated examples (with care that synthetic labels don't leak). | P2 | Low | |
| **F-18** | **Rolling recalibration.** Monthly isotonic recalibration, not just retrain; keeps score semantics stable as class priors drift. | P1 | Medium | |
| **F-19** | **Concept-drift detection on inputs.** Feature-distribution drift alerts (PSI per feature, not just output). | P1 | Medium | Current FRS monitors output distribution only. |

### 2.5 Explainability & Governance

| # | Improvement | Priority | Expected Lift | Note |
|---|---|---|---|---|
| **F-20** | **Decomposed adjuster-facing reasons.** Show the contribution of (photo-forensics, behavioural, network, narrative, external) as separate visible components, not one blended score. | P0 | High | Adjusters reject opaque scores. |
| **F-21** | **Subgroup / bias monitoring.** Weekly false-positive-rate dashboard by region, language, vehicle class; alert thresholds tied to DPDP fair-processing obligations. | P0 | — | Required for governance. |
| **F-22** | **Red-team programme with adversarial corpus.** Dedicated evaluation set of real and engineered fraud attempts; every model change re-runs against it as a gate. | P0 | High | Fraud is adversarial — static test sets decay. |

---

## 3. Unrealistic Assumptions in the Inferred Use Case

Each item names the assumption, why it fails in practice, and a pragmatic replacement. Priority reflects the risk of the assumption surviving into production.

### 3.1 Model-Quality Assumptions

| # | Current Assumption | Why It Is Unrealistic | Realistic Replacement | Priority |
|---|---|---|---|---|
| **U-01** | YOLOv8 mAP@0.5 ≥ 0.80 on dent/scratch/broken_glass at launch (AC-3.1.1). | Public damage corpora (CarDD, Stanford cars, COCO-Damage) cap at ~0.55–0.75 on targeted classes; novel real-world distribution is harder. Without a 6-figure labelling budget, 0.80 on day one is aspirational. | Phase targets: 0.60 at v1, 0.70 by month 6 on an insurer-specific held-out set; gate thresholds to measured accuracy; keep human-review fallback broad. | **P0** |
| **U-02** | Cost estimate within ±10 % of a human-adjuster baseline (AC-4.1.1). | Inter-adjuster disagreement itself exceeds ±10 % on a material share of claims (industry studies: ~25–35 % of claims show ±15 % spread between two qualified adjusters). Using a human baseline as a point-estimate target is poorly defined. | Re-frame AC as: the **distribution** of model-vs-adjuster deltas must have 70 % of claims within ±15 %, median absolute error bounded; measure against *settled* amounts, not initial adjuster estimates. | **P0** |
| **U-03** | GPT-4o Vision produces **byte-identical** outputs given `temperature=0` + pinned seed (AC-7.1.1). | Hosted LLMs do not guarantee byte-determinism. Token-level non-determinism from batching, kernel variance, and provider-side routing is documented. | Keep byte-identical as a desired but soft property; make the primary AC "semantic equivalence per reference evaluator on a 100-case regression set, ≥ 95 % pass". | **P0** |
| **U-04** | P95 end-to-end AI pipeline ≤ 30 s including GPT-4o Vision (NFR-P-003, AC-7.1.3). | Vision-model calls with ≥ 4 images commonly take 5–15 s P95 alone; plus YOLOv8 per-image + cost + fraud + forensics. 30 s is feasible only if forensics and explanation run in parallel on a warm, dedicated stack. | Publish a latency budget per stage, explicitly allow degraded mode (explanation trails the decision by ≤ 60 s), and target 30 s only after tuning. | **P1** |

### 3.2 Data & Integration Assumptions

| # | Current Assumption | Why It Is Unrealistic | Realistic Replacement | Priority |
|---|---|---|---|---|
| **U-05** | A labelled damage dataset is "available or procurable" (A-06). | Quality proprietary datasets (Mitchell, Audatex, Solera) are expensive; building in-house requires 10–50 k labelled images and 6–12 weeks of annotator work. | Treat data acquisition as a named workstream with budget, not an assumption. Include cold-start plan using public + synthetic + progressive labelling. | **P0** |
| **U-06** | A parts-and-labour catalog for India is procurable (A-04). | Comprehensive Indian parts catalogs are proprietary and costly; OEM dealer networks guard pricing; regional variance is large. | Multi-source strategy (OEM partnerships + scraped/negotiated + human correction); start with top-selling 20 vehicles covering ~70 % of claims. | **P0** |
| **U-07** | Policyholder mobile number uniquely identifies the policy (FR-1). | Shared family phones, changed numbers, data-entry errors — a meaningful minority of customers (~3–7 %) fail the mobile-match. | Multi-signal linking (mobile + DOB + policy number) with a soft-match + hard-match ladder; explicit unresolved-link workflow. | **P1** |
| **U-08** | Policyholder KYC was completed upstream and the System only verifies at login (A-07). | Claims often surface KYC gaps (address mismatch, expired ID); re-verification is routinely required before payout. | Add a claim-time KYC check and an exceptions workflow. | **P1** |

### 3.3 Regulatory / Legal Assumptions

| # | Current Assumption | Why It Is Unrealistic | Realistic Replacement | Priority |
|---|---|---|---|---|
| **U-09** | Right-to-erasure implemented as PII redaction-in-place (FR-11 Validation). | DPDP requires *erasure* absent a lawful retention basis; redaction may or may not satisfy it depending on regulator interpretation and retention obligation overlap. | Legal opinion required. Implement true deletion for data not under a lawful retention obligation, and redaction for data that is; both paths audit-logged. | **P0** |
| **U-10** | Sending face-masked images to a third-party LLM satisfies data-protection requirements (NFR-S-007). | Provider data-handling policies typically log inputs for up to 30 days for abuse monitoring, regardless of training opt-out. Combined with other signals in the payload, re-identification is not ruled out. | Use a DPA that specifies zero-retention; prefer regional/self-hosted deployments; keep sensitive inference entirely on-prem where feasible (e.g., damage detection). | **P0** |
| **U-11** | Thresholds (auto-approve ceiling, fraud cut-off) are a runtime Admin knob (FR-10). | IRDAI model/product approval regimes (and the AI regulatory trend globally) will require fixed thresholds between approved versions; runtime mutation is a governance red flag. | Thresholds are change-controlled artefacts with versioned approvals (already partially modelled); strip runtime edit UI in production; edits become pull requests with audit. | **P1** |
| **U-12** | YOLOv8 is usable as-is (FRS §8.1). | Ultralytics YOLOv8 is AGPL-3.0 by default; commercial/SaaS use without a paid Enterprise licence creates copyleft exposure. | Either procure the Enterprise licence or switch to a permissively-licensed detector (RT-DETR, MMDetection-based alternatives, YOLO-NAS with its own terms). Resolve before code commits. | **P0** |
| **U-13** | "GPT-4o Vision" will be available for the project's production lifetime (FRS §8.2). | OpenAI deprecates models on ~12-month cycles; the name/ID will shift. | Architect a provider-and-model-agnostic interface; pin by capability not by name; budget for migrations annually. | **P1** |

### 3.4 Operational Assumptions

| # | Current Assumption | Why It Is Unrealistic | Realistic Replacement | Priority |
|---|---|---|---|---|
| **U-14** | 3 photos are a sufficient minimum for reliable detection + estimation (FR-2). | For multi-panel damage or total-loss judgement, 8–12 photos are typically needed; 3 produces systematic underestimation. | Tiered minimums by incident type (collision: 6, theft: varies, natural_event: 4); AI-guided "you still need these angles" feedback. | **P1** |
| **U-15** | Mobile-first coverage is sufficient (FRS §9.1). | Large segments of Indian policyholders still file claims through agents, call centres, or physical offices; mobile-only excludes them. | Multi-channel intake as a first-class requirement (see M-32). | **P0** |
| **U-16** | Adjuster overrides will be rare and outliers (implicit in AC-9.1.2 analytics alert). | Year-one override rates in AI-triage insurance deployments are widely reported at 20–40 %; this is the normal learning-phase cost. | Instrument and tolerate, not alarm. Recast analytics alert at override > 50 % at claim level or > 30 % week-over-week trend shift. | **P1** |
| **U-17** | Stable 4G connectivity at submission time (AC-2.1.3). | Claims often originate at accident sites with poor signal. | Offline-first submission with deferred upload is a functional requirement, not a nice-to-have. See `AMBIG-24`. | **P0** |
| **U-18** | Hash-chained audit log + optional external anchoring is sufficient for tamper evidence (FR-11, `AMBIG-15`). | If the same operator controls the signing key and the log store, a parallel chain is forgeable. Optional anchoring is not enough for a regulator-facing claim. | External anchoring is mandatory, not optional, for production: publish daily Merkle root to an independent notary / public timestamp service. | **P0** |

---

## 4. Consolidated Action List (Recommended Next 30 Days)

Items are actions, not restatements. Owners are suggestions.

| # | Action | Owner | Gates | Sources |
|---|---|---|---|---|
| **A-01** | Confirm or correct `AMBIG-00` (scope, jurisdiction, LOB). | Product | All downstream work | §14, M-08 |
| **A-02** | Commission legal opinion on (a) DPDP erasure vs redaction, (b) IRDAI outsourcing implications of cloud LLM usage. | Compliance + Legal | Before any production LLM call | U-09, U-10, M-13 |
| **A-03** | Procure or select YOLOv8 licence path (Enterprise vs alternative detector). | Engineering | Before code commits | U-12 |
| **A-04** | Scope IIB + VAHAN + IGMS + bank-penny-drop integrations. | Integrations | FR-5 / FR-9 | M-09, M-20, M-21, M-24 |
| **A-05** | Formalise AI/Model Risk Management framework and bias-testing protocol. | Data Science + Risk | First model promotion | M-28, M-29, F-21 |
| **A-06** | Re-write model-quality acceptance criteria (AC-3.1.1, AC-4.1.1, AC-7.1.1) against realistic distributional targets. | Data Science + QA | Before acceptance testing | U-01, U-02, U-03 |
| **A-07** | Redesign fraud engine into three layers (F-01) and add graph features (F-05). | Data Science | Before first fraud-route promotion | F-01, F-05 |
| **A-08** | Add FNOL vs formal-claim separation to the state machine. | Engineering | Before onboarding the first insurer | M-01 |
| **A-09** | Adopt C2PA-style capture-time signing in the mobile app. | Mobile + Security | Before first fraud rules depend on EXIF | F-11 |
| **A-10** | External audit-anchoring to a public timestamp service. | SRE + Compliance | Before production | U-18 |

---

## 5. Notes on Use

- This document does not change the FRS. It *adds* to it. Accepted items should be absorbed into the next FRS revision (v0.2) with their identifiers preserved for traceability.
- Items marked P0 should be cleared before design freeze; items marked P1 before the first production deployment.
- The gap analysis assumes the inferred use case in FRS §1.2 is correct. If `AMBIG-00` resolves differently (e.g., non-motor LOB, non-India jurisdiction), the regulatory and integration items here require re-evaluation.

---

*— Document end —*
