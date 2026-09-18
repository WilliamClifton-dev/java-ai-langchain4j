# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HBTI Coach is a Spring Boot 3.2.6 + LangChain4j 1.0.0-beta3 personal weight management assistant backend. It combines deterministic health calculations with AI coaching through a modular monolith architecture. The system uses MySQL 8 as the single source of truth, with Redis for ephemeral state.

**Important**: HBTI is an exploratory behavioral tendency assessment, not a medical diagnosis or validated biological metabolic typing. All health calculations are planning estimates, not prescriptions.

## Build and Test Commands

### Standard Development

```bash
# Run all tests (uses H2, no external dependencies)

**Note:** the test profile expects AUTH_SIGNING_KEY to be set in the shell environment. Any 32+ byte secret works (for example baseline-test-key-padded-to-32-bytes); the well-known dev key shipped in docker-compose.yml is also accepted because the test profile is in the allow-list. Without an AUTH_SIGNING_KEY the @WebMvcTest slice tests cannot resolve the hbti.auth.signing-key placeholder and mvn test fails.
mvn test

# Run a specific test class
mvn -Dtest=HbtiScoringEngineTest test

# Run a specific test method
mvn -Dtest=HbtiScoringEngineTest#shouldScoreGoldenFixtureV1 test

# Run the application (requires MYSQL_PASSWORD and MINIMAX_API_KEY)
mvn spring-boot:run

# Package the application
mvn package
```

### External Model Tests

The default test suite uses H2 and mock models. To run tests against real external models:

```bash
# Windows PowerShell
$env:RUN_EXTERNAL_TESTS = "true"
$env:MINIMAX_API_KEY = "your-api-key"
mvn -Dtest=ExternalModelSmokeTest test
```

### Profile Configuration

Default profile is `minimax` (MiniMax OpenAI-compatible API). To use local Ollama:

```bash
# Windows PowerShell
$env:APP_PROFILE = "local"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
$env:OLLAMA_MODEL_NAME = "qwen:latest"
$env:MYSQL_PASSWORD = "your-password"
mvn spring-boot:run
```

## Architecture

### Module Structure

The system is a **modular monolith** with Java interface boundaries, not internal HTTP calls:

```
identity -> profile -> assessment -> planning -> tracking
                                      |            |
                                      +---- coach -+
knowledge -------------------------------^
```

**Key modules** (under `src/main/java/com/atguigu/java/ai/langchain4j/`):
- **identity**: user accounts, password credentials, JWT access/refresh tokens, ownership context
- **profile**: adult profile, goals, preferences, safety screening eligibility
- **assessment**: HBTI definitions, responses, deterministic scoring, result versions
- **planning**: BMI/BMR/TDEE calculators, nutrition targets, immutable plan versions
- **tracking**: daily metrics (weight, nutrition, training), weekly review with deterministic trend calculation
- **coach**: conversations, ordered messages, scene routing, LangChain4j integration, authorized tool execution
- **knowledge**: reviewed document versions, chunks, deterministic lexical retrieval (not vector search)
- **common**: errors, clocks, IDs, security primitives
- **infrastructure**: Redis rate policies, circuit breakers, streaming resilience

### Dependency Rules

- Controllers translate HTTP to application commands
- Application services enforce authorization and transactions
- Domain logic is deterministic and framework-light
- MyBatis mappers are outbound adapters
- Cross-module reads use narrow query interfaces; direct mapper access across modules is prohibited
- **Prompt text never grants permissions and never replaces validation**

### Data Architecture

**Single source of truth**: MySQL 8 via MyBatis and Flyway migrations

**Key tables**:
- `user_account`, `refresh_token` (V2): identity with BCrypt passwords, SHA-256 token digests, rotation/reuse detection
- `user_profile`, `safety_screening` (V3): minimal calculation inputs, immutable screening versions
- `assessment_definition`, `assessment_attempt`, `assessment_score` (V4, V5): versioned HBTI definitions, idempotent submissions
- `weight_plan`, `weight_plan_version` (V6): one aggregate per user, immutable target snapshots, lifecycle state transitions
- `daily_metric`, `nutrition_log`, `training_log` (V7): typed execution facts with profile time-zone date policy
- `weekly_review` (V8): immutable review versions with deterministic 7-day trend/adherence policy
- `coach_conversation`, `coach_message` (V1): relational ordered messages, transactional memory updates
- `knowledge_document`, `knowledge_chunk` (V9): reviewed sources, immutable content versions, published/retired lifecycle

**All user-owned tables** include ownership predicates in queries. Public identifiers are UUIDs.

### Authentication & Authorization

- Passwords: BCrypt adaptive hash (cost 12)
- Access tokens: HS256 JWT, 15 min TTL, subject + lifecycle claims
- Refresh tokens: 256-bit opaque values, SHA-256 stored digests, 30-day TTL with family rotation
- Cookie delivery: secure, HTTP-only, same-site cookies + CSRF double-submit (`/api/v1/auth/csrf`)
- **Every protected command receives authenticated user ID from JWT subject**
- **Ownership predicates are required in SQL queries, not post-fetch checks**

Environment variables required:
- `AUTH_SIGNING_KEY`: at least 32 UTF-8 bytes
- `AUTH_SECURE_COOKIES`: true (production), false only for local HTTP

### Deterministic vs AI Boundaries

**Code must own** (never delegate to AI):
- Adult eligibility and safety routing
- HBTI scoring (`HBTI_SCORING_V1` from prototype commit `bdd1e9f`)
- BMI/BMR/TDEE calculations (`MIFFLIN_ST_JEOR_METRIC_V1`)
- Target ranges (`CONSERVATIVE_ENERGY_RANGE_V1` - never below BMR)
- Plan lifecycle state transitions, daily aggregation, weekly trend statistics
- Authorization, validation, persistence, rate limits

**AI may own**:
- Explanation, reflective questions, supportive wording, summarization
- Selection among explicitly allowed read tools
- Proposed adjustments that remain drafts until validated

### Authorized Coach Tools

Six typed tools registered with LangChain4j (defined in `coach/tool/`):
1. Read active plan
2. Read daily summary
3. Read weekly review
4. Write daily metric
5. Write nutrition log
6. Write training log

**Tool security model**:
- Server-derived invocation context binds JWT subject (never user-provided owner)
- Write idempotency keys: server nonce + tool name + canonical arguments
- Synchronous calls: thread-bound context
- Streaming calls: explicit registration by memory ID, cleanup on cancellation/completion
- Bounded error codes without exception details

See ADR-009 and ADR-011 for full boundary specification.

### Knowledge Retrieval

**L1 implementation**: deterministic local lexical scoring (not vector search)
- Chinese Han bigrams + normalized alphanumeric terms
- SQL filters: `PUBLISHED` versions + exact locale
- Bounded: max 500 candidates, 0.20 match threshold, max 5 passages returned
- Every passage includes source key, title, URL, publisher, locale, version, content hash
- **Only reviewed sources** enter the published index (no arbitrary URL fetch)

This is deliberately external-service-free for L1 bounded corpus. Not suitable for vector-scale workloads.

### Streaming Resilience

SSE endpoint at `/api/v1/coach/stream` with named JSON events:
- Configurable 5s first-token timeout, 30s total timeout
- Circuit breaker: 3 consecutive failures → 30s open → 1 half-open probe
- Concurrency cap: max 5 model streams by default (local single-process limit)
- Client disconnect cancels session, releases permit, removes tool authorization
- Model unavailable returns typed error; deterministic features remain available

Configuration: `hbti.coach.streaming.*` properties

## Prompt Structure

```
src/main/resources/prompts/hbti/
├── core.txt                    # sent every request
└── scenes/
    ├── general-chat.txt
    ├── plan-generation.txt
    ├── daily-checkin.txt
    ├── weekly-review.txt
    ├── hbti-interpretation.txt
    └── safety-screening.txt
```

Core prompt is always loaded; scene prompts are loaded per request. Startup validates no empty files.

## Testing Strategy

**Default behavior**: external-service-free
- H2 database in MySQL compatibility mode
- Mock LangChain4j models
- No network calls to external APIs

**Explicit external tests**: set `RUN_EXTERNAL_TESTS=true` to run `ExternalModelSmokeTest`

**Test profiles**: use `@ActiveProfiles("test")` for test-specific configuration

**Golden fixtures**: `src/test/resources/fixtures/hbti-scoring-golden-v1.json` proves JavaScript prototype parity

## Common Patterns

### Running Flyway Migrations

Migrations run automatically on startup when `spring.flyway.enabled=true`. Migrations are **append-only** after merge to main.

To check migration status:
```bash
mvn flyway:info
```

### MyBatis Mapper Conventions

- Interface in module package (e.g., `identity/UserAccountMapper.java`)
- XML in `src/main/resources/mapper/` (e.g., `mapper/UserAccountMapper.xml`)
- `map-underscore-to-camel-case=true` enabled
- **Always include ownership predicates in WHERE clauses for user-owned tables**

### Transaction Boundaries

Service layer methods are transactional. Account-row locking for serializable operations:
```java
@Transactional
public void createIdempotentResource(...) {
    accountMapper.lockAccountRow(userId);
    // check idempotency, create resource
}
```

### Idempotency Pattern

1. Lock user account row
2. Compute SHA-256 digest of canonical input
3. Check existing digest
4. On match: return existing result
5. On mismatch with same key: throw conflict
6. On new: validate, persist, commit

## Documentation

- **Product spec**: `docs/specs/hbti-coach-product-spec.md`
- **Current architecture**: `docs/architecture/hbti-coach-architecture.md`
- **Historical evolution**: `docs/architecture/xiaozhi-to-hbti-coach-architecture.md`
- **ADRs**: `docs/decisions/ADR-*.md`
- **Shared HBTI agreement**: `docs/decisions/ADR-015-adopt-shared-hbti-research-development-agreement.md` (required before changing HBTI constructs, scoring, recommendation rules, or scientific claims)
- **Execution plan**: `tasks/plan.md` and `tasks/todo.md`

## Key Constraints

1. **Never commit production credentials** - use environment variables
2. **User health data requires explicit ownership** - always include `user_id` predicates
3. **Immutable after publication** - assessment definitions, plan version payloads, weekly reviews
4. **Fail closed** - when deterministic validation fails, do not fall back to AI decision
5. **L1 is not production-grade** - promotion requires 30 days measured SLO compliance, load tests, security review, backup restoration exercise

## MySQL Connection

Default: `jdbc:mysql://localhost:3306/hbti_coach`
- Username: `root` (default, override with `MYSQL_USERNAME`)
- Password: **required** via `MYSQL_PASSWORD`
- Pool size: 10 (override with `MYSQL_POOL_SIZE`)
- Connection timeout: 2s

**Important**: Must explicitly provide `MYSQL_PASSWORD`; production must not use default credentials.

## Redis Connection

Default: `redis://localhost:6379`
- Connect timeout: 200ms
- Command timeout: 200ms
- **Redis stores only ephemeral state** - never the sole copy of user data

## API Conventions

- Base path: `/api/v1`
- JSON: camelCase
- Error envelope: `{ error: { code, message, details } }`
- Paginated lists with deterministic ordering
- Idempotency keys for create/activate operations
- SSE streams: named events for metadata, tokens, completion, errors

OpenAPI generated and checked in build (Knife4j available at `/doc.html` when running).
