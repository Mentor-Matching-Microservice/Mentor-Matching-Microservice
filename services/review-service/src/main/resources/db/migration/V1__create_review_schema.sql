CREATE TABLE review_tag_options (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_review_tag_options PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    is_active BIT NOT NULL CONSTRAINT df_review_tag_options_active DEFAULT (1),
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_review_tag_options_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    updated_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_review_tag_options_updated_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00')
);

CREATE TABLE reviews (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_reviews PRIMARY KEY,
    booking_id BIGINT NOT NULL CONSTRAINT uk_reviews_booking_id UNIQUE,
    student_user_id NVARCHAR(64) NOT NULL,
    student_name NVARCHAR(255) NULL,
    mentor_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(MAX) NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_reviews_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    updated_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_reviews_updated_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX ix_reviews_mentor_id ON reviews (mentor_id, created_at DESC);
CREATE INDEX ix_reviews_student_user_id ON reviews (student_user_id, created_at DESC);

CREATE TABLE review_tags (
    id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_review_tags PRIMARY KEY,
    review_id BIGINT NOT NULL,
    review_tag_option_id BIGINT NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_review_tags_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    CONSTRAINT fk_review_tags_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_tags_option FOREIGN KEY (review_tag_option_id) REFERENCES review_tag_options (id) ON DELETE CASCADE,
    CONSTRAINT uk_review_tags_review_option UNIQUE (review_id, review_tag_option_id)
);

CREATE INDEX ix_review_tags_review_id ON review_tags (review_id);
CREATE INDEX ix_review_tags_option_id ON review_tags (review_tag_option_id);

CREATE TABLE outbox_events (
    id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_outbox_events PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    payload NVARCHAR(MAX) NOT NULL,
    created_at DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_outbox_events_created_at DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    published_at DATETIMEOFFSET(6) NULL,
    publish_attempts INT NOT NULL CONSTRAINT df_outbox_events_publish_attempts DEFAULT (0),
    last_error NVARCHAR(2000) NULL
);

CREATE INDEX ix_outbox_events_pending ON outbox_events (published_at, created_at);

INSERT INTO review_tag_options (name, description) VALUES
(N'Nhiệt tình', N'Mentor giải thích rất nhiệt tình và chu đáo'),
(N'Đúng giờ', N'Mentor bắt đầu và kết thúc buổi học đúng giờ'),
(N'Dễ hiểu', N'Phương pháp giảng dạy dễ hiểu, truyền cảm hứng'),
(N'Chuyên môn cao', N'Kiến thức chuyên môn vững vàng, giải đáp sâu sắc'),
(N'Tài liệu hữu ích', N'Chia sẻ nhiều tài liệu và bài tập thực tế');
