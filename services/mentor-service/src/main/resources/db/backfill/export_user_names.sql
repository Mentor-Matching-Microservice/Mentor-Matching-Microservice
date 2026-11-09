-- Read-only export on user-service DB. Pipe output into the mentor DB connection.
-- Hex encoding preserves Unicode/quotes without relying on SQL escaping modes.
SELECT 'CREATE TEMPORARY TABLE tmp_user_names (user_id BINARY(16) PRIMARY KEY, full_name VARCHAR(255) NOT NULL) CHARACTER SET utf8mb4;';
SELECT CONCAT('INSERT INTO tmp_user_names (user_id, full_name) VALUES (UNHEX(',
    QUOTE(HEX(user_id)), '), CONVERT(UNHEX(', QUOTE(HEX(full_name)), ') USING utf8mb4));')
FROM user_profiles
WHERE full_name IS NOT NULL AND CHAR_LENGTH(TRIM(full_name)) > 0;
