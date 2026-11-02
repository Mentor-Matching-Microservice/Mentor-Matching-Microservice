-- Run against auth DB; output is SQL to load in the mentor seed connection.
-- Export only the ten demo accounts that actually have MENTOR role.
SELECT 'CREATE TEMPORARY TABLE tmp_mentor_accounts (email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY, user_id BINARY(16) NOT NULL UNIQUE);';
SELECT CONCAT('INSERT INTO tmp_mentor_accounts (email, user_id) VALUES (',
    QUOTE(a.login_email), ', UNHEX(', QUOTE(HEX(a.id)), '));')
FROM accounts a
WHERE a.login_email IN (
    'account16@example.com', 'account17@example.com', 'account18@example.com',
    'account19@example.com', 'account20@example.com', 'account21@example.com',
    'account22@example.com', 'account23@example.com', 'account24@example.com',
    'account25@example.com'
) AND EXISTS (
    SELECT 1 FROM account_roles ar JOIN roles r ON r.id = ar.role_id
    WHERE ar.account_id = a.id AND r.code = 'MENTOR'
)
ORDER BY a.login_email;
