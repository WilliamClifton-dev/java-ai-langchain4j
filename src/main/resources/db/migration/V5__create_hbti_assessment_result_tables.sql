CREATE TABLE assessment_attempt (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    definition_id VARCHAR(36) NOT NULL,
    idempotency_key_hash CHAR(64) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    type_code CHAR(4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assessment_attempt_idempotency
        UNIQUE (user_id, idempotency_key_hash),
    CONSTRAINT fk_assessment_attempt_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assessment_attempt_definition
        FOREIGN KEY (definition_id) REFERENCES assessment_definition (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_assessment_attempt_status
        CHECK (status = 'COMPLETED')
);

CREATE TABLE assessment_answer (
    attempt_id VARCHAR(36) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    item_key VARCHAR(16) NOT NULL,
    answer_value TINYINT NOT NULL,
    PRIMARY KEY (attempt_id, item_key),
    CONSTRAINT fk_assessment_answer_attempt
        FOREIGN KEY (attempt_id) REFERENCES assessment_attempt (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_assessment_answer_item
        FOREIGN KEY (item_id) REFERENCES assessment_item (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_assessment_answer_value
        CHECK (answer_value BETWEEN 1 AND 5)
);

CREATE TABLE assessment_score (
    attempt_id VARCHAR(36) NOT NULL,
    dimension_code VARCHAR(8) NOT NULL,
    ordinal TINYINT NOT NULL,
    chosen_pole CHAR(1) NOT NULL,
    left_score TINYINT NOT NULL,
    right_score TINYINT NOT NULL,
    PRIMARY KEY (attempt_id, dimension_code),
    CONSTRAINT uk_assessment_score_ordinal UNIQUE (attempt_id, ordinal),
    CONSTRAINT fk_assessment_score_attempt
        FOREIGN KEY (attempt_id) REFERENCES assessment_attempt (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_assessment_score_range CHECK (
        left_score BETWEEN 0 AND 100
        AND right_score BETWEEN 0 AND 100
        AND left_score + right_score = 100
    )
);

CREATE INDEX idx_assessment_attempt_user_completed
    ON assessment_attempt (user_id, completed_at, id);
