ALTER TABLE mentor_profiles
    ADD COLUMN average_rating DOUBLE NULL AFTER major,
    ADD COLUMN total_reviews BIGINT NOT NULL DEFAULT 0 AFTER average_rating,
    ADD COLUMN rating_version BIGINT NOT NULL DEFAULT 0 AFTER total_reviews;

CREATE INDEX idx_mentor_profiles_average_rating ON mentor_profiles (average_rating);
