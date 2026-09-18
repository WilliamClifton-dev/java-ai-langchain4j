CREATE TABLE audit_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    remote_address VARCHAR(45),
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    details VARCHAR(4000)
);

CREATE INDEX idx_audit_user_time ON audit_event (user_id, event_time DESC);
CREATE INDEX idx_audit_type_time ON audit_event (event_type, event_time DESC);
