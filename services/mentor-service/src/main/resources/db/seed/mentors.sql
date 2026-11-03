-- Optional local/demo seed, NOT a Flyway migration. Run after V2.
-- First load export_mentor_accounts.sql OUTPUT in the same connection.
-- Fictional profiles/documents; URLs on example.invalid intentionally do not resolve.
-- subject_grade_id 1..10 are demo references, not verified catalog mappings.
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Fail before any persistent write if the expected ten MENTOR accounts are missing.
CREATE TEMPORARY TABLE tmp_mentor_seed_guard (
    account_count INT NOT NULL CHECK (account_count = 10)
);
INSERT INTO tmp_mentor_seed_guard
SELECT COUNT(*) FROM tmp_mentor_accounts
WHERE email IN ('account16@example.com', 'account17@example.com', 'account18@example.com',
    'account19@example.com', 'account20@example.com', 'account21@example.com',
    'account22@example.com', 'account23@example.com', 'account24@example.com', 'account25@example.com');

CREATE TEMPORARY TABLE tmp_mentor_demo (
    seq INT PRIMARY KEY,
    email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    gender VARCHAR(50) NOT NULL,
    experience_years INT NOT NULL,
    meeting_type VARCHAR(50) NOT NULL,
    approval_status VARCHAR(50) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    subject_grade_id BIGINT NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO tmp_mentor_demo VALUES
    (16, 'account16@example.com', 'Nguyễn Minh An', 'Toán', 'MALE', 4, 'OFFLINE', 'PENDING', 150000, 1),
    (17, 'account17@example.com', 'Trần Hoàng Bình', 'Vật lý', 'FEMALE', 5, 'HYBRID', 'PENDING', 175000, 2),
    (18, 'account18@example.com', 'Lê Thu Chi', 'Tiếng Anh', 'MALE', 6, 'ONLINE', 'PENDING', 200000, 3),
    (19, 'account19@example.com', 'Phạm Anh Dũng', 'Hóa học', 'FEMALE', 7, 'OFFLINE', 'PENDING', 225000, 4),
    (20, 'account20@example.com', 'Hoàng Ngọc Hà', 'Ngữ văn', 'MALE', 8, 'HYBRID', 'PENDING', 250000, 5),
    (21, 'account21@example.com', 'Vũ Minh Khang', 'Tin học', 'FEMALE', 2, 'ONLINE', 'PENDING', 275000, 6),
    (22, 'account22@example.com', 'Đặng Phương Linh', 'Sinh học', 'MALE', 3, 'OFFLINE', 'PENDING', 300000, 7),
    (23, 'account23@example.com', 'Bùi Quang Nam', 'Lịch sử', 'FEMALE', 4, 'HYBRID', 'PENDING', 325000, 8),
    (24, 'account24@example.com', 'Đỗ Thanh Thảo', 'Địa lý', 'MALE', 5, 'ONLINE', 'DRAFT', 350000, 9),
    (25, 'account25@example.com', 'Ngô Hải Yến', 'Tiếng Anh giao tiếp', 'FEMALE', 6, 'OFFLINE', 'REJECTED', 375000, 10);

-- Only enrich profiles created by this run. Existing user profiles are untouched.
CREATE TEMPORARY TABLE tmp_new_mentors (user_id BINARY(16) PRIMARY KEY);
START TRANSACTION;
INSERT INTO tmp_new_mentors
SELECT a.user_id FROM tmp_mentor_accounts a JOIN tmp_mentor_demo d ON d.email = a.email
WHERE NOT EXISTS (SELECT 1 FROM mentor_profiles p WHERE p.user_id = a.user_id);

-- Seed base mentor profile options.

SET NAMES utf8mb4;

CREATE TEMPORARY TABLE tmp_personality_options (
    name VARCHAR(255) NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_personality_options (name, description)
VALUES
    ('Kiên nhẫn', 'Phù hợp với học sinh cần thời gian để hiểu bài, dễ mất tự tin hoặc cần được giải thích lại nhiều lần.'),
    ('Thân thiện', 'Tạo cảm giác thoải mái trong buổi học, giúp học sinh dễ đặt câu hỏi và bớt áp lực.'),
    ('Kỷ luật', 'Giữ nhịp học rõ ràng, theo sát bài tập và phù hợp với học sinh cần rèn thói quen học đều.'),
    ('Truyền cảm hứng', 'Biết khơi gợi hứng thú học tập, liên hệ kiến thức với mục tiêu và câu chuyện thực tế.'),
    ('Tỉ mỉ', 'Chú ý từng lỗi nhỏ trong cách làm bài, cách diễn đạt và quá trình luyện tập của học sinh.'),
    ('Năng động', 'Buổi học có nhiều tương tác, ví dụ thực tế và hoạt động giúp học sinh duy trì sự tập trung.'),
    ('Lắng nghe tốt', 'Quan sát phản ứng của học sinh, điều chỉnh tốc độ dạy và phản hồi theo nhu cầu cá nhân.'),
    ('Tư duy hệ thống', 'Giúp học sinh nhìn thấy cấu trúc kiến thức, mối liên hệ giữa các chủ đề và cách tự ôn tập.'),
    ('Hài hước vừa phải', 'Biết tạo không khí nhẹ nhàng mà vẫn giữ mục tiêu học tập nghiêm túc.'),
    ('Định hướng mục tiêu', 'Tập trung vào mục tiêu cụ thể như nâng điểm, ôn thi, luyện chứng chỉ hoặc xây nền kiến thức.');

INSERT INTO personality_options (name, description)
SELECT option_data.name, option_data.description
FROM tmp_personality_options option_data
WHERE NOT EXISTS (
    SELECT 1
    FROM personality_options existing
    WHERE existing.name = option_data.name
);

CREATE TEMPORARY TABLE tmp_mentor_highlight_options (
    name VARCHAR(255) NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_mentor_highlight_options (name, description)
VALUES
    ('Đã xác minh danh tính', 'Mentor đã hoàn tất bước xác minh hồ sơ cá nhân trên hệ thống.'),
    ('Được duyệt bởi admin', 'Hồ sơ mentor đã được kiểm tra và cho phép hiển thị công khai.'),
    ('Kinh nghiệm 3+ năm', 'Mentor có ít nhất ba năm kinh nghiệm dạy học, kèm cặp hoặc đào tạo.'),
    ('Kinh nghiệm 5+ năm', 'Mentor có ít nhất năm năm kinh nghiệm dạy học, phù hợp với nhu cầu học dài hạn hoặc luyện thi.'),
    ('Chuyên ôn thi', 'Có kinh nghiệm xây lộ trình, luyện đề và theo dõi tiến độ cho các kỳ thi quan trọng.'),
    ('Phù hợp học sinh mất gốc', 'Có phương pháp dạy chậm, chắc và biết xây lại nền tảng từ đầu.'),
    ('Dạy online tốt', 'Sử dụng công cụ học online hiệu quả, có tài liệu số và tương tác rõ trong buổi học từ xa.'),
    ('Có giáo án cá nhân hóa', 'Điều chỉnh nội dung học theo mục tiêu, trình độ và tốc độ tiếp thu của từng học sinh.'),
    ('Có bài tập sau buổi học', 'Cung cấp bài luyện thêm hoặc checklist ôn tập để học sinh duy trì tiến độ giữa các buổi.'),
    ('Feedback sau buổi học', 'Có nhận xét sau buổi học để học sinh hoặc phụ huynh nắm được tiến độ và điểm cần cải thiện.'),
    ('Linh hoạt lịch học', 'Có khả năng sắp xếp lịch học đa dạng, phù hợp với học sinh bận hoặc cần học ngắn hạn.'),
    ('Có chứng chỉ chuyên môn', 'Mentor có chứng chỉ, giải thưởng hoặc bằng cấp liên quan đến môn dạy.'),
    ('Giọng giảng dễ hiểu', 'Cách diễn đạt rõ ràng, chậm rãi và giúp học sinh dễ theo dõi trong suốt buổi học.'),
    ('Theo sát tiến bộ', 'Có thói quen ghi nhận lỗi sai, đánh giá tiến bộ và điều chỉnh lộ trình theo từng giai đoạn.'),
    ('Phù hợp học sinh nhỏ tuổi', 'Có kinh nghiệm giữ nhịp học, tạo tương tác và giao tiếp phù hợp với học sinh tiểu học.');

INSERT INTO mentor_highlight_options (name, description)
SELECT option_data.name, option_data.description
FROM tmp_mentor_highlight_options option_data
WHERE NOT EXISTS (
    SELECT 1
    FROM mentor_highlight_options existing
    WHERE existing.name = option_data.name
);

DROP TEMPORARY TABLE tmp_mentor_highlight_options;
DROP TEMPORARY TABLE tmp_personality_options;

INSERT INTO mentor_profiles (user_id, full_name, avatar_url, gender, headline, introduction, teaching_style,
    experience_years, current_position, workplace, education, major, meeting_type, approval_status, approval_note)
SELECT a.user_id, d.full_name, CONCAT('https://example.invalid/mentors/', d.seq, '/avatar.png'), d.gender,
    CONCAT('Mentor ', d.subject_name, ' — lộ trình học cá nhân hóa'),
    CONCAT('Hồ sơ minh họa của ', d.full_name, '. Đồng hành cùng học viên học ', d.subject_name,
        ', củng cố nền tảng và luyện tập theo mục tiêu.'),
    'Đánh giá đầu vào, giải thích qua ví dụ, luyện tập từng bước và phản hồi sau mỗi buổi.',
    d.experience_years, CONCAT('Gia sư ', d.subject_name), 'Trung tâm giáo dục Demo',
    'Đại học', d.subject_name, d.meeting_type, d.approval_status,
    IF(d.approval_status = 'REJECTED', 'Demo: cần bổ sung thông tin hồ sơ.', NULL)
FROM tmp_mentor_demo d JOIN tmp_mentor_accounts a ON a.email = d.email
JOIN tmp_new_mentors n ON n.user_id = a.user_id;

INSERT INTO mentor_verifications (mentor_id, full_name, id_card_front_url, id_card_back_url,
    selfie_with_id_url, verification_status, rejection_reason)
SELECT p.id, d.full_name,
    CONCAT('https://example.invalid/mentors/', d.seq, '/id-front.png'),
    CONCAT('https://example.invalid/mentors/', d.seq, '/id-back.png'),
    CONCAT('https://example.invalid/mentors/', d.seq, '/selfie.png'),
    IF(d.approval_status = 'REJECTED', 'REJECTED', 'PENDING'),
    IF(d.approval_status = 'REJECTED', 'Demo: ảnh giấy tờ chưa rõ.', NULL)
FROM tmp_mentor_demo d JOIN tmp_mentor_accounts a ON a.email = d.email
JOIN mentor_profiles p ON p.user_id = a.user_id JOIN tmp_new_mentors n ON n.user_id = a.user_id;

INSERT INTO mentor_subjects (mentor_id, subject_grade_id, proficiency_level, teaching_note, price_per_hour, is_active)
SELECT p.id, d.subject_grade_id, IF(d.experience_years >= 5, 'EXPERT', 'ADVANCED'),
    CONCAT('Demo: xây nền tảng và luyện tập ', d.subject_name), d.price, TRUE
FROM tmp_mentor_demo d JOIN tmp_mentor_accounts a ON a.email = d.email
JOIN mentor_profiles p ON p.user_id = a.user_id JOIN tmp_new_mentors n ON n.user_id = a.user_id;

INSERT INTO mentor_personalities (mentor_id, personality_option_id)
SELECT p.id, o.id FROM mentor_profiles p JOIN tmp_new_mentors n ON n.user_id = p.user_id
JOIN personality_options o ON o.name IN ('Kiên nhẫn', 'Lắng nghe tốt', 'Định hướng mục tiêu');

INSERT INTO mentor_highlights (mentor_id, highlight_option_id)
SELECT p.id, o.id FROM mentor_profiles p JOIN tmp_new_mentors n ON n.user_id = p.user_id
JOIN mentor_highlight_options o ON o.name IN ('Có giáo án cá nhân hóa', 'Feedback sau buổi học')
    OR (o.name = 'Kinh nghiệm 3+ năm' AND p.experience_years >= 3)
    OR (o.name = 'Kinh nghiệm 5+ năm' AND p.experience_years >= 5);

INSERT INTO mentor_achievements (mentor_id, title, description, achievement_type, issuer, achieved_at, proof_url)
SELECT p.id, CONCAT('Demo: hoàn thành đào tạo giảng dạy ', d.subject_name),
    'Thành tích giả lập phục vụ phát triển, chưa xác minh.', 'CERTIFICATE', 'Học viện Demo', '2024-06-15',
    CONCAT('https://example.invalid/mentors/', d.seq, '/certificate.pdf')
FROM tmp_mentor_demo d JOIN tmp_mentor_accounts a ON a.email = d.email
JOIN mentor_profiles p ON p.user_id = a.user_id JOIN tmp_new_mentors n ON n.user_id = a.user_id;

INSERT INTO mentor_achievements (mentor_id, title, description, achievement_type, issuer, achieved_at)
SELECT p.id, 'Demo: xây dựng bộ bài tập cá nhân hóa',
    'Dự án minh họa phân loại bài tập theo mức độ và mục tiêu học tập.', 'PROJECT', 'Trung tâm giáo dục Demo', '2025-08-01'
FROM mentor_profiles p JOIN tmp_new_mentors n ON n.user_id = p.user_id;
COMMIT;

DROP TEMPORARY TABLE tmp_new_mentors;
DROP TEMPORARY TABLE tmp_mentor_demo;
DROP TEMPORARY TABLE tmp_mentor_seed_guard;
DROP TEMPORARY TABLE tmp_mentor_accounts;
