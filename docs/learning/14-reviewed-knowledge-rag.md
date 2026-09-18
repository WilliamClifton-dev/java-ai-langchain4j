# Learning Note 14: Reviewed Knowledge And RAG

## What This Module Demonstrates

Task 14 makes retrieval provenance and publication state deterministic. MySQL stores reviewed source versions and chunks; the model receives only published, locale-matched passages with citations. No model or vector provider is needed to test the behavior.

## Code Map

- `V9__create_reviewed_knowledge_tables.sql`: source, immutable version, lifecycle, chunk ordering, uniqueness, and foreign keys.
- `KnowledgeIngestionService`: validation, hashing, deterministic chunking, version replay, and transactional publication.
- `KnowledgeMapper`: published-only SQL, exact locale filter, and hard 500-row candidate limit.
- `ReviewedKnowledgeRetriever`: bounded lexical scoring, no-evidence behavior, result cap, and LangChain4j citation metadata.
- `KnowledgeIngestionTest`: idempotency, changed versions, publication replacement, and validation.
- `ReviewedKnowledgeRetrieverTest`: publication, locale, limits, ordering, and citation conversion.
- `KnowledgeRetrievalEvaluationTest`: expected-source recall, citation correctness, stale exclusion, prompt-injection isolation, and no-evidence fixtures.

## Why Lifecycle Filtering Is In SQL

Draft and retired rows must never become candidates. Filtering after retrieval would increase exposure and let future code accidentally use stale text. The query therefore requires `status = 'PUBLISHED'` and an exact locale before its hard candidate limit.

## Why Citations Exist In Metadata And Text

Structured metadata lets application code inspect provenance without parsing prose. The model-visible prefix keeps the source attached when LangChain4j assembles augmented context. Both include a version and content hash, so an answer can be traced to the reviewed artifact.

## Why This Is Not A Vector Search Claim

The current scorer measures exact normalized term overlap, including Chinese bigrams. It is deterministic and adequate for a small controlled corpus, but it does not understand synonyms or semantic similarity. A vector service should be introduced only after evaluation and load evidence show the lexical boundary is insufficient.

## Interview Questions

1. Why must publication status and locale be filtered in SQL rather than only in Java?
2. What does a content hash prove, and what does it not prove about source quality?
3. How does publishing a new version prevent stale evidence from being retrieved?
4. Why are citations stored in both metadata and model-visible context?
5. How do candidate and result limits reduce denial-of-service risk?
6. Why is source text untrusted even after a human review?
7. When would a vector database become justified?
8. What race remains during concurrent first ingestion, and why is the unique constraint still necessary?

## Accurate Resume Boundary

You can claim versioned reviewed knowledge, transactional publication, published-only bounded retrieval, citation-bearing LangChain4j RAG, and deterministic evaluation tests. Do not claim semantic vector search, internet-scale ingestion, or measured production recall and throughput until Task 23 provides that evidence.
