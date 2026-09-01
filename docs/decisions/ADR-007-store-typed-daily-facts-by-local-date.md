# ADR-007: Store Typed Daily Facts By Local Date

## Status

Accepted

## Date

2026-08-14

## Context

Weekly review needs reproducible raw facts, while mobile clients must be able to retry writes after uncertain network outcomes. One free-form journal row would blur units and make deterministic aggregation unreliable. UTC timestamps alone also do not answer which calendar day a user intended to record.

## Decision

- Store daily metrics, nutrition summaries, and training sessions in separate typed tables with explicit metric units and database bounds.
- Use the owner's profile IANA time zone to determine the current date. Accept today through 90 days ago and reject future or older writes.
- Allow one metric row and one nutrition row per owner/local date. Allow multiple typed training sessions on the same date.
- Require a bounded idempotency key for every write, persist only its SHA-256 digest, and compare a canonical payload digest on retry.
- Lock the authenticated account row before checking replay or date uniqueness so same-user writes serialize across instances.
- Derive the owner from JWT and include `user_id` in every lookup. Aggregate daily totals in deterministic code rather than through a model.

## Alternatives Considered

### One JSON daily journal

Rejected because schema drift, ambiguous units, and database-unchecked ranges make review calculations unreliable.

### Use server UTC date

Rejected because a record near midnight can land on the wrong day for the user and distort seven-day trends.

### Upsert by date

Rejected for the first public contract because retries could silently overwrite facts. Explicit conflict semantics make edits a separate future use case.

## Consequences

- Weekly aggregation has bounded, unit-stable inputs and does not depend on model interpretation.
- Clients must use a new idempotency key for each distinct training session.
- Changing a daily metric or nutrition summary requires a future explicit edit contract; create requests never overwrite.
- The account-row lock limits same-user write parallelism and must be measured during Task 23.
