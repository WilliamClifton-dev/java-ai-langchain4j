# ADR-010: Use Reviewed Bounded Lexical Retrieval At L1

## Status

Accepted

## Date

2026-08-15

## Context

The coach needs traceable nutrition and training evidence without making deterministic APIs depend on a vector service or model provider. Unreviewed text, stale versions, missing citations, and prompt-injected document content are unsafe foundations for health-adjacent guidance. The initial public-beta corpus is expected to be small, and no measured requirement yet justifies operating a separate vector database.

## Decision

- Persist stable sources, immutable content versions, and ordered chunks in MySQL through Flyway V9.
- Allow only one published version per source; publishing a draft retires the prior published version transactionally.
- Keep ingestion behind an internal operator service boundary and require reviewed provenance with an HTTPS source URL, publisher, locale, retrieval time, reviewer, and SHA-256 content hash.
- Retrieve only rows filtered as `PUBLISHED` and exact-locale in SQL, with a hard 500-candidate limit.
- Score deterministically with Chinese Han bigrams and normalized alphanumeric terms, require a `0.20` match ratio, and return at most five passages.
- Preserve source key, title, URL, publisher, locale, version, and content hash in every LangChain4j passage. Return no evidence when validation or scoring fails.
- Keep user-owned data out of these tables and treat all source text as untrusted model context.

## Alternatives Considered

### Add a vector database immediately

Rejected because corpus size, semantic-recall need, and load have not been measured. It would add another durable dependency and operational failure mode before Task 23 produces evidence.

### Retrieve drafts and ask the prompt to ignore unsafe content

Rejected because lifecycle and authorization controls belong in deterministic SQL and code, not prompt instructions.

### Let users upload or import arbitrary URLs

Rejected for L1 because it creates moderation, copyright, malware, SSRF, and resource-exhaustion boundaries that the current product does not need.

## Consequences

- Default ingestion and retrieval tests require no model, vector store, Redis, or network access.
- Citations and stale-version exclusion are deterministic and testable.
- Lexical retrieval may miss synonyms and semantically related wording. This is an explicit L1 limitation, not vector-scale search.
- SQL reads at most 500 candidate chunks, so Task 23 must measure latency and recall before the reviewed corpus grows beyond that bounded design.
- A concurrent first insert for the same new source is safely constrained but may return a database conflict to one caller; an operator retry is idempotent. Transparent retry can be added if measured ingestion concurrency requires it.
