-- Seed subject-grade mappings used by mentor subjects and booking.

SET NAMES utf8mb4;

CREATE TEMPORARY TABLE tmp_subject_grade_ranges (
    subject_name VARCHAR(255) NOT NULL,
    min_grade INT,
    max_grade INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_subject_grade_ranges (subject_name, min_grade, max_grade)
VALUES
    ('Toán', 1, 12),
    ('Toán tư duy', 1, 9),
    ('Đại số', 6, 12),
    ('Hình học', 6, 12),
    ('Xác suất - Thống kê', 10, 12),

    ('Tiếng Việt', 1, 5),
    ('Ngữ văn', 6, 12),
    ('Viết sáng tạo', 3, 12),

    ('Tiếng Anh', 1, 12),
    ('Vật lý', 6, 12),
    ('Hóa học', 8, 12),
    ('Sinh học', 6, 12),
    ('Khoa học tự nhiên', 6, 9),

    ('Lịch sử', 6, 12),
    ('Địa lý', 6, 12),
    ('Giáo dục công dân', 6, 12),
    ('Lịch sử và Địa lý', 6, 9),

    ('Tin học', 1, 12),
    ('Lập trình Scratch', 1, 8),
    ('Lập trình Python', 6, 12),
    ('Lập trình Web', 8, 12),

    ('Ôn thi vào lớp 10', 9, 9),
    ('Ôn thi tốt nghiệp THPT', 12, 12);

CREATE TEMPORARY TABLE tmp_subject_grade_general (
    subject_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_subject_grade_general (subject_name)
VALUES
    ('IELTS'),
    ('TOEIC'),
    ('Tiếng Nhật'),
    ('Tiếng Hàn'),
    ('Tiếng Trung'),
    ('SAT'),
    ('Phương pháp học tập'),
    ('Thuyết trình'),
    ('Quản lý thời gian');

INSERT INTO subject_grades (subject_id, grade_id)
SELECT s.id, g.id
FROM tmp_subject_grade_ranges r
JOIN subjects s ON s.name = r.subject_name
JOIN grades g ON g.name = CONCAT('Lớp ', r.min_grade)
WHERE r.min_grade = r.max_grade
  AND NOT EXISTS (
      SELECT 1
      FROM subject_grades existing
      WHERE existing.subject_id = s.id
        AND existing.grade_id = g.id
  );

INSERT INTO subject_grades (subject_id, grade_id)
SELECT s.id, g.id
FROM tmp_subject_grade_ranges r
JOIN subjects s ON s.name = r.subject_name
JOIN grades g ON CAST(SUBSTRING(g.name, 5) AS UNSIGNED) BETWEEN r.min_grade AND r.max_grade
WHERE r.min_grade <> r.max_grade
  AND NOT EXISTS (
      SELECT 1
      FROM subject_grades existing
      WHERE existing.subject_id = s.id
        AND existing.grade_id = g.id
  );

INSERT INTO subject_grades (subject_id, grade_id)
SELECT s.id, NULL
FROM tmp_subject_grade_general general
JOIN subjects s ON s.name = general.subject_name
WHERE NOT EXISTS (
    SELECT 1
    FROM subject_grades existing
    WHERE existing.subject_id = s.id
      AND existing.grade_id IS NULL
);

DROP TEMPORARY TABLE tmp_subject_grade_general;
DROP TEMPORARY TABLE tmp_subject_grade_ranges;
