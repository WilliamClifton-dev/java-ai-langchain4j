CREATE TABLE audit_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    remote_address VARCHAR(45),
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    details JSON,
    INDEX idx_user_time (user_id, event_time DESC),
    INDEX idx_type_time (event_type, event_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
