# HBTI Coach Product Specification

## Status

Approved for implementation on 2026-08-14.

## Objective

Build a public-beta-ready personalized weight-management platform for adults. The product combines deterministic health calculations, versioned HBTI behavioral assessment, daily execution tracking, and an AI coach. HBTI remains an exploratory behavioral preference model and must never be presented as a diagnosis or biological metabolic type.

Primary users can:

1. Create an account and control their own data.
2. Complete safety screening and a versioned HBTI assessment.
3. Receive a deterministic, explainable weight-management plan.
4. Record weight, nutrition, activity, training, and sleep.
5. Review weekly trends and adjust a plan deliberately.
6. Chat with an AI coach that can read and write data only through authorized tools.

Success means the complete critical flow works in a browser, is covered by automated tests, can be started through documented commands, and has deployment and recovery instructions.

## Product Boundary

### Included

- Responsive web application and REST/SSE backend.
- Account authentication and per-user resource ownership.
- User profile, safety screening, HBTI assessment, plans, daily records, weekly reviews, and coach conversations.
- MySQL as the durable source of truth.
- Redis for bounded ephemeral state, rate limiting, and cache only when a database fallback exists.
- Versioned prompts, assessment definitions, plan rules, and knowledge documents.
- RAG with source attribution, AI evaluation, observability, Docker, CI, backup and rollback documentation.

### Excluded From The Public Beta

- Medical diagnosis, treatment, prescriptions, or emergency services.
- Minors, pregnancy weight-loss plans, eating-disorder treatment, and complex clinical cases.
- Payments, social community, wearable synchronization, and food-photo recognition.
- Microservices. The target is a modular monolith until measured scale justifies extraction.

## Technology Stack

- Java 17, Spring Boot 3.2.x, Maven.
- MyBatis, Flyway, MySQL 8.
- Spring Security, JWT access and rotating refresh tokens.
- Redis for ephemeral controls; the service must fail conservatively when Redis is unavailable.
- LangChain4j with MiniMax-compatible and Ollama profiles.
- React, TypeScript, Vite, React Router, TanStack Query, and a restrained application UI.
- JUnit 5, AssertJ, Mockito, H2 for fast SQL integration tests, and browser tests for critical flows.
- Docker Compose and GitHub Actions.

## Commands

```powershell
mvn clean test
mvn -DskipTests package
mvn spring-boot:run
npm --prefix web ci
npm --prefix web test
npm --prefix web run build
docker compose up --build
```

Commands become mandatory only after the corresponding module exists.

## Project Structure

```text
src/main/java/.../
  identity/       authentication and ownership
  profile/        user profile and safety screening
  assessment/     HBTI definitions, responses, and scoring
  planning/       deterministic calculations and plan versions
  tracking/       daily records and weekly reviews
  coach/          AI orchestration, prompts, tools, and memory
  knowledge/      document ingestion and retrieval
  common/         narrow shared infrastructure
src/main/resources/
  db/migration/   Flyway migrations
  prompts/        versioned prompt assets
web/              React application
docs/             architecture, ADRs, operations, learning material
tasks/            approved plan and progress checklist
```

Dependencies flow from API and infrastructure adapters toward domain/application code. Domain modules do not call controllers, framework security contexts, or model-provider SDKs directly.

## Code Style

Use constructor injection, immutable request/response records, explicit domain names, and boundary validation. Avoid placeholder types and speculative abstractions.

```java
@Service
public class AssessmentService {
    private final AssessmentRepository assessments;

    public AssessmentService(AssessmentRepository assessments) {
        this.assessments = assessments;
    }
}
```

## Testing Strategy

- Unit tests: scoring, calculations, plan rules, authorization policies, and serialization.
- SQL integration tests: migrations, mapper queries, constraints, ordering, and transaction rollback.
- MVC tests: validation, authentication, ownership, error contracts, and response schemas.
- AI tests: deterministic prompt/tool contract tests by default; real models only through explicit opt-in.
- Browser tests: registration, assessment, plan, tracking, and chat critical flow.
- No default test may depend on an external model, MySQL, Redis, or vector service.

## Security And Safety Boundaries

Always validate external input, enforce ownership in application queries, keep secrets outside Git, cap model consumption, treat model output as untrusted, and persist an audit trail for security-sensitive changes.

Ask before adding a new external vendor, collecting a new class of sensitive data, or changing the scientific meaning of HBTI.

Never let prompts enforce authorization, expose another user's data, claim a model-generated value was persisted without a successful tool result, or present HBTI as medically validated.

## L1 Success Criteria

- The complete browser flow works with seeded demonstration data.
- Every protected resource is scoped to the authenticated user.
- Calculations and HBTI scoring are deterministic, versioned, and golden-tested.
- AI tools cannot bypass ownership or write invalid state.
- Default test and build commands pass without external services.
- Production-like Compose startup, health checks, migrations, logs, metrics, backup and rollback procedures are documented and exercised.
- API p95 excluding model generation is at most 500 ms at the documented L1 load; availability target is 99.5% with measured evidence.
- Security, AI evaluation, cost limits, data retention, deletion, and incident procedures have explicit release gates.

## Assumptions

- The first public beta targets a small number of adult users in one region and one application instance plus replaceable replicas.
- The current HBTI prototype remains the source for assessment semantics until definitions are imported and versioned here.
- A same-origin web deployment is preferred, while the API remains usable by documented clients.
- L2 production claims are prohibited until operational evidence satisfies the architecture promotion gate.
