-- Local read copy of user-service fullName. Existing rows stay NULL until synchronized.
-- Verification full_name is identity-document data, not the source of profile display names.
ALTER TABLE mentor_profiles
    ADD COLUMN full_name VARCHAR(255) NULL AFTER user_id;

CREATE INDEX idx_mentor_profiles_full_name ON mentor_profiles (full_name);
