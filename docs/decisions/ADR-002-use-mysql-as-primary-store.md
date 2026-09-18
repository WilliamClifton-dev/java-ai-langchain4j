# ADR-002: Use MySQL As The Primary Durable Store

## Status

Accepted

## Date

2026-08-14

## Context

The initial course-based implementation stored each conversation as one serialized JSON document in MongoDB. The target product contains relational and transactional data: users, assessment versions and answers, safety screening, plan versions, daily records, weekly reviews, conversations, and ordered messages.

Keeping MongoDB only for chat would add another operational dependency while conversation ownership, deletion, auditing, and backup still depend on MySQL. The current whole-document update also creates avoidable write amplification and lost-update risk.

## Decision

Use MySQL 8 as the single durable source of truth for the public beta.

- Store each conversation and message as relational rows.
- Use MyBatis for explicit SQL and Flyway for versioned migrations.
- Store complete durable history in MySQL; pass only a bounded recent window to the model.
- Use Redis later only for ephemeral cache, rate limiting, and coordination. Redis is never the sole copy of user data.
- Do not retain MongoDB in the target runtime unless measured requirements justify a new ADR.

## Alternatives Considered

### MySQL For Business Data And MongoDB For Chat

This keeps flexible chat documents but doubles backup, monitoring, credentials, and deletion workflows. The expected beta traffic does not justify the operational cost.

### MongoDB As The Only Database

This would make relational ownership, versioning, uniqueness, and transactional plan updates harder to express and less aligned with the Java backend learning goal.

### PostgreSQL

PostgreSQL is a strong technical option, but MySQL matches the existing learning background and target Java job market. The selected design avoids database-specific behavior outside adapters so it can be revisited later.

## Consequences

- The MongoDB dependency, entity, configuration, and tests will be removed.
- Chat-memory updates initially replace the bounded memory window transactionally. An append-only optimization requires measured need and concurrency semantics.
- MySQL availability becomes part of the critical service path and needs migrations, pool monitoring, backups, and recovery tests.
- Developers can inspect the SQL used by the application and learn the Java-to-database boundary directly.
