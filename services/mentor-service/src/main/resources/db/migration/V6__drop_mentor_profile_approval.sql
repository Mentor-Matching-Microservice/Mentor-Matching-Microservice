-- KYC status and review metadata are owned by mentor_verifications.
DROP INDEX idx_mentor_profiles_approval_account_status ON mentor_profiles;
DROP INDEX idx_mentor_profiles_approval_status ON mentor_profiles;
DROP INDEX idx_mentor_profiles_approved_by ON mentor_profiles;

ALTER TABLE mentor_profiles
    DROP CHECK chk_mentor_profiles_approval_status,
    DROP COLUMN approval_status,
    DROP COLUMN approval_note,
    DROP COLUMN approved_by,
    DROP COLUMN approved_at;

CREATE INDEX idx_mentor_profiles_account_status ON mentor_profiles (account_status);
