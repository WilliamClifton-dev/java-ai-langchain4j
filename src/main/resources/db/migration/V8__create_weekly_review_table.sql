CREATE TABLE weekly_review (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    plan_version_id VARCHAR(36) NOT NULL,
    window_start DATE NOT NULL,
    window_end DATE NOT NULL,
    version_no INT NOT NULL,
    input_hash CHAR(64) NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    weight_observation_days TINYINT NOT NULL,
    nutrition_logged_days TINYINT NOT NULL,
    steps_observed_days TINYINT NOT NULL,
    sleep_observed_days TINYINT NOT NULL,
    training_days TINYINT NOT NULL,
    average_weight_kg DECIMAL(5, 2),
    weight_trend_percent DECIMAL(6, 2),
    nutrition_adherence_percent TINYINT,
    average_steps INT,
    average_sleep_minutes SMALLINT,
    total_training_minutes SMALLINT NOT NULL,
    recommendation VARCHAR(32) NOT NULL,
    proposed_energy_delta_kcal SMALLINT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_weekly_review_version
        UNIQUE (user_id, window_end, version_no),
    CONSTRAINT uk_weekly_review_input
        UNIQUE (user_id, window_end, input_hash),
    CONSTRAINT fk_weekly_review_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_weekly_review_plan_version
        FOREIGN KEY (plan_version_id) REFERENCES weight_plan_version (id),
    CONSTRAINT ck_weekly_review_window CHECK (window_start <= window_end),
    CONSTRAINT ck_weekly_review_version CHECK (version_no >= 1),
    CONSTRAINT ck_weekly_review_coverage CHECK (
        weight_observation_days BETWEEN 0 AND 7
        AND nutrition_logged_days BETWEEN 0 AND 7
        AND steps_observed_days BETWEEN 0 AND 7
        AND sleep_observed_days BETWEEN 0 AND 7
        AND training_days BETWEEN 0 AND 7
    ),
    CONSTRAINT ck_weekly_review_values CHECK (
        (average_weight_kg IS NULL OR average_weight_kg BETWEEN 30 AND 350)
        AND (nutrition_adherence_percent IS NULL
            OR nutrition_adherence_percent BETWEEN 0 AND 100)
        AND (average_steps IS NULL OR average_steps BETWEEN 0 AND 100000)
        AND (average_sleep_minutes IS NULL OR average_sleep_minutes BETWEEN 0 AND 1440)
        AND total_training_minutes BETWEEN 0 AND 4200
        AND proposed_energy_delta_kcal BETWEEN -100 AND 100
    ),
    CONSTRAINT ck_weekly_review_recommendation CHECK (
        recommendation IN ('INSUFFICIENT_DATA', 'HOLD', 'INCREASE_ENERGY', 'DECREASE_ENERGY')
    )
);

CREATE INDEX idx_weekly_review_user_window
    ON weekly_review (user_id, window_end, version_no);
