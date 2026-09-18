# ADR-015: Adopt the shared HBTI research and development agreement

## Status

Accepted

## Date

2026-08-16

## Context

HBTI Coach is under active development while the scientific basis of HBTI is being refined in the sibling `weight-management` research repository. Waiting until implementation is complete would create avoidable rework. Replacing the model immediately would freeze new assumptions before evidence review is complete.

The canonical cross-project agreement is:

- [`weight-management/docs/decisions/ADR-002-hbti-research-development-agreement.md`](../../../weight-management/docs/decisions/ADR-002-hbti-research-development-agreement.md)

## Decision

Adopt the canonical agreement as the scientific and integration boundary for HBTI Coach.

Local implementation rules:

1. Keep published HBTI V1 definitions, scores, and historical results immutable, consistent with ADR-004.
2. Continue general Java development against HBTI V1 while research proceeds independently.
3. Introduce changed constructs, items, scoring, or recommendation mappings only through a new version such as HBTI V2.
4. Require every new version proposal to state its evidence, limitations, compatibility, migration behavior, and acceptance tests.
5. Treat HBTI as an explainable personalization modifier, not the sole source of diet, exercise, medical, or safety decisions.
6. Keep deterministic code responsible for calculations, eligibility, safety routing, authorization, and persisted facts. AI may explain and personalize within approved boundaries.
7. Preserve evidence source and evidence level for recommendation rules when that capability is introduced.
8. Treat `hbti-scoring-golden-v1.json` only as a software regression fixture, not a clinical or scientific gold standard.

## Consequences

- Current frontend and backend development can continue without an immediate HBTI rewrite.
- Research decisions enter Java through explicit, reviewable, versioned changes.
- Unsupported scientific claims remain outside product copy, prompts, APIs, and documentation.
- Future HBTI versions can evolve without corrupting V1 reproducibility.

## Related Decisions

- ADR-004: Freeze HBTI V1 definition and scoring
- ADR-005: Use versioned deterministic health calculations
- ADR-010: Use reviewed bounded lexical retrieval
