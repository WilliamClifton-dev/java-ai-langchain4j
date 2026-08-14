# Learning Note 13: Authorized Coach Tools

## What This Module Demonstrates

Task 13 treats model tool calls as untrusted requests. The model can select only reviewed operations and values; the server selects identity, retry material, application service, and transaction semantics.

## Code Map

- `CoachTools`: six `@Tool` methods and bounded result codes.
- `CoachToolContext`: short-lived authenticated identity and server request nonce.
- `CoachChatService`: opens and always clears context around the agent invocation.
- `CoachMemoryKey`: owner-namespaced internal conversation identifier.
- `HbtiCoachAgent`: explicit registration of the single `coachTools` bean.
- `CoachToolAuthorizationTest`: prompt text cannot replace the bound owner.
- `CoachToolSchemaTest`: allowlist and no-owner-argument contract.
- `CoachToolRollbackTest`: a failed application write is never reported as success.

## Why A Tool Is Not An Authorization Boundary

The model chooses whether to request a tool, but it is not a trusted principal. Authorization starts from the verified JWT subject and remains enforced in owner-scoped application services and SQL. Tool descriptions and prompts do not grant permission.

## Why Success Comes After The Service Call

Spring applies the transaction around the application service proxy. The tool waits for that method to return before constructing `success=true`. If validation, SQL, or commit processing throws, the tool returns `TOOL_WRITE_FAILED` with no data and no internal error text.

## Interview Questions

1. Why must `userId` be absent from a model-visible tool schema?
2. What protection remains if a prompt injection convinces the model to call a write tool?
3. Why should tools delegate to application services rather than mappers?
4. How is tool retry material produced without trusting the model?
5. Why is a `ThreadLocal` context unsafe to reuse unchanged for asynchronous streaming?
6. What does owner-namespaced memory prevent, and what does it not solve?
7. How would you audit a tool call without logging sensitive arguments?

## Accurate Resume Boundary

You can claim a typed tool allowlist with server-bound JWT ownership, application-service authorization, retry-safe writes, and injection/rollback tests. Do not claim asynchronous context safety, complete chat data lifecycle, or model resilience until Tasks 15 and 18 deliver those controls.
