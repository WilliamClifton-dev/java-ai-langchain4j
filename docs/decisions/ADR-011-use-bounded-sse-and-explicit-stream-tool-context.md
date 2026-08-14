# ADR-011: Use Bounded SSE And Explicit Stream Tool Context

## Status

Accepted

## Date

2026-08-15

## Context

Synchronous coach calls bind tool identity in a thread-local scope. A streaming model invokes callbacks and tools on SDK-managed threads, so copying that design unchanged could lose authorization or accidentally reuse stale context. Streaming also needs a stable browser contract and bounded failure behavior when the provider is slow or unavailable.

## Decision

- Expose authenticated coach streaming as SSE with named `metadata`, `token`, `completion`, and `error` JSON events.
- Put provider integration behind `CoachStreamingModel`; keep MVC, timeout, concurrency, and breaker logic independent of LangChain4j.
- Register a server-created owner, conversation, memory ID, and nonce for the lifetime of one stream. Use a dynamic `ToolProvider` to bind that invocation on the actual tool-execution thread; never add owner or nonce to model-visible schemas.
- Resolve timeout, provider error, completion, and cancellation through one atomic terminal state. Send exactly one completion or error event and suppress late callbacks.
- Enforce configurable first-token and total timeouts, a local concurrency semaphore, and a consecutive-failure circuit with one half-open probe.
- Do not retry after output begins or when a tool might have committed. Keep deterministic modules independent of the model port.

## Alternatives Considered

### Reuse the request-thread context around `TokenStream.start()`

Rejected because tool execution may occur on another thread after `start()` returns.

### Put owner identity in tool arguments

Rejected because model-visible identity is not an authorization boundary and can be influenced by prompt injection.

### Return an unstructured text stream

Rejected because clients could not distinguish tokens, completion, bounded failures, and future metadata without fragile parsing.

### Add automatic provider retries

Rejected after output or tool execution because replay could duplicate user-visible text or side effects. Pre-output retry remains deferred until provider error classification and idempotency evidence justify it.

## Consequences

- Tool authorization survives asynchronous execution without trusting prompt text or inheriting a thread-local across threads.
- Model failure opens only the coach circuit; deterministic profile, assessment, planning, tracking, and review APIs remain independent.
- Breaker and concurrency state are local to one process and are not a distributed quota.
- Cancellation removes authorization, releases application resources, and suppresses callbacks. LangChain4j `1.0.0-beta3` exposes no provider-request cancellation handle, so physical HTTP interruption is not guaranteed.
- Stream state is ephemeral and reconstructable; durable messages remain in MySQL through the chat memory adapter.
