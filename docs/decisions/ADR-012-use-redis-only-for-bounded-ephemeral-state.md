# ADR-012: Use Redis Only For Bounded Ephemeral State

## Status

Accepted

## Date

2026-08-15

## Context

Authentication and model admission must be shared across application instances, and
concurrent expensive requests benefit from a short coordination lease. A small public
HBTI definition is also read frequently but changes only by publishing a new version.
None of these needs justify moving durable user facts out of MySQL.

Redis can disappear, evict keys, or be unavailable during a failover. Storing tokens,
assessment answers, health measurements, plans, messages, prompts, or the only copy of
an idempotency result there would make correctness depend on a cache.

## Decision

- Keep MySQL as the durable source of truth and Redis as an optional expiring-state
  boundary behind `EphemeralStateStore`.
- Use atomic `INCR` plus first-write `PEXPIRE` for login and coach fixed-window limits.
  Login keys combine IP and normalized email through SHA-256 and expire after 15 minutes
  by default. Coach owner keys are SHA-256 namespaced and expire after 1 minute by
  default. Raw IP, email, owner and idempotency values never enter Redis keys.
- Coordinate HBTI assessment submissions with `SET NX` leases. Keys contain a SHA-256
  digest of owner and idempotency key; values contain a SHA-256 digest of a random lease
  token. Lease TTL is constrained to 1 second through 5 minutes and is 30 seconds for
  assessment submission. Release uses atomic compare-and-delete so an expired holder
  cannot delete its replacement.
- Cache only the published HBTI definition, a public and fully reconstructable value,
  under namespace `cache:hbti-definition:v1` for 1 hour. A miss, malformed entry, write
  failure, or Redis outage reads MySQL and does not change the result.
- Fail closed when Redis cannot enforce security-sensitive login or model admission.
  Bypass optional request coordination and cache reads so durable MySQL idempotency and
  source-of-truth behavior remain available.
- Never cache credentials, raw tokens, prompts, messages, assessment answers, user
  profiles, health measurements, plans, tracking facts, audit payloads, or model output.

## Alternatives Considered

### Store completed idempotency responses in Redis

Rejected because eviction or outage would change replay correctness. Completed results
and payload hashes remain transactional MySQL facts.

### Fail every operation when Redis is unavailable

Rejected because deterministic reads and durably idempotent writes do not require Redis.
Only admission controls whose absence would weaken abuse protection fail closed.

### Cache user profiles and active plans

Rejected for the public beta because invalidation and sensitive-data exposure add more
risk than the measured read volume justifies. These reads remain owner-scoped in MySQL.

## Consequences

- Rate limits and active leases coordinate across backend instances that share Redis.
- The model circuit breaker and local stream semaphore remain process-local; Redis does
  not make all reliability state distributed.
- Fixed windows allow a boundary burst and are not a token bucket. Task 23 must measure
  behavior before changing the algorithm.
- Redis outage intentionally reduces availability for login and model admission while
  keeping deterministic MySQL-backed features available.
- Default tests use an expiring in-memory fake and do not contact Redis.
