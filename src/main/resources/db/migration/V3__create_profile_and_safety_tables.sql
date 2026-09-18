CREATE TABLE user_profile (
    user_id VARCHAR(36) NOT NULL,
    date_of_birth DATE NOT NULL,
    calculation_sex VARCHAR(16) NOT NULL,
    height_cm DECIMAL(5, 2) NOT NULL,
    current_weight_kg DECIMAL(5, 2) NOT NULL,
    target_weight_kg DECIMAL(5, 2) NOT NULL,
    activity_level VARCHAR(24) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    screening_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profile_account
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_user_profile_calculation_sex
        CHECK (calculation_sex IN ('FEMALE', 'MALE')),
    CONSTRAINT ck_user_profile_activity_level
        CHECK (activity_level IN ('SEDENTARY', 'LIGHT', 'MODERATE', 'VERY_ACTIVE'))
);

CREATE TABLE safety_screening (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    version INT NOT NULL,
    pregnant_or_breastfeeding BOOLEAN NOT NULL,
    eating_disorder_history BOOLEAN NOT NULL,
    medical_guidance_required BOOLEAN NOT NULL,
    weight_affecting_medication BOOLEAN NOT NULL,
    concerning_symptoms BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    automatic_planning_allowed BOOLEAN NOT NULL,
    reason_codes VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_safety_screening_version UNIQUE (user_id, version),
    CONSTRAINT fk_safety_screening_account
        FOREIGN KEY (user_id) REFERENCES user_account (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_safety_screening_status
        CHECK (status IN ('ELIGIBLE', 'PROFESSIONAL_REVIEW', 'INELIGIBLE'))
);
