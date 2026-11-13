-- ==============================================================================
-- Demo Mentors Seed for Microservices (mm_auth, mm_user, mm_mentor)
-- ==============================================================================

-- 1. SEED AUTH ACCOUNTS & ROLES (mm_auth)
USE mm_auth;

INSERT INTO accounts (id, login_email, status, email_verified_at, created_at, updated_at, version)
VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000016'), 'mentor01@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000017'), 'mentor02@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000018'), 'mentor03@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000019'), 'mentor04@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000020'), 'mentor05@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000021'), 'mentor06@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000022'), 'mentor07@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000023'), 'mentor08@example.com', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0)
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

INSERT INTO account_roles (account_id, role_id, assigned_at)
SELECT a.id, r.id, UTC_TIMESTAMP(6)
FROM accounts a
JOIN roles r ON r.code = 'MENTOR'
WHERE a.login_email LIKE 'mentor%@example.com'
  AND NOT EXISTS (SELECT 1 FROM account_roles ar WHERE ar.account_id = a.id AND ar.role_id = r.id);


-- 2. SEED USER PROFILES (mm_user)
USE mm_user;

INSERT INTO user_profiles (user_id, email, full_name, phone, user_type, account_status, account_status_version, created_at, updated_at, version)
VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000016'), 'mentor01@example.com', 'Nguyễn Minh An', '0901234516', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000017'), 'mentor02@example.com', 'Trần Hoàng Bình', '0901234517', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000018'), 'mentor03@example.com', 'Lê Thu Chi', '0901234518', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000019'), 'mentor04@example.com', 'Phạm Anh Dũng', '0901234519', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000020'), 'mentor05@example.com', 'Hoàng Ngọc Hà', '0901234520', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000021'), 'mentor06@example.com', 'Vũ Minh Khang', '0901234521', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000022'), 'mentor07@example.com', 'Đặng Phương Linh', '0901234522', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000023'), 'mentor08@example.com', 'Bùi Quang Nam', '0901234523', 'WORKING_ADULT', 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);


-- 3. SEED MENTOR SERVICE (mm_mentor)
USE mm_mentor;
SET NAMES utf8mb4;

-- Options
INSERT INTO personality_options (name, description, created_at, updated_at) VALUES
('Kiên nhẫn', 'Phù hợp với học sinh cần thời gian để hiểu bài, dễ mất tự tin.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Thân thiện', 'Tạo cảm giác thoải mái trong buổi học, giúp học sinh dễ đặt câu hỏi.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Kỷ luật', 'Giữ nhịp học rõ ràng, theo sát bài tập và tiến độ.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Truyền cảm hứng', 'Biết khơi gợi hứng thú học tập và liên hệ thực tế.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Tỉ mỉ', 'Chú ý từng lỗi nhỏ trong cách làm bài và diễn đạt.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Lắng nghe tốt', 'Điều chỉnh tốc độ dạy theo nhu cầu cá nhân của học sinh.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO mentor_highlight_options (name, description, created_at, updated_at) VALUES
('Đã xác minh danh tính', 'Mentor đã hoàn tất bước xác minh hồ sơ cá nhân trên hệ thống.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Được duyệt bởi admin', 'Hồ sơ mentor đã được kiểm tra và cho phép hiển thị công khai.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Kinh nghiệm 3+ năm', 'Mentor có ít nhất ba năm kinh nghiệm dạy học hoặc kèm cặp.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Kinh nghiệm 5+ năm', 'Mentor có ít nhất năm năm kinh nghiệm dạy học chuyên sâu.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Chuyên ôn thi', 'Có kinh nghiệm xây lộ trình và luyện đề cho các kỳ thi quan trọng.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Dạy online tốt', 'Sử dụng công cụ học online hiệu quả, tương tác trực quan.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Có giáo án cá nhân hóa', 'Điều chỉnh nội dung học theo mục tiêu và trình độ học sinh.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Mentor Profiles
INSERT INTO mentor_profiles (
    id, user_id, full_name, avatar_url, gender, hometown_city_id, current_district_id,
    headline, introduction, teaching_style, experience_years, current_position, workplace,
    education, major, average_rating, total_reviews, meeting_type, approval_status, account_status,
    created_at, updated_at
) VALUES
(1, UUID_TO_BIN('00000000-0000-0000-0000-000000000016'), 'Nguyễn Minh An', 'https://i.pravatar.cc/300?img=11', 'MALE', 1, 1,
 'Gia sư Toán THCS & THPT — Luyện thi chuyên sâu',
 'Mình có hơn 4 năm kinh nghiệm dạy kèm môn Toán cho học sinh lớp 9 đến 12. Tập trung vào việc hiểu sâu bản chất định lý, phương pháp suy luận thay vì học vẹt công thức.',
 'Phương pháp phân tích từng bước, bài tập từ cơ bản đến nâng cao, rèn luyện kỹ năng phản xạ tư duy.',
 4, 'Giảng viên trợ giảng', 'Đại học Sư phạm Hà Nội', 'Thạc sĩ', 'Toán học ứng dụng', 4.9, 28, 'HYBRID', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(2, UUID_TO_BIN('00000000-0000-0000-0000-000000000017'), 'Trần Hoàng Bình', 'https://i.pravatar.cc/300?img=12', 'MALE', 1, 2,
 'Mentor Vật lý THPT — Chinh phục điểm 9+ kỳ thi tốt nghiệp',
 'Chuyên bồi dưỡng học sinh thi đại học môn Vật lý. Hướng dẫn giải nhanh trắc nghiệm bằng sơ đồ tư duy và đồ thị trực quan.',
 'Giảng giải hiện tượng thực tế, liên hệ trực quan, kỹ thuật bấm máy tính và quản lý thời gian thi.',
 5, 'Giáo viên Vật lý', 'THPT Chuyên', 'Đại học', 'Sư phạm Vật lý', 5.0, 42, 'ONLINE', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(3, UUID_TO_BIN('00000000-0000-0000-0000-000000000018'), 'Lê Thu Chi', 'https://i.pravatar.cc/300?img=16', 'FEMALE', 1, 3,
 'IELTS 8.0 & Tiếng Anh giao tiếp mọi cấp độ',
 'Cựu du học sinh với 6 năm kinh nghiệm đào tạo IELTS và tiếng Anh học thuật. Giúp học viên vượt qua rào cản phát âm và tự tin giao tiếp.',
 'Tập trung phản xạ tự nhiên, sửa lỗi phát âm chi tiết, giáo trình cá nhân hóa theo mục tiêu từng bạn.',
 6, 'IELTS Trainer', 'Anh ngữ Quốc tế', 'Đại học', 'Ngôn ngữ Anh', 4.95, 65, 'ONLINE', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(4, UUID_TO_BIN('00000000-0000-0000-0000-000000000019'), 'Phạm Anh Dũng', 'https://i.pravatar.cc/300?img=14', 'MALE', 1, 4,
 'Mentor Hóa học — Lấy gốc thần tốc & Luyện thi',
 'Giúp học sinh mất gốc lấy lại đam mê môn Hóa học. Hệ thống hóa toàn bộ chuỗi phản ứng vô cơ và hữu cơ cực dễ nhớ.',
 'Sử dụng bảng tóm tắt màu sắc, mindmap phương trình và các mẹo cân bằng siêu nhanh.',
 3, 'Kỹ sư Hóa dược', 'Viện Khoa học & Công nghệ', 'Đại học', 'Hóa dược', 4.85, 19, 'HYBRID', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(5, UUID_TO_BIN('00000000-0000-0000-0000-000000000020'), 'Hoàng Ngọc Hà', 'https://i.pravatar.cc/300?img=20', 'FEMALE', 1, 5,
 'Gia sư Ngữ văn — Rèn luyện tư duy nghị luận & Cảm thụ văn học',
 'Đồng hành cùng học sinh khám phá vẻ đẹp ngôn từ, phát triển tư duy phản biện và xây dựng bài văn nghị luận sắc bén.',
 'Không học thuộc bài mẫu; hướng dẫn lập dàn ý mạch lạc, chọn lọc dẫn chứng đương đại và diễn đạt truyền cảm.',
 5, 'Biên tập viên & Gia sư', 'Nhà xuất bản Giáo dục', 'Thạc sĩ', 'Văn học Việt Nam', 4.9, 31, 'OFFLINE', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(6, UUID_TO_BIN('00000000-0000-0000-0000-000000000021'), 'Vũ Minh Khang', 'https://i.pravatar.cc/300?img=15', 'MALE', 1, 1,
 'Lập trình Python, C++ & Thuật toán cho học sinh giỏi Tin',
 'Giải nhất kỳ thi Olympic Tin học sinh viên. Đào tạo tư duy lập trình căn bản, cấu trúc dữ liệu và giải thuật.',
 'Học qua dự án thực chiến, giải bài tập trực tiếp trên hệ thống chấm tự động, code review chi tiết.',
 3, 'Kỹ sư phần mềm cao cấp', 'Tập đoàn Công nghệ FPT', 'Đại học', 'Khoa học Máy tính', 5.0, 24, 'ONLINE', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(7, UUID_TO_BIN('00000000-0000-0000-0000-000000000022'), 'Đặng Phương Linh', 'https://i.pravatar.cc/300?img=25', 'FEMALE', 1, 2,
 'Mentor Sinh học & Khoa học Tự nhiên THCS, THPT',
 'Hơn 4 năm kèm cặp môn Sinh học. Kết nối các kiến thức di truyền, tế bào với đời sống và y học hiện đại.',
 'Học qua mô hình 3D, sơ đồ chu trình sống động, phương pháp ghi nhớ khoa học.',
 4, 'Nghiên cứu sinh Sinh học', 'Đại học Khoa học Tự nhiên', 'Thạc sĩ', 'Công nghệ Sinh học', 4.88, 17, 'ONLINE', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),

(8, UUID_TO_BIN('00000000-0000-0000-0000-000000000023'), 'Bùi Quang Nam', 'https://i.pravatar.cc/300?img=18', 'MALE', 1, 3,
 'Gia sư Lịch sử & Địa lý — Sơ đồ hóa sự kiện & Kỹ năng phân tích số liệu',
 'Giúp học sinh không còn nỗi sợ ghi nhớ mốc thời gian hay số liệu địa lý. Học bằng câu chuyện và infographic sinh động.',
 'Xây dựng dòng thời gian tương tác, rèn luyện kỹ năng đọc bản đồ Atlat và trả lời câu hỏi tự luận.',
 4, 'Giảng viên Lịch sử', 'Đại học KHXH&NV', 'Thạc sĩ', 'Lịch sử Thế giới', 4.92, 22, 'HYBRID', 'APPROVED', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), approval_status = 'APPROVED', account_status = 'ACTIVE';

-- Verifications
INSERT INTO mentor_verifications (
    mentor_id, full_name, id_card_number, id_card_front_url, id_card_back_url, verification_status, created_at, updated_at
) VALUES
(1, 'Nguyễn Minh An', '001200001016', 'https://example.com/id1_f.jpg', 'https://example.com/id1_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(2, 'Trần Hoàng Bình', '001200001017', 'https://example.com/id2_f.jpg', 'https://example.com/id2_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(3, 'Lê Thu Chi', '001200001018', 'https://example.com/id3_f.jpg', 'https://example.com/id3_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(4, 'Phạm Anh Dũng', '001200001019', 'https://example.com/id4_f.jpg', 'https://example.com/id4_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(5, 'Hoàng Ngọc Hà', '001200001020', 'https://example.com/id5_f.jpg', 'https://example.com/id5_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(6, 'Vũ Minh Khang', '001200001021', 'https://example.com/id6_f.jpg', 'https://example.com/id6_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(7, 'Đặng Phương Linh', '001200001022', 'https://example.com/id7_f.jpg', 'https://example.com/id7_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(8, 'Bùi Quang Nam', '001200001023', 'https://example.com/id8_f.jpg', 'https://example.com/id8_b.jpg', 'VERIFIED', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE verification_status = 'VERIFIED';

-- Mentor Subjects (Subject-grade mappings: 14 = Toán 10, 74 = Tiếng Anh 10, 82 = Vật lý 10, 87 = Hóa học 10, 53 = Ngữ văn 10, 135 = Tin học, etc.)
INSERT INTO mentor_subjects (mentor_id, subject_grade_id, proficiency_level, teaching_note, price_per_hour, is_active, created_at, updated_at) VALUES
(1, 14, 'EXPERT', 'Chuyên sâu giải toán đại số & hình học lớp 10', 200000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(1, 15, 'ADVANCED', 'Ôn thi chuyển cấp và Toán cơ bản', 180000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(2, 82, 'EXPERT', 'Bồi dưỡng học sinh giỏi và luyện thi đại học Vật lý', 220000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(3, 74, 'EXPERT', 'Luyện phát âm, ngữ pháp và 4 kỹ năng IELTS', 300000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(4, 87, 'ADVANCED', 'Hóa học đại cương và chuyên đề hữu cơ', 190000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(5, 53, 'EXPERT', 'Rèn kỹ năng viết luận xã hội và văn học', 210000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(6, 135, 'EXPERT', 'Lập trình giải thuật, Python cơ bản đến nâng cao', 250000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(7, 14, 'ADVANCED', 'Sinh học phân tử và di truyền học', 180000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
(8, 53, 'ADVANCED', 'Phương pháp ôn tập logic các mốc lịch sử', 160000.00, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE price_per_hour = VALUES(price_per_hour), is_active = 1;

-- Traits & Highlights
INSERT INTO mentor_personalities (mentor_id, personality_option_id, created_at)
SELECT p.id, o.id, UTC_TIMESTAMP(6)
FROM mentor_profiles p
CROSS JOIN personality_options o
WHERE (p.id = 1 AND o.name IN ('Kiên nhẫn', 'Tỉ mỉ', 'Kỷ luật'))
   OR (p.id = 2 AND o.name IN ('Kỷ luật', 'Truyền cảm hứng'))
   OR (p.id = 3 AND o.name IN ('Thân thiện', 'Truyền cảm hứng', 'Lắng nghe tốt'))
   OR (p.id = 4 AND o.name IN ('Kiên nhẫn', 'Thân thiện'))
   OR (p.id = 5 AND o.name IN ('Lắng nghe tốt', 'Truyền cảm hứng'))
   OR (p.id = 6 AND o.name IN ('Tỉ mỉ', 'Kỷ luật'))
   OR (p.id = 7 AND o.name IN ('Kiên nhẫn', 'Thân thiện'))
   OR (p.id = 8 AND o.name IN ('Thân thiện', 'Truyền cảm hứng'))
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO mentor_highlights (mentor_id, highlight_option_id, created_at)
SELECT p.id, o.id, UTC_TIMESTAMP(6)
FROM mentor_profiles p
CROSS JOIN mentor_highlight_options o
WHERE (p.id = 1 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 3+ năm', 'Chuyên ôn thi'))
   OR (p.id = 2 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 5+ năm', 'Dạy online tốt'))
   OR (p.id = 3 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 5+ năm', 'Có giáo án cá nhân hóa'))
   OR (p.id = 4 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 3+ năm'))
   OR (p.id = 5 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 5+ năm'))
   OR (p.id = 6 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 3+ năm', 'Dạy online tốt'))
   OR (p.id = 7 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 3+ năm'))
   OR (p.id = 8 AND o.name IN ('Đã xác minh danh tính', 'Kinh nghiệm 3+ năm'))
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);
