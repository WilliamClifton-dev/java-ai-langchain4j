# PR #1 Review Notes

Companion to `PR1_DESCRIPTION.md`. This file gives the reviewer a compact
entry point for verifying the 14 hardening commits.

## 1. Headline commits (14, newest first)

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

## 2. Contract guard rails

| # | Guard | File | Test |
|---|---|---|---|
| 1 | Dev signing key rejected under `minimax` profile | `src/main/java/.../identity/AuthSigningKeyEnvironmentPostProcessor.java` | `AuthSigningKeyStartupGuardTest`, `AuthSigningKeyValidatorTest`, `AuthSigningKeyEnvironmentPostProcessorTest` |
| 2 | Prompt text snapshot | `src/test/resources/fixtures/hbti-prompt-baseline.json` | `HbtiPromptSnapshotTest` |
| 3 | OpenAPI schema snapshot | `src/test/resources/openapi/hbti-coach-v1-schemas.json` | `OpenApiContractTest#schemaFieldShapesMatchTheCommittedBaseline` |
| 4a | `@AiService` bean references resolve | `assistant/HbtiCoachAgent.java` | `CoachAgentWiringContractTest` |
| 4b | `@V` parameters match `core.txt` placeholders | `assistant/HbtiCoachAgent.java`, `assistant/HbtiCoachStreamingAgent.java` | `CoachAgentPromptVariablesContractTest` |

## 3. Test evidence

- `mvn test`: **168 / 0 / 0 / 2 skipped**, BUILD SUCCESS
- `npm test`: **24 / 24**, BUILD SUCCESS
- New tests: **23**
- Skipped: one test gated by `-Dopenapi.dump.schemas=true` (off by default)

## 4. How to reproduce locally

```powershell
cd D:\Projects\java-ai-langchain4j
$env:AUTH_SIGNING_KEY = "ci-only-signing-key-padded-to-32-bytes-min"

# Backend
mvn --batch-mode --no-transfer-progress test

# Frontend
cd web
npm ci
npm test
npm run build

# Regenerate OpenAPI schema baseline (only if deliberately changed)
mvn -Dtest=OpenApiContractTest#dumpSchemas -Dopenapi.dump.schemas=true test
Copy-Item target/openapi/hbti-coach-v1-schemas.json src/test/resources/openapi/
```

## 5. CI behaviour

`mvn test` now requires `AUTH_SIGNING_KEY` in the shell environment
because `application.properties` binds the property to `${AUTH_SIGNING_KEY}`
without a default. The CI workflow injects
`AUTH_SIGNING_KEY=ci-only-signing-key-padded-to-32-bytes-min` so the
`@WebMvcTest` slice no longer fails on placeholder resolution. Local
runs need any 32+ byte secret.

## 6. Review checklist

- [ ] Walk the 13 commits in chronological order (`git log --reverse 4f362b4..HEAD`).
- [ ] Read `docs/decisions/ADR-016-...` for the fail-fast rationale.
- [ ] Spot-check `OpenApiContractTest#schemaFieldShapesMatchTheCommittedBaseline`
      by deleting one entry from `hbti-coach-v1-schemas.json` and rerunning
      `mvn -Dtest=OpenApiContractTest test` -- the test must fail.
- [ ] Spot-check `HbtiPromptSnapshotTest` by appending one character to
      `prompts/hbti/core.txt` and rerunning the test -- the test must fail.
- [ ] Spot-check `AuthSigningKeyStartupGuardTest` by running
      `mvn -Dtest=AuthSigningKeyStartupGuardTest test` -- the test must pass.
- [ ] Confirm `mvn test` and `npm test` both end green.

## 7. Unchanged scope

- HBTI positioning stays "exploratory behavioural tendency, not medical /
  not L2".
- Public API surface baseline
  (`src/test/resources/openapi/hbti-coach-v1-paths.json`) untouched.
- `tasks/todo.md` keeps all 24 tasks complete; this PR adds governance on
  top of the implementation, not new features.
