-- ==============================================================================
-- Full Monolith Demo Data Import into Mentor-Matching Microservices
-- Databases: mm_auth, mm_user, mm_mentor
-- Password for all accounts: 123456
-- ==============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ==============================================================================
-- 1. MM_AUTH: Accounts, Roles, and Password Credentials
-- ==============================================================================
USE mm_auth;
SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_users;
CREATE TEMPORARY TABLE tmp_demo_users (
    seq INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(50),
    user_type VARCHAR(50),
    status VARCHAR(50),
    uuid_hex VARCHAR(36)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

INSERT INTO tmp_demo_users (full_name, email, phone, role, user_type, status, uuid_hex) VALUES
('Admin Test', 'admin@test.com', '0901100001', 'ADMIN', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0000-000000000001'),
('Manager Nguyen Khoa', 'manager01@mentor-matching.local', '0901100002', 'MANAGER', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0000-000000000002'),
('Manager Thuy Duong', 'manager02@mentor-matching.local', '0901100003', 'MANAGER', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0000-000000000003'),
('Mentor Minh Anh', 'mentor01@mentor-matching.local', '0901100004', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000001'),
('Mentor Quoc Huy', 'mentor02@mentor-matching.local', '0901100005', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000002'),
('Mentor Ngoc Mai', 'mentor03@mentor-matching.local', '0901100006', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000003'),
('Mentor Thanh Tung', 'mentor04@mentor-matching.local', '0901100007', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000004'),
('Mentor Bao Vy', 'mentor05@mentor-matching.local', '0901100008', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000005'),
('Mentor Hoang Yen', 'mentor06@mentor-matching.local', '0901100009', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000006'),
('Mentor Duc Tri', 'mentor07@mentor-matching.local', '0901100010', 'MENTOR', 'WORKING_ADULT', 'DISABLED', '00000000-0000-0000-0001-000000000007'),
('Learner Kim Ngan', 'learner01@mentor-matching.local', '0902200001', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000001'),
('Learner An Nhi', 'learner02@mentor-matching.local', '0902200002', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000002'),
('Learner Hoang Long', 'learner03@mentor-matching.local', '0902200003', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000003'),
('Learner My Linh', 'learner04@mentor-matching.local', '0902200004', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000004'),
('Learner Quang Huy', 'learner05@mentor-matching.local', '0902200005', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000005'),
('Learner Diem My', 'learner06@mentor-matching.local', '0902200006', 'LEARNER', 'PARENT', 'ACTIVE', '00000000-0000-0000-0002-000000000006'),
('Learner Phuoc An', 'learner07@mentor-matching.local', '0902200007', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000007'),
('Learner Tram Lam', 'learner08@mentor-matching.local', '0902200008', 'LEARNER', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000008'),
('Learner Anh Kiet', 'learner09@mentor-matching.local', '0902200009', 'LEARNER', 'STUDENT', 'ACTIVE', '00000000-0000-0000-0002-000000000009'),
('Learner Thanh Ha', 'learner10@mentor-matching.local', '0902200010', 'LEARNER', 'STUDENT', 'DISABLED', '00000000-0000-0000-0002-000000000010'),
('Mentor Gia Bao', 'mentor08@mentor-matching.local', '0901100011', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000008'),
('Mentor Khanh Linh', 'mentor09@mentor-matching.local', '0901100012', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000009'),
('Mentor Tuan Minh', 'mentor10@mentor-matching.local', '0901100013', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000010'),
('Mentor Phuong Nhi', 'mentor11@mentor-matching.local', '0901100014', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000011'),
('Mentor Duy Anh', 'mentor12@mentor-matching.local', '0901100015', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000012'),
('Mentor Ngoc Han', 'mentor13@mentor-matching.local', '0901100016', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000013'),
('Mentor Minh Quan', 'mentor14@mentor-matching.local', '0901100017', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000014'),
('Mentor Bao Chau', 'mentor15@mentor-matching.local', '0901100018', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000015'),
('Mentor Hoang Nam', 'mentor16@mentor-matching.local', '0901100019', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000016'),
('Mentor Thao Nguyen', 'mentor17@mentor-matching.local', '0901100020', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000017'),
('Mentor Thanh Lam', 'mentor18@mentor-matching.local', '0901100021', 'MENTOR', 'WORKING_ADULT', 'DISABLED', '00000000-0000-0000-0001-000000000018'),
('Mentor Hai Dang', 'mentor19@mentor-matching.local', '0901100022', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000019'),
('Mentor Minh Chau', 'mentor20@mentor-matching.local', '0901100023', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000020'),
('Mentor Gia Han', 'mentor21@mentor-matching.local', '0901100024', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000021'),
('Mentor Nam Phong', 'mentor22@mentor-matching.local', '0901100025', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000022'),
('Mentor Tue Lam', 'mentor23@mentor-matching.local', '0901100026', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000023'),
('Mentor Bao Ngoc', 'mentor24@mentor-matching.local', '0901100027', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000024'),
('Mentor Quynh Anh', 'mentor25@mentor-matching.local', '0901100028', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000025'),
('Mentor Viet Anh', 'mentor26@mentor-matching.local', '0901100029', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000026'),
('Mentor Ha My', 'mentor27@mentor-matching.local', '0901100030', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000027'),
('Mentor Minh Khoi', 'mentor28@mentor-matching.local', '0901100031', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000028'),
('Mentor Khanh Vy', 'mentor29@mentor-matching.local', '0901100032', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000029'),
('Mentor Nhat Minh', 'mentor30@mentor-matching.local', '0901100033', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000030'),
('Mentor Phuong Linh', 'mentor31@mentor-matching.local', '0901100034', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000031'),
('Mentor Bao Long', 'mentor32@mentor-matching.local', '0901100035', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000032'),
('Mentor Thien An', 'mentor33@mentor-matching.local', '0901100036', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000033'),
('Mentor Ngoc Diep', 'mentor34@mentor-matching.local', '0901100037', 'MENTOR', 'UNIVERSITY_STUDENT', 'DISABLED', '00000000-0000-0000-0001-000000000034'),
('Mentor Thanh Phuc', 'mentor35@mentor-matching.local', '0901100038', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000035'),
('Mentor Anh Thu', 'mentor36@mentor-matching.local', '0901100039', 'MENTOR', 'UNIVERSITY_STUDENT', 'ACTIVE', '00000000-0000-0000-0001-000000000036'),
('Mentor Gia Khang', 'mentor37@mentor-matching.local', '0901100040', 'MENTOR', 'WORKING_ADULT', 'ACTIVE', '00000000-0000-0000-0001-000000000037');

-- 1.1 Insert or update accounts
INSERT INTO accounts (id, login_email, status, email_verified_at, created_at, updated_at, version)
SELECT UUID_TO_BIN(u.uuid_hex), u.email, u.status, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM tmp_demo_users u
ON DUPLICATE KEY UPDATE status = VALUES(status), email_verified_at = VALUES(email_verified_at);

-- 1.2 Insert or update password credentials (BCrypt hash for 123456: $2a$10$f8K0p/rAvE9SrsZcahthQuKA79pIAqGt9APO14f3.czxnCPp9q6Iu)
INSERT INTO password_credentials (account_id, password_hash, password_changed_at)
SELECT UUID_TO_BIN(u.uuid_hex), '$2a$10$f8K0p/rAvE9SrsZcahthQuKA79pIAqGt9APO14f3.czxnCPp9q6Iu', UTC_TIMESTAMP(6)
FROM tmp_demo_users u
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), password_changed_at = VALUES(password_changed_at);

-- 1.3 Insert account roles
INSERT INTO account_roles (account_id, role_id, assigned_at)
SELECT UUID_TO_BIN(u.uuid_hex), r.id, UTC_TIMESTAMP(6)
FROM tmp_demo_users u
JOIN roles r ON r.code = (CASE WHEN u.role = 'LEARNER' THEN _ascii 'LEANER' WHEN u.role = 'MENTOR' THEN _ascii 'MENTOR' ELSE _ascii 'ADMIN' END)
WHERE NOT EXISTS (
    SELECT 1 FROM account_roles ar WHERE ar.account_id = UUID_TO_BIN(u.uuid_hex) AND ar.role_id = r.id
);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_users;


-- ==============================================================================
-- 2. MM_USER: User Profiles & Learner Profiles
-- ==============================================================================
USE mm_user;
SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_users_user;
CREATE TEMPORARY TABLE tmp_demo_users_user (
    uuid_hex VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    user_type VARCHAR(50),
    status VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

INSERT INTO tmp_demo_users_user (uuid_hex, full_name, email, phone, user_type, status) VALUES
('00000000-0000-0000-0000-000000000001', 'Admin Test', 'admin@test.com', '0901100001', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0000-000000000002', 'Manager Nguyen Khoa', 'manager01@mentor-matching.local', '0901100002', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0000-000000000003', 'Manager Thuy Duong', 'manager02@mentor-matching.local', '0901100003', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000001', 'Mentor Minh Anh', 'mentor01@mentor-matching.local', '0901100004', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000002', 'Mentor Quoc Huy', 'mentor02@mentor-matching.local', '0901100005', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000003', 'Mentor Ngoc Mai', 'mentor03@mentor-matching.local', '0901100006', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000004', 'Mentor Thanh Tung', 'mentor04@mentor-matching.local', '0901100007', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000005', 'Mentor Bao Vy', 'mentor05@mentor-matching.local', '0901100008', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000006', 'Mentor Hoang Yen', 'mentor06@mentor-matching.local', '0901100009', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000007', 'Mentor Duc Tri', 'mentor07@mentor-matching.local', '0901100010', 'WORKING_ADULT', 'DISABLED'),
('00000000-0000-0000-0002-000000000001', 'Learner Kim Ngan', 'learner01@mentor-matching.local', '0902200001', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000002', 'Learner An Nhi', 'learner02@mentor-matching.local', '0902200002', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000003', 'Learner Hoang Long', 'learner03@mentor-matching.local', '0902200003', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000004', 'Learner My Linh', 'learner04@mentor-matching.local', '0902200004', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000005', 'Learner Quang Huy', 'learner05@mentor-matching.local', '0902200005', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000006', 'Learner Diem My', 'learner06@mentor-matching.local', '0902200006', 'PARENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000007', 'Learner Phuoc An', 'learner07@mentor-matching.local', '0902200007', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000008', 'Learner Tram Lam', 'learner08@mentor-matching.local', '0902200008', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000009', 'Learner Anh Kiet', 'learner09@mentor-matching.local', '0902200009', 'STUDENT', 'ACTIVE'),
('00000000-0000-0000-0002-000000000010', 'Learner Thanh Ha', 'learner10@mentor-matching.local', '0902200010', 'STUDENT', 'DISABLED'),
('00000000-0000-0000-0001-000000000008', 'Mentor Gia Bao', 'mentor08@mentor-matching.local', '0901100011', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000009', 'Mentor Khanh Linh', 'mentor09@mentor-matching.local', '0901100012', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000010', 'Mentor Tuan Minh', 'mentor10@mentor-matching.local', '0901100013', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000011', 'Mentor Phuong Nhi', 'mentor11@mentor-matching.local', '0901100014', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000012', 'Mentor Duy Anh', 'mentor12@mentor-matching.local', '0901100015', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000013', 'Mentor Ngoc Han', 'mentor13@mentor-matching.local', '0901100016', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000014', 'Mentor Minh Quan', 'mentor14@mentor-matching.local', '0901100017', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000015', 'Mentor Bao Chau', 'mentor15@mentor-matching.local', '0901100018', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000016', 'Mentor Hoang Nam', 'mentor16@mentor-matching.local', '0901100019', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000017', 'Mentor Thao Nguyen', 'mentor17@mentor-matching.local', '0901100020', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000018', 'Mentor Thanh Lam', 'mentor18@mentor-matching.local', '0901100021', 'WORKING_ADULT', 'DISABLED'),
('00000000-0000-0000-0001-000000000019', 'Mentor Hai Dang', 'mentor19@mentor-matching.local', '0901100022', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000020', 'Mentor Minh Chau', 'mentor20@mentor-matching.local', '0901100023', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000021', 'Mentor Gia Han', 'mentor21@mentor-matching.local', '0901100024', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000022', 'Mentor Nam Phong', 'mentor22@mentor-matching.local', '0901100025', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000023', 'Mentor Tue Lam', 'mentor23@mentor-matching.local', '0901100026', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000024', 'Mentor Bao Ngoc', 'mentor24@mentor-matching.local', '0901100027', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000025', 'Mentor Quynh Anh', 'mentor25@mentor-matching.local', '0901100028', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000026', 'Mentor Viet Anh', 'mentor26@mentor-matching.local', '0901100029', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000027', 'Mentor Ha My', 'mentor27@mentor-matching.local', '0901100030', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000028', 'Mentor Minh Khoi', 'mentor28@mentor-matching.local', '0901100031', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000029', 'Mentor Khanh Vy', 'mentor29@mentor-matching.local', '0901100032', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000030', 'Mentor Nhat Minh', 'mentor30@mentor-matching.local', '0901100033', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000031', 'Mentor Phuong Linh', 'mentor31@mentor-matching.local', '0901100034', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000032', 'Mentor Bao Long', 'mentor32@mentor-matching.local', '0901100035', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000033', 'Mentor Thien An', 'mentor33@mentor-matching.local', '0901100036', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000034', 'Mentor Ngoc Diep', 'mentor34@mentor-matching.local', '0901100037', 'UNIVERSITY_STUDENT', 'DISABLED'),
('00000000-0000-0000-0001-000000000035', 'Mentor Thanh Phuc', 'mentor35@mentor-matching.local', '0901100038', 'WORKING_ADULT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000036', 'Mentor Anh Thu', 'mentor36@mentor-matching.local', '0901100039', 'UNIVERSITY_STUDENT', 'ACTIVE'),
('00000000-0000-0000-0001-000000000037', 'Mentor Gia Khang', 'mentor37@mentor-matching.local', '0901100040', 'WORKING_ADULT', 'ACTIVE');

-- 2.1 Insert/update user_profiles
INSERT INTO user_profiles (user_id, email, full_name, phone, user_type, account_status, account_status_version, created_at, updated_at, version)
SELECT UUID_TO_BIN(u.uuid_hex), u.email, u.full_name, u.phone, u.user_type, u.status, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM tmp_demo_users_user u
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), phone = VALUES(phone), user_type = VALUES(user_type), account_status = VALUES(account_status);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_users_user;

-- 2.2 Learner Profiles
DROP TEMPORARY TABLE IF EXISTS tmp_demo_learner_profiles;
CREATE TEMPORARY TABLE tmp_demo_learner_profiles (
    learner_number INT PRIMARY KEY,
    gender VARCHAR(50),
    birth_year INT,
    school_name VARCHAR(255),
    grade_id BIGINT,
    learning_goal TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

INSERT INTO tmp_demo_learner_profiles (learner_number, gender, birth_year, school_name, grade_id, learning_goal) VALUES
(1, 'FEMALE', 2011, 'THCS Nguyễn Du', 9, 'Ôn thi vào lớp 10, tập trung Toán và Tiếng Anh.'),
(2, 'FEMALE', 2013, 'THCS Trưng Vương', 7, 'Củng cố kiến thức Toán, Ngữ văn và rèn phương pháp học đều đặn.'),
(3, 'MALE', 2012, 'THCS Lê Quý Đôn', 8, 'Nâng cao Toán và Khoa học tự nhiên, chuẩn bị học lớp 9.'),
(4, 'FEMALE', 2010, 'THPT Phan Đình Phùng', 10, 'Cải thiện Tiếng Anh giao tiếp và nền tảng IELTS.'),
(5, 'MALE', 2009, 'THPT Nguyễn Trãi', 11, 'Học chắc Vật lý và Toán để chuẩn bị định hướng khối A.'),
(6, 'FEMALE', 1986, 'Phụ huynh học sinh', 6, 'Tìm gia sư đồng hành cho con trong giai đoạn chuyển cấp.'),
(7, 'MALE', 2014, 'Tiểu học Lê Văn Tám', 5, 'Ôn Toán, Tiếng Việt và chuẩn bị vào trung học cơ sở.'),
(8, 'FEMALE', 2005, 'Đại học Khoa học Tự nhiên', 12, 'Luyện thi IELTS và củng cố kỹ năng viết học thuật.'),
(9, 'MALE', 2015, 'Tiểu học Nguyễn Huệ', 4, 'Xây nền Toán tư duy và thói quen tự học.'),
(10, 'FEMALE', 2011, 'THCS Võ Trường Toản', 9, 'Ôn thi vào lớp 10, ưu tiên Ngữ văn và Tiếng Anh.');

INSERT INTO learner_profiles (id, user_id, gender, birth_year, school_name, grade_id, learning_goal, created_at, updated_at, version)
SELECT 
    UUID_TO_BIN(UUID()), 
    UUID_TO_BIN(CONCAT('00000000-0000-0000-0002-', LPAD(lp.learner_number, 12, '0'))), 
    lp.gender, lp.birth_year, lp.school_name, lp.grade_id, lp.learning_goal, 
    UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM tmp_demo_learner_profiles lp
WHERE NOT EXISTS (
    SELECT 1 FROM learner_profiles existing 
    WHERE existing.user_id = UUID_TO_BIN(CONCAT('00000000-0000-0000-0002-', LPAD(lp.learner_number, 12, '0')))
);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_learner_profiles;


-- ==============================================================================
-- 3. MM_MENTOR: Mentor Profiles, Subjects, Traits & Verifications
-- ==============================================================================
USE mm_mentor;
SET NAMES utf8mb4;

-- 3.1 Base Options
INSERT INTO personality_options (name, description, created_at, updated_at) VALUES
('Kiên nhẫn', 'Phù hợp với học sinh cần thời gian để hiểu bài, dễ mất tự tin hoặc cần được giải thích lại nhiều lần.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Thân thiện', 'Tạo cảm giác thoải mái trong buổi học, giúp học sinh dễ đặt câu hỏi và bớt áp lực.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Kỷ luật', 'Giữ nhịp học rõ ràng, theo sát bài tập và phù hợp với học sinh cần rèn thói quen học đều.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Truyền cảm hứng', 'Biết khơi gợi hứng thú học tập, liên hệ kiến thức với mục tiêu và câu chuyện thực tế.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Tỉ mỉ', 'Chú ý từng lỗi nhỏ trong cách làm bài, cách diễn đạt và quá trình luyện tập của học sinh.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Năng động', 'Buổi học có nhiều tương tác, ví dụ thực tế và hoạt động giúp học sinh duy trì sự tập trung.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Lắng nghe tốt', 'Quan sát phản ứng của học sinh, điều chỉnh tốc độ dạy và phản hồi theo nhu cầu cá nhân.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Tư duy hệ thống', 'Giúp học sinh nhìn thấy cấu trúc kiến thức, mối liên hệ giữa các chủ đề và cách tự ôn tập.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Hài hước vừa phải', 'Biết tạo không khí nhẹ nhàng mà vẫn giữ mục tiêu học tập nghiêm túc.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Định hướng mục tiêu', 'Tập trung vào mục tiêu cụ thể như nâng điểm, ôn thi, luyện chứng chỉ hoặc xây nền kiến thức.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO mentor_highlight_options (name, description, created_at, updated_at) VALUES
('Đã xác minh danh tính', 'Mentor đã hoàn tất bước xác minh hồ sơ cá nhân trên hệ thống.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Được duyệt bởi admin', 'Hồ sơ mentor đã được kiểm tra và cho phép hiển thị công khai.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Kinh nghiệm 3+ năm', 'Mentor có ít nhất ba năm kinh nghiệm dạy học, kèm cặp hoặc đào tạo.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Kinh nghiệm 5+ năm', 'Mentor có ít nhất năm năm kinh nghiệm dạy học, phù hợp với nhu cầu học dài hạn hoặc luyện thi.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Chuyên ôn thi', 'Có kinh nghiệm xây lộ trình, luyện đề và theo dõi tiến độ cho các kỳ thi quan trọng.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Phù hợp học sinh mất gốc', 'Có phương pháp dạy chậm, chắc và biết xây lại nền tảng từ đầu.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Dạy online tốt', 'Sử dụng công cụ học online hiệu quả, có tài liệu số và tương tác rõ trong buổi học từ xa.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Có giáo án cá nhân hóa', 'Điều chỉnh nội dung học theo mục tiêu, trình độ và tốc độ tiếp thu của từng học sinh.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Có bài tập sau buổi học', 'Cung cấp bài luyện thêm hoặc checklist ôn tập để học sinh duy trì tiến độ giữa các buổi.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Feedback sau buổi học', 'Có nhận xét sau buổi học để học sinh hoặc phụ huynh nắm được tiến độ và điểm cần cải thiện.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Linh hoạt lịch học', 'Có khả năng sắp xếp lịch học đa dạng, phù hợp với học sinh bận hoặc cần học ngắn hạn.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Có chứng chỉ chuyên môn', 'Mentor có chứng chỉ, giải thưởng hoặc bằng cấp liên quan đến môn dạy.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Giọng giảng dễ hiểu', 'Cách diễn đạt rõ ràng, chậm rãi và giúp học sinh dễ theo dõi trong suốt buổi học.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Theo sát tiến bộ', 'Có thói quen ghi nhận lỗi sai, đánh giá tiến bộ và điều chỉnh lộ trình theo từng giai đoạn.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Phù hợp học sinh nhỏ tuổi', 'Có kinh nghiệm giữ nhịp học, tạo tương tác và giao tiếp phù hợp với học sinh tiểu học.', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 3.2 Mentor Profiles
DROP TEMPORARY TABLE IF EXISTS tmp_demo_mentor_profiles;
CREATE TEMPORARY TABLE tmp_demo_mentor_profiles (
    mentor_number INT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    avatar_url TEXT NOT NULL,
    gender VARCHAR(50),
    hometown_city_code VARCHAR(50) NOT NULL,
    current_district_code VARCHAR(50) NOT NULL,
    headline VARCHAR(255),
    introduction TEXT,
    teaching_style TEXT,
    experience_years INT,
    current_position VARCHAR(255),
    workplace VARCHAR(255),
    education VARCHAR(255),
    major VARCHAR(255),
    meeting_type VARCHAR(50),
    approval_status VARCHAR(50),
    approval_note TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_demo_mentor_profiles (
    mentor_number, full_name, avatar_url, gender, hometown_city_code, current_district_code,
    headline, introduction, teaching_style, experience_years,
    current_position, workplace, education, major,
    meeting_type, approval_status, approval_note
)
VALUES
(1, 'Mentor Minh Anh', 'https://i.pravatar.cc/300?img=1', 'FEMALE', '01', '00166',
     'Gia sư Toán THCS, đồng hành ôn thi vào lớp 10',
     'Mình có kinh nghiệm kèm học sinh THCS, đặc biệt là các bạn bị mất gốc Toán hoặc thiếu tự tin khi làm bài. Mình từng hỗ trợ học sinh cải thiện từ mức trung bình lên khá bằng cách xây lại nền tảng đại số, hình học và kỹ năng trình bày. Mình phù hợp với học sinh cần một lộ trình rõ ràng và có người theo sát tiến bộ.',
     'Buổi đầu mình kiểm tra nhanh lỗ hổng kiến thức để biết học sinh đang vướng ở đâu. Sau đó mình chia bài học thành từng phần nhỏ: nhắc lại kiến thức, làm ví dụ mẫu, luyện bài cùng mentor và giao bài tự luyện. Mỗi vài buổi mình sẽ tổng kết lỗi thường gặp để học sinh thấy được tiến bộ thật.',
     3, 'Sinh viên ngành Sư phạm Toán', 'Đại học Sư phạm Hà Nội', 'Đại học', 'Sư phạm Toán', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (2, 'Mentor Quoc Huy', 'https://i.pravatar.cc/300?img=2', 'MALE', '79', '26740',
     'Mentor Vật lý định hướng thi THPT',
     'Mình xuất phát từ nền tảng kỹ thuật nên luôn cố gắng giúp học sinh hiểu bản chất công thức thay vì học thuộc máy móc. Mình có kinh nghiệm dạy Vật lý cho học sinh lớp 10 đến lớp 12, từ mức cơ bản đến luyện đề vận dụng. Điểm mạnh của mình là biến các bài toán dài thành từng bước xử lý rõ ràng.',
     'Mình thường bắt đầu bằng hiện tượng thực tế, sau đó mới đi vào công thức và dạng bài. Học sinh sẽ được luyện theo từng nhóm kỹ năng: đọc đề, chọn công thức, đổi đơn vị, trình bày lời giải và kiểm tra đáp án. Sau mỗi buổi mình ghi lại lỗi sai chính để buổi sau không lặp lại.',
     5, 'Kỹ sư phần mềm', 'Công ty công nghệ tại TP.HCM', 'Đại học', 'Kỹ thuật máy tính', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (3, 'Mentor Ngoc Mai', 'https://i.pravatar.cc/300?img=3', 'FEMALE', '48', '20263',
     'Mentor Tiếng Anh và IELTS nền tảng cho học sinh cấp 3',
     'Mình hỗ trợ học sinh xây nền tiếng Anh vững trước khi bước vào luyện đề. Với các bạn mới bắt đầu IELTS, mình ưu tiên ngữ pháp, từ vựng học thuật, phát âm và kỹ năng đọc hiểu thay vì ép làm đề quá sớm. Mình phù hợp với học sinh muốn học có lộ trình và cần người chữa lỗi kỹ.',
     'Mình dạy theo từng kỹ năng, có bài tập ngắn sau mỗi buổi và phần chữa lỗi chi tiết. Khi học Writing hoặc Speaking, mình không chỉ sửa câu mà còn giải thích vì sao cách diễn đạt đó tự nhiên hoặc chưa tự nhiên. Mỗi tháng mình review mục tiêu để điều chỉnh tốc độ học phù hợp.',
     4, 'IELTS Tutor', 'Trung tâm Anh ngữ', 'Đại học', 'Ngôn ngữ Anh', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (4, 'Mentor Thanh Tung', 'https://i.pravatar.cc/300?img=4', 'MALE', '01', '00367',
     'Mentor Python và tư duy lập trình cho học sinh mới bắt đầu',
     'Mình dạy lập trình cho học sinh từ cấp 2 đến cấp 3, đặc biệt là các bạn chưa từng viết code. Mục tiêu của mình là giúp học sinh hiểu tư duy giải quyết vấn đề, biết chia nhỏ bài toán và tự tin tự viết chương trình đầu tiên. Mình cũng hỗ trợ các bạn muốn chuẩn bị nền tảng cho ngành công nghệ thông tin.',
     'Mình ưu tiên học qua bài tập nhỏ, trực quan và có sản phẩm sau mỗi giai đoạn. Học sinh sẽ được hướng dẫn cách đọc lỗi, debug từng bước và giải thích lại ý tưởng bằng lời của mình. Khi học tốt nền tảng, mình mới tăng dần độ khó với bài toán dữ liệu, game nhỏ hoặc web đơn giản.',
     4, 'Backend Developer', 'Startup giáo dục', 'Đại học', 'Khoa học máy tính', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (5, 'Mentor Bao Vy', 'https://i.pravatar.cc/300?img=5', 'FEMALE', '79', '26929',
     'Gia sư Ngữ văn giúp học sinh biết lập ý và viết rõ',
     'Mình đồng hành với học sinh THCS và THPT trong môn Ngữ văn, nhất là các bạn sợ viết dài hoặc chưa biết triển khai ý. Mình không bắt học sinh học thuộc văn mẫu mà hướng dẫn cách đọc đề, tìm luận điểm và dùng dẫn chứng phù hợp. Mình tin rằng viết tốt bắt đầu từ việc hiểu mình muốn nói gì.',
     'Mỗi buổi học có phần đọc hiểu ngắn, phân tích cấu trúc bài và luyện viết theo từng đoạn. Mình chữa bài bằng cách chỉ ra điểm mạnh trước, sau đó mới sửa lỗi diễn đạt, bố cục và dẫn chứng. Học sinh sẽ có sổ lỗi cá nhân để theo dõi những điểm cần cải thiện qua từng tuần.',
     6, 'Giáo viên Ngữ văn', 'Trường THPT tư thục', 'Đại học', 'Sư phạm Ngữ văn', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (6, 'Mentor Hoang Yen', 'https://i.pravatar.cc/300?img=6', 'FEMALE', '92', '31153',
     'Mentor Hóa học và Sinh học cho học sinh THCS, THPT',
     'Mình có kinh nghiệm hỗ trợ học sinh học Hóa và Sinh từ nền tảng đến luyện đề. Với các bạn mới làm quen Hóa học, mình tập trung vào bản chất phản ứng, cách cân bằng phương trình và phương pháp ghi nhớ hệ thống. Với Sinh học, mình giúp học sinh liên kết kiến thức thay vì học rời rạc.',
     'Mình dạy bằng sơ đồ, bảng so sánh và câu hỏi gợi mở để học sinh tự giải thích lại bài. Bài tập được chia theo mức: nhận biết, thông hiểu, vận dụng và bài tổng hợp. Sau mỗi chủ đề mình có một buổi ôn nhanh để học sinh tự kiểm tra xem phần nào còn lỏng.',
     5, 'Gia sư Khoa học tự nhiên', 'Tự do', 'Đại học', 'Công nghệ sinh học', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (7, 'Mentor Duc Tri', 'https://i.pravatar.cc/300?img=7', 'MALE', '75', '26068',
     'Mentor Toán nâng cao cho học sinh khá giỏi',
     'Mình từng tham gia đội tuyển Toán và có kinh nghiệm hướng dẫn học sinh khá giỏi luyện chuyên đề. Mình phù hợp với học sinh đã có nền tảng ổn, muốn thử sức với bài toán khó hơn hoặc chuẩn bị cho kỳ thi chọn lớp, chọn đội tuyển. Mình chú trọng tư duy phân tích thay vì mẹo giải nhanh thiếu nền.',
     'Mình thường yêu cầu học sinh trình bày hướng nghĩ trước khi xem lời giải. Sau đó mình cùng học sinh so sánh nhiều cách tiếp cận để chọn cách gọn và chắc nhất. Với bài khó, mình chia thành các gợi ý nhỏ để học sinh vẫn là người tự đi tới lời giải.',
     2, 'Sinh viên ngành Toán ứng dụng', 'Đại học Bách khoa', 'Đại học', 'Toán ứng dụng', 'ONLINE', 'SUSPENDED', 'Demo mentor tạm khóa để test trạng thái.'),

    (8, 'Mentor Gia Bao', 'https://i.pravatar.cc/300?img=8', 'MALE', '66', '22303',
     'Mentor Tin học phổ thông và Scratch cho học sinh nhỏ tuổi',
     'Mình thích dạy học sinh nhỏ tuổi tiếp cận máy tính một cách vui vẻ nhưng có cấu trúc. Với Scratch và Tin học cơ bản, mình giúp học sinh làm quen tư duy thuật toán, vòng lặp, điều kiện và cách biến ý tưởng thành sản phẩm nhỏ. Phụ huynh có thể theo dõi kết quả qua từng dự án học sinh làm được.',
     'Mỗi buổi học có một mục tiêu cụ thể như làm trò chơi, hoạt cảnh hoặc bài tập tương tác. Mình hướng dẫn từng bước nhưng luôn để học sinh tự chọn chi tiết sáng tạo. Khi có lỗi, mình biến lỗi thành câu hỏi để học sinh học cách quan sát và sửa vấn đề.',
     3, 'Giảng viên câu lạc bộ STEM', 'Trung tâm STEM địa phương', 'Cao đẳng', 'Tin học ứng dụng', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (9, 'Mentor Khanh Linh', 'https://i.pravatar.cc/300?img=9', 'FEMALE', '56', '22516',
     'Gia sư Tiếng Anh giao tiếp và ngữ pháp căn bản',
     'Mình làm việc tốt với học sinh mất gốc tiếng Anh hoặc ngại nói. Mình giúp các bạn xây lại từ phát âm, cấu trúc câu cơ bản, từ vựng theo chủ đề và phản xạ giao tiếp hằng ngày. Mục tiêu của mình là để học sinh thấy tiếng Anh có thể học từng chút một, không phải một khối kiến thức quá đáng sợ.',
     'Mình kết hợp luyện nói ngắn, bài tập ngữ pháp vừa sức và hoạt động nghe lặp lại. Mỗi buổi học đều có phần sửa lỗi phát âm hoặc dùng từ, nhưng mình giữ nhịp học nhẹ để học sinh không bị áp lực. Cuối tuần mình gửi checklist nhỏ để học sinh tự ôn.',
     4, 'English Teaching Assistant', 'Trung tâm ngoại ngữ', 'Đại học', 'Ngôn ngữ Anh', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (10, 'Mentor Tuan Minh', 'https://i.pravatar.cc/300?img=10', 'MALE', '68', '25000',
     'Mentor Toán và Lý cho học sinh cần kỷ luật học tập',
     'Mình phù hợp với học sinh cần một người kèm sát, nhắc tiến độ và rèn thói quen làm bài đều. Mình đã hỗ trợ nhiều bạn cải thiện điểm kiểm tra bằng cách chia nhỏ mục tiêu từng tuần, không để dồn bài tới sát ngày thi. Ngoài kiến thức Toán Lý, mình chú ý cả cách ghi vở và tự ôn.',
     'Mình đặt mục tiêu rõ cho từng buổi và có bài kiểm tra nhanh đầu hoặc cuối buổi. Khi học sinh sai, mình không chỉ chữa đáp án mà truy lại bước tư duy dẫn đến lỗi. Phụ huynh có thể nhận nhận xét ngắn sau buổi nếu cần theo dõi tiến độ.',
     6, 'Kỹ sư điện', 'Công ty năng lượng', 'Đại học', 'Kỹ thuật điện', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (11, 'Mentor Phuong Nhi', 'https://i.pravatar.cc/300?img=11', 'FEMALE', '86', '29254',
     'Mentor Lịch sử và Địa lý theo hướng kể chuyện dễ nhớ',
     'Mình giúp học sinh học các môn xã hội bằng cách kết nối sự kiện, nhân vật, bản đồ và bối cảnh thay vì học thuộc từng đoạn rời rạc. Mình phù hợp với học sinh cần cải thiện kỹ năng ghi nhớ, trình bày ý và trả lời câu hỏi tự luận. Mình tin các môn xã hội sẽ dễ hơn nhiều khi học sinh nhìn thấy câu chuyện phía sau dữ kiện.',
     'Mình dùng timeline, sơ đồ tư duy và câu hỏi gợi mở trong mỗi buổi học. Sau phần kiến thức, học sinh luyện trả lời ngắn rồi mở rộng thành đoạn văn hoàn chỉnh. Mình cũng hướng dẫn cách tự tạo flashcard và cách ôn trước kiểm tra để giảm học vẹt.',
     3, 'Sinh viên ngành Sư phạm Lịch sử', 'Đại học Cần Thơ', 'Đại học', 'Sư phạm Lịch sử', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (12, 'Mentor Duy Anh', 'https://i.pravatar.cc/300?img=12', 'MALE', '48', '20194',
     'Mentor Lập trình Web cho học sinh cấp 3',
     'Mình hướng dẫn học sinh cấp 3 làm quen với HTML, CSS, JavaScript và cách xây một trang web hoàn chỉnh. Mình không chỉ dạy cú pháp mà còn giải thích cách tổ chức file, cách đọc tài liệu và cách biến một ý tưởng nhỏ thành sản phẩm có thể chạy được. Mình phù hợp với học sinh muốn thử ngành công nghệ trước khi chọn đại học.',
     'Mình dạy theo dự án nhỏ, mỗi giai đoạn có sản phẩm nhìn thấy được. Học sinh sẽ được tự chỉnh giao diện, thêm tương tác và học cách debug bằng trình duyệt. Khi nền tảng ổn, mình giới thiệu thêm Git, API đơn giản và tư duy chia component.',
     4, 'Frontend Developer', 'Công ty sản phẩm số', 'Đại học', 'Công nghệ phần mềm', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (13, 'Mentor Ngoc Han', 'https://i.pravatar.cc/300?img=13', 'FEMALE', '01', '00004',
     'Gia sư Tiếng Việt tiểu học kiên nhẫn và sát sao',
     'Mình có kinh nghiệm kèm học sinh tiểu học luyện đọc, chính tả, tập làm văn và kỹ năng trình bày bài. Mình đặc biệt kiên nhẫn với các bạn nhỏ thiếu tập trung hoặc chưa có thói quen học ở nhà. Mục tiêu của mình là giúp học sinh tiến bộ từng chút mà vẫn giữ cảm giác vui khi học.',
     'Mình chia buổi học thành nhiều hoạt động ngắn để học sinh không bị mệt. Các phần đọc, viết và luyện từ được xen kẽ với câu hỏi gợi mở hoặc trò chơi nhỏ. Sau mỗi buổi mình ghi lại điểm cần luyện thêm để phụ huynh có thể hỗ trợ con ở nhà.',
     5, 'Giáo viên tiểu học', 'Trường tiểu học tư thục', 'Đại học', 'Giáo dục tiểu học', 'OFFLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (14, 'Mentor Minh Quan', 'https://i.pravatar.cc/300?img=14', 'MALE', '79', '26824',
     'Mentor SAT Math và Toán tiếng Anh',
     'Mình hỗ trợ học sinh làm quen với SAT Math, toán bằng tiếng Anh và kỹ năng xử lý đề chuẩn hóa. Mình phù hợp với học sinh đã có nền Toán ổn nhưng chưa quen cách đọc đề tiếng Anh hoặc áp lực thời gian. Mình tập trung vào chiến lược làm bài, quản lý thời gian và nhận diện dạng câu hỏi.',
     'Mình bắt đầu bằng diagnostic test để xác định nhóm lỗi chính. Sau đó học sinh luyện theo từng chủ đề, từ đại số, hàm số, hình học đến dữ liệu và xác suất. Mỗi buổi đều có phần timing ngắn để học sinh quen nhịp thi thật.',
     4, 'SAT Math Mentor', 'Tự do', 'Đại học', 'Tài chính định lượng', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (15, 'Mentor Bao Chau', 'https://i.pravatar.cc/300?img=15', 'FEMALE', '79', '27073',
     'Mentor Sinh học giúp học sinh hiểu hệ thống kiến thức',
     'Mình yêu thích Sinh học vì môn này có nhiều liên kết thú vị giữa cơ thể, môi trường và đời sống. Mình giúp học sinh học theo hệ thống thay vì ghi nhớ từng đoạn sách giáo khoa. Với học sinh lớp 9 đến lớp 12, mình tập trung vào khái niệm cốt lõi, sơ đồ hóa kiến thức và luyện câu hỏi vận dụng.',
     'Mình thường dùng hình vẽ, bảng so sánh và câu hỏi vì sao để học sinh tự giải thích hiện tượng. Mỗi chủ đề có phần tổng kết bằng sơ đồ, sau đó luyện câu hỏi từ dễ đến khó. Khi học sinh sai, mình truy ngược lại khái niệm gốc để sửa tận nơi.',
     3, 'Sinh viên Y khoa', 'Đại học Y Dược', 'Đại học', 'Y đa khoa', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (16, 'Mentor Hoang Nam', 'https://i.pravatar.cc/300?img=16', 'MALE', '75', '26041',
     'Gia sư Toán tiểu học và Toán tư duy',
     'Mình dạy Toán cho học sinh tiểu học theo hướng nhẹ nhàng, nhiều ví dụ gần với đời sống. Mình giúp các bạn nhỏ hiểu phép tính, bài toán có lời văn và các dạng tư duy logic thay vì chỉ làm theo mẫu. Mình phù hợp với học sinh cần xây nền tự tin từ sớm.',
     'Mình dùng câu hỏi, hình minh họa và bài tập ngắn để giữ nhịp học. Với bài toán khó, mình hướng dẫn học sinh kể lại đề bằng lời của mình trước khi giải. Mình luôn khuyến khích học sinh giải thích cách nghĩ, vì đó là dấu hiệu quan trọng hơn đáp án đúng một lần.',
     2, 'Gia sư tiểu học', 'Tự do', 'Đại học', 'Giáo dục học', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (17, 'Mentor Thao Nguyen', 'https://i.pravatar.cc/300?img=17', 'FEMALE', '01', '00235',
     'Mentor TOEIC và tiếng Anh công việc',
     'Mình hỗ trợ học viên lớn hơn hoặc học sinh cuối cấp cần TOEIC, tiếng Anh công việc và kỹ năng đọc hiểu tài liệu. Mình có kinh nghiệm xây lộ trình cho người bận rộn, cần học đều nhưng không có quá nhiều thời gian mỗi ngày. Mình tập trung vào điểm số thực tế nhưng vẫn giữ nền ngôn ngữ chắc.',
     'Mình chia mục tiêu theo tuần với lượng bài vừa phải. Trong buổi học, học viên luyện nghe đọc theo dạng câu hỏi, phân tích bẫy thường gặp và học từ vựng theo ngữ cảnh. Mỗi giai đoạn có bài mini test để xem phần nào cần tăng tốc.',
     7, 'Chuyên viên đào tạo tiếng Anh', 'Doanh nghiệp giáo dục', 'Đại học', 'Ngôn ngữ Anh', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (18, 'Mentor Thanh Lam', 'https://i.pravatar.cc/300?img=18', 'MALE', '79', '27004',
     'Mentor Hóa học luyện nền tảng và bài tập tính toán',
     'Mình giúp học sinh học Hóa từ những phần nền như mol, phương trình phản ứng, dung dịch đến bài tập tính toán tổng hợp. Mình hiểu nhiều bạn sợ Hóa vì có quá nhiều công thức, nên mình ưu tiên cách phân loại bài và nhận biết dữ kiện. Khi học sinh nắm được khung xử lý, môn Hóa sẽ bớt rối hơn nhiều.',
     'Mình dạy theo dạng bài, mỗi dạng có công thức nền, ví dụ mẫu và bài tự luyện tăng dần. Học sinh được yêu cầu ghi rõ giả thiết, đổi đơn vị và kiểm tra kết quả. Mình cũng có các bảng tóm tắt phản ứng quan trọng để học sinh ôn nhanh trước kiểm tra.',
     5, 'Kỹ thuật viên phòng thí nghiệm', 'Viện nghiên cứu ứng dụng', 'Đại học', 'Hóa học', 'ONLINE', 'SUSPENDED', 'Demo mentor tạm khóa để test trạng thái.'),

    (19, 'Mentor Hai Dang', 'https://i.pravatar.cc/300?img=19', 'MALE', '92', '31153',
     'Mentor Địa lý và kỹ năng đọc Atlat',
     'Mình hướng dẫn học sinh học Địa lý bằng bản đồ, số liệu và cách đọc Atlat hiệu quả. Mình phù hợp với học sinh lớp 9 đến lớp 12 cần cải thiện điểm kiểm tra hoặc ôn thi tốt nghiệp. Mục tiêu của mình là giúp học sinh biết cách lấy thông tin, so sánh và trình bày câu trả lời mạch lạc.',
     'Mỗi buổi mình chọn một nhóm kỹ năng cụ thể như đọc biểu đồ, phân tích bảng số liệu hoặc xác định vùng trên bản đồ. Học sinh luyện trả lời theo khung ý để tránh lan man. Sau vài buổi, mình cho bài tổng hợp để kiểm tra khả năng liên kết kiến thức.',
     4, 'Gia sư Địa lý', 'Tự do', 'Đại học', 'Địa lý học', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (20, 'Mentor Minh Chau', 'https://i.pravatar.cc/300?img=20', 'FEMALE', '01', '00166',
     'Mentor IELTS Writing chữa bài kỹ và có lộ trình',
     'Mình chuyên hỗ trợ học sinh và sinh viên luyện IELTS Writing từ band nền tảng lên mức sử dụng câu chữ rõ ràng hơn. Mình không khuyến khích học thuộc bài mẫu mà tập trung vào cách phân tích đề, lập dàn ý và phát triển luận điểm. Học viên phù hợp là người muốn được chữa lỗi chi tiết và biết vì sao mình sai.',
     'Mình chữa bài theo ba lớp: ý tưởng, bố cục và ngôn ngữ. Sau mỗi bài viết, học viên nhận được nhận xét cụ thể kèm phiên bản gợi ý để học lại cách diễn đạt. Mình cũng giao bài ngắn giữa tuần để duy trì nhịp viết thay vì chỉ học trong buổi chính.',
     4, 'IELTS Writing Coach', 'Trung tâm luyện thi', 'Đại học', 'Ngôn ngữ Anh', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (21, 'Mentor Gia Han', 'https://i.pravatar.cc/300?img=21', 'FEMALE', '79', '26743',
     'Gia sư Toán lớp 6 đến lớp 8 cho học sinh mất gốc',
     'Mình thường làm việc với học sinh chuyển cấp từ tiểu học lên THCS, khi chương trình Toán bắt đầu nhiều khái niệm mới. Mình giúp học sinh lấy lại nền phân số, số nguyên, phương trình cơ bản và hình học đầu cấp. Mình kiên nhẫn và không tạo áp lực điểm số ngay từ đầu.',
     'Mình kiểm tra nền trước, sau đó thiết kế bài học theo từng lỗ hổng cụ thể. Bài tập không quá nhiều nhưng được chọn để học sinh hiểu đúng bản chất. Khi học sinh tiến bộ, mình tăng dần bài tổng hợp để các bạn quen với đề kiểm tra trên lớp.',
     3, 'Sinh viên ngành Toán', 'Đại học Khoa học Tự nhiên', 'Đại học', 'Toán học', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (22, 'Mentor Nam Phong', 'https://i.pravatar.cc/300?img=22', 'MALE', '48', '20263',
     'Mentor Khoa học tự nhiên tích hợp bậc THCS',
     'Mình hỗ trợ học sinh THCS học môn Khoa học tự nhiên tích hợp, nơi các phần Lý, Hóa, Sinh thường đan xen khiến nhiều bạn bối rối. Mình giúp học sinh nhìn thấy mối liên hệ giữa hiện tượng, thí nghiệm và lý thuyết trong sách. Cách học của mình phù hợp với các bạn cần hiểu tổng thể trước khi luyện bài.',
     'Mình dùng sơ đồ chủ đề và câu hỏi thực tế để mở bài. Sau phần lý thuyết, học sinh làm bài kiểm tra ngắn theo từng mức độ để biết mình đã hiểu đến đâu. Mình cũng hướng dẫn cách ghi chú theo bảng để không lẫn kiến thức giữa các phân môn.',
     5, 'Giáo viên Khoa học tự nhiên', 'Trường THCS liên cấp', 'Đại học', 'Sư phạm Khoa học tự nhiên', 'OFFLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (23, 'Mentor Tue Lam', 'https://i.pravatar.cc/300?img=23', 'FEMALE', '75', '26068',
     'Mentor Tiếng Trung cơ bản và HSK nhập môn',
     'Mình dạy Tiếng Trung cho người mới bắt đầu, từ phát âm, pinyin, chữ Hán cơ bản đến hội thoại đời sống. Mình phù hợp với học sinh muốn học thêm ngoại ngữ thứ hai hoặc người mới cần nền tảng để luyện HSK sau này. Mình chú trọng phát âm đúng ngay từ đầu để học viên không phải sửa quá nhiều về sau.',
     'Mình kết hợp luyện nghe nói ngắn với nhận diện mặt chữ và viết từ theo bộ thủ. Mỗi buổi có phần ôn từ vựng cũ, mẫu câu mới và hội thoại ứng dụng. Học viên được giao bài nghe ngắn để giữ cảm giác với âm thanh tiếng Trung mỗi ngày.',
     3, 'Gia sư Tiếng Trung', 'Tự do', 'Đại học', 'Ngôn ngữ Trung Quốc', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (24, 'Mentor Bao Ngoc', 'https://i.pravatar.cc/300?img=24', 'MALE', '79', '26884',
     'Mentor quản lý thời gian và phương pháp học tập',
     'Mình hỗ trợ học sinh xây lịch học, chia mục tiêu và duy trì thói quen tự học. Nhiều bạn không yếu kiến thức nhưng dễ trì hoãn hoặc học dồn sát ngày kiểm tra, nên mình tập trung vào cách tổ chức công việc học tập. Mình phù hợp với học sinh cấp 2, cấp 3 cần một người đồng hành định hướng.',
     'Mình bắt đầu bằng việc nhìn lại lịch học hiện tại và các điểm hay bị vỡ kế hoạch. Sau đó học sinh cùng mình thiết kế lịch tuần, checklist môn học và cách tự đánh giá cuối ngày. Các buổi sau sẽ điều chỉnh kế hoạch dựa trên dữ liệu thật, không ép theo khuôn cứng.',
     6, 'Academic Coach', 'Trung tâm kỹ năng học tập', 'Đại học', 'Tâm lý giáo dục', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (25, 'Mentor Quynh Anh', 'https://i.pravatar.cc/300?img=25', 'FEMALE', '56', '22516',
     'Mentor Tiếng Nhật sơ cấp và JLPT N5 N4',
     'Mình dạy Tiếng Nhật cho người mới bắt đầu, ưu tiên phát âm, bảng chữ, mẫu câu nền và thói quen ôn từ vựng đều. Với học viên hướng đến JLPT N5 hoặc N4, mình giúp xây nền ngữ pháp và luyện đọc câu ngắn trước khi làm đề. Mình thích dạy chậm mà chắc để học viên không bỏ cuộc giữa chừng.',
     'Mỗi buổi có phần ôn chữ, từ vựng, mẫu câu và hội thoại ngắn. Mình thường dùng ví dụ gần với đời sống để học viên nhớ cách dùng thay vì chỉ thuộc công thức. Khi luyện đề, mình hướng dẫn cách đọc câu hỏi và loại đáp án gây nhiễu.',
     4, 'Japanese Tutor', 'Trung tâm ngoại ngữ', 'Đại học', 'Nhật Bản học', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (26, 'Mentor Viet Anh', 'https://i.pravatar.cc/300?img=26', 'MALE', '01', '00367',
     'Mentor Đại số và Hình học lớp 9 đến lớp 12',
     'Mình tập trung vào các chuyên đề Toán quan trọng ở cuối cấp THCS và THPT như phương trình, bất đẳng thức, hình học tọa độ và hình học không gian. Mình phù hợp với học sinh muốn tăng điểm kiểm tra hoặc chuẩn bị nền cho thi tốt nghiệp. Mình thích giải thích bài toán bằng nhiều góc nhìn để học sinh chọn cách hợp với mình.',
     'Mình yêu cầu học sinh nắm định nghĩa và điều kiện áp dụng trước khi luyện công thức. Bài tập được xếp theo chuỗi tăng dần, từ nhận diện dạng đến bài phối hợp nhiều ý. Sau mỗi chuyên đề, mình cho bài tổng hợp và cùng học sinh phân tích chiến lược làm bài.',
     8, 'Giáo viên Toán', 'Trường THPT chuyên', 'Thạc sĩ', 'Toán học', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (27, 'Mentor Ha My', 'https://i.pravatar.cc/300?img=27', 'FEMALE', '92', '31153',
     'Gia sư lớp 5 chuẩn bị chuyển cấp',
     'Mình hỗ trợ học sinh lớp 5 củng cố Toán, Tiếng Việt và kỹ năng học tập trước khi bước vào THCS. Đây là giai đoạn nhiều bạn bắt đầu cần tự học hơn, nên mình không chỉ dạy kiến thức mà còn rèn cách ghi bài, làm bài và kiểm tra lại đáp án. Mình làm việc tốt với học sinh cần sự động viên nhẹ nhàng.',
     'Mình chia buổi học thành phần kiến thức chính, bài luyện và phần tự giải có hướng dẫn. Với Tiếng Việt, mình chú trọng đọc hiểu và diễn đạt câu rõ ràng. Với Toán, mình hướng dẫn học sinh tự tóm tắt đề trước khi chọn phép tính.',
     4, 'Giáo viên tiểu học', 'Trường tiểu học song ngữ', 'Đại học', 'Giáo dục tiểu học', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (28, 'Mentor Minh Khoi', 'https://i.pravatar.cc/300?img=28', 'MALE', '79', '26740',
     'Mentor Python ứng dụng cho học sinh yêu công nghệ',
     'Mình dạy Python theo hướng ứng dụng, giúp học sinh làm được công cụ nhỏ, xử lý dữ liệu đơn giản hoặc trò chơi console. Mình phù hợp với học sinh đã biết chút lập trình hoặc muốn học nghiêm túc hơn sau Scratch. Mình luôn nhấn mạnh cách đặt câu hỏi và tự tìm lỗi, vì đó là kỹ năng rất quan trọng khi học công nghệ.',
     'Mình dạy bằng bài tập có ngữ cảnh thay vì chỉ liệt kê cú pháp. Học sinh sẽ viết code trong buổi, chạy thử, đọc lỗi và cải tiến chương trình. Sau vài buổi, mình giao mini project để học sinh có cảm giác hoàn thành một sản phẩm thật.',
     5, 'Data Engineer', 'Công ty phân tích dữ liệu', 'Đại học', 'Khoa học dữ liệu', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (29, 'Mentor Khanh Vy', 'https://i.pravatar.cc/300?img=29', 'FEMALE', '75', '26080',
     'Mentor Thuyết trình và viết sáng tạo',
     'Mình giúp học sinh cải thiện khả năng diễn đạt ý tưởng, viết đoạn văn rõ và thuyết trình tự tin hơn. Môn này đặc biệt phù hợp với học sinh ngại nói, ngại viết hoặc có nhiều ý nhưng chưa biết sắp xếp. Mình kết hợp kỹ năng ngôn ngữ với sự tự tin khi trình bày trước người khác.',
     'Mình bắt đầu từ bài nói hoặc bài viết rất ngắn để học sinh không bị áp lực. Sau đó mình hướng dẫn cách mở ý, chọn ví dụ, dùng giọng nói và ngôn ngữ cơ thể. Mỗi buổi học sinh đều có một phần trình bày nhỏ và nhận feedback cụ thể.',
     3, 'Huấn luyện viên kỹ năng mềm', 'Câu lạc bộ tranh biện', 'Đại học', 'Truyền thông', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (30, 'Mentor Nhat Minh', 'https://i.pravatar.cc/300?img=30', 'MALE', '66', '22303',
     'Mentor Hóa Sinh cho học sinh định hướng khối B',
     'Mình hỗ trợ học sinh định hướng khối B học Hóa và Sinh theo lộ trình dài hơi. Mình giúp các bạn nắm nền trước, sau đó mới chuyển sang luyện đề và kỹ thuật làm bài nhanh. Mình phù hợp với học sinh có mục tiêu y dược, sinh học hoặc các ngành khoa học sức khỏe.',
     'Mình lập kế hoạch theo chủ đề lớn và gắn mỗi chủ đề với dạng câu hỏi thường gặp. Học sinh luyện bài theo mức độ tăng dần và có phần tự giải thích lại khái niệm sau mỗi buổi. Với bài sai, mình phân loại lỗi do kiến thức, đọc đề hay tính toán để sửa đúng điểm.',
     6, 'Sinh viên Y khoa năm cuối', 'Đại học Y Dược', 'Đại học', 'Y đa khoa', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (31, 'Mentor Phuong Linh', 'https://i.pravatar.cc/300?img=31', 'FEMALE', '01', '00070',
     'Mentor Ngữ văn ôn thi tốt nghiệp THPT',
     'Mình hỗ trợ học sinh lớp 12 ôn Ngữ văn theo hướng nắm chắc tác phẩm, biết triển khai nghị luận và tránh học thuộc máy móc. Mình giúp học sinh xây bộ ý chính, cách mở bài kết bài và cách dùng dẫn chứng linh hoạt. Mục tiêu là bài viết đủ ý, rõ mạch và có giọng văn tự nhiên.',
     'Mình dạy theo từng dạng đề, sau đó luyện viết từng phần trước khi viết cả bài. Học sinh được chữa lỗi về luận điểm, bố cục, dẫn chứng và diễn đạt. Mỗi tuần mình có phần ôn nhanh tác phẩm để giữ kiến thức không bị rơi.',
     7, 'Giáo viên Ngữ văn', 'Trung tâm luyện thi', 'Đại học', 'Sư phạm Ngữ văn', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (32, 'Mentor Bao Long', 'https://i.pravatar.cc/300?img=32', 'MALE', '79', '27094',
     'Mentor Tin học văn phòng và kỹ năng số',
     'Mình hướng dẫn học sinh và người mới sử dụng máy tính các kỹ năng tin học cơ bản, soạn thảo văn bản, bảng tính và trình bày slide. Mình phù hợp với học viên cần học thực tế để dùng ngay trong học tập hoặc công việc. Mình cũng hỗ trợ tư duy tổ chức file, tìm kiếm thông tin và an toàn số cơ bản.',
     'Mình dạy theo tình huống thực hành, ví dụ tạo bảng điểm, làm bài thuyết trình hoặc định dạng báo cáo. Học viên thao tác trực tiếp trong buổi và được sửa từng bước. Sau mỗi buổi có bài thực hành ngắn để củng cố kỹ năng vừa học.',
     5, 'IT Support Specialist', 'Doanh nghiệp dịch vụ', 'Cao đẳng', 'Hệ thống thông tin', 'OFFLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (33, 'Mentor Thien An', 'https://i.pravatar.cc/300?img=33', 'FEMALE', '48', '20194',
     'Mentor Tiếng Hàn sơ cấp và TOPIK nhập môn',
     'Mình dạy Tiếng Hàn cho người mới bắt đầu, từ bảng chữ Hangeul, phát âm, mẫu câu cơ bản đến giao tiếp đời sống. Mình phù hợp với học sinh hoặc sinh viên muốn học ngoại ngữ thứ hai theo nhịp nhẹ nhưng đều. Với TOPIK nhập môn, mình giúp học viên xây nền từ vựng và ngữ pháp trước khi luyện đề.',
     'Mình kết hợp luyện đọc, nghe, nói ngắn và viết câu đơn giản trong mỗi buổi. Học viên được ôn lại bằng flashcard và đoạn hội thoại ngắn. Khi học viên phát âm sai, mình sửa ngay bằng cách tách âm và luyện lặp có kiểm soát.',
     3, 'Korean Tutor', 'Tự do', 'Đại học', 'Hàn Quốc học', 'ONLINE', 'PENDING', 'Chờ admin duyệt hồ sơ demo.'),

    (34, 'Mentor Ngoc Diep', 'https://i.pravatar.cc/300?img=34', 'FEMALE', '79', '27058',
     'Gia sư Toán và Tiếng Anh cho học sinh tiểu học',
     'Mình thích làm việc với học sinh nhỏ tuổi và giúp các bạn xây cảm giác học tập tích cực. Với Toán, mình tập trung vào hiểu đề, tính toán cẩn thận và tư duy logic. Với Tiếng Anh, mình ưu tiên từ vựng đời sống, phát âm và mẫu câu ngắn dễ dùng.',
     'Mình tổ chức buổi học thành nhiều hoạt động nhỏ để học sinh không mất tập trung. Các bài luyện được chọn vừa sức nhưng có một chút thử thách để học sinh thấy mình tiến bộ. Mình thường gửi nhận xét ngắn cho phụ huynh sau buổi học để cùng theo dõi thói quen học.',
     2, 'Sinh viên ngành Giáo dục tiểu học', 'Đại học Sư phạm', 'Đại học', 'Giáo dục tiểu học', 'HYBRID', 'PENDING', 'Chờ admin duyệt hồ sơ demo.'),

    (35, 'Mentor Thanh Phuc', 'https://i.pravatar.cc/300?img=35', 'MALE', '75', '26020',
     'Mentor luyện thi vào lớp 10 môn Toán',
     'Mình chuyên kèm học sinh lớp 9 chuẩn bị thi vào lớp 10, đặc biệt là các bạn cần tăng tốc trong 3 đến 6 tháng cuối. Mình giúp học sinh hệ thống lại các chuyên đề trọng tâm, luyện đề theo cấu trúc và xử lý lỗi sai có kế hoạch. Mình phù hợp với học sinh cần mục tiêu rõ và lịch học nghiêm túc.',
     'Mình chia lộ trình thành ba giai đoạn: vá nền, luyện dạng và luyện đề tổng hợp. Sau mỗi đề, học sinh không chỉ biết điểm mà còn biết mất điểm ở dạng nào. Mình giữ bảng theo dõi lỗi sai để các buổi sau tập trung đúng phần cần cải thiện.',
     6, 'Giáo viên Toán luyện thi', 'Trung tâm luyện thi lớp 10', 'Đại học', 'Sư phạm Toán', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.'),

    (36, 'Mentor Anh Thu', 'https://i.pravatar.cc/300?img=36', 'FEMALE', '92', '31153',
     'Mentor tiếng Anh cho học sinh mất gốc cấp 2',
     'Mình hỗ trợ học sinh cấp 2 lấy lại nền tiếng Anh từ phát âm, từ vựng cơ bản và cấu trúc câu thường gặp. Mình hiểu nhiều bạn sợ tiếng Anh vì học lâu nhưng không biết bắt đầu lại từ đâu. Mình giúp học sinh học từng phần nhỏ và có cảm giác kiểm soát được tiến độ.',
     'Mỗi buổi học có phần ôn cũ, kiến thức mới và luyện bài ngắn ngay tại lớp. Mình sửa lỗi bằng ví dụ đơn giản và cho học sinh lặp lại nhiều lần trong ngữ cảnh khác nhau. Sau vài tuần, học sinh sẽ có bộ ghi chú riêng để tự ôn trước kiểm tra.',
     4, 'Gia sư Tiếng Anh THCS', 'Tự do', 'Đại học', 'Ngôn ngữ Anh', 'HYBRID', 'APPROVED', 'Demo mentor đã duyệt.'),

    (37, 'Mentor Gia Khang', 'https://i.pravatar.cc/300?img=37', 'MALE', '79', '26944',
     'Mentor tư duy logic và giải quyết vấn đề',
     'Mình dạy các chủ đề tư duy logic, toán vui và kỹ năng giải quyết vấn đề cho học sinh tiểu học lớn và THCS. Mục tiêu không chỉ là làm đúng bài, mà là biết đặt câu hỏi, thử cách tiếp cận và không sợ bài lạ. Mình phù hợp với học sinh thích khám phá hoặc cần rèn khả năng suy luận.',
     'Mình dùng bài toán ngắn, trò chơi logic và thử thách theo cấp độ. Học sinh được khuyến khích giải thích cách nghĩ, kể cả khi đáp án chưa đúng. Sau mỗi buổi mình tổng kết một chiến thuật tư duy nhỏ để học sinh áp dụng vào bài học sau.',
     3, 'Mentor tư duy logic', 'Câu lạc bộ học thuật', 'Đại học', 'Khoa học máy tính', 'ONLINE', 'APPROVED', 'Demo mentor đã duyệt.');

-- 3.3 Insert/update mentor_profiles
INSERT INTO mentor_profiles (
    user_id,
    full_name,
    avatar_url,
    gender,
    hometown_city_id,
    current_district_id,
    headline,
    introduction,
    teaching_style,
    experience_years,
    current_position,
    workplace,
    education,
    major,
    meeting_type,
    approval_status,
    account_status,
    average_rating,
    total_reviews,
    created_at,
    updated_at
)
SELECT
    UUID_TO_BIN(CONCAT('00000000-0000-0000-0001-', LPAD(mp.mentor_number, 12, '0'))),
    mp.full_name,
    mp.avatar_url,
    mp.gender,
    c.id,
    d.id,
    mp.headline,
    mp.introduction,
    mp.teaching_style,
    mp.experience_years,
    mp.current_position,
    mp.workplace,
    mp.education,
    mp.major,
    mp.meeting_type,
    mp.approval_status,
    'ACTIVE',
    ROUND(4.7 + (RAND() * 0.3), 2),
    FLOOR(12 + (RAND() * 40)),
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
FROM tmp_demo_mentor_profiles mp
JOIN cities c ON c.code = mp.hometown_city_code
JOIN districts d ON d.code = mp.current_district_code AND d.city_id = c.id
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    avatar_url = VALUES(avatar_url),
    gender = VALUES(gender),
    hometown_city_id = VALUES(hometown_city_id),
    current_district_id = VALUES(current_district_id),
    headline = VALUES(headline),
    introduction = VALUES(introduction),
    teaching_style = VALUES(teaching_style),
    experience_years = VALUES(experience_years),
    current_position = VALUES(current_position),
    workplace = VALUES(workplace),
    education = VALUES(education),
    major = VALUES(major),
    meeting_type = VALUES(meeting_type),
    approval_status = VALUES(approval_status),
    account_status = 'ACTIVE';

-- 3.4 Verifications for all 37 mentors
INSERT INTO mentor_verifications (
    mentor_id, full_name, id_card_number, id_card_front_url, id_card_back_url,
    verification_status, created_at, updated_at
)
SELECT
    p.id, p.full_name,
    CONCAT('00120000', LPAD(p.id, 4, '0')),
    'https://example.com/id_front.jpg',
    'https://example.com/id_back.jpg',
    'VERIFIED',
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
FROM mentor_profiles p
ON DUPLICATE KEY UPDATE verification_status = 'VERIFIED';

-- 3.5 Personalities and Highlights
DROP TEMPORARY TABLE IF EXISTS tmp_mentor_trait_options;
CREATE TEMPORARY TABLE tmp_mentor_trait_options (
    trait_group INT NOT NULL,
    personality_name VARCHAR(255) NOT NULL,
    highlight_name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_mentor_trait_options (trait_group, personality_name, highlight_name) VALUES
(0, 'Kiên nhẫn', 'Phù hợp học sinh mất gốc'),
(1, 'Thân thiện', 'Giọng giảng dễ hiểu'),
(2, 'Kỷ luật', 'Theo sát tiến bộ'),
(3, 'Truyền cảm hứng', 'Chuyên ôn thi'),
(4, 'Tỉ mỉ', 'Có bài tập sau buổi học'),
(5, 'Năng động', 'Dạy online tốt'),
(6, 'Lắng nghe tốt', 'Có giáo án cá nhân hóa'),
(7, 'Tư duy hệ thống', 'Kinh nghiệm 3+ năm'),
(8, 'Hài hước vừa phải', 'Phù hợp học sinh nhỏ tuổi'),
(9, 'Định hướng mục tiêu', 'Feedback sau buổi học');

INSERT INTO mentor_personalities (mentor_id, personality_option_id, created_at)
WITH RECURSIVE mentor_numbers (mentor_number) AS (
    SELECT 1 UNION ALL SELECT mentor_number + 1 FROM mentor_numbers WHERE mentor_number < 37
),
mentor_trait_names AS (
    SELECT mentor_number, MOD(mentor_number, 10) AS trait_group FROM mentor_numbers
    UNION ALL SELECT mentor_number, MOD(mentor_number + 3, 10) FROM mentor_numbers
    UNION ALL SELECT mentor_number, MOD(mentor_number + 6, 10) FROM mentor_numbers
)
SELECT mp.id, po.id, UTC_TIMESTAMP(6)
FROM mentor_trait_names trait
JOIN mentor_profiles mp ON mp.user_id = UUID_TO_BIN(CONCAT('00000000-0000-0000-0001-', LPAD(trait.mentor_number, 12, '0')))
JOIN tmp_mentor_trait_options option_data ON option_data.trait_group = trait.trait_group
JOIN personality_options po ON po.name = option_data.personality_name
WHERE NOT EXISTS (
    SELECT 1 FROM mentor_personalities existing WHERE existing.mentor_id = mp.id AND existing.personality_option_id = po.id
);

INSERT INTO mentor_highlights (mentor_id, highlight_option_id, created_at)
WITH RECURSIVE mentor_numbers (mentor_number) AS (
    SELECT 1 UNION ALL SELECT mentor_number + 1 FROM mentor_numbers WHERE mentor_number < 37
),
mentor_highlight_names AS (
    SELECT mentor_number, MOD(mentor_number, 10) AS trait_group FROM mentor_numbers
    UNION ALL SELECT mentor_number, MOD(mentor_number + 2, 10) FROM mentor_numbers
    UNION ALL SELECT mentor_number, MOD(mentor_number + 5, 10) FROM mentor_numbers
)
SELECT mp.id, ho.id, UTC_TIMESTAMP(6)
FROM mentor_highlight_names trait
JOIN mentor_profiles mp ON mp.user_id = UUID_TO_BIN(CONCAT('00000000-0000-0000-0001-', LPAD(trait.mentor_number, 12, '0')))
JOIN tmp_mentor_trait_options option_data ON option_data.trait_group = trait.trait_group
JOIN mentor_highlight_options ho ON ho.name = option_data.highlight_name
WHERE NOT EXISTS (
    SELECT 1 FROM mentor_highlights existing WHERE existing.mentor_id = mp.id AND existing.highlight_option_id = ho.id
);

DROP TEMPORARY TABLE IF EXISTS tmp_mentor_trait_options;

-- 3.6 Mentor Subjects
DROP TEMPORARY TABLE IF EXISTS tmp_demo_mentor_subjects;
CREATE TEMPORARY TABLE tmp_demo_mentor_subjects (
    mentor_number INT NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    grade_name VARCHAR(50),
    proficiency_level VARCHAR(50),
    teaching_note TEXT,
    price_per_hour DECIMAL(12, 2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_demo_mentor_subjects (
    mentor_number, subject_name, grade_name, proficiency_level, teaching_note, price_per_hour, is_active
)
VALUES
(1, 'Toán', 'Lớp 9', 'ADVANCED', 'Dạy Toán Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (1, 'Đại số', 'Lớp 9', 'ADVANCED', 'Dạy Đại số Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 230000.00, TRUE),
    (1, 'Hình học', 'Lớp 9', 'ADVANCED', 'Dạy Hình học Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 230000.00, TRUE),
    (1, 'Ôn thi vào lớp 10', 'Lớp 9', 'EXPERT', 'Dạy Ôn thi vào lớp 10 Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (2, 'Vật lý', 'Lớp 10', 'ADVANCED', 'Dạy Vật lý Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 250000.00, TRUE),
    (2, 'Vật lý', 'Lớp 11', 'ADVANCED', 'Dạy Vật lý Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (2, 'Vật lý', 'Lớp 12', 'EXPERT', 'Dạy Vật lý Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (3, 'Tiếng Anh', 'Lớp 10', 'ADVANCED', 'Dạy Tiếng Anh Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (3, 'Tiếng Anh', 'Lớp 11', 'ADVANCED', 'Dạy Tiếng Anh Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 250000.00, TRUE),
    (3, 'IELTS', NULL, 'EXPERT', 'Dạy IELTS theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 350000.00, TRUE),
    (4, 'Lập trình Python', 'Lớp 8', 'ADVANCED', 'Dạy Lập trình Python Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 280000.00, TRUE),
    (4, 'Lập trình Python', 'Lớp 10', 'ADVANCED', 'Dạy Lập trình Python Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (4, 'Lập trình Web', 'Lớp 10', 'INTERMEDIATE', 'Dạy Lập trình Web Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 280000.00, TRUE),
    (5, 'Ngữ văn', 'Lớp 9', 'ADVANCED', 'Dạy Ngữ văn Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (5, 'Ngữ văn', 'Lớp 12', 'EXPERT', 'Dạy Ngữ văn Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 280000.00, TRUE),
    (5, 'Viết sáng tạo', 'Lớp 8', 'ADVANCED', 'Dạy Viết sáng tạo Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (6, 'Hóa học', 'Lớp 9', 'ADVANCED', 'Dạy Hóa học Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (6, 'Sinh học', 'Lớp 9', 'ADVANCED', 'Dạy Sinh học Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 230000.00, TRUE),
    (6, 'Khoa học tự nhiên', 'Lớp 8', 'ADVANCED', 'Dạy Khoa học tự nhiên Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (7, 'Toán', 'Lớp 10', 'EXPERT', 'Dạy Toán Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 320000.00, FALSE),
    (7, 'Đại số', 'Lớp 11', 'EXPERT', 'Dạy Đại số Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 330000.00, FALSE),
    (7, 'Hình học', 'Lớp 12', 'EXPERT', 'Dạy Hình học Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 340000.00, FALSE),
    (8, 'Tin học', 'Lớp 5', 'INTERMEDIATE', 'Dạy Tin học Lớp 5 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (8, 'Lập trình Scratch', 'Lớp 4', 'ADVANCED', 'Dạy Lập trình Scratch Lớp 4 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (8, 'Lập trình Scratch', 'Lớp 6', 'ADVANCED', 'Dạy Lập trình Scratch Lớp 6 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 230000.00, TRUE),
    (9, 'Tiếng Anh', 'Lớp 6', 'INTERMEDIATE', 'Dạy Tiếng Anh Lớp 6 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (9, 'Tiếng Anh', 'Lớp 8', 'ADVANCED', 'Dạy Tiếng Anh Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 210000.00, TRUE),
    (9, 'Tiếng Anh', 'Lớp 9', 'ADVANCED', 'Dạy Tiếng Anh Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (10, 'Toán', 'Lớp 10', 'ADVANCED', 'Dạy Toán Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 250000.00, TRUE),
    (10, 'Vật lý', 'Lớp 11', 'ADVANCED', 'Dạy Vật lý Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 270000.00, TRUE),
    (10, 'Vật lý', 'Lớp 12', 'ADVANCED', 'Dạy Vật lý Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 290000.00, TRUE),
    (11, 'Lịch sử', 'Lớp 8', 'ADVANCED', 'Dạy Lịch sử Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (11, 'Địa lý', 'Lớp 9', 'ADVANCED', 'Dạy Địa lý Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (11, 'Lịch sử và Địa lý', 'Lớp 7', 'ADVANCED', 'Dạy Lịch sử và Địa lý Lớp 7 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (12, 'Lập trình Web', 'Lớp 10', 'ADVANCED', 'Dạy Lập trình Web Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (12, 'Lập trình Web', 'Lớp 12', 'ADVANCED', 'Dạy Lập trình Web Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 320000.00, TRUE),
    (12, 'Tin học', 'Lớp 11', 'ADVANCED', 'Dạy Tin học Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (13, 'Tiếng Việt', 'Lớp 2', 'ADVANCED', 'Dạy Tiếng Việt Lớp 2 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 170000.00, TRUE),
    (13, 'Tiếng Việt', 'Lớp 4', 'ADVANCED', 'Dạy Tiếng Việt Lớp 4 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (13, 'Viết sáng tạo', 'Lớp 5', 'INTERMEDIATE', 'Dạy Viết sáng tạo Lớp 5 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (14, 'SAT', NULL, 'EXPERT', 'Dạy SAT theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 420000.00, TRUE),
    (14, 'Toán', 'Lớp 11', 'EXPERT', 'Dạy Toán Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 320000.00, TRUE),
    (14, 'Xác suất - Thống kê', 'Lớp 12', 'ADVANCED', 'Dạy Xác suất - Thống kê Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 330000.00, TRUE),
    (15, 'Sinh học', 'Lớp 9', 'ADVANCED', 'Dạy Sinh học Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 230000.00, TRUE),
    (15, 'Sinh học', 'Lớp 11', 'ADVANCED', 'Dạy Sinh học Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 270000.00, TRUE),
    (15, 'Sinh học', 'Lớp 12', 'EXPERT', 'Dạy Sinh học Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (16, 'Toán', 'Lớp 3', 'ADVANCED', 'Dạy Toán Lớp 3 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 170000.00, TRUE),
    (16, 'Toán', 'Lớp 5', 'ADVANCED', 'Dạy Toán Lớp 5 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (16, 'Toán tư duy', 'Lớp 4', 'ADVANCED', 'Dạy Toán tư duy Lớp 4 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 210000.00, TRUE),
    (17, 'TOEIC', NULL, 'EXPERT', 'Dạy TOEIC theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 320000.00, TRUE),
    (17, 'Tiếng Anh', 'Lớp 12', 'ADVANCED', 'Dạy Tiếng Anh Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (17, 'IELTS', NULL, 'ADVANCED', 'Dạy IELTS theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 340000.00, TRUE),
    (18, 'Hóa học', 'Lớp 10', 'ADVANCED', 'Dạy Hóa học Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, FALSE),
    (18, 'Hóa học', 'Lớp 11', 'ADVANCED', 'Dạy Hóa học Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 280000.00, FALSE),
    (18, 'Hóa học', 'Lớp 12', 'EXPERT', 'Dạy Hóa học Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 320000.00, FALSE),
    (19, 'Địa lý', 'Lớp 9', 'ADVANCED', 'Dạy Địa lý Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (19, 'Địa lý', 'Lớp 12', 'EXPERT', 'Dạy Địa lý Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 250000.00, TRUE),
    (19, 'Lịch sử và Địa lý', 'Lớp 8', 'ADVANCED', 'Dạy Lịch sử và Địa lý Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (20, 'IELTS', NULL, 'EXPERT', 'Dạy IELTS theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 380000.00, TRUE),
    (20, 'Tiếng Anh', 'Lớp 11', 'ADVANCED', 'Dạy Tiếng Anh Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (20, 'Tiếng Anh', 'Lớp 12', 'ADVANCED', 'Dạy Tiếng Anh Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 270000.00, TRUE),
    (21, 'Toán', 'Lớp 6', 'ADVANCED', 'Dạy Toán Lớp 6 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 200000.00, TRUE),
    (21, 'Toán', 'Lớp 7', 'ADVANCED', 'Dạy Toán Lớp 7 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 210000.00, TRUE),
    (21, 'Toán', 'Lớp 8', 'ADVANCED', 'Dạy Toán Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (22, 'Khoa học tự nhiên', 'Lớp 6', 'ADVANCED', 'Dạy Khoa học tự nhiên Lớp 6 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (22, 'Khoa học tự nhiên', 'Lớp 7', 'ADVANCED', 'Dạy Khoa học tự nhiên Lớp 7 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 200000.00, TRUE),
    (22, 'Khoa học tự nhiên', 'Lớp 8', 'ADVANCED', 'Dạy Khoa học tự nhiên Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 210000.00, TRUE),
    (23, 'Tiếng Trung', NULL, 'ADVANCED', 'Dạy Tiếng Trung theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 260000.00, TRUE),
    (23, 'Tiếng Anh', 'Lớp 8', 'INTERMEDIATE', 'Dạy Tiếng Anh Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (24, 'Phương pháp học tập', NULL, 'EXPERT', 'Dạy Phương pháp học tập theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 260000.00, TRUE),
    (24, 'Quản lý thời gian', NULL, 'EXPERT', 'Dạy Quản lý thời gian theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 260000.00, TRUE),
    (24, 'Thuyết trình', NULL, 'ADVANCED', 'Dạy Thuyết trình theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 240000.00, TRUE),
    (25, 'Tiếng Nhật', NULL, 'ADVANCED', 'Dạy Tiếng Nhật theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 280000.00, TRUE),
    (25, 'Tiếng Anh', 'Lớp 10', 'INTERMEDIATE', 'Dạy Tiếng Anh Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (26, 'Đại số', 'Lớp 10', 'EXPERT', 'Dạy Đại số Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (26, 'Hình học', 'Lớp 11', 'EXPERT', 'Dạy Hình học Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 320000.00, TRUE),
    (26, 'Toán', 'Lớp 12', 'EXPERT', 'Dạy Toán Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 350000.00, TRUE),
    (27, 'Toán', 'Lớp 5', 'ADVANCED', 'Dạy Toán Lớp 5 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (27, 'Tiếng Việt', 'Lớp 5', 'ADVANCED', 'Dạy Tiếng Việt Lớp 5 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (27, 'Phương pháp học tập', NULL, 'ADVANCED', 'Dạy Phương pháp học tập theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 210000.00, TRUE),
    (28, 'Lập trình Python', 'Lớp 9', 'ADVANCED', 'Dạy Lập trình Python Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 290000.00, TRUE),
    (28, 'Lập trình Python', 'Lớp 11', 'EXPERT', 'Dạy Lập trình Python Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 330000.00, TRUE),
    (28, 'Tin học', 'Lớp 12', 'ADVANCED', 'Dạy Tin học Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (29, 'Thuyết trình', NULL, 'EXPERT', 'Dạy Thuyết trình theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 260000.00, TRUE),
    (29, 'Viết sáng tạo', 'Lớp 7', 'ADVANCED', 'Dạy Viết sáng tạo Lớp 7 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (29, 'Viết sáng tạo', 'Lớp 10', 'ADVANCED', 'Dạy Viết sáng tạo Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (30, 'Hóa học', 'Lớp 12', 'EXPERT', 'Dạy Hóa học Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 340000.00, TRUE),
    (30, 'Sinh học', 'Lớp 12', 'EXPERT', 'Dạy Sinh học Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 340000.00, TRUE),
    (30, 'Sinh học', 'Lớp 11', 'ADVANCED', 'Dạy Sinh học Lớp 11 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (31, 'Ngữ văn', 'Lớp 12', 'EXPERT', 'Dạy Ngữ văn Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 300000.00, TRUE),
    (31, 'Ôn thi tốt nghiệp THPT', 'Lớp 12', 'EXPERT', 'Dạy Ôn thi tốt nghiệp THPT Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 320000.00, TRUE),
    (31, 'Viết sáng tạo', 'Lớp 12', 'ADVANCED', 'Dạy Viết sáng tạo Lớp 12 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (32, 'Tin học', 'Lớp 7', 'ADVANCED', 'Dạy Tin học Lớp 7 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 200000.00, TRUE),
    (32, 'Tin học', 'Lớp 10', 'ADVANCED', 'Dạy Tin học Lớp 10 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 230000.00, TRUE),
    (32, 'Thuyết trình', NULL, 'INTERMEDIATE', 'Dạy Thuyết trình theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 220000.00, TRUE),
    (33, 'Tiếng Hàn', NULL, 'ADVANCED', 'Dạy Tiếng Hàn theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 280000.00, TRUE),
    (33, 'Tiếng Anh', 'Lớp 9', 'INTERMEDIATE', 'Dạy Tiếng Anh Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 210000.00, TRUE),
    (34, 'Toán', 'Lớp 2', 'ADVANCED', 'Dạy Toán Lớp 2 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 170000.00, TRUE),
    (34, 'Tiếng Anh', 'Lớp 3', 'ADVANCED', 'Dạy Tiếng Anh Lớp 3 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 180000.00, TRUE),
    (34, 'Tiếng Việt', 'Lớp 3', 'ADVANCED', 'Dạy Tiếng Việt Lớp 3 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 170000.00, TRUE),
    (35, 'Ôn thi vào lớp 10', 'Lớp 9', 'EXPERT', 'Dạy Ôn thi vào lớp 10 Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 280000.00, TRUE),
    (35, 'Toán', 'Lớp 9', 'EXPERT', 'Dạy Toán Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 260000.00, TRUE),
    (35, 'Đại số', 'Lớp 9', 'ADVANCED', 'Dạy Đại số Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (35, 'Hình học', 'Lớp 9', 'ADVANCED', 'Dạy Hình học Lớp 9 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (36, 'Tiếng Anh', 'Lớp 6', 'ADVANCED', 'Dạy Tiếng Anh Lớp 6 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 190000.00, TRUE),
    (36, 'Tiếng Anh', 'Lớp 7', 'ADVANCED', 'Dạy Tiếng Anh Lớp 7 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 200000.00, TRUE),
    (36, 'Tiếng Anh', 'Lớp 8', 'ADVANCED', 'Dạy Tiếng Anh Lớp 8 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 210000.00, TRUE),
    (37, 'Toán tư duy', 'Lớp 3', 'ADVANCED', 'Dạy Toán tư duy Lớp 3 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 220000.00, TRUE),
    (37, 'Toán tư duy', 'Lớp 6', 'ADVANCED', 'Dạy Toán tư duy Lớp 6 theo lộ trình cá nhân hóa, có kiểm tra nền tảng và bài luyện sau buổi học.', 240000.00, TRUE),
    (37, 'Phương pháp học tập', NULL, 'ADVANCED', 'Dạy Phương pháp học tập theo mục tiêu cá nhân, có lộ trình rõ ràng và theo dõi tiến bộ qua từng giai đoạn.', 220000.00, TRUE);

INSERT INTO mentor_subjects (
    mentor_id, subject_grade_id, proficiency_level, teaching_note, price_per_hour, is_active, created_at, updated_at
)
SELECT mp.id, sg.id, ms.proficiency_level, ms.teaching_note, ms.price_per_hour, ms.is_active, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM tmp_demo_mentor_subjects ms
JOIN mentor_profiles mp ON mp.user_id = UUID_TO_BIN(CONCAT('00000000-0000-0000-0001-', LPAD(ms.mentor_number, 12, '0')))
JOIN subjects s ON s.name = ms.subject_name
JOIN subject_grades sg ON sg.subject_id = s.id
LEFT JOIN grades g ON g.id = sg.grade_id
WHERE (
        (ms.grade_name IS NULL AND sg.grade_id IS NULL)
        OR g.name = ms.grade_name
    )
  AND NOT EXISTS (
      SELECT 1 FROM mentor_subjects existing WHERE existing.mentor_id = mp.id AND existing.subject_grade_id = sg.id
  );

DROP TEMPORARY TABLE IF EXISTS tmp_demo_mentor_subjects;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_mentor_profiles;
