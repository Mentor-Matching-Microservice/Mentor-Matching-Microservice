-- Dữ liệu mẫu tiếng Việt; chạy bằng scripts/seed-users.py sau Flyway V1.
-- seed_accounts chứa UUID thực đọc từ auth; không tự sinh user_id.
SET NAMES utf8mb4;
CREATE TEMPORARY TABLE seed_profiles (
    email VARCHAR(255) PRIMARY KEY,
    full_name VARCHAR(255), user_type VARCHAR(50), gender VARCHAR(20),
    birth_year INT, school_name VARCHAR(255), learning_goal VARCHAR(2000)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;
INSERT INTO seed_profiles VALUES
    ('0901dattran@gmail.com', 'Trần Thành Đạt', 'UNIVERSITY_STUDENT', 'MALE', 2003, 'Trường Đại học Bách khoa Đà Nẵng', 'Củng cố kiến thức lập trình và rèn luyện tư duy giải quyết vấn đề.'),
    ('letranbaophucxhb@gmail.com', 'Lê Trần Bảo Phúc', 'UNIVERSITY_STUDENT', 'FEMALE', 2004, 'Trường Đại học Kinh tế Đà Nẵng', 'Nâng cao kỹ năng giao tiếp và tự tin trình bày trước đám đông.'),
    ('lengocminhkien06@gmail.com', 'Lê Ngọc Minh Kiên', 'UNIVERSITY_STUDENT', 'MALE', 2005, 'Trường Đại học Sư phạm Đà Nẵng', 'Xây dựng kế hoạch học tập và định hướng nghề nghiệp phù hợp.'),
    ('tranquocthanhdat@gmail.com', 'Trần Quốc Thành Đạt', 'UNIVERSITY_STUDENT', 'FEMALE', 2006, 'Trường Đại học Công nghệ Thông tin', 'Rèn luyện kỹ năng phân tích dữ liệu và vận dụng vào bài tập thực tế.'),
    ('dattqde181019@fpt.edu.vn', 'Nguyễn Minh Đức', 'UNIVERSITY_STUDENT', 'MALE', 2007, 'Trường Đại học Khoa học Tự nhiên', 'Cải thiện kỹ năng làm việc nhóm và quản lý thời gian học tập.'),
    ('account01@example.com', 'Nguyễn Thị Mai Anh', 'UNIVERSITY_STUDENT', 'FEMALE', 2003, 'Trường Đại học Bách khoa Đà Nẵng', 'Củng cố kiến thức lập trình và rèn luyện tư duy giải quyết vấn đề.'),
    ('account02@example.com', 'Trần Hoàng Nam', 'UNIVERSITY_STUDENT', 'MALE', 2004, 'Trường Đại học Kinh tế Đà Nẵng', 'Nâng cao kỹ năng giao tiếp và tự tin trình bày trước đám đông.'),
    ('account03@example.com', 'Lê Thị Ngọc Hân', 'UNIVERSITY_STUDENT', 'FEMALE', 2005, 'Trường Đại học Sư phạm Đà Nẵng', 'Xây dựng kế hoạch học tập và định hướng nghề nghiệp phù hợp.'),
    ('account04@example.com', 'Phạm Đức Huy', 'UNIVERSITY_STUDENT', 'MALE', 2006, 'Trường Đại học Công nghệ Thông tin', 'Rèn luyện kỹ năng phân tích dữ liệu và vận dụng vào bài tập thực tế.'),
    ('account05@example.com', 'Hoàng Thị Thu Hà', 'UNIVERSITY_STUDENT', 'FEMALE', 2007, 'Trường Đại học Khoa học Tự nhiên', 'Cải thiện kỹ năng làm việc nhóm và quản lý thời gian học tập.'),
    ('account06@example.com', 'Vũ Minh Quân', 'UNIVERSITY_STUDENT', 'MALE', 2003, 'Trường Đại học Bách khoa Đà Nẵng', 'Củng cố kiến thức lập trình và rèn luyện tư duy giải quyết vấn đề.'),
    ('account07@example.com', 'Đặng Thị Thanh Trúc', 'UNIVERSITY_STUDENT', 'FEMALE', 2004, 'Trường Đại học Kinh tế Đà Nẵng', 'Nâng cao kỹ năng giao tiếp và tự tin trình bày trước đám đông.'),
    ('account08@example.com', 'Bùi Gia Bảo', 'UNIVERSITY_STUDENT', 'MALE', 2005, 'Trường Đại học Sư phạm Đà Nẵng', 'Xây dựng kế hoạch học tập và định hướng nghề nghiệp phù hợp.'),
    ('account09@example.com', 'Đỗ Thị Phương Linh', 'UNIVERSITY_STUDENT', 'FEMALE', 2006, 'Trường Đại học Công nghệ Thông tin', 'Rèn luyện kỹ năng phân tích dữ liệu và vận dụng vào bài tập thực tế.'),
    ('account10@example.com', 'Hồ Quốc Khánh', 'UNIVERSITY_STUDENT', 'MALE', 2007, 'Trường Đại học Khoa học Tự nhiên', 'Cải thiện kỹ năng làm việc nhóm và quản lý thời gian học tập.'),
    ('account11@example.com', 'Ngô Thị Bảo Ngọc', 'UNIVERSITY_STUDENT', 'FEMALE', 2003, 'Trường Đại học Bách khoa Đà Nẵng', 'Củng cố kiến thức lập trình và rèn luyện tư duy giải quyết vấn đề.'),
    ('account12@example.com', 'Dương Tuấn Kiệt', 'UNIVERSITY_STUDENT', 'MALE', 2004, 'Trường Đại học Kinh tế Đà Nẵng', 'Nâng cao kỹ năng giao tiếp và tự tin trình bày trước đám đông.'),
    ('account13@example.com', 'Lý Thị Minh Châu', 'UNIVERSITY_STUDENT', 'FEMALE', 2005, 'Trường Đại học Sư phạm Đà Nẵng', 'Xây dựng kế hoạch học tập và định hướng nghề nghiệp phù hợp.'),
    ('account14@example.com', 'Đinh Nhật Minh', 'UNIVERSITY_STUDENT', 'MALE', 2006, 'Trường Đại học Công nghệ Thông tin', 'Rèn luyện kỹ năng phân tích dữ liệu và vận dụng vào bài tập thực tế.'),
    ('account15@example.com', 'Mai Thị Thảo Vy', 'UNIVERSITY_STUDENT', 'FEMALE', 2007, 'Trường Đại học Khoa học Tự nhiên', 'Cải thiện kỹ năng làm việc nhóm và quản lý thời gian học tập.'),
    ('account16@example.com', 'Nguyễn Văn Thành', 'WORKING_ADULT', 'MALE', 1985, 'Trường Đại học Bách khoa Đà Nẵng', 'Củng cố kiến thức lập trình và rèn luyện tư duy giải quyết vấn đề.'),
    ('account17@example.com', 'Trần Thị Hồng Nhung', 'WORKING_ADULT', 'FEMALE', 1986, 'Trường Đại học Kinh tế Đà Nẵng', 'Nâng cao kỹ năng giao tiếp và tự tin trình bày trước đám đông.'),
    ('account18@example.com', 'Lê Quang Huy', 'WORKING_ADULT', 'MALE', 1987, 'Trường Đại học Sư phạm Đà Nẵng', 'Xây dựng kế hoạch học tập và định hướng nghề nghiệp phù hợp.'),
    ('account19@example.com', 'Phạm Thị Thu Hương', 'WORKING_ADULT', 'FEMALE', 1988, 'Trường Đại học Công nghệ Thông tin', 'Rèn luyện kỹ năng phân tích dữ liệu và vận dụng vào bài tập thực tế.'),
    ('account20@example.com', 'Hoàng Minh Tuấn', 'WORKING_ADULT', 'MALE', 1989, 'Trường Đại học Khoa học Tự nhiên', 'Cải thiện kỹ năng làm việc nhóm và quản lý thời gian học tập.'),
    ('account21@example.com', 'Vũ Thị Lan Anh', 'WORKING_ADULT', 'FEMALE', 1990, 'Trường Đại học Bách khoa Đà Nẵng', 'Củng cố kiến thức lập trình và rèn luyện tư duy giải quyết vấn đề.'),
    ('account22@example.com', 'Đặng Quốc Bảo', 'WORKING_ADULT', 'MALE', 1991, 'Trường Đại học Kinh tế Đà Nẵng', 'Nâng cao kỹ năng giao tiếp và tự tin trình bày trước đám đông.'),
    ('account23@example.com', 'Bùi Thị Kim Ngân', 'WORKING_ADULT', 'FEMALE', 1992, 'Trường Đại học Sư phạm Đà Nẵng', 'Xây dựng kế hoạch học tập và định hướng nghề nghiệp phù hợp.'),
    ('account24@example.com', 'Đỗ Đức Thịnh', 'WORKING_ADULT', 'MALE', 1993, 'Trường Đại học Công nghệ Thông tin', 'Rèn luyện kỹ năng phân tích dữ liệu và vận dụng vào bài tập thực tế.'),
    ('account25@example.com', 'Hồ Thị Thanh Tâm', 'WORKING_ADULT', 'FEMALE', 1994, 'Trường Đại học Khoa học Tự nhiên', 'Cải thiện kỹ năng làm việc nhóm và quản lý thời gian học tập.');

START TRANSACTION;
INSERT INTO user_profiles (user_id, full_name, phone, user_type, created_at, updated_at, version)
SELECT a.user_id, p.full_name, NULL, p.user_type, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM seed_accounts a JOIN seed_profiles p ON p.email = a.email
WHERE NOT EXISTS (SELECT 1 FROM user_profiles u WHERE u.user_id = a.user_id);

INSERT INTO learner_profiles
    (id, user_id, gender, birth_year, school_name, grade_id, learning_goal, created_at, updated_at, version)
SELECT UUID_TO_BIN(UUID()), a.user_id, p.gender, p.birth_year, p.school_name, NULL,
       p.learning_goal, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM seed_accounts a JOIN seed_profiles p ON p.email = a.email
WHERE a.is_learner = 1
  AND NOT EXISTS (SELECT 1 FROM learner_profiles l WHERE l.user_id = a.user_id);
COMMIT;

SELECT COUNT(*) AS so_ho_so_nguoi_dung FROM user_profiles u JOIN seed_accounts a ON a.user_id = u.user_id;
SELECT COUNT(*) AS so_ho_so_hoc_vien FROM learner_profiles l JOIN seed_accounts a ON a.user_id = l.user_id;
DROP TEMPORARY TABLE seed_profiles;
DROP TEMPORARY TABLE seed_accounts;
