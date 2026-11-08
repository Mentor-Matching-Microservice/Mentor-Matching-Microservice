CREATE TABLE mentor_availabilities (
    id BIGINT IDENTITY(1,1) NOT NULL,
    mentor_id BIGINT NOT NULL,
    mentor_user_id NVARCHAR(64) NULL,
    availability_type NVARCHAR(50) NOT NULL DEFAULT 'RECURRING',
    day_of_week INT NULL,
    available_date DATE NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT pk_mentor_availabilities PRIMARY KEY (id),
    CONSTRAINT chk_mentor_availabilities_type CHECK (availability_type IN ('RECURRING', 'SPECIFIC_DATE')),
    CONSTRAINT chk_mentor_availabilities_rule CHECK (
        (availability_type = 'RECURRING' AND day_of_week IS NOT NULL AND available_date IS NULL)
        OR
        (availability_type = 'SPECIFIC_DATE' AND available_date IS NOT NULL AND day_of_week IS NULL)
    ),
    CONSTRAINT chk_mentor_availabilities_day_of_week CHECK (day_of_week IS NULL OR day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_mentor_availabilities_time_range CHECK (start_time < end_time)
);

CREATE INDEX idx_mentor_availabilities_mentor_id ON mentor_availabilities (mentor_id);
CREATE INDEX idx_mentor_availabilities_mentor_user_id ON mentor_availabilities (mentor_user_id);
CREATE INDEX idx_mentor_availabilities_calendar ON mentor_availabilities (mentor_id, availability_type, day_of_week, available_date);

CREATE TABLE bookings (
    id BIGINT IDENTITY(1,1) NOT NULL,
    student_user_id NVARCHAR(64) NOT NULL,
    student_name NVARCHAR(255) NULL,
    mentor_id BIGINT NOT NULL,
    mentor_user_id NVARCHAR(64) NULL,
    mentor_name NVARCHAR(255) NULL,
    mentor_subject_id BIGINT NOT NULL,
    subject_name NVARCHAR(255) NULL,
    grade_name NVARCHAR(255) NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    price_per_hour DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    meeting_type NVARCHAR(50) NULL,
    meeting_link NVARCHAR(MAX) NULL,
    meeting_address NVARCHAR(MAX) NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note NVARCHAR(MAX) NULL,
    cancelled_by NVARCHAR(64) NULL,
    cancel_reason NVARCHAR(MAX) NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT chk_bookings_time_range CHECK (start_time < end_time),
    CONSTRAINT chk_bookings_price_per_hour CHECK (price_per_hour > 0),
    CONSTRAINT chk_bookings_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_bookings_meeting_type CHECK (meeting_type IS NULL OR meeting_type IN ('ONLINE', 'OFFLINE')),
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW'))
);

CREATE INDEX idx_bookings_student_user_id_created_at ON bookings (student_user_id, created_at);
CREATE INDEX idx_bookings_mentor_id_booking_date ON bookings (mentor_id, booking_date);
CREATE INDEX idx_bookings_status ON bookings (status);

