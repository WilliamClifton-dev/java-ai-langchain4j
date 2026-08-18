# HBTI Coach AI Model Handoff

> Last verified: 2026-08-18
> Repository: `D:\Projects\java-ai-langchain4j`
> Branch: `codex/hbti-platform`
> Handoff baseline: current branch HEAD; verify with `git rev-parse HEAD`

## 1. Prompt For The Next Model

Use the following as the first instruction when handing this repository to another
coding model:

```text
You are taking over the HBTI Coach repository at D:\Projects\java-ai-langchain4j.

Your goal is to complete the existing 24-task plan as an L1 public-beta-ready,
runnable, testable, demonstrable and deployable personalized weight-management
platform. Do not replace the approved architecture with a new project or reduce
the scope to a backend demo.

Before changing files, read these sources in order:
1. docs/AI_HANDOFF.md
2. tasks/plan.md
3. .codex/plan-runs/hbti-platform/state.md
4. docs/specs/hbti-coach-product-spec.md
5. docs/architecture/hbti-coach-architecture.md
6. docs/decisions/ADR-015-adopt-shared-hbti-research-development-agreement.md
7. accepted ADRs under docs/decisions/
8. tasks/todo.md and the current Git diff/log

Treat Git, tests and repository files as authoritative. Conversation summaries are
only hints. Preserve user changes and continue from the current branch. For every
task: update the durable ledger, implement incrementally, run the stated verification,
write evidence, review the diff, and commit an atomic checkpoint. Never mark a task
complete from code inspection alone.

Product safety constraints are mandatory: follow ADR-015 and its canonical shared
agreement. HBTI is exploratory and non-diagnostic; continuous dimensions are primary
and four-letter codes are secondary. HBTI may personalize wording, emphasis and
monitoring, but it never owns calories, treatment, safety or high-risk exercise
decisions. Keep V1 immutable and introduce construct, item, scoring or recommendation
changes only through a reviewed new version. Golden fixtures prove software parity,
not scientific validity. Deterministic Java rules own calculations, safety gates and
durable facts. Prompts and model output never grant authorization. MySQL is the durable
source of truth; Redis stores only bounded ephemeral/reconstructable data. Default
tests must not require an external database, Redis or model provider. Never log or
commit credentials, tokens, health facts, assessment answers, prompts or model content.

Current priority: re-verify the completed 24-task baseline from the current commit.
Tasks 1 through 24 are implemented; keep architecture, ADRs, API docs, learning docs
and evidence synchronized with behavior. Do not claim enterprise/L2 or regulated-health
readiness.
```

## 2. Product Definition

HBTI Coach is a personalized weight-management platform for adults. It combines:

- a versioned HBTI behavioral-preference assessment;
- deterministic BMI, BMR, TDEE and conservative target calculations;
- a guarded, versioned weight-plan lifecycle;
- daily weight, nutrition and training records;
- deterministic seven-day trends and weekly reviews;
- an AI coach with server-authorized tools and reviewed knowledge retrieval.

HBTI is an exploratory preference model. It is not a medical diagnosis, disease
screening tool or validated biological metabolic type. AI text is guidance, not a
durable business fact; a write is successful only when the corresponding application
service transaction commits.

Target maturity is **L1 public beta**, not enterprise/L2. Task 23 supplies reproducible
pre-release load, recovery and rollback evidence. Availability and first-token SLOs
remain production targets until measured after launch.

## 3. Source Of Truth

Use this precedence when sources disagree:

1. Current code, migrations and automated test behavior.
2. `tasks/plan.md` acceptance criteria.
3. `.codex/plan-runs/hbti-platform/state.md` and task reports.
4. Current architecture and accepted ADRs.
5. Product specification and API documentation.
6. README and chat history.

Do not trust a checked box without its required test/runtime evidence. The ignored
`.codex/plan-runs/hbti-platform/` directory is the durable local execution ledger;
update it even though it is not committed by default.

## 4. Current Architecture

```text
React Web (complete public-beta product loop)
        |
Nginx static delivery + same-origin API/actuator proxy
        |
Spring Boot modular monolith
  identity -> profile -> assessment -> planning -> tracking
                                      |            |
                                      +---- coach -+
  knowledge -----------------------------^
        |               |                 |
      MySQL           Redis          LLM/Ollama
  durable truth   ephemeral only    untrusted boundary
```

Backend modules live under
`src/main/java/com/atguigu/java/ai/langchain4j/`:

| Module | Responsibility |
|---|---|
| `identity` | account registration, JWT, rotating refresh tokens, CSRF, export/deletion |
| `profile` | minimal profile and immutable safety screening |
| `assessment` | versioned HBTI definition, scoring and owned history |
| `planning` | deterministic calculations and plan lifecycle |
| `tracking` | daily facts, aggregation and weekly review |
| `coach` | sync/SSE orchestration, resilience and authorized tools |
| `knowledge` | reviewed versioned ingestion, retrieval and citations |
| `common` / `infrastructure` | errors, observability and outbound adapters |

Key persistence rules:

- Flyway migrations are append-only; current schema version is V12.
- Every personal-data query must include an ownership path derived from JWT identity.
- Chat memory stores one ordered message per row in MySQL.
- Redis may contain rate counters, short request leases and reconstructable public
  definition cache only.
- Prompt text, model output, tokens, credentials and user facts must not become Redis
  durable state or telemetry payloads.

## 5. Implemented And Verified

Tasks 1-24 are recorded complete. The platform currently implements:

- MySQL/MyBatis/Flyway persistence and H2-compatible default tests;
- BCrypt credentials, signed access JWTs, rotating opaque refresh tokens and CSRF;
- server-derived ownership across profile, assessment, plan, tracking and coach data;
- versioned HBTI V1 definitions and JavaScript-parity scoring fixtures;
- idempotent assessment, plan and tracking write paths;
- deterministic calculations, plan transitions and weekly review policy;
- six typed, server-authorized LangChain4j tools;
- reviewed, versioned, citation-bearing lexical RAG;
- named JSON SSE events, cancellation cleanup, timeouts, concurrency cap and breaker;
- shared Redis rate limits, leases and public-definition cache with explicit outages;
- correlated JSON logs, audit events, bounded metrics, liveness and readiness;
- account data export/deletion and immediate active-account JWT validation;
- OpenAPI 1.0.0 path/method baseline, explicit CORS and security-header tests.

Latest Task 18 targeted verification:

```powershell
mvn -q "-Dtest=DataLifecycleApiTest,ConversationOwnershipTest,AccessTokenAccountStatusTest,OpenApiContractTest,SecurityHeadersTest" test
```

Result: 10 tests, 0 failures, 0 errors.

## 6. Public API Surface

The committed OpenAPI drift baseline is
`src/test/resources/openapi/hbti-coach-v1-paths.json`.

| Prefix | Main operations |
|---|---|
| `/api/v1/auth` | CSRF, register, login, refresh, logout |
| `/api/v1/account` | data export and confirmed deletion |
| `/api/v1/profile` | profile read/write and safety screenings |
| `/api/v1/assessments/hbti` | submit, current result and result history |
| `/api/v1/plans` | draft, read version, validate, confirm, activate, current active |
| `/api/v1/tracking` | daily metric, nutrition, training and day summary |
| `/api/v1/tracking/weekly-reviews` | create and read weekly review |
| `/api/v1/coach/messages` | synchronous message and SSE stream |
| `/api/v1/coach/capabilities` | authenticated model availability check for the web composer |

API details live in `docs/api/`. Ownership is never accepted from request bodies.
Cookie-authenticated writes require CSRF. Browser auth cookies remain HTTP-only and
secure in production.

## 7. Completion Checkpoints And Remaining Work

### Task 18: OpenAPI And Security Hardening

Status: **complete**.

Supply-chain audit and framework upgrade completed:

- Spring Boot 3.5.16, Springdoc 2.8.17 and MyBatis Starter 3.0.5.
- Security BOM overrides keep Jackson 2.21.5, Netty 4.1.136.Final and Log4j
  2.26.1 from being downgraded by the legacy LangChain4j BOM.
- OpenNLP is explicitly managed at 2.5.11.
- `scripts/security/osv-audit.ps1` scanned 126 runtime dependencies with 0
  current OSV findings.
- Full suite passes: 132 tests, 0 failures, 0 errors and 1 opt-in external model
  test skipped.
- Audit report: `docs/security/DEPENDENCY_AUDIT_2026-08-15.md`.

### Tasks 19-21: Web Product

Status: **complete**. Tasks 19 through 21 provide the real browser product loop.

- Task 19 delivered the React application shell, register/login/logout, CSRF, cookie
  auth, protected routes and accessible error/loading states.
- Task 20 delivered profile, safety screening, HBTI assessment/result and guarded plan
  lifecycle flows. Its result UI presents continuous dimensions before the auxiliary
  four-letter code as required by ADR-015.
- Task 21 delivered unit-explicit daily tracking and summaries, sparse-data-aware
  deterministic weekly reviews, and a validated POST-SSE coach with cancellation and
  typed retryable errors. HBTI and model output cannot bypass deterministic plan or
  safety boundaries.
- Final evidence: 22 frontend tests, production build, zero high-severity npm audit
  findings, Chrome checks at 320/768/1024/1440 px with no document overflow, clean
  console, empty browser storage and Lighthouse accessibility/best-practices 100.
  Browser flows used a local contract mock; Compose end-to-end evidence remains Task 22.

### Task 22: Delivery

Status: **complete**. CI enforces clean backend tests/package/OSV audit and frozen
frontend install/tests/build/high-severity audit. Independent multi-stage backend and
Web images run as non-root users. Nginx serves the SPA and proxies `/api` plus
`/actuator` on one origin. The default offline Compose topology starts MySQL, ephemeral
Redis, backend and Web without a model key.

Final evidence: 137 backend tests passed with 1 explicit external-model skip; 22/22
frontend tests passed; both production builds passed; npm and OSV gates reported 0
findings; all four Compose services were healthy; direct/proxied readiness returned
`UP`; the SPA shell and Web health endpoint passed; runtime users were backend uid 100
and Web uid 101. A real isolated Chrome load reached the anonymous login page with a
clean console, complete accessibility tree, no horizontal overflow, LCP 122 ms and CLS
0.00. Evidence is written under `target/compose-smoke/` by the smoke script.

### Task 23: Release Evidence

Status: **complete**. Versioned AI/RAG evaluation, public-API demo data, retention,
operator runbooks, load, fresh-volume restore, rollback and machine-readable release
gates are implemented. The full three-phase load observed 20 concurrent sessions,
20 RPS for 60 seconds and 100 RPS for 10 seconds with zero errors or dropped
iterations, zero errors and zero dropped iterations. Restore compares 21 durable
tables on a fresh pinned MySQL image with no count differences or retained dump.
Invalid candidate configuration is rejected before cutover and known-good readiness
must recover. The release gate requires seven AI safety cases, 500 RAG candidates,
zero OSV/npm findings and every report's `gitCommit` to match the candidate commit.
Read exact timings and hashes from `target/release-evidence/release-manifest.json`.

### Task 24: Final Handoff

Status: **complete**. The architecture now has a code-to-claim index, the learning
guide has real source links, migration coverage, implementation patterns, exercises
and interview questions, and the Markdown link checker passed. `tasks/todo.md`, the
deployment checklist and this handoff agree that Tasks 1-24 are complete. The final
verification and commit are recorded in the durable execution ledger.

## 8. Recommended Execution Order

1. Reconcile Git status and the durable ledger before editing.
2. Run the link checker and `scripts/release/verify-release.ps1` for the current commit.
3. Read `docs/LEARNING.md` before changing a module or extending the product.

## 9. Verification Commands

Backend default suite must not contact external services:

```powershell
mvn -q clean test
mvn -q -DskipTests package
docker compose config --quiet
git diff --check
./scripts/security/osv-audit.ps1
./scripts/smoke/compose-smoke.ps1
./scripts/evaluation/run-ai-safety-evaluation.ps1 -DeploymentMode Offline
./scripts/load/run-l1-load.ps1
./scripts/recovery/test-mysql-restore.ps1
./scripts/release/test-rollback.ps1
./scripts/release/verify-release.ps1 -DeploymentMode Offline -Purpose Evidence
```

The Web application exists, so these commands are mandatory:

```powershell
npm --prefix web ci
npm --prefix web test
npm --prefix web run build
npm --prefix web audit --audit-level=high
```

External model smoke tests are opt-in only:

```powershell
$env:RUN_EXTERNAL_TESTS = "true"
$env:MINIMAX_API_KEY = "<secret>"
mvn -Dtest=ExternalModelSmokeTest test
```

Never place real secrets in commands saved to reports or committed files.

## 10. Environment And Local Runtime

Use `.env.example` as the variable catalog. Important settings include:

- `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`
- `REDIS_URL`
- `AUTH_SIGNING_KEY`, `AUTH_SECURE_COOKIES`
- `CORS_ALLOWED_ORIGINS`, `CORS_MAX_AGE`
- `MINIMAX_API_KEY`, or local Ollama profile variables
- coach first-token/total timeout, concurrency and model token limits
- retention cleanup interval, refresh-token grace and audit-event retention

Typical verified start:

```powershell
./scripts/smoke/compose-smoke.ps1 -KeepRunning
```

With the smoke script's isolated defaults, the browser entry point is
`http://localhost:5272/`; direct backend readiness is
`http://localhost:8179/actuator/health/readiness`. Ordinary `docker compose up` keeps
the documented `5173/8080` defaults. A successful Compose config expansion is not an
end-to-end runtime smoke test.

## 11. Non-Negotiable Safety And Quality Rules

- No medical diagnosis, treatment claims or unsafe automatic planning.
- HBTI output must always remain exploratory and non-diagnostic.
- The authenticated owner comes from the server security context, never client `userId`.
- Prompts and LLM output are untrusted; tools must call authorized application services.
- Never log request bodies, cookies, credentials, tokens, assessment answers, health
  facts, prompt text, retrieved chunks, model input/output or exception secrets.
- Use parameterized MyBatis queries and explicit export allowlists.
- Preserve CSRF, secure-cookie, CORS, rate-limit and security-header boundaries.
- Do not add MongoDB back. MySQL is the durable source of truth.
- Do not make Redis durable or store recoverable personal facts in it.
- Do not rewrite merged Flyway migrations; add a new migration.
- Do not make default tests depend on live MySQL, Redis or an external model.
- Do not describe the system as enterprise/L2 until 30-day SLO, recovery, security and
  rollback evidence exists.

## 12. Known State Caveats

At this handoff baseline:

- Local untracked `tmp/` content is user-owned, outside the plan, and must not be
  modified or committed.
- The current dependency audit data was retrieved from OSV on 2026-08-17. Re-query the
  database after dependency changes because advisory status and fix ranges can change.

## 13. Completion Standard

The overall goal is complete only when all 24 task acceptance criteria have current,
reviewable evidence; backend and frontend gates pass; the product works in a real
browser; Compose starts a healthy system; release evaluation/load/restore/rollback
artifacts exist; documentation matches behavior; and no unresolved reachable
critical/high security finding remains.

Until then, keep the project goal active and report the precise incomplete gates rather
than changing the definition of done.
