-- #66 Admin user management.
-- Read-only copy of the account status owned by auth-service, kept up to date from AccountRegistered and
-- AccountStatusChanged events. account_status_version = aggregateVersion of the last applied event, so an
-- older event arriving late never overwrites a newer status.
ALTER TABLE user_profiles
    ADD COLUMN account_status VARCHAR(32) NULL AFTER user_type,
    ADD COLUMN account_status_version BIGINT NULL AFTER account_status,
    ADD CONSTRAINT ck_user_profiles_account_status CHECK (account_status IS NULL OR account_status IN
        ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED'));

-- Admin list: newest first, optionally filtered by user type.
CREATE INDEX idx_user_profiles_created ON user_profiles (created_at);
CREATE INDEX idx_user_profiles_type_created ON user_profiles (user_type, created_at);
