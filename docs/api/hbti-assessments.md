# HBTI Assessment API

Base path: `/api/v1/assessments/hbti`

All endpoints require an access token. The server derives the owner from the JWT subject. Client `userId`, score, type, or interpretation fields are ignored and never trusted.

## Submit

`POST /submissions` requires CSRF proof for cookie authentication and an `Idempotency-Key` header of 1 to 128 UTF-8 bytes.

```json
{
  "definitionVersion": "1.0.0",
  "answers": [
    { "itemKey": "q1", "value": 3 },
    { "itemKey": "q2", "value": 4 }
  ]
}
```

The real request must contain exactly one valid answer for every item in the selected published definition. The example is abbreviated. The service validates and scores the full answer set, then commits the attempt, answers, and four dimension scores in one transaction.

The first accepted request returns `201`; replaying the same key and canonical payload returns the original result with `200` and `replayed: true`. Reusing a key with a different definition or answer payload returns `409 IDEMPOTENCY_CONFLICT`. Raw idempotency keys are not stored; MySQL stores a SHA-256 digest.

## Read Results

| Method | Path | Result |
|---|---|---|
| `GET` | `/results/current` | latest result owned by the authenticated user |
| `GET` | `/results?page=0&pageSize=20` | deterministic newest-first owned history; page size 1-100 |

Each result returns definition and scoring versions, ordered continuous dimensions, a secondary four-letter code, completion time, and this limitation:

> HBTI is an exploratory behavioral tendency assessment, not a diagnosis.

Answers are not returned by these endpoints. They remain durable facts for version traceability and future authenticated data export.

## Stable Errors

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | JSON fields fail boundary validation |
| 400 | `INVALID_ASSESSMENT_REQUEST` | idempotency or pagination boundary is invalid |
| 400 | `INVALID_ASSESSMENT_ANSWERS` | answer set is incomplete, duplicate, unknown, or out of range |
| 404 | `ASSESSMENT_DEFINITION_NOT_FOUND` | requested published definition does not exist |
| 404 | `ASSESSMENT_RESULT_NOT_FOUND` | authenticated user has no current result |
| 409 | `IDEMPOTENCY_CONFLICT` | key was already used with another payload |
