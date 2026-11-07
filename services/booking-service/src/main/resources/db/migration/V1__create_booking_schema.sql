CREATE TABLE bookings (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_bookings PRIMARY KEY,
    learner_id UNIQUEIDENTIFIER NOT NULL,
    mentor_id UNIQUEIDENTIFIER NOT NULL,
    mentor_subject_id UNIQUEIDENTIFIER NOT NULL,
    booking_status VARCHAR(24) NOT NULL,
    meeting_type VARCHAR(20) NOT NULL,
    start_at DATETIMEOFFSET(6) NOT NULL,
    end_at DATETIMEOFFSET(6) NOT NULL,
    learner_name_snapshot NVARCHAR(255) NOT NULL,
    mentor_name_snapshot NVARCHAR(255) NOT NULL,
    subject_name_snapshot NVARCHAR(255) NOT NULL,
    grade_name_snapshot NVARCHAR(255) NULL,
    price_per_hour_snapshot DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    learner_note NVARCHAR(2000) NULL,
    mentor_response NVARCHAR(2000) NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_bookings_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    updated_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_bookings_updated_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    CONSTRAINT ck_bookings_status CHECK (booking_status IN ('PENDING','CONFIRMED','RESCHEDULED','COMPLETED','CANCELLED','REJECTED')),
    CONSTRAINT ck_bookings_meeting_type CHECK (meeting_type IN ('ONLINE','OFFLINE','HYBRID')),
    CONSTRAINT ck_bookings_positive_time CHECK (end_at > start_at),
    CONSTRAINT ck_bookings_positive_amount CHECK (price_per_hour_snapshot >= 0 AND total_amount >= 0),
    CONSTRAINT uk_bookings_learner_idempotency UNIQUE (learner_id, idempotency_key)
);

CREATE INDEX ix_bookings_learner_start ON bookings (learner_id, start_at DESC);
CREATE INDEX ix_bookings_mentor_start ON bookings (mentor_id, start_at DESC);
CREATE INDEX ix_bookings_status_start ON bookings (booking_status, start_at);
