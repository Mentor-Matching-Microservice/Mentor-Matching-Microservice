-- Enforce the existing case-insensitive name uniqueness rule under concurrent writes.
-- These tables use utf8mb4_unicode_ci from V1.
ALTER TABLE subjects ADD CONSTRAINT uk_subjects_name UNIQUE (name);
ALTER TABLE grades ADD CONSTRAINT uk_grades_name UNIQUE (name);
