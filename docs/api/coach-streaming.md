# Coach Streaming API

## Start A Stream

`POST /api/v1/coach/messages/stream`

- Authentication: access JWT cookie or bearer token.
- CSRF: required for cookie-authenticated browser requests.
- Request content type: `application/json`.
- Response content type: `text/event-stream`.
- Request body: the same validated `conversationId`, `scene`, and `message` fields as synchronous coach chat. No owner, tool permission, or retry nonce is accepted from the client.

```json
{
  "conversationId": "conversation-1",
  "scene": "GENERAL_CHAT",
  "message": "How should I start this week?"
}
```

## Event Contract

Events arrive in this order: one `metadata`, zero or more `token`, then exactly one `completion` or `error`. JSON field names are stable and token sequence numbers start at 1.

```text
event:metadata
data:{"conversationId":"conversation-1","scene":"GENERAL_CHAT"}

event:token
data:{"sequence":1,"text":"Start by recording"}

event:completion
data:{"conversationId":"conversation-1"}
```

An error is a terminal SSE event, not an internal exception dump:

```text
event:error
data:{"code":"MODEL_TIMEOUT","message":"Coach model response timed out","retryable":true}
```

| Code | Meaning |
|---|---|
| `MODEL_CIRCUIT_OPEN` | recent provider failures opened the local circuit |
| `MODEL_CONCURRENCY_LIMIT` | local model-stream capacity is full |
| `MODEL_FIRST_TOKEN_TIMEOUT` | no token arrived within the configured first-token budget |
| `MODEL_TIMEOUT` | the configured total stream budget expired |
| `MODEL_UNAVAILABLE` | model startup or provider streaming failed |

The client may reconnect with a new request after a retryable error. The server does not automatically replay a request after any output because a streamed turn may already have executed a committed tool write.

## Cancellation

Closing the connection causes the server to close the application session, release its local permit, remove its tool authorization registration, and ignore late callbacks. The current LangChain4j version does not expose physical provider HTTP cancellation, so upstream transport work may continue until its own timeout even though no further events or tools are accepted.
