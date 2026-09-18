# Learning Note 16: Redis Ephemeral Controls

## What This Module Demonstrates

Task 16 uses Redis for coordination and acceleration without making it a second source
of truth. The design classifies each operation by what an outage is allowed to change.

## Code Map

- `EphemeralStateStore`: narrow port for expiring counters, values and leases.
- `RedisEphemeralStateStore`: atomic Lua counter and compare-and-delete operations.
- `InMemoryEphemeralStateStore`: deterministic Redis-free test implementation.
- `LoginAttemptGuard` and `CoachRateGuard`: shared fixed-window admission controls.
- `RequestLeaseCoordinator`: SHA-256 namespaced in-flight request leases.
- `CachedHbtiDefinitionCatalog`: one-hour cache of the public published definition.
- `RedisRatePolicyTest`: bounded pseudonymous rate keys and fail-closed admission.
- `RedisIdempotencyPolicyTest`: duplicate, TTL and stale-release lease behavior.
- `ReconstructableCacheTest`: cache hit and source-of-truth reconstruction.
- `RedisOutageModeTest`: explicit outage behavior by operation class.

## Why Compare-And-Delete Matters

A lease can expire while its original request still runs. Another request may then own a
new lease at the same key. Plain `DEL` from the first request would delete the new lease.
The Lua operation deletes only when the stored token digest still matches the caller.

## Why Cache Failure Is Not A Business Failure

The HBTI definition is published in MySQL and contains no user data. Redis may avoid a
query, but a cache miss, malformed JSON, timeout, or failed population reads MySQL. A
successful durable result is never changed into an error because caching failed.

## Outage Matrix

| Operation | Redis outage behavior | Reason |
|---|---|---|
| Login admission | reject | cannot safely weaken brute-force protection |
| Coach model admission | reject | protects bounded model cost and concurrency |
| In-flight assessment lease | bypass | MySQL lock, unique key and payload hash remain authoritative |
| Published definition cache | read MySQL | value is reconstructable and public |

## Interview Questions

1. Why does Redis not store the completed idempotency result?
2. How does `SET NX` differ from durable idempotency?
3. Why must lease release compare a token before deleting?
4. Which Redis failures fail closed, and which bypass the cache?
5. Why are owner and idempotency values hashed separately from lease values?
6. What burst behavior does a fixed-window counter permit?
7. Which state remains process-local after Task 16?

## Accurate Resume Boundary

You can claim shared expiring login/model limits, pseudonymous Redis keys, atomic
short-lived request leases, one reconstructable public-definition cache, and tested
outage modes. Do not claim a distributed model circuit breaker, globally exact token
bucket, Redis-backed durable idempotency, or measured multi-instance capacity.
