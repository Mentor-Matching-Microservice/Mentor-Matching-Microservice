-- #65 User provisioning.
-- email: read-only copy of the login email from auth-service's AccountRegistered event (auth stays the owner).
ALTER TABLE user_profiles
    ADD COLUMN email VARCHAR(254) COLLATE utf8mb4_0900_as_ci NULL AFTER user_id;

-- Inbox: events consumed from other services; event_id is the primary key so each event is applied once,
-- in the same transaction as its effect.
CREATE TABLE inbox_events (
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
