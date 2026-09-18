CREATE TABLE weight_plan (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    active_version_id VARCHAR(36),
    next_version_no INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_weight_plan_user UNIQUE (user_id),
    CONSTRAINT fk_weight_plan_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_weight_plan_next_version CHECK (next_version_no >= 1)
);

CREATE TABLE weight_plan_version (
    id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(36) NOT NULL,
    version_no INT NOT NULL,
    draft_idempotency_key_hash CHAR(64) NOT NULL,
    activation_idempotency_key_hash CHAR(64),
    status VARCHAR(16) NOT NULL,
    goal VARCHAR(16) NOT NULL,
    profile_updated_at TIMESTAMP(6) NOT NULL,
    screening_id VARCHAR(36) NOT NULL,
    screening_version INT NOT NULL,
    assessment_attempt_id VARCHAR(36) NOT NULL,
    formula_version VARCHAR(64) NOT NULL,
    target_policy_version VARCHAR(64) NOT NULL,
    bmi DECIMAL(4, 1) NOT NULL,
    bmr_kcal_per_day INT NOT NULL,
    tdee_kcal_per_day INT NOT NULL,
    energy_min_kcal_per_day INT NOT NULL,
    energy_max_kcal_per_day INT NOT NULL,
    weekly_weight_change_min_percent DECIMAL(4, 2) NOT NULL,
    weekly_weight_change_max_percent DECIMAL(4, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    validated_at TIMESTAMP(6),
    confirmed_at TIMESTAMP(6),
    activated_at TIMESTAMP(6),
    replaced_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_weight_plan_version_no UNIQUE (plan_id, version_no),
    CONSTRAINT uk_weight_plan_version_draft_key
        UNIQUE (plan_id, draft_idempotency_key_hash),
    CONSTRAINT uk_weight_plan_version_activation_key
        UNIQUE (plan_id, activation_idempotency_key_hash),
    CONSTRAINT fk_weight_plan_version_plan
        FOREIGN KEY (plan_id) REFERENCES weight_plan (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_weight_plan_version_screening
        FOREIGN KEY (screening_id) REFERENCES safety_screening (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_weight_plan_version_assessment
        FOREIGN KEY (assessment_attempt_id) REFERENCES assessment_attempt (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_weight_plan_version_status
        CHECK (status IN ('DRAFT', 'VALIDATED', 'CONFIRMED', 'ACTIVE', 'REPLACED')),
    CONSTRAINT ck_weight_plan_version_goal
        CHECK (goal IN ('LOSS', 'MAINTENANCE', 'GAIN')),
    CONSTRAINT ck_weight_plan_version_values CHECK (
        version_no >= 1
        AND bmi > 0
        AND bmr_kcal_per_day > 0
        AND tdee_kcal_per_day >= bmr_kcal_per_day
        AND energy_min_kcal_per_day > 0
        AND energy_max_kcal_per_day >= energy_min_kcal_per_day
        AND weekly_weight_change_max_percent >= weekly_weight_change_min_percent
    )
);

ALTER TABLE weight_plan
    ADD CONSTRAINT fk_weight_plan_active_version
    FOREIGN KEY (active_version_id)
    REFERENCES weight_plan_version (id)
    ON DELETE SET NULL;

CREATE INDEX idx_weight_plan_version_status
    ON weight_plan_version (plan_id, status, version_no);
