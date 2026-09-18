# ADR-004: Freeze HBTI V1 Definition And Scoring

## Status

Accepted

## Date

2026-08-14

## Context

The HBTI browser prototype is the current source of assessment semantics, but mutable JavaScript constants are not sufficient for a multi-user product. Historical results must remain interpretable after wording or scoring changes, and Java scoring must not drift from the research prototype.

The imported source is repository `WilliamClifton-dev/hbti-prototype` at commit `bdd1e9fbd75ae9ebdb869d42c61ae7c82cafc76e`. The recorded SHA-256 `dee8280f...de531a` is the hash of that commit's `hbti-data.js`, which contains the Chinese questionnaire, dimension definitions, poles, and item directions. English wording is imported from the same commit's `hbti-i18n.js`.

## Decision

Publish HBTI definition `1.0.0` and scoring rule `1.0.0` through append-only Flyway V4:

- 16 ordered self-report items use integer values from 1 to 5.
- Each item targets one pole of `FS`, `HC`, `RW`, or `ND`.
- Normalize an answer as `(value - 1) / 4`.
- A left-pole item contributes the normalized value to the left score; a right-pole item contributes `1 - normalized`.
- The dimension's left percentage is the JavaScript-compatible rounded mean times 100; the right percentage is `100 - left`.
- A 50/50 tie selects the left pole, matching the prototype.
- The four-letter type code is secondary to the ordered continuous dimension scores.
- Optional biomarker inputs do not alter questionnaire scores or type.

Application code exposes only a read-only definition catalog. Any change to items, wording, dimensions, answer range, direction, normalization, rounding, or tie behavior requires a new definition/scoring version and new golden fixtures. Merged Flyway migrations are never edited.

## Alternatives Considered

### Copy JavaScript constants into Java source

Rejected because provenance, database history, bilingual delivery, and future version selection would be difficult to audit.

### Call the browser scoring function from the backend

Rejected because it couples deterministic backend behavior to a UI runtime and weakens validation and operability.

### Include biomarker modifiers in V1 scoring

Rejected because the prototype itself keeps those values separate, and no empirical calibration justifies changing the questionnaire result.

## Consequences

- Flyway checksum validation protects the published import from silent migration edits.
- MySQL stores explicit definition provenance and ordered bilingual items.
- Java golden tests execute the actual persisted definition and match the prototype's outputs.
- HBTI remains exploratory and non-diagnostic; versioning improves traceability, not scientific validity.
