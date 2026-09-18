## Overview

Promote `codex/hbti-platform` to `main` so the 24-task public-beta worktree
lands on the default branch. The PR also introduces 14 new hardening commits
on top of that base.

This branch and `main` share no common ancestor in the local clone, so the
GitHub PR diff is the **full branch vs `main`**: **476 files,
+30233 / -418**. The 14 new commits below add **23 files, +1302 / -5** of
net change.

## Headline: 14 new hardening commits

```
a3cfb8c docs(pr): capture PR #1 description and review notes on the branch
ebab3da chore(repo): lock LF line endings via .gitattributes
7d67aa9 chore(repo): ignore local tmp scratch directory
7872754 docs(release): add agent wiring and prompt variable contracts to L1 checklist
0379a87 test(assistant): keep @V parameters aligned with core prompt placeholders
e0396ca test(assistant): assert @AiService bean references resolve under test profile
2b9da3c docs(release): add openapi schemas contract to L1 checklist
96cf289 test(openapi): commit v1 schemas baseline
d376486 test(openapi): assert components.schemas property and required shapes
151ac37 docs: document AUTH_SIGNING_KEY env var and fail-fast release gate
9b238fc test(coach): snapshot hbti prompts against committed baseline
a92c84c test(identity): cover auth signing key validator and post processor
c33c50d feat(identity): fail-fast when default dev signing key used under prod
d8b6771 docs(adr): reject default auth signing key outside dev profiles
```

### New files (16)

- `docs/decisions/ADR-016-reject-default-auth-signing-key-outside-dev-profiles.md`
- `src/main/java/.../identity/AuthSigningKeyDefaults.java`
- `src/main/java/.../identity/AuthSigningKeyValidator.java`
- `src/main/java/.../identity/AuthSigningKeyEnvironmentPostProcessor.java`
- `src/main/resources/META-INF/spring.factories`
- `src/test/java/.../assistant/CoachAgentWiringContractTest.java`
- `src/test/java/.../assistant/CoachAgentPromptVariablesContractTest.java`
- `src/test/java/.../coach/prompt/HbtiPromptSnapshotTest.java`
- `src/test/java/.../identity/AuthSigningKeyValidatorTest.java`
- `src/test/java/.../identity/AuthSigningKeyEnvironmentPostProcessorTest.java`
- `src/test/java/.../identity/AuthSigningKeyStartupGuardTest.java`
- `src/test/resources/fixtures/hbti-prompt-baseline.json`
- `src/test/resources/openapi/hbti-coach-v1-schemas.json`
- `.gitattributes`
- `docs/PR1_DESCRIPTION.md`
- `docs/PR1_REVIEW_NOTES.md`

### Modified files (7)

`CLAUDE.md`, `README.md`, `docs/AI_HANDOFF.md`, `docs/RELEASE_CHECKLIST.md`,
`.github/workflows/ci.yml`, `.gitignore`, `src/test/java/.../config/OpenApiContractTest.java`

### Contract guard rails added in this PR

| # | Guard | Catches |
|---|---|---|
| 1 | ADR-016 EnvironmentPostProcessor | Dev signing key reused under `minimax` profile refuses to boot |
| 2 | `HbtiPromptSnapshotTest` | SHA-256 LF-normalized baseline of `core.txt` + 6 scene prompts |
| 3 | `OpenApiContractTest#schemaFieldShapesMatchTheCommittedBaseline` | 53 application DTO property + required arrays |
| 4 | `CoachAgentWiringContractTest` + `CoachAgentPromptVariablesContractTest` | `@AiService` bean references resolve; `@V` parameters stay in sync with `{{...}}` placeholders |

### Tests

- `mvn test` -> 168 tests, 0 failures, 0 errors, 2 skipped
- `npm test` -> 24 / 24 passed
- 23 new tests added in this PR
- CI now exports `AUTH_SIGNING_KEY=ci-only-...-min` so the `@WebMvcTest`
  slice no longer fails on `${AUTH_SIGNING_KEY}` placeholder resolution

## Underlying branch

The remaining ~71 commits implement the 24-task public-beta baseline already
documented in `tasks/todo.md`, `docs/AI_HANDOFF.md`,
`docs/architecture/hbti-coach-architecture.md` and
`docs/RELEASE_CHECKLIST.md`. They are the worktree that produced:

- HBTI bilingual questions with deterministic scoring
- Identity / JWT / refresh tokens / CSRF / session ownership
- Profile, assessment, plan, tracking, weekly review APIs
- Six server-authorised AI tools with bounded RAG
- Named SSE streaming events, timeouts, concurrency cap, circuit breaker
- Redis ephemeral controls, Micrometer metrics, audit events
- Account data export / deletion, off-host backup rehearsal, rollback evidence

## Unchanged

- HBTI positioning: exploratory behavioural tendency, not medical / not L2.
- Public API surface: `src/test/resources/openapi/hbti-coach-v1-paths.json`
  baseline already enforced.
- All 24 tasks remain `tasks/todo.md` complete.
