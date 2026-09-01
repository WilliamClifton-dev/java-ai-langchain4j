# Weight Plan API

All routes require authentication. The owner is always the validated JWT subject; request bodies do not accept ownership, status, formula, provenance, or calculated target fields.

## Lifecycle

```text
DRAFT -> VALIDATED -> CONFIRMED -> ACTIVE -> REPLACED
```

Skipping or repeating a guarded transition returns `409 INVALID_PLAN_TRANSITION`. Validation, confirmation, and activation fail closed if the profile, screening, HBTI result, formula, or target policy changed after draft creation.

## Create Draft

`POST /api/v1/plans/drafts`

Required header: `Idempotency-Key`, 1-128 UTF-8 bytes.

```json
{
  "goal": "LOSS"
}
```

Goals are `LOSS`, `MAINTENANCE`, or `GAIN`. A successful request returns `201` and a version with status `DRAFT`. Replaying the same key and goal returns the original version. Reusing the key for another goal returns `409 PLAN_IDEMPOTENCY_CONFLICT`.

## Read

- `GET /api/v1/plans/active`
- `GET /api/v1/plans/{planId}/versions/{versionId}`

Cross-user identifiers and missing versions both return `404 PLAN_VERSION_NOT_FOUND`.

## Transition

- `POST /api/v1/plans/{planId}/versions/{versionId}/validation`
- `POST /api/v1/plans/{planId}/versions/{versionId}/confirmation`
- `POST /api/v1/plans/{planId}/versions/{versionId}/activation`

Activation requires a bounded `Idempotency-Key`. A successful activation atomically replaces the prior active version. Replaying the same activation key returns its original version; using that key for another version returns `409 PLAN_IDEMPOTENCY_CONFLICT`.

## Response Boundary

Responses expose deterministic calculation and target-policy versions, BMI/BMR/TDEE estimates, bounded target ranges, lifecycle timestamps, and this limitation:

> Targets are planning estimates, not medical prescriptions or guaranteed outcomes.

They do not expose user IDs, raw idempotency keys, idempotency digests, assessment answers, or safety-screening answers.

## Stable Errors

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `INVALID_PLAN_REQUEST` | The bounded request or idempotency key is invalid |
| 400 | `VALIDATION_ERROR` | The request body failed boundary validation |
| 404 | `PLAN_VERSION_NOT_FOUND` | No owned version is visible |
| 409 | `INVALID_PLAN_TRANSITION` | The version is not in the required state |
| 409 | `PLAN_IDEMPOTENCY_CONFLICT` | An idempotency key was reused for another operation |
| 409 | `PLANNING_PREREQUISITE_NOT_MET` | Required profile, screening, assessment, or provenance is missing, blocked, or stale |
