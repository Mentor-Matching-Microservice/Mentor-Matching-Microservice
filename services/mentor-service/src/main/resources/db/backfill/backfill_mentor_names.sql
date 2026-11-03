-- Run after migration V2, in the same connection as the exported user-name SQL.
-- Fill missing names only; do not overwrite names already present in mentor-service.
START TRANSACTION;
UPDATE mentor_profiles m
JOIN tmp_user_names u ON u.user_id = m.user_id
SET m.full_name = u.full_name
WHERE (m.full_name IS NULL OR CHAR_LENGTH(TRIM(m.full_name)) = 0)
    AND CHAR_LENGTH(TRIM(u.full_name)) > 0;
SELECT ROW_COUNT() AS updated_profiles;
SELECT COUNT(*) AS profiles_still_missing_name
FROM mentor_profiles WHERE full_name IS NULL OR CHAR_LENGTH(TRIM(full_name)) = 0;
COMMIT;
DROP TEMPORARY TABLE tmp_user_names;
