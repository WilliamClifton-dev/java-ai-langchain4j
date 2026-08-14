# Learning Note 15: Streaming And Model Resilience

## What This Module Demonstrates

Task 15 turns model streaming into a bounded application workflow. SSE is only the transport adapter; a framework-neutral coordinator owns timeout, concurrency, cancellation, terminal state, and circuit-breaker behavior.

## Code Map

- `CoachController` and `SseEmitterCoachEventSink`: authenticated SSE endpoint and stable JSON event names.
- `CoachStreamingService`: first-token/total budgets, sequence numbers, concurrency permits, one terminal state, and bounded public errors.
- `ModelCircuitBreaker`: closed, open, and single-probe half-open state.
- `CoachStreamingModel`: narrow provider port used by deterministic tests.
- `LangChain4jCoachStreamingModel`: `TokenStream` adapter and exact invocation cleanup.
- `CoachInvocationRegistry`: short-lived server-owned context keyed by owner-namespaced memory ID.
- `CoachToolProvider`: dynamic tools that bind owner/nonce on the actual tool thread.
- `CoachStreamingConfig`: profile-specific provider wiring and external-service-free test port.
- `ModelOutageIsolationTest`: deterministic calculation remains available during model failure.

## Why SSE Has Named Events

Raw text cannot reliably distinguish content from completion or failure. Named events let the frontend update text for `token`, finish state for `completion`, and retry affordances for `error` without parsing model prose.

## Why Async Identity Is Explicit

A thread-local value belongs to one thread, not one logical request. The registry associates trusted invocation facts with the server-derived memory ID. LangChain4j supplies that ID to the dynamic tool provider, which opens the thread-local scope only while the selected tool executor runs.

## Why There Is No Automatic Retry

Once a token is visible or a write tool commits, replay is no longer generally safe. The coordinator returns a bounded terminal error and lets the client choose a new turn. Safe pre-output retries can be added only with classified provider failures and evidence that no tool side effect occurred.

## Interview Questions

1. Why is a request-thread `ThreadLocal` insufficient for streaming tool calls?
2. How does the dynamic tool provider derive identity without exposing it to the model?
3. Why must completion and error be mutually exclusive?
4. What is the difference between first-token timeout and total timeout?
5. Which failures should open the model circuit, and which should not?
6. Why does a half-open circuit allow only one probe?
7. Why is automatic retry unsafe after output or a tool call?
8. What does application cancellation guarantee when the SDK cannot cancel provider HTTP?
9. Why do deterministic APIs remain available during model outage?

## Accurate Resume Boundary

You can claim authenticated named-event SSE, explicit cross-thread tool authorization, configurable timeout/concurrency controls, a tested circuit breaker, and deterministic outage isolation. Do not claim distributed quotas, physical provider-request cancellation, measured first-token SLO, or multi-instance breaker consistency until later operational tasks provide them.
