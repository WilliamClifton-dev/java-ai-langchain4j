CREATE TABLE coach_conversation (
    id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
);

CREATE TABLE coach_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(128) NOT NULL,
    sequence_no INT NOT NULL,
    message_json TEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_coach_message_sequence UNIQUE (conversation_id, sequence_no),
    CONSTRAINT fk_coach_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES coach_conversation (id)
        ON DELETE CASCADE
);
