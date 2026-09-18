# ADR-014: Enforce Account Data Lifecycle And Active JWT Validation

## Status

Accepted

## Date

2026-08-15

## Context

The platform stores health-adjacent profile facts, assessment answers, plans,
tracking records and coach conversations. Users need a bounded export and a
predictable deletion workflow. Access JWTs are intentionally short-lived, but
an account deletion or lock must take effect before the token's nominal expiry.
Coach conversations also began as a legacy shared-memory table, so ownership
must be claimed without making old rows silently appear to belong to a user.

## Decision

- Derive the account owner only from the validated JWT subject. Export queries use
  explicit column allowlists and bounded row limits; credentials, token material,
  idempotency/payload hashes and model nonces are never exported.
- Require the exact `DELETE_MY_ACCOUNT` confirmation and CSRF proof for browser
  deletion. Delete restrictive user-owned rows first, anonymize retained audit
  rows, then hard-delete the account so owned conversations and messages cascade.
- Preserve global HBTI definitions and reviewed knowledge because they are not
  user-owned data. Record export and deletion lifecycle events without payloads.
- Query the account status during bearer JWT validation and accept only an
  existing `ACTIVE` account. Missing, locked, deleted or lookup-failure cases
  return the same authentication failure boundary.
- Add nullable `coach_conversation.user_id`; an authenticated request claims an
  unclaimed conversation, while a conflicting owner fails closed. Existing legacy
  rows are not bulk-assigned by migration.

## Alternatives Considered

### Trust JWT expiry for deletion

Rejected because a deleted account could retain access until token expiry.

### Export arbitrary serialized aggregates

Rejected because it risks leaking secrets and makes the public export contract
unstable. Explicit SQL allowlists make the boundary reviewable.

### Bulk-assign legacy conversations during migration

Rejected because the server cannot prove which historical account created an
unowned row. Claim-on-authentication avoids inventing ownership.

## Consequences

- Account deletion is a destructive, audited operation and must be covered by
  backups and a documented recovery process before L2 promotion.
- JWT validation adds one bounded MySQL status lookup to authenticated requests;
  database failure fails closed rather than accepting an unverifiable account.
- Conversation rows can remain unclaimed until their next authenticated use, so
  export completeness is defined over data that is provably owned.
- Redis remains ephemeral; durable lifecycle facts remain in MySQL.
