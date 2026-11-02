-- Based on legacy backend V1, V14, V17 and V18; schema only, no demo data.
-- Account references are auth UUIDs stored as BINARY(16), without byte swapping.
-- Catalog/media references retain BIGINT; no foreign keys outside these eight tables.
-- DATETIME values represent UTC.
CREATE TABLE mentor_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BINARY(16) NOT NULL,
    avatar_url TEXT NOT NULL,
    avatar_media_id BIGINT NULL,
    gender VARCHAR(50),
    hometown_city_id BIGINT,
    current_district_id BIGINT,
    headline VARCHAR(255),
    introduction TEXT,
    teaching_style TEXT,
    experience_years INT,
    current_position VARCHAR(255),
    workplace VARCHAR(255),
    education VARCHAR(255),
    major VARCHAR(255),
    meeting_type VARCHAR(50),
    approval_status VARCHAR(50),
    approval_note TEXT,
    approved_by BINARY(16),
    approved_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_profiles PRIMARY KEY (id),
    CONSTRAINT uk_mentor_profiles_user_id UNIQUE (user_id),
    CONSTRAINT chk_mentor_profiles_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_mentor_profiles_meeting_type CHECK (meeting_type IS NULL OR meeting_type IN ('ONLINE', 'OFFLINE', 'HYBRID')),
    CONSTRAINT chk_mentor_profiles_approval_status CHECK (approval_status IS NULL OR approval_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentor_profiles_hometown_city_id ON mentor_profiles (hometown_city_id);
CREATE INDEX idx_mentor_profiles_current_district_id ON mentor_profiles (current_district_id);
CREATE INDEX idx_mentor_profiles_approved_by ON mentor_profiles (approved_by);
CREATE INDEX idx_mentor_profiles_approval_status ON mentor_profiles (approval_status);

CREATE TABLE mentor_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    id_card_number VARCHAR(50),
    id_card_front_url TEXT NOT NULL,
    id_card_front_media_id BIGINT NULL,
    id_card_back_url TEXT NOT NULL,
    id_card_back_media_id BIGINT NULL,
    selfie_with_id_url TEXT,
    selfie_with_id_media_id BIGINT NULL,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verified_by BINARY(16),
    verified_at DATETIME(6),
    rejection_reason TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_verifications PRIMARY KEY (id),
    CONSTRAINT uk_mentor_verifications_mentor_id UNIQUE (mentor_id),
    CONSTRAINT fk_mentor_verifications_mentor_id FOREIGN KEY (mentor_id) REFERENCES mentor_profiles (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT chk_mentor_verifications_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentor_verifications_verified_by ON mentor_verifications (verified_by);
CREATE INDEX idx_mentor_verifications_status ON mentor_verifications (verification_status);

CREATE TABLE personality_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_personality_options PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mentor_personalities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    personality_option_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_personalities PRIMARY KEY (id),
    CONSTRAINT fk_mentor_personalities_mentor_id FOREIGN KEY (mentor_id) REFERENCES mentor_profiles (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_mentor_personalities_personality_option_id FOREIGN KEY (personality_option_id) REFERENCES personality_options (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT uk_mentor_personalities_pair UNIQUE (mentor_id, personality_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentor_personalities_personality_option_id ON mentor_personalities (personality_option_id);

CREATE TABLE mentor_highlight_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_highlight_options PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mentor_highlights (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    highlight_option_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_highlights PRIMARY KEY (id),
    CONSTRAINT fk_mentor_highlights_mentor_id FOREIGN KEY (mentor_id) REFERENCES mentor_profiles (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_mentor_highlights_highlight_option_id FOREIGN KEY (highlight_option_id) REFERENCES mentor_highlight_options (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT uk_mentor_highlights_pair UNIQUE (mentor_id, highlight_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentor_highlights_highlight_option_id ON mentor_highlights (highlight_option_id);

CREATE TABLE mentor_achievements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    achievement_type VARCHAR(50),
    issuer VARCHAR(255),
    achieved_at DATE,
    proof_url TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    verified_by BINARY(16),
    verified_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_achievements PRIMARY KEY (id),
    CONSTRAINT fk_mentor_achievements_mentor_id FOREIGN KEY (mentor_id) REFERENCES mentor_profiles (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT chk_mentor_achievements_type CHECK (achievement_type IS NULL OR achievement_type IN ('AWARD', 'CERTIFICATE', 'EXAM_SCORE', 'COMPETITION', 'PROJECT', 'WORK_EXPERIENCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentor_achievements_mentor_id ON mentor_achievements (mentor_id);
CREATE INDEX idx_mentor_achievements_verified_by ON mentor_achievements (verified_by);

CREATE TABLE mentor_subjects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    subject_grade_id BIGINT NOT NULL,
    proficiency_level VARCHAR(50),
    teaching_note TEXT,
    price_per_hour DECIMAL(12, 2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_mentor_subjects PRIMARY KEY (id),
    CONSTRAINT fk_mentor_subjects_mentor_id FOREIGN KEY (mentor_id) REFERENCES mentor_profiles (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT chk_mentor_subjects_proficiency_level CHECK (proficiency_level IS NULL OR proficiency_level IN ('BASIC', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_mentor_subjects_price_per_hour CHECK (price_per_hour IS NULL OR price_per_hour >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_mentor_subjects_mentor_id ON mentor_subjects (mentor_id);
CREATE INDEX idx_mentor_subjects_subject_grade_id ON mentor_subjects (subject_grade_id);

CREATE INDEX idx_mentor_profiles_avatar_media_id ON mentor_profiles (avatar_media_id);
CREATE INDEX idx_mentor_verifications_id_card_front_media_id ON mentor_verifications (id_card_front_media_id);
CREATE INDEX idx_mentor_verifications_id_card_back_media_id ON mentor_verifications (id_card_back_media_id);
CREATE INDEX idx_mentor_verifications_selfie_with_id_media_id ON mentor_verifications (selfie_with_id_media_id);
