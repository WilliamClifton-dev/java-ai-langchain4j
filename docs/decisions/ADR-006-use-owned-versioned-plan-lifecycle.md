# ADR-006: Use An Owned Versioned Plan Lifecycle

## Status

Accepted

## Date

2026-08-14

## Context

A weight-management target must be reviewable before activation and reproducible afterward. Updating one mutable plan row would erase the calculation, safety-screening, and HBTI inputs that justified the active target. Network retries and concurrent activation also must not create duplicate drafts or multiple active versions.

## Decision

- Store one user-owned `weight_plan` aggregate and append a `weight_plan_version` for each draft.
- Keep target and provenance fields immutable after insertion. Only lifecycle status and transition timestamps change.
- Require `DRAFT -> VALIDATED -> CONFIRMED -> ACTIVE`; activating a confirmed replacement changes the previous ACTIVE version to REPLACED in the same transaction.
- Treat `weight_plan.active_version_id` as the authoritative current pointer. The owner-scoped activation transaction assigns only a version loaded from the same aggregate; the foreign key uses `ON DELETE SET NULL` to preserve account-deletion viability.
- Lock the authenticated account row before allocating version numbers or activating versions so same-user writes serialize across application instances.
- Require the current eligible screening and unchanged profile, assessment, formula, and target-policy provenance before validation, confirmation, and activation.
- Require bounded idempotency keys for draft creation and activation. Persist only SHA-256 digests; replaying the same operation returns its original result and reusing a key for another operation is a conflict.
- Derive ownership only from the authenticated JWT subject and include `user_id` in every plan lookup path.

## Alternatives Considered

### Mutate one current plan row

Rejected because it destroys historical provenance and makes confirmation or rollback ambiguous.

### Let the latest version number be active

Rejected because drafts could become effective without explicit confirmation and activation.

### Rely only on optimistic status updates

Rejected because concurrent versions can both observe a valid predecessor. The account-row lock and aggregate pointer make activation order explicit.

### Store raw idempotency keys

Rejected because keys are opaque client material and do not need to remain recoverable.

## Consequences

- Users can inspect exactly which inputs and deterministic rules produced an active plan.
- Profile, screening, assessment, or policy changes require a new draft rather than silently altering an active version.
- Same-user plan writes serialize, which is appropriate for L1 scale but must be measured during Task 23 load testing.
- Plan payload corrections require a replacement version; migrations must not rewrite historical values.
