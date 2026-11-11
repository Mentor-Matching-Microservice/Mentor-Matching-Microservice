-- Re-introduce mentor profile approval status, audit fields and outbox table for domain events.

ALTER TABLE mentor_profiles
    ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN rejection_reason TEXT NULL,
    ADD COLUMN approved_by BINARY(16) NULL,
    ADD COLUMN approved_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_mentor_profiles_approval_status CHECK (approval_status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'));

CREATE INDEX idx_mentor_profiles_approval_status ON mentor_profiles (approval_status);

ALTER TABLE mentor_verifications
    MODIFY COLUMN id_card_front_media_id VARCHAR(64) NULL,
    MODIFY COLUMN id_card_back_media_id VARCHAR(64) NULL,
    MODIFY COLUMN selfie_with_id_media_id VARCHAR(64) NULL;

CREATE TABLE outbox_events (
    id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'MENTOR',
    aggregate_id VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    publish_attempts INT NOT NULL DEFAULT 0,
    last_error TEXT NULL,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_outbox_events_published_at ON outbox_events (published_at, created_at);
