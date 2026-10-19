# Master Build Prompt: Contract Compiler + Counsel Workflow

> **Cách dùng:** đặt 3 file md vào `docs/` của repo (`01-system-overview.md`, `02-architecture-and-folder-structure.md`, `03-database-design.md`), mở AI coding agent (Claude Code, Cursor...) ở thư mục gốc, rồi dán toàn bộ phần bên dưới đường kẻ.
> Prompt viết bằng tiếng Anh để agent hiểu chính xác. Agent làm **từng phase một**, báo cáo xong thì bạn gõ `continue`.

---

## ROLE

You are a senior backend engineer (10+ years, Java/Spring Boot, document processing, rule engines) joining a 7-day hackathon build. You own this system end to end. You write production-quality code that another senior engineer would approve in code review without major changes.

## SOURCE OF TRUTH

Read these three files fully before writing any code:

1. `docs/01-system-overview.md` — product, actors, workflow, features and priorities (P0/P1/P2)
2. `docs/02-architecture-and-folder-structure.md` — architecture, API, folder structure, conventions
3. `docs/03-database-design.md` — PostgreSQL schema, Flyway migrations, Redis keys

Follow them. If you find a conflict or a clear improvement, **do not silently deviate**: record it in `docs/adr/NNN-title.md` (context, decision, consequence) and mention it in your phase report. Known gap to resolve: the transition `SENT_TO_COUNSEL → CONFIRMED` exists in doc 03 but not in the diagram in doc 01. Treat doc 03 as correct.

## PRODUCT IN ONE PARAGRAPH

Reviewers upload a fund contract (DOCX/PDF with a text layer). The system parses it into a clause tree, extracts defined terms, cross-references and typed values, runs a deterministic rule engine to detect input errors (typos, broken references, contradictions, ambiguous or inconsistent terms), and shows each finding with exact evidence. Reviewers triage findings, bundle the ones needing legal judgment into a question package, and email it to external counsel who answers through a tokenized link. Uploading a new version shows which findings are gone, still open, or newly introduced.

## NON-NEGOTIABLE ENGINEERING RULES

### 1. No hard-coding (this is the most important rule)

Everything that can vary by contract style, language, policy or deployment must be **configuration or data, never a literal in business logic**.

| Must be externalized | Where |
|---|---|
| All regex patterns (article/section/sub-clause headings, cross-reference forms, money, percent, date, defined-term quotes) | `resources/patterns/*.yml`, loaded into a typed `PatternCatalog` and precompiled once at startup. Each pattern has an `id`, `description`, `regex`, `flags`, and a test fixture |
| Number-words, ordinals, month names, currency symbols/codes | Locale packs in `resources/locale/{en}.yml` behind a `LocalePack` interface (use ICU4J where it is the right tool, config for the rest) |
| Rule enablement, severity, thresholds, tolerances, stop-lists, allow-lists | `rule_config` table (seeded by Flyway) with `params` JSONB, plus defaults in `application.yml` via `@ConfigurationProperties`. Rules read their params, never constants |
| Question templates and answer options per rule | `resources/question-templates.yml`, rendered with a safe template engine. Template key = `rule_id` |
| Email subject/body | `resources/email/*.html` templates |
| Legal-domain vocabulary (allowed capitalized words, section keywords like Article/Section/Schedule/Exhibit, party suffixes L.P./LLC/Ltd) | `resources/vocabulary/*.yml` |
| Limits: upload size, token TTL, rate limits, retries, timeouts | `application.yml`, overridable by env vars |
| Secrets (JWT secret, SendGrid key, DB/Redis credentials) | Environment variables only. Commit `.env.example`, never real values |

Rules for code itself:
- No magic numbers or magic strings. Use enums, constants with names, or config.
- **Never special-case the sample contracts.** No code path may reference a sample file name, a sample party name or a sample section number. Tests must not be satisfied by returning expected values.
- No fake data, stubs, `// TODO implement` or `throw new UnsupportedOperationException` in shipped paths. If something is out of scope, it is absent and listed in the README under "Not supported".
- Rule IDs are stable `UPPER_SNAKE_CASE` constants defined once (enum or constants class) and reused everywhere, including fingerprints.

### 2. Senior-level code quality

- Layering and module boundaries exactly as in doc 02. Dependency direction: `rules → analysis.model` only; `counsel → finding` only through `FindingWorkflowService`.
- SOLID, constructor injection, immutable domain objects (`record`, `final`), small focused classes (target < 200 lines), small methods, meaningful names.
- Interfaces at the seams that vary: `DocumentReader`, `Rule`, `NotificationService`, `FileStorage`, `LocalePack`. Add an abstraction only where a second implementation is plausible or testing needs it.
- DTOs for all API I/O (records). Never expose entities.
- A single `GlobalExceptionHandler` with a consistent error body `{code, message, details, traceId}`. Typed domain exceptions. No swallowed exceptions, no `catch (Exception e) {}`.
- `@Transactional` boundaries on services; status change and audit event in the same transaction. Optimistic locking on `finding` (`@Version`).
- Structured logging with correlation id (`traceId`), no sensitive data (tokens, passwords, full contract text) in logs.
- Input validation (`jakarta.validation`) on every request. Validate uploads by extension, MIME and magic bytes; store under a random UUID name; enforce size limit from config.
- Security: BCrypt, JWT with expiry, role checks (`REVIEWER`, `ADMIN`), public counsel endpoints only via hashed single-package token with TTL and Redis rate limit, CORS from config, no stack traces to clients.
- Idempotency: re-running analysis on the same version must produce the same findings and fingerprints and not duplicate rows.
- Javadoc on public interfaces and on every rule (what it detects, what it deliberately does not).

### 3. Performance and robustness

- Parse once, analyze many: extractors produce an indexed `ContractModel` (maps by clause label, by normalized term, by subject) so rules are O(n) or O(n log n). No nested scans of the full text inside rules.
- Precompile all patterns once. No regex compilation inside loops.
- Batch inserts (`saveAll` with JDBC batching enabled), avoid N+1 (use fetch joins / projections), add the indexes in doc 03 and verify with `EXPLAIN` on the main queries.
- Large file safety: stream DOCX/PDF, cap page and paragraph counts from config, fail gracefully with `FAILED` status and a human-readable `failure_reason`.
- The analysis pipeline must **never silently return zero findings** when parsing failed. Populate `unparsed_lines` and surface a `ParseReport`; if parse coverage is below a configurable threshold, mark the version with a warning finding (`PARSE_COVERAGE_LOW`).
- Redis outage must not corrupt state: counsel token validation falls back to the DB hash and expiry columns.

## ERROR-DETECTION QUALITY BAR (the heart of the product)

The goal is **high recall with controlled false positives**, with every finding provably tied to evidence. Implement the catalog below as separate `Rule` classes (one rule, one responsibility, one test class). Doc 01/02 list the core rules; **this catalog extends them. Implement all P0 and P1 here; P2 only after everything else is green.**

### Detection catalog

| Category | Rule ID | What it detects | Pri |
|---|---|---|---|
| Reference | `REF_BROKEN` | Reference to a section/sub-clause/exhibit/schedule that does not exist. Handle forms: `Section 7.4`, `Sections 2.1 and 2.3`, `Section 2.1 through 2.4`, `Section 5.1(b)(ii)`, `Article IV`, `Schedule A`, `Exhibit B` | P0 |
| Reference | `REF_SELF` | Clause that references itself | P1 |
| Reference | `REF_TARGET_MISMATCH` | Reference whose label exists but whose title/subject contradicts the described subject (e.g. "the Hurdle Rate in Section 2.1" but 2.1 is about Fees), using configurable keyword overlap and a confidence score | P2 |
| Reference | `REF_CIRCULAR` | Cycles in the reference graph (JGraphT) | P2 |
| Reference | `REF_RELATIVE_AMBIGUOUS` | "above", "below", "the preceding Section" without resolvable antecedent | P2 |
| Definition | `TERM_UNDEFINED` | Capitalized multi-word term used as a defined term but never defined (candidate detection via config: capitalization pattern minus stop-list/allow-list, minimum occurrences) | P0 |
| Definition | `TERM_UNUSED` | Defined but never used | P0 |
| Definition | `TERM_DUPLICATE` | Same term defined more than once with different text | P1 |
| Definition | `TERM_VARIANT` | Inconsistent variants of one term (singular/plural, case, `LP` vs `Limited Partner`) using normalization + configurable similarity threshold | P1 |
| Definition | `TERM_CIRCULAR_DEF` | Term defined in terms of itself directly or through a chain | P2 |
| Value | `DATE_LOGIC` | Impossible or contradictory ordering (end before start, closing after termination, date in an invalid calendar form such as Feb 30). Which date pairs to compare is driven by config (term-pair mapping), not code | P0 |
| Value | `WORDS_VS_DIGITS` | "thirty (30) days", "One Hundred Thousand Dollars ($1,000,000)" mismatches, for money, percent and counts | P0 |
| Value | `VALUE_CONFLICT` | Same subject (e.g. Management Fee) with different values in different clauses, with tolerance and unit/period normalization from config | P0 |
| Value | `PERCENT_SUM` | Percentages in one allocation group not summing to the configured total (default 100) | P1 |
| Value | `CURRENCY_INCONSISTENT` | Mixed currencies for the same subject | P1 |
| Value | `RANGE_INVALID` | Minimum greater than maximum, start > end for numeric ranges | P1 |
| Value | `DATE_FORMAT_INCONSISTENT` | Mixed date formats in one document (informational) | P2 |
| Structure | `NUMBERING_GAP` | Missing numbers in sequence (2.1, 2.3) | P1 |
| Structure | `NUMBERING_DUPLICATE` | Same label appearing twice | P0 |
| Structure | `NUMBERING_ORDER` | Out-of-order labels | P1 |
| Structure | `SUBCLAUSE_ORPHAN` | `(b)` without `(a)`, or `(a)` alone where config says min 2 | P1 |
| Structure | `CLAUSE_EMPTY` | Heading with no body | P1 |
| Structure | `TOC_MISMATCH` | Table of contents entries not matching actual headings | P2 |
| Text quality | `PLACEHOLDER_LEFT` | `[●]`, `[Insert]`, `TBD`, `XX`, `____` leftovers (patterns from config) | P0 |
| Text quality | `TEXT_REPEATED_WORD` | "the the", repeated punctuation | P1 |
| Text quality | `TEXT_UNBALANCED` | Unbalanced parentheses, brackets or quotation marks within a clause | P1 |
| Text quality | `TEXT_TYPO` | Probable typos via dictionary lookup (e.g. SymSpell/Hunspell-style) **with a legal-domain allow-list and defined-terms exclusion**, emitted as `WARNING`/`INFO` with a suggestion and confidence | P2 |
| Entity | `PARTY_NAME_INCONSISTENT` | Same party referred to with different legal names/suffixes | P1 |
| Entity | `DUPLICATE_CLAUSE_TEXT` | Near-identical clauses (shingling/Jaccard, threshold from config) that may be accidental copy-paste | P2 |
| Meta | `PARSE_COVERAGE_LOW` | Too many unparsed lines to trust the result | P0 |
| AI plugin | `AI_AMBIGUITY` | Optional Gemini plugin, off by default. JSON output only, **every returned quote must exist verbatim in the clause or the finding is discarded**; `source = AI`, `severity = INFO` | P2 |

### Finding contract (every finding must have)

`ruleId`, `severity`, `clauseId` + `sectionLabel`, **verbatim evidence with offsets** (offsets must slice the exact evidence from the stored full text, assert this in a test), `message` (human readable, actionable), `details` JSON (rule-specific), `confidence` (0–1, rules that are heuristic must lower it), `fingerprint` (as in doc 03, normalized so cosmetic edits do not change it).

### False-positive control (as important as recall)

- Every heuristic rule exposes thresholds in `rule_config.params` and has tests for the **negative case** (clean text must produce nothing).
- De-duplicate: one root cause yields one finding (e.g. an undefined term used 12 times is one finding with `occurrences: 12`, not 12 findings).
- Severity calibration: deterministic contradictions = `ERROR`; heuristic/probabilistic = `WARNING`/`INFO` with confidence shown.
- Findings below a configurable confidence floor are stored but flagged `lowConfidence` and hidden by default in the UI list.
- A clean reference contract (`lpa_clean`) must produce **zero** `ERROR` findings. This is a regression gate.

### Measurement (prove the quality, do not claim it)

- Build a **mutation engine** (`test/benchmark`): takes a clean contract model/DOCX and applies configurable mutations (break a reference, change a number, delete a definition, swap dates, introduce words-vs-digits mismatch, duplicate a label, leave a placeholder...). Each mutation records the ground-truth finding it should trigger. Mutations and their parameters are data-driven, not hard-coded per sample.
- Sample DOCX fixtures are **generated programmatically with Apache POI** from a fixture builder, not hand-copied binaries. Keep at least one hand-written realistic LPA excerpt as a parser realism test.
- `BenchmarkRunner` produces precision/recall per rule and overall, writes `docs/benchmark-report.md` and rows into `benchmark_run/benchmark_result`.
- CI gate: build fails if overall recall or precision falls below thresholds defined in config (initial: recall ≥ 0.90, precision ≥ 0.85; adjust only via ADR).

## TESTING REQUIREMENTS

- JUnit 5 + AssertJ. Each rule: ≥ 1 positive, ≥ 1 negative, ≥ 1 edge case. Each extractor and pattern: table-driven tests.
- State machine: test every legal and every illegal transition.
- Fingerprint: stable across cosmetic edits, changes on semantic edits, identical across re-analysis.
- Integration (Testcontainers PostgreSQL + Redis): upload → findings → triage → package → send (mock `NotificationService`) → counsel answers → statuses and audit trail → upload v2 → comparison.
- Security tests: expired/used/forged token, missing role, oversized/invalid upload.
- No test may depend on test execution order or on wall-clock time (inject `Clock`).
- Coverage target: ≥ 80% on `analysis`, `rules`, `finding`, `counsel`. Report it.

## DELIVERY PLAN (work phase by phase)

**Stop after each phase, post the report below, and wait for me to say `continue`.**

| Phase | Scope | Definition of Done |
|---|---|---|
| 0 | Read docs, post a short plan, list assumptions, create repo skeleton per doc 02, Docker Compose, `.env.example`, CI workflow, Flyway V1–V6 from doc 03 | `docker compose up` starts Postgres, Redis, backend; Flyway migrates clean; health endpoint OK |
| 1 | Config infrastructure: `PatternCatalog`, `LocalePack`, vocabulary loader, `rule_config` loading, typed properties | Patterns load, validate at startup (fail fast on bad regex), unit tested |
| 2 | Ingest + normalize + clause tree + `ParseReport` (DOCX first, then PDF) | Fixture contracts parse to the expected tree; unparsed lines reported |
| 3 | Extractors: terms, references (all forms), values (money, percent, date, words) → indexed `ContractModel` | Table-driven tests green |
| 4 | Rule engine + P0 rules | P0 rules green incl. negative cases; clean fixture yields no ERROR |
| 5 | P1 rules | Same bar |
| 6 | Persistence, auth, REST API for contracts/versions/findings; fingerprint + version comparison | API tests green; Swagger complete |
| 7 | Workflow: state machine, audit, question generator, package, token (Redis + DB hash), SendGrid adapter, public counsel API | Integration test of full loop green |
| 8 | Mutation benchmark + report + CI gate | Report generated, thresholds met |
| 9 | Frontend (React): upload, split-screen review, triage, package, counsel page, compare | Full demo flow works end to end |
| 10 | P2 items in this order: GitHub Action lint, rules marked P2, dashboard, AI plugin | Only if all above are done |
| 11 | README (quick start, architecture, how to add a rule in 5 steps, how to add a pattern, limitations), final cleanup | A new engineer can run and extend it from the README alone |

### Phase report format (post after each phase)

1. What was built (files/modules, brief)
2. How to verify (exact commands) and the actual test/benchmark output
3. Deviations from the docs and ADRs created
4. Assumptions made
5. Risks / known limitations
6. What the next phase will do

## EXTENSIBILITY CHECK

Adding a new rule must require: one new `@Component` class implementing `Rule`, one row in `rule_config` (Flyway migration), one question template entry, one test class. **Zero changes to the engine.** Adding a new pattern must require only a YAML entry plus a fixture. Verify this explicitly by adding one throwaway example rule during Phase 5 and then deleting it, and say so in the report.

## ANTI-PATTERNS (reject your own work if you catch these)

- Rules with regex or thresholds inline in the Java class
- A rule that scans the whole text instead of using the indexed model
- Findings without verbatim evidence or with offsets that do not match the text
- One finding per occurrence flooding the list for a single root cause
- Returning empty findings when parsing silently failed
- Business logic in controllers, entities returned from controllers
- Status changed anywhere except `FindingWorkflowService`
- Tests that assert on values copied from the implementation, or that pass only because of a sample-specific branch
- Committed secrets, committed binaries that could be generated, unexplained `@SuppressWarnings`
- Large "utility" god classes, copy-pasted rule logic instead of shared helpers

## OPERATING RULES FOR YOU

- Work in small, reviewable commits, conventional messages, one rule or one concern per commit with its tests.
- Run the build and tests yourself before reporting; show real output, never claim success you did not observe.
- If a requirement is ambiguous, choose the most conservative, configurable option, document it as an assumption, and continue. Ask me only if blocked.
- Prefer boring, proven solutions over clever ones. Do not add libraries without a stated reason.
- Do not exceed scope: P2 never starts before P0/P1 are complete and benchmarked.

**Begin with Phase 0 now.**
