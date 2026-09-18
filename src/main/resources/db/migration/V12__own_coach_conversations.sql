ALTER TABLE coach_conversation
    ADD COLUMN user_id VARCHAR(36);

ALTER TABLE coach_conversation
    ADD CONSTRAINT fk_coach_conversation_user
    FOREIGN KEY (user_id) REFERENCES user_account (id)
    ON DELETE CASCADE;

CREATE INDEX idx_coach_conversation_user_updated
    ON coach_conversation (user_id, updated_at, id);
