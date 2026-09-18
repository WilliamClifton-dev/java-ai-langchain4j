# Weekly Review API

All routes require authentication. Owner identity comes only from the validated JWT subject. Reviews are deterministic planning aids; they are not diagnoses, medical advice, or automatically applied plan changes.

## Generate Or Replay

`POST /api/v1/tracking/weekly-reviews`

```json
{
  "windowEnd": "2026-08-14"
}
```

The server derives `windowStart` as six days before `windowEnd` in the profile IANA time zone. The complete window must be within the supported 90-day tracking history and cannot extend into the future. An active plan is required.

The first generation for a distinct snapshot returns `201` and `replayed: false`. Repeating against the same active plan, policy, and ordered facts returns the original immutable review with `200` and `replayed: true`. Late facts or a changed active plan create the next version for that window.

## Read

`GET /api/v1/tracking/weekly-reviews/{reviewId}`

Cross-user and unknown identifiers both return `404 WEEKLY_REVIEW_NOT_FOUND`.

## Interpretation Contract

- Weight trend is absent unless at least three weight days exist.
- Nutrition adherence is absent when no nutrition days exist.
- An adjustment requires at least three weight days, four nutrition days, and 75% energy-range adherence.
- An adjustment proposal is limited to `-100`, `0`, or `+100 kcal/day`.
- `INSUFFICIENT_DATA`, `HOLD`, `INCREASE_ENERGY`, and `DECREASE_ENERGY` are proposals only.
- Every response includes a limitation stating that no plan change is automatically applied.

## Stable Errors

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `INVALID_WEEKLY_REVIEW_REQUEST` | The local-date window is invalid |
| 400 | `VALIDATION_ERROR` | The request body failed boundary validation |
| 404 | `WEEKLY_REVIEW_NOT_FOUND` | No owned review is visible |
| 409 | `WEEKLY_REVIEW_PREREQUISITE_NOT_MET` | An owned profile or active plan is missing |
