-- Add retention policy for inactive coach conversations
-- Conversations older than 90 days with no activity will be eligible for cleanup

ALTER TABLE coach_conversation
    ADD COLUMN last_message_at TIMESTAMP(6);

-- Backfill existing conversations with updated_at value
UPDATE coach_conversation SET last_message_at = updated_at;

-- Make the column non-nullable after backfill (H2 compatible syntax)
ALTER TABLE coach_conversation
    ALTER COLUMN last_message_at TIMESTAMP(6) NOT NULL;

-- Add index for retention cleanup queries
CREATE INDEX idx_coach_conversation_retention
    ON coach_conversation (last_message_at);