CREATE TABLE user_account (
    id VARCHAR(36) NOT NULL,
    normalized_email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_account_normalized_email UNIQUE (normalized_email),
    CONSTRAINT ck_user_account_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DELETED'))
);

CREATE TABLE refresh_token (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    replaced_by_token_id VARCHAR(36),
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replacement
        FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_token (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_refresh_token_user_family
    ON refresh_token (user_id, family_id);
