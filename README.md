# LegalDoc QA — Contract Compiler + Counsel Workflow

> **Anduin Transactions Challenge** | *"We treat contracts like source code: compile, lint, and only send lawyers proven errors."*

A backend system that automatically analyses legal contract documents (DOCX/PDF), detects issues (broken references, undefined terms, value conflicts, date logic errors), manages the full review lifecycle, and routes complex findings to external counsel via a token-based Q&A workflow.

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Key Differentiators](#2-key-differentiators)
3. [Architecture Overview](#3-architecture-overview)
4. [Tech Stack](#4-tech-stack)
5. [Folder Structure](#5-folder-structure)
6. [Quick Start](#6-quick-start)
7. [End-to-End Test Flow](#7-end-to-end-test-flow)
8. [API Reference](#8-api-reference)
9. [Entity Model](#9-entity-model)
10. [Analysis Pipeline](#10-analysis-pipeline)
11. [Finding State Machine](#11-finding-state-machine)
12. [Design Decisions & Challenges](#12-design-decisions--challenges)

---

## 1. Problem Statement

Investment fund contracts (LPA, Subscription Agreement, Side Letters) are complex documents reviewed manually before digitization. Reviewers must catch:

- **Broken cross-references** — "as per Section 7.4" but Section 7.4 does not exist
- **Undefined terms** — "Limited Partner" used but never formally defined
- **Value conflicts** — Management Fee defined as 2% in section 2.1 but 2.5% in section 2.2
- **Date logic errors** — Fund termination date is before the Closing Date
- **Words vs. digits mismatch** — "One Hundred Thousand Dollars ($1,000,000)"

Missing any of these errors means the entire downstream digitization team works from incorrect input and must redo their work.

**The solution:** A system that reads the contract, builds a structured clause tree, runs a deterministic rule engine to detect errors with evidence, and manages the complete lifecycle from detection to lawyer confirmation.

---

## 2. Key Differentiators

| Point | Description |
|-------|-------------|
| **Contract = Source Code** | Parser builds AST (clause tree) + symbol table (defined terms). Rule engine reports errors like a compiler: location, evidence, message |
| **Deterministic First, AI Optional** | Core rules are pure Java — results are reproducible and testable with JUnit. Gemini AI is a P2 plugin |
| **Evidence-Based Findings** | Every finding quotes verbatim text from the document with character offsets and page number |
| **Complete Workflow** | `DETECTED → CONFIRMED → SENT_TO_COUNSEL → RESOLVED`, with full audit trail |
| **Version Diffing** | Upload v2 and the system shows which errors are fixed, still open, or newly introduced (fingerprint-based) |
| **Measurable Quality** | Benchmark suite with known-error contracts measures precision/recall per rule |

---

## 3. Architecture Overview

```
HTTP Clients (Swagger UI / test-ui.html)
                  |
          Spring Boot 3 REST API
          /api/auth  /api/contracts  /api/versions  /api/findings
                  |
         Service Layer (business logic)
                  |
       Analysis Pipeline (@Async)
       DocxReader / PdfReader
       ClauseTreeBuilder (regex parser)
       ModelBuilder (extract terms, refs, values)
       RuleEngine (run enabled rules)
       Persist findings → markReviewReady()
                  |
         PostgreSQL 16 + Flyway
         Redis 7 (counsel tokens, rate limiting)
```

---

## 4. Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 (Web, Data JPA, Security, Validation, Actuator) |
| Document Reading | Apache POI 5.3 (DOCX), Apache PDFBox 3.0.4 (PDF) |
| Analysis | Java regex, `java.time`, ICU4J 76 (number-word parsing) |
| Database | PostgreSQL 16 + Flyway 10 (schema migrations) |
| Cache / Tokens | Redis 7 |
| Authentication | Spring Security stateless JWT via JJWT 0.12.6 |
| API Docs | SpringDoc OpenAPI 2.8 (Swagger UI auto-generated) |
| Testing | JUnit 5, Testcontainers, Spring Security Test |
| Infrastructure | Docker, Docker Compose |
| Build | Maven 3 (Maven Wrapper included) |

---

## 5. Folder Structure

```
LegalDoc-QA/
├── backend/
│   ├── src/main/java/com/contractcompiler/
│   │   ├── auth/                     # JWT auth, BCrypt, REVIEWER/ADMIN roles
│   │   ├── contract/                 # Upload, versioning, storage
│   │   ├── analysis/
│   │   │   ├── AnalysisPipeline.java # @Async orchestrator (entry point)
│   │   │   ├── ingest/               # DocxReader, PdfReader, RawLine
│   │   │   ├── parse/                # ClauseTreeBuilder, ModelBuilder
│   │   │   ├── model/                # ContractModel, DefinedTerm, CrossReference
│   │   │   └── persistence/          # JPA entities for analysis output
│   │   ├── rules/
│   │   │   ├── Rule.java             # Interface every rule implements
│   │   │   ├── RuleEngine.java       # Runs all enabled rules
│   │   │   ├── RuleIds.java          # String constants for rule IDs
│   │   │   └── impl/                 # RefBrokenRule, TermUndefinedRule, ...
│   │   ├── finding/                  # State machine, audit trail
│   │   ├── counsel/                  # Question packages, external counsel
│   │   ├── benchmark/                # Precision/recall tracking (ADMIN)
│   │   └── common/                   # Security, config, exceptions, utils
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/             # V1..V7 Flyway SQL migrations
│   ├── Dockerfile
│   └── pom.xml
├── docs/
│   └── 01-system-overview.md
├── samples/                          # Sample contracts with known errors
├── test-ui.html                      # Single-file test UI (no build required)
├── docker-compose.yml
├── .env
└── .env.example
```

---

## 6. Quick Start

### Prerequisites

- Docker Desktop (recommended — zero local setup required)
- OR: JDK 21, Maven 3, PostgreSQL 16, Redis 7

### Option A — Docker Compose (recommended)

```bash
# 1. Clone the repo
git clone <repo-url>
cd LegalDoc-QA

# 2. Copy environment file
cp .env.example .env
# Defaults work out of the box for local dev

# 3. Start everything
docker compose up

# Services:
#   PostgreSQL  → localhost:5436
#   Redis       → localhost:6379
#   Backend API → localhost:8080
```

### Option B — Run backend locally

```bash
# Make sure PostgreSQL (port 5436) and Redis (port 6379) are running
cd backend
./mvnw spring-boot:run
```

### Verify

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Open Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

### Open Test UI

```bash
# From project root (requires Node.js):
npx serve . -p 3000
# Then open: http://localhost:3000/test-ui.html
```

---

## 7. End-to-End Test Flow

### Preset Accounts (seeded automatically by Flyway V7)
If cannot Login by these accounts, please try to register the new one

| Email | Password | Role |
|-------|----------|------|
| `admin@test.com` | `Admin1234!` | ADMIN |
| `reviewer@test.com` | `Review1234!` | REVIEWER |

### Happy Path

| Step | Endpoint | What Happens |
|------|----------|-------------|
| 1 | `POST /api/auth/login` | Login → copy JWT token → click Authorize in Swagger |
| 2 | `POST /api/contracts` | Upload `.docx` or `.pdf` file with a title |
| 3 | `GET /api/contracts/{id}` | Poll until `status = REVIEW_READY` (pipeline runs async) |
| 4 | `GET /api/versions/{vid}/findings` | View all detected findings with evidence |
| 5 | `PATCH /api/findings/{id}/status` | Confirm or Dismiss each finding |
| 6 | `GET /api/findings/{id}/events` | View audit trail for a finding |
| 7 | `GET /api/versions/{vid}/text` | View parsed clause tree |
| 8 | `GET /api/benchmarks` | View precision/recall stats (ADMIN only) |

### Unhappy Path Cases

| Scenario | Expected Response |
|----------|------------------|
| Upload wrong format (e.g. `.xlsx`) | `400 INVALID_FILE` |
| File exceeds size limit | `413 FILE_TOO_LARGE` |
| Missing or expired JWT | `401 UNAUTHORIZED` |
| Version ID not found | `404 NOT_FOUND` |
| Concurrent modification (stale `version` field) | `409 CONCURRENT_MODIFICATION` |
| Invalid status transition (e.g. `RESOLVED → DETECTED`) | `409 INVALID_TRANSITION` |
| REVIEWER accessing `/api/benchmarks` | `403 FORBIDDEN` |
| PDF scan (no text layer) | Upload succeeds, version status → `FAILED` |

---

## 8. API Reference

Full interactive docs: `http://localhost:8080/swagger-ui/index.html`

```
POST   /api/auth/login                     Login → JWT token
POST   /api/auth/register                  Register (role: REVIEWER)

POST   /api/contracts                      Upload contract + trigger analysis
GET    /api/contracts                      List all contracts
GET    /api/contracts/{id}                 Get contract with version list
POST   /api/contracts/{id}/versions        Upload new version

GET    /api/versions/{vid}/findings        List findings (filter by severity/status/ruleId)
GET    /api/versions/{vid}/text            Get parsed clause tree
PATCH  /api/findings/{id}/status           Change finding status
GET    /api/findings/{id}/events           Get audit trail

POST   /api/packages                       Create counsel question package
GET    /api/packages/{id}                  Get package details
POST   /api/packages/{id}/send             Send via email

POST   /public/counsel/{token}/answer      Counsel answers (no auth required)

GET    /api/benchmarks                     Benchmark runs (ADMIN only)
GET    /api/benchmarks/{id}                Benchmark run detail (ADMIN only)
```

---

## 9. Entity Model

```
contract
  id, title, status (IN_REVIEW | CLOSED)

contract_version  (one contract → many versions)
  id, versionNo, fileName, fileType, fileSha256
  status: ANALYZING → REVIEW_READY | FAILED
  analyzedAt, pageCount, unparsedLines

clause  (parsed clause tree, adjacency list)
  level (1=Article, 2=Section, 3=Sub-clause)
  label, title, body, seq, page, startOffset, endOffset

defined_term  (formally defined terms extracted from text)
  term, definitionText, definitionClause, usageCount

cross_reference  (all clause references in the document)
  rawText, targetLabel, sourceClause, resolved (boolean)

finding  (issue detected by a rule)
  ruleId, severity (ERROR|WARNING|INFO)
  status (DETECTED|CONFIRMED|DISMISSED|RESOLVED|SENT_TO_COUNSEL)
  evidence (verbatim text), evidenceStart, evidenceEnd, page
  message, details (JSONB), fingerprint (SHA-256)
  lockVersion (@Version optimistic locking)

finding_event  (audit trail — one entry per status change)
  fromStatus, toStatus, actor, note, createdAt
```

---

## 10. Analysis Pipeline

After upload, the pipeline runs **asynchronously** in a dedicated thread pool (`analysis-1..4`):

```
POST /api/contracts → 201 (immediate)
        |
[background thread]
        |
Step 1: Select reader by file extension
        DocxReader (Apache POI) or PdfReader (PDFBox 3.x)
Step 2: Read file → List<RawLine>
Step 3: ClauseTreeBuilder.build(rawLines)
        Regex patterns for:
          - "Dieu N" / "DIEU N" (Vietnamese)
          - "Article N" / "Section N" (English)
          - "N.M" sections, "N.M.P" sub-clauses, "(a)" alphabetic
        → ClauseTree + ParseReport (coverage %)
Step 4: Persist ClauseEntity records (adjacency list in DB)
Step 5: ModelBuilder.extract(tree)
        → DefinedTerms (quoted + "means"/"nghia la")
        → CrossReferences ("Dieu N" / "Section N.M")
        → Percentages, monetary values
Step 6: Persist DefinedTermEntity, CrossReferenceEntity
Step 7: Build ContractModel (indexed maps for O(1) rule access)
Step 8: RuleEngine.analyze(model)
        Load enabled rules from rule_config table
        Run each Rule.check(model) → List<RuleFinding>
        Apply severity overrides from config
Step 9: Persist Finding entities with SHA-256 fingerprints
Step 10: version.markReviewReady() → status = REVIEW_READY

On any exception: version.markFailed(reason) → status = FAILED
```

---

## 11. Finding State Machine

```
              ┌──────────────────────► DISMISSED (false positive, reviewer notes why)
              │                              │
DETECTED ──► CONFIRMED ──► SENT_TO_COUNSEL  │
    ▲              │               │         │
    │              ▼               ▼         │
    └── DISMISSED ◄──── RESOLVED ◄───────────┘

Rules:
- RESOLVED is terminal (no further transitions allowed)
- DISMISSED → DETECTED allowed (reviewer changed mind)
- Every transition writes one finding_event record (audit trail)
- Concurrent edits protected by @Version (409 on stale version)
```

---

## 12. Design Decisions & Challenges

### Challenge 1: Async pipeline without blocking HTTP

**Problem:** Analysis can take several seconds — blocking the HTTP thread is unacceptable.

**Solution:** `@Async("analysisExecutor")` with a dedicated `ThreadPoolTaskExecutor` (2–4 threads, queue of 50). Upload returns `201` with `status: ANALYZING`. Clients poll until `status: REVIEW_READY`.

### Challenge 2: Multi-format document reading

**Problem:** DOCX and PDF have completely different internal structures.

**Solution:** `DocumentReader` interface with two implementations — `DocxReader` (Apache POI `XWPFDocument`) and `PdfReader` (PDFBox 3.x `Loader.loadPDF()`). The pipeline selects by file extension. Everything downstream only sees `List<RawLine>`.

> PDFBox 3.x breaking change: `PDDocument.load(File)` was removed. Must use `Loader.loadPDF(File)`.

### Challenge 3: Vietnamese + English legal numbering

**Problem:** Contracts mix `Dieu 1 / Khoan 3` with `Article 1 / Section 2.3`.

**Solution:** Multi-pattern regex in `ClauseTreeBuilder`. Unknown lines increment `unparsedLines` — never silently ignored. Surfaced in `ParseReport` and returned in the API response.

### Challenge 4: Concurrent modification

**Problem:** Two reviewers editing the same finding simultaneously causes lost updates.

**Solution:** JPA `@Version` on `Finding.lockVersion`. Every PATCH must include the current `version` value. If it changed, Spring throws `OptimisticLockException` → caught by `GlobalExceptionHandler` → `409 CONCURRENT_MODIFICATION`.

### Challenge 5: Cross-version finding identity

**Problem:** How do we know which findings from v1 are still present in v2?

**Solution:** Each finding has `fingerprint = SHA-256(versionId|ruleId|sectionLabel|evidenceStart|evidenceEnd)`. The version compare service matches fingerprints across versions to classify: `FIXED`, `PERSISTED`, or `NEW`.

### Challenge 6: Zero-change rule extensibility

**Problem:** Adding a new rule should not require modifying the engine.

**Solution:** Every rule is a Spring `@Component` implementing `Rule` interface. Spring auto-injects `List<Rule>` into `RuleEngine`. The only registration needed is one row in `rule_config` (seeded by Flyway). Zero changes to `RuleEngine.java`.

---

## Success Criteria

| Criterion | Status |
|-----------|--------|
| Detect >= 90% of known errors in sample contracts | Pipeline complete — benchmarks runnable |
| Every finding has verifiable evidence | evidence + evidenceStart/End + sectionLabel + page |
| `docker compose up` starts the full system | docker-compose.yml ready |
| Full loop: upload → findings → counsel → v2 compare | All endpoints implemented |
| Full audit trail | finding_event table, one record per transition |
| Concurrent modification protection | @Version optimistic locking on Finding |
| Role-based access (ADMIN vs REVIEWER) | @PreAuthorize on benchmark endpoints |