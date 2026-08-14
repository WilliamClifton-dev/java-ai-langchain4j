# ADR-005: Use Versioned Deterministic Health Calculations

## Status

Accepted

## Date

2026-08-14

## Context

Weight-management plans need explainable starting estimates. A language model must not invent BMI, energy expenditure, or calorie targets, and silent formula changes would make plan versions impossible to audit. At the same time, these calculations are population estimates rather than diagnoses or guaranteed individual requirements.

## Decision

Use deterministic metric calculations identified by explicit versions:

- `MIFFLIN_ST_JEOR_METRIC_V1` calculates BMI and Mifflin-St Jeor BMR from adult age, calculation sex, centimetres, and kilograms.
- TDEE multiplies unrounded BMR by the profile activity factor: sedentary `1.2`, light `1.375`, moderate `1.55`, or very active `1.725`.
- BMI rounds to one decimal and energy estimates to whole kcal using `HALF_UP`.
- `CONSERVATIVE_ENERGY_RANGE_V1` proposes starting ranges from TDEE: loss `80%-90%`, maintenance `97%-103%`, and gain `105%-110%`.
- A loss-range lower bound never falls below the calculated BMR.
- Weekly weight-change ranges are policy guardrails, not promised outcomes.

Automatic range generation requires the latest safety-screening version, an eligible status, and a profile that has not changed after screening. Missing, risk-routed, mismatched, or stale screening state fails closed.

## Alternatives Considered

### Ask the model to estimate targets

Rejected because results would be non-repeatable, difficult to test, and vulnerable to prompt manipulation.

### Store only a single calorie target

Rejected because false precision hides uncertainty and makes user confirmation less meaningful.

### Add more complex adaptive formulas immediately

Rejected until daily tracking and weekly review provide measured evidence. Complexity without outcome data would be cosmetic.

## Consequences

- Calculations are fast, testable, and available during model outages.
- Every future formula or target-policy change requires a new version and regression fixtures.
- Plan APIs must expose assumptions and ranges, and must never describe them as medical prescriptions.
- Individual adjustment happens through bounded, evidence-based weekly review rather than model improvisation.
