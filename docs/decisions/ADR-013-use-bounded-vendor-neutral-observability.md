# ADR-013: Use Bounded Vendor-Neutral Observability

## Status

Accepted

## Date

2026-08-15

## Context

The public beta must answer whether HTTP requests, model streams, coach tools and
security-relevant workflows are working without placing credentials, health facts,
assessment answers or model content into telemetry. It also needs distinct process
liveness and dependency readiness so an orchestrator can stop routing traffic without
restarting a healthy process.

Telemetry labels derived from users, request IDs, raw URLs or exception text would
create unbounded metric cardinality. Logging request bodies or exceptions from data and
model boundaries could leak secrets into a less protected log pipeline. Selecting a
telemetry vendor before release evidence exists would add operational coupling without
improving these boundaries.

## Decision

- Use Spring Actuator and Micrometer as the vendor-neutral metrics boundary. Do not add
  a metrics, tracing or logging vendor SDK at L1.
- Accept `X-Request-ID` only when it matches `[A-Za-z0-9._:-]{1,64}`; otherwise generate
  a UUID. Return it in the response, bind it to MDC for the request and always remove it.
- Use Logback's built-in JSON encoder and SLF4J key-value events. HTTP completion logs
  contain only request ID in MDC plus method, status class and duration. Audit failures
  contain only a fixed event name and audit-event enum. Request paths, headers, bodies,
  cookies, user IDs and exception text are excluded.
- Persist implemented audit workflows after their business transaction returns:
  registration, login success/failure, refresh success, token reuse, logout and plan
  activation. Audit persistence is best effort and never changes the API result.
- Allow anonymous audit subjects. Store the bounded request ID and remote IP separately.
  Recursively retain only approved detail keys and primitive values, with depth,
  collection, string and 2,000-character serialized limits.
- Publish these application metrics with fixed label sets:
  - `hbti.audit.events`: audit event enum and persistence outcome;
  - `hbti.coach.stream.duration`: eight fixed terminal outcomes;
  - `hbti.coach.stream.first_token`: first emitted chunk duration;
  - `hbti.coach.stream.tokens`: emitted SSE token chunks, not provider billing tokens;
  - `hbti.coach.sse.events`: metadata, token, completion or error;
  - `hbti.coach.tool.calls`: six allowlisted tool names and bounded result codes.
- Expose public `/actuator/health/liveness` with process state only. Expose public
  `/actuator/health/readiness` with process readiness, MySQL and Redis status. Component
  names are public, but health details require the dedicated `ACTUATOR_ADMIN` authority
  and remain hidden from ordinary authenticated users.
- Defer distributed tracing, dashboards and alerts until Task 23 supplies a telemetry
  backend, measured thresholds, runbooks and test-fired alert evidence.

## Alternatives Considered

### Log every request and model payload for debugging

Rejected because the highest-value debugging data is also the most sensitive. Correlation
IDs, bounded outcome codes and audit facts provide operational evidence without copying
credentials, health data or conversations into the log store.

### Use user, request and exception values as metric labels

Rejected because each distinct value creates a time series. High-cardinality lookups
belong in bounded audit records or correlated logs, not metrics.

### Add a third-party JSON logging encoder and tracing SDK now

Rejected because Logback already provides JSON encoding and Micrometer provides the
stable metric API. Task 18 must review every new dependency; Task 23 will choose an
export backend only when deployment and alert requirements are concrete.

## Consequences

- A request can be correlated between its response header, JSON logs and audit row.
- Redis or MySQL failure removes readiness while liveness remains up.
- Model and tool behavior can be aggregated without cardinality growth from user data.
- Audit writes can be lost during a database failure; the failure counter and JSON event
  reveal that degradation without failing the user's already-completed operation.
- Metrics count emitted text chunks, not tokenizer-accurate provider usage or cost.
- L1 has instrumentation, not an operated observability platform. SLO compliance,
  traces, dashboards, alerts and on-call evidence remain release-gate work.
