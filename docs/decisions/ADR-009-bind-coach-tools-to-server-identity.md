# ADR-009: Bind Coach Tools To Server Identity

## Status

Accepted

## Date

2026-08-15

## Context

Tool calls are generated from untrusted model output influenced by user text. Letting a tool accept `userId`, arbitrary tool names, free-form JSON, or client-selected retry keys would turn prompt injection into an authorization or duplicate-write path.

## Decision

- Register a fixed allowlist of six typed LangChain4j tools.
- Derive the authenticated owner from the controller JWT and bind it in a server-only invocation context around the synchronous agent call.
- Exclude owner IDs and idempotency keys from every model-visible tool schema.
- Derive write keys from a server nonce, tool name, and canonical arguments; delegate all authorization, validation, transactions, and persistence to existing application services.
- Return success only after the service call returns. Convert argument, authorization, not-found, read, and write failures to bounded codes without exception details.
- Namespace chat memory with a server-derived hash of JWT owner and public conversation ID.

## Alternatives Considered

### Put the owner in the system prompt

Rejected because prompts provide guidance, not an authorization boundary, and can be influenced by model behavior.

### Let the model pass a user ID

Rejected because prompt injection could select another user even if the tool later attempted ad hoc checks.

### Let tools write mapper rows directly

Rejected because it bypasses application validation, transaction ownership, idempotency, and future audit hooks.

## Consequences

- Tool code remains a narrow adapter over tested application services.
- A model cannot expand the allowlist or choose an owner through arguments.
- Thread-local identity is valid only for the current synchronous agent. Task 15 streaming must use explicit context propagation or a request-bound executor.
- Memory rows are isolated by opaque owner namespace, but Task 18 still needs a relational owner path for export and deletion.
