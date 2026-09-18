# Daily Tracking API

All routes require authentication. The owner is always the validated JWT subject. A request body cannot select another user, and responses do not expose owner IDs or idempotency digests.

## Date And Unit Contract

- `localDate` is interpreted in the time zone stored in the owner's profile.
- Accepted dates are the owner's current local date through 90 days ago; future dates are rejected.
- Weight is kilograms, energy is kcal, macronutrients are grams, and durations are whole minutes.
- Sleep quality is an integer from 1 through 5.

## Write Daily Metrics

`POST /api/v1/tracking/daily-metrics`

Required header: `Idempotency-Key`, 1-128 UTF-8 bytes.

```json
{
  "localDate": "2026-08-14",
  "weightKg": 70.2,
  "steps": 8000,
  "activityMinutes": 45,
  "sleepMinutes": 450,
  "sleepQuality": 4
}
```

At least one measurement is required. Weight and the other fields may be omitted independently.

## Write Nutrition

`POST /api/v1/tracking/nutrition`

Required header: `Idempotency-Key`, 1-128 UTF-8 bytes.

```json
{
  "localDate": "2026-08-14",
  "energyKcal": 2050,
  "proteinG": 125.5,
  "carbohydrateG": 220.0,
  "fatG": 65.0
}
```

There is one nutrition summary per owner and local date.

## Write Training

`POST /api/v1/tracking/training`

Required header: `Idempotency-Key`, 1-128 UTF-8 bytes. Multiple sessions may be recorded for one date.

```json
{
  "localDate": "2026-08-14",
  "trainingType": "STRENGTH",
  "durationMinutes": 60,
  "intensity": "HIGH"
}
```

Training types are `STRENGTH`, `CARDIO`, `MOBILITY`, `SPORT`, and `OTHER`. Intensities are `LOW`, `MODERATE`, and `HIGH`.

New writes return `201` with `{ "record": ..., "replayed": false }`. Replaying the same key and canonical payload returns the original record with `200` and `replayed: true`. Reusing a key for different facts returns a conflict.

## Read Daily Summary

`GET /api/v1/tracking/days/{localDate}`

The response contains nullable metric and nutrition facts, training sessions ordered by creation time and ID, and deterministic `trainingMinutes`.

## Stable Errors

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `INVALID_TRACKING_REQUEST` | Date, values, profile requirement, or idempotency key is invalid |
| 400 | `VALIDATION_ERROR` | JSON fields fail boundary validation |
| 409 | `TRACKING_DATE_CONFLICT` | A metric or nutrition row already exists for that local date |
| 409 | `TRACKING_IDEMPOTENCY_CONFLICT` | An idempotency key was reused for different facts |
