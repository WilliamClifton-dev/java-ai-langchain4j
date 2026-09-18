# Learning Note 12: Weekly Deterministic Review

## What This Module Demonstrates

Task 12 converts raw tracking rows into a reproducible business decision without delegating arithmetic or writes to an LLM. It combines missing-data semantics, a versioned policy, immutable snapshots, owner-scoped SQL, and retry behavior derived from source facts.

## Code Map

- `V8__create_weekly_review_table.sql`: immutable outputs, version/input uniqueness, bounds, and foreign keys.
- `WeeklyReviewPolicy`: regression trend, coverage gates, adherence, and bounded proposals.
- `WeeklyReviewService`: transaction, account lock, active-plan snapshot, canonical input digest, replay, and version allocation.
- `WeeklyReviewMapper`: ordered owner-scoped fact reads and review persistence.
- `WeeklyReviewController`: JWT-derived generation and owned reads.
- `WeeklyReviewPolicyTest`: golden trend and bounded recommendation behavior.
- `WeeklyReviewMissingDataTest`: single-observation and unknown-value semantics.
- `WeeklyReviewPersistenceTest`: replay, late-data versioning, history preservation, and ownership.

## Why Missing Is Not Zero

No nutrition row means the system does not know what the user ate. Treating it as zero kcal would falsely increase adherence or create an unsafe adjustment. Coverage counts and nullable averages make uncertainty visible in both code and API responses.

## Why The Input Digest Is Not An Idempotency Key

The server derives this digest from the active plan, policy, window, and ordered facts. Clients do not choose it. Identical source state maps to the same immutable review; changed source state maps to a new version. This is content-addressed replay rather than request-key idempotency.

## Interview Questions

1. Why use regression rather than only first and last weight?
2. Why are weight and nutrition minimum-day thresholds different?
3. How does the account lock coordinate tracking writes with review generation?
4. Why persist the plan version and policy version with each review?
5. Why does late data create a version instead of updating a row?
6. What is the difference between coverage and adherence?
7. Why is an energy delta a proposal rather than a direct plan update?

## Accurate Resume Boundary

You can claim deterministic seven-day trend/adherence analysis, explicit sparse-data handling, immutable input-addressed versions, and bounded adjustment proposals. Do not claim clinical validity, automatic coaching safety, or production throughput without the later evaluation and load gates.
