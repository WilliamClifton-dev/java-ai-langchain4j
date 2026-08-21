# L1 Public-Beta Release Checklist

This checklist qualifies only the documented L1 single-instance public beta. It does
not establish enterprise/L2 readiness, multi-region availability or regulated-health
compliance.

## Required Gates

- [ ] Clean backend tests/package and frontend frozen install/tests/build pass.
- [ ] OSV runtime and npm high-severity audits have zero unresolved findings.
- [ ] Compose smoke reports MySQL, Redis, backend and Web healthy.
- [ ] AI/RAG evaluation report matches the current Prompt and manifest hashes.
- [ ] Model-enabled releases include exact provider/model case results and prove every
      request costs at most USD 0.02. Provider/platform limits are USD 8 daily alert,
      USD 10 daily hard stop, USD 160 monthly alert and USD 200 monthly hard stop.
- [ ] L1 load evidence covers 20 concurrent sessions for 10 seconds, 20 RPS for 60
      seconds and 100 RPS for 10 seconds with zero dropped iterations, error rate below 1%, p95 at most 500 ms and
      p99 at most 1,500 ms.
- [ ] Fresh-volume restore evidence meets 60-minute RPO and four-hour RTO targets.
- [ ] Invalid candidate rejection and known-good rollback recovery pass.
- [ ] Demo seeding through public APIs verifies profile, screening, HBTI, active plan
      and seven tracking days without persisting its password.
- [ ] TLS, managed secrets, exact CORS origin, encrypted off-host hourly backups,
      35-day expiry, log/metric collection and alert delivery are configured by the
      deployment platform.
- [ ] Release commit, image digests, Flyway range, Prompt hash, evaluation hash,
      operator and rollback command are attached to the release record.

## Application-Layer Security Contracts

- [ ] `AuthSigningKeyStartupGuardTest` refuses to boot under the `minimax` profile
      when `hbti.auth.signing-key` is the well-known development value shipped in
      `docker-compose.yml`. The same operator-supplied secret boots cleanly under
      every other profile.
- [ ] `AuthSigningKeyValidatorTest` covers the three branches: dev key rejected
      under production-grade profiles, dev key accepted under
      `offline` / `local` / `test`, and custom keys accepted unconditionally.

## Contract Tests

- [ ] `OpenApiContractTest` matches the committed
      `src/test/resources/openapi/hbti-coach-v1-paths.json` baseline. The
      springdoc-generated `/v3/api-docs` output is checked into the build.
- [ ] `HbtiPromptSnapshotTest` matches the committed
      `src/test/resources/fixtures/hbti-prompt-baseline.json` baseline. Provider
      switches between `offline`, `local` and `minimax` must not silently rewrite
      prompt text.

Offline mode is a valid public-beta release when all deterministic gates pass. It must
present coaching as unavailable. Model-enabled release is blocked without provider
evaluation and cost evidence, even when offline tests pass.

Availability target is 99.5% over a rolling 30-day window. The first release configures
measurement and alerting; it cannot manufacture historical compliance. Thirty days of
measured compliance plus on-call ownership is required before any L2 claim.
