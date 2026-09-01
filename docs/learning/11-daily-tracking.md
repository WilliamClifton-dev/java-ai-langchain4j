# Learning Note 11: Daily Tracking

## What This Module Demonstrates

Task 11 models user execution data as typed facts rather than chat text. It combines relational constraints, owner-scoped SQL, local-date semantics, idempotent HTTP writes, and deterministic aggregation.

## Code Map

- `V7__create_daily_tracking_tables.sql`: constrained metric, nutrition, and training tables.
- `DailyTrackingService`: validation, profile-time-zone date policy, transactions, hashes, and aggregation.
- `DailyTrackingMapper`: parameterized owner-scoped SQL and account-row locking.
- `DailyTrackingController`: JWT-derived identity and typed REST resources.
- `DailyTrackingIdempotencyTest`: replay, conflicting payload, optional-weight, and future-date cases.
- `DailyTrackingApiTest`: authentication ownership, response status, stable errors, and summary contract.

## Why Local Date Is A Domain Value

A user's Friday is not always the server's Friday. The service reads the IANA time zone from the owned profile and calculates today's allowed boundary with an injected `Clock`. Persisting both `local_date` and `created_at` preserves user intent and technical audit timing.

## Why A Retry Needs Two Hashes

The key digest finds the original operation without storing opaque client material. The canonical payload digest distinguishes a legitimate retry from accidental reuse of the same key for different facts. Equal key and payload returns the original row; equal key and different payload returns `409`.

## Why Metric And Nutrition Differ From Training

A day has one consolidated metric row and one nutrition summary in the current product contract, so a second create is ambiguous and conflicts. A user can legitimately perform multiple training sessions, so training uses idempotency uniqueness rather than date uniqueness.

## Interview Questions

1. Why is an IANA time zone stored instead of a fixed UTC offset?
2. How do database constraints and service validation complement each other?
3. Why does idempotency compare both a key digest and payload digest?
4. Why must SQL include `user_id` even when resource IDs are UUIDs?
5. What race does the account-row lock prevent?
6. Why are multiple training rows valid while duplicate nutrition rows conflict?
7. How would you add an edit API without breaking create idempotency?

## Accurate Resume Boundary

You can claim typed MySQL tracking facts, owner-scoped APIs, retry-safe writes, local-date validation, and deterministic daily aggregation. Do not claim weekly trend analysis until Task 12 or distributed throughput until Task 23 provides evidence.
