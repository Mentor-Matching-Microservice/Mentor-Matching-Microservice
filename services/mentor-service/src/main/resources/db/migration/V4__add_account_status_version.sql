-- Auth account version protects against delayed/out-of-order snapshots.
ALTER TABLE mentor_profiles
    ADD COLUMN account_status_version BIGINT NULL AFTER account_status,
    ADD CONSTRAINT chk_mentor_profiles_account_status_version
        CHECK (account_status_version IS NULL OR account_status_version >= 0);
