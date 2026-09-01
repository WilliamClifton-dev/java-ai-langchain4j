CREATE TABLE daily_metric (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    local_date DATE NOT NULL,
    idempotency_key_hash CHAR(64) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    weight_kg DECIMAL(5, 2),
    steps INT,
    activity_minutes SMALLINT,
    sleep_minutes SMALLINT,
    sleep_quality TINYINT,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_metric_date UNIQUE (user_id, local_date),
    CONSTRAINT uk_daily_metric_idempotency UNIQUE (user_id, idempotency_key_hash),
    CONSTRAINT fk_daily_metric_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_daily_metric_values CHECK (
        (weight_kg IS NOT NULL OR steps IS NOT NULL OR activity_minutes IS NOT NULL
            OR sleep_minutes IS NOT NULL OR sleep_quality IS NOT NULL)
        AND (weight_kg IS NULL OR weight_kg BETWEEN 30 AND 350)
        AND (steps IS NULL OR steps BETWEEN 0 AND 100000)
        AND (activity_minutes IS NULL OR activity_minutes BETWEEN 0 AND 1440)
        AND (sleep_minutes IS NULL OR sleep_minutes BETWEEN 0 AND 1440)
        AND (sleep_quality IS NULL OR sleep_quality BETWEEN 1 AND 5)
    )
);

CREATE TABLE nutrition_log (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    local_date DATE NOT NULL,
    idempotency_key_hash CHAR(64) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    energy_kcal INT NOT NULL,
    protein_g DECIMAL(6, 1) NOT NULL,
    carbohydrate_g DECIMAL(6, 1) NOT NULL,
    fat_g DECIMAL(6, 1) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_nutrition_log_date UNIQUE (user_id, local_date),
    CONSTRAINT uk_nutrition_log_idempotency UNIQUE (user_id, idempotency_key_hash),
    CONSTRAINT fk_nutrition_log_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_nutrition_log_values CHECK (
        energy_kcal BETWEEN 0 AND 10000
        AND protein_g BETWEEN 0 AND 1000
        AND carbohydrate_g BETWEEN 0 AND 1000
        AND fat_g BETWEEN 0 AND 1000
    )
);

CREATE TABLE training_log (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    local_date DATE NOT NULL,
    idempotency_key_hash CHAR(64) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    training_type VARCHAR(16) NOT NULL,
    duration_minutes SMALLINT NOT NULL,
    intensity VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_training_log_idempotency UNIQUE (user_id, idempotency_key_hash),
    CONSTRAINT fk_training_log_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_training_log_type CHECK (
        training_type IN ('STRENGTH', 'CARDIO', 'MOBILITY', 'SPORT', 'OTHER')
    ),
    CONSTRAINT ck_training_log_intensity CHECK (
        intensity IN ('LOW', 'MODERATE', 'HIGH')
    ),
    CONSTRAINT ck_training_log_duration CHECK (duration_minutes BETWEEN 1 AND 600)
);

CREATE INDEX idx_training_log_user_date
    ON training_log (user_id, local_date, created_at, id);
