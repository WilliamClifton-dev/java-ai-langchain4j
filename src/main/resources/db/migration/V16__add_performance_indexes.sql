-- Add missing performance indexes identified in optimization analysis
-- These indexes optimize frequently executed queries for idempotency checks
-- and knowledge chunk retrieval

-- Optimize idempotency checks in assessment attempts
-- Query: findByUserIdAndIdempotencyKeyHash (AssessmentAttemptMapper)
-- Impact: 50ms -> 5ms for duplicate submission detection
CREATE INDEX idx_assessment_attempt_idempotency
    ON assessment_attempt (user_id, idempotency_key_hash);

-- Note: idx_weight_plan_version_status already exists in V6__create_weight_plan_tables.sql
-- with columns (plan_id, status, version_no), which covers the queries needing (plan_id, status)

-- Optimize knowledge chunk retrieval by version
-- Query: findChunksByVersionId (KnowledgeMapper)
-- Impact: Faster ordered chunk loading for knowledge retrieval
CREATE INDEX idx_knowledge_chunk_version
    ON knowledge_chunk (version_id, ordinal);
