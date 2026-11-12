-- Read copy owned by auth-service; NULL means not synchronized yet.
-- Never assume an existing mentor account is ACTIVE during migration.
ALTER TABLE mentor_profiles
    ADD COLUMN account_status VARCHAR(32) NULL AFTER user_id,
    ADD CONSTRAINT chk_mentor_profiles_account_status CHECK (
        account_status IS NULL OR account_status IN
        ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED')
    );

CREATE INDEX idx_mentor_profiles_approval_account_status
    ON mentor_profiles (approval_status, account_status);
