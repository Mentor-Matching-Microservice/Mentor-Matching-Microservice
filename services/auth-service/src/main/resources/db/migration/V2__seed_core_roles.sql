-- Core roles required by the application itself (registration assigns LEANER, mentor approval assigns MENTOR).
-- Idempotent: existing role rows (for example from db/seed/accounts.sql) are kept unchanged.
-- Role codes follow the team-wide convention used by every service (LEANER, MENTOR, ADMIN).
INSERT INTO roles (id, code, description)
SELECT UUID_TO_BIN(UUID()), core.code, core.description
FROM (
    SELECT 'LEANER' AS code, 'Leaner' AS description
    UNION ALL SELECT 'MENTOR', 'Mentor'
    UNION ALL SELECT 'ADMIN', 'Admin'
) AS core
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.code = core.code);
