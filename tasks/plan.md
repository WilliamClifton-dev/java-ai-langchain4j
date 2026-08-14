# Implementation Plan: HBTI Coach Public Beta

## Source Of Truth

- Product spec: `docs/specs/hbti-coach-product-spec.md`
- Target architecture: `docs/architecture/hbti-coach-architecture.md`
- Database decision: `docs/decisions/ADR-002-use-mysql-as-primary-store.md`
- Target release level: L1 public beta

## Execution Rules

- Build vertical, usable slices; do not add empty placeholders.
- Write behavior tests before implementation and keep default tests external-service-free.
- Update architecture, ADRs, API docs and learning notes with the code that changes them.
- Complete dependency-ready tasks continuously unless an external decision genuinely blocks progress.
- Do not claim L2/enterprise readiness without the evidence listed in the architecture.

## Dependency Graph

```text
Architecture baseline
  -> MySQL foundation -> chat memory
  -> identity/auth -> profile/safety
  -> HBTI definition/scoring -> assessment API
  -> calculators -> plan versions -> daily tracking -> weekly review
  -> authorized AI tools -> RAG -> SSE/resilience
  -> Redis controls + observability
  -> frontend critical flows
  -> CI/containers -> evaluation/load/security/recovery gates
```

## Tasks

### Phase A: Foundation

1. Architecture baseline and durable execution ledger.
   - Acceptance: current spec, architecture, ADR and task plan agree; historical document is clearly non-current.
   - Verify: document link and consistency scan.

2. MySQL build and migration foundation.
   - Acceptance: MyBatis, Flyway and MySQL configured; H2 test profile runs migrations without external services.
   - Verify: migration integration test and `mvn test`.

3. MySQL chat memory vertical slice.
   - Acceptance: one row per ordered message; get/update/delete behavior works transactionally; Mongo runtime is removed.
   - Verify: storage integration tests, context test and package build.

### Phase B: Identity And Personal Data

4. Identity schema and credential domain.
   - Acceptance: unique normalized account identity, adaptive password hash, token persistence and migration tests.
   - Verify: unit and SQL integration tests.

5. Authentication API and security filter chain.
   - Acceptance: register, login, refresh rotation, logout and stable errors; protected routes reject anonymous access.
   - Verify: MVC security tests and token-reuse tests.

6. Profile ownership and safety screening.
   - Acceptance: authenticated users can write/read only their profile; high-risk screening blocks automatic planning.
   - Verify: ownership, validation and domain policy tests.

### Phase C: Assessment And Planning

7. Versioned HBTI definition import and scoring engine.
   - Acceptance: immutable published definition; continuous dimension scores; Java results match prototype golden fixtures.
   - Verify: golden unit tests and migration tests.

8. HBTI assessment API and result history.
   - Acceptance: authenticated submission, idempotency, current/history endpoints and non-diagnostic response contract.
   - Verify: MVC and ownership tests.

9. Deterministic health calculators.
   - Acceptance: unit-aware BMI/BMR/TDEE and bounded target ranges with explicit assumptions and safety exclusions.
   - Verify: boundary and property-oriented unit tests.

10. Versioned plan lifecycle.
    - Acceptance: draft, validate, confirm, activate and replace plan versions transactionally; one active version.
    - Verify: rule, transaction, conflict and ownership tests.

### Phase D: Execution Loop

11. Daily tracking records.
    - Acceptance: typed weight, nutrition, activity, training and sleep records with units, dates and idempotent writes.
    - Verify: API, mapper and aggregation tests.

12. Weekly deterministic review.
    - Acceptance: at least seven days of facts produce trends, adherence and bounded adjustment proposals; single-day noise is rejected.
    - Verify: golden aggregation and missing-data tests.

13. Authorized coach tools.
    - Acceptance: read/write tools call application services with server-derived identity; writes report success only after commit.
    - Verify: prompt-injection, authorization, schema and rollback tests.

14. Knowledge ingestion and RAG.
    - Acceptance: versioned reviewed documents, citations, isolation, retrieval evaluation and safe no-evidence behavior.
    - Verify: ingestion, retrieval and evaluation tests.

15. Streaming and model resilience.
    - Acceptance: stable SSE event contract, cancellation, timeouts, circuit breaker and deterministic feature availability during model outage.
    - Verify: MVC stream, timeout and degradation tests.

### Phase E: Operations And Product UI

16. Redis rate, idempotency and cache controls.
    - Acceptance: bounded consumption and reconstructable cache; conservative behavior during Redis outage.
    - Verify: policy tests and failure-mode integration tests.

17. Observability and audit trail.
    - Acceptance: required metrics/log fields and audit events exist without sensitive payloads; health/readiness are distinct.
    - Verify: actuator, log-capture and audit tests.

18. OpenAPI and security hardening.
    - Acceptance: complete API contract, security headers, CORS policy, dependency audit triage and data lifecycle endpoints.
    - Verify: contract diff, header tests and security checklist.

19. Web foundation and authentication flow.
    - Acceptance: responsive application shell, login/register/logout, route protection and accessible error/loading states.
    - Verify: unit tests, production build and browser test.

20. Web assessment, profile and plan flow.
    - Acceptance: users complete screening/HBTI, inspect scores, create and activate a plan without unsafe claims.
    - Verify: browser critical-flow test and accessibility scan.

21. Web tracking, review and coach flow.
    - Acceptance: daily records, trends, weekly review and streaming coach are usable on mobile and desktop.
    - Verify: browser tests, console/network inspection and responsive screenshots.

### Phase F: Release Evidence

22. CI, Docker and environment delivery.
    - Acceptance: frozen dependency installs, backend/frontend tests, images, Compose health checks and documented environment variables.
    - Verify: clean CI-equivalent run and Compose smoke test.

23. Evaluation, load, backup and rollback gates.
    - Acceptance: versioned AI evaluation, L1 load report, backup restoration, incident runbooks, seed/demo data and release checklist.
    - Verify: recorded commands and artifacts satisfy every L1 success criterion.

24. Final architecture and learning handoff.
    - Acceptance: diagrams and ADRs match code; module learning guides and interview questions reference actual implementations.
    - Verify: full diff review, link check, clean tests/builds and no unresolved critical findings.

## Global Verification

```powershell
mvn clean test
mvn -DskipTests package
npm --prefix web ci
npm --prefix web test
npm --prefix web run build
docker compose config
git diff --check
```

The frontend and Compose commands enter the mandatory set when their tasks create those artifacts.

## Risks And Mitigations

| Risk | Mitigation |
|---|---|
| HBTI semantics drift from prototype | import versioned definitions and golden fixtures before public API |
| Health advice exceeds product boundary | deterministic safety gate, explicit wording, AI evaluation and escalation |
| Scope becomes an unreviewable rewrite | complete and verify each vertical task before the next |
| External AI makes tests flaky | mock contracts by default; opt-in provider smoke tests only |
| Authentication is bolted on too late | identity and ownership precede every new personal-data module |
| Infrastructure is added for resume optics | require a measured use case and ADR for each dependency |
