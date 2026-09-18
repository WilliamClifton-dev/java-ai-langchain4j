# ADR-008: Version Deterministic Weekly Reviews

## Status

Accepted

## Date

2026-08-14

## Context

A review can be regenerated after retries, late tracking entries, or a plan change. Overwriting one row would hide which facts and targets produced an earlier recommendation. Sparse measurements also make a single-day change too noisy for automatic adjustment.

## Decision

- Analyze exactly seven owner-local calendar dates against the current active plan.
- Version the algorithm as `DETERMINISTIC_WEEKLY_REVIEW_V1` and calculate trend with an ordered least-squares slope projected to seven days.
- Require at least three weight observations and four nutrition logs before proposing an adjustment. Require at least 75% of logged nutrition days inside the plan energy range.
- Bound proposals to `100 kcal/day` in either direction. A proposal never updates or activates a plan.
- Persist immutable review outputs with the active plan version, input digest, policy version, window, and coverage counts.
- Replay an identical input snapshot. Create the next window version when late facts or the active plan change.
- Serialize generation with plan and tracking writes using the owner account row lock, and include `user_id` in every query.

## Alternatives Considered

### Ask the model to summarize and adjust

Rejected because model output is not a reproducible safety or calculation boundary.

### Compare only the first and last weight

Rejected because two noisy measurements dominate the result and missing middle observations provide no robustness.

### Overwrite the review after late data

Rejected because it destroys the audit trail and makes a previously shown recommendation irreproducible.

## Consequences

- Sparse weeks return an explicit data-insufficient result rather than fabricated certainty.
- Late facts produce another immutable version, increasing storage modestly.
- The trend is an estimate over one week, not proof of tissue change or a medical conclusion.
- Thresholds and algorithm changes require a new policy version and golden fixtures.
