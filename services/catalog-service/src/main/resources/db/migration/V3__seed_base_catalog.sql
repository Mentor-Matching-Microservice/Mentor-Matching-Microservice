-- Seed base learning catalog.
-- Categories group subjects for browsing/filtering; subjects are the concrete items mentors teach.

SET NAMES utf8mb4;

INSERT INTO categories (name, description)
VALUES
    ('Toán - Tư duy', 'Các môn toán phổ thông, toán nâng cao và tư duy logic.'),
    ('Ngôn ngữ - Văn học', 'Tiếng Việt, Ngữ văn và các môn phát triển năng lực đọc viết.'),
    ('Ngoại ngữ', 'Các ngoại ngữ phổ biến và chương trình luyện chứng chỉ.'),
    ('Khoa học tự nhiên', 'Các môn Vật lý, Hóa học, Sinh học và khoa học ứng dụng.'),
    ('Khoa học xã hội', 'Các môn Lịch sử, Địa lý, giáo dục công dân và kiến thức xã hội.'),
    ('Tin học - Lập trình', 'Tin học cơ bản, tư duy máy tính và lập trình.'),
    ('Luyện thi', 'Các chương trình ôn thi chuyển cấp, tốt nghiệp, đại học và chứng chỉ.'),
    ('Kỹ năng học tập', 'Các kỹ năng bổ trợ như phương pháp học, thuyết trình và quản lý thời gian.');

INSERT INTO grades (name, level_group)
VALUES
    ('Lớp 1', 'PRIMARY'),
    ('Lớp 2', 'PRIMARY'),
    ('Lớp 3', 'PRIMARY'),
    ('Lớp 4', 'PRIMARY'),
    ('Lớp 5', 'PRIMARY'),
    ('Lớp 6', 'SECONDARY'),
    ('Lớp 7', 'SECONDARY'),
    ('Lớp 8', 'SECONDARY'),
    ('Lớp 9', 'SECONDARY'),
    ('Lớp 10', 'HIGH_SCHOOL'),
    ('Lớp 11', 'HIGH_SCHOOL'),
    ('Lớp 12', 'HIGH_SCHOOL');

CREATE TEMPORARY TABLE tmp_catalog_subjects (
    category_name VARCHAR(255) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_catalog_subjects (category_name, subject_name, description)
VALUES
    ('Toán - Tư duy', 'Toán', 'Toán phổ thông theo chương trình từ tiểu học đến trung học phổ thông.'),
    ('Toán - Tư duy', 'Toán tư duy', 'Phát triển tư duy logic, suy luận và giải quyết vấn đề.'),
    ('Toán - Tư duy', 'Đại số', 'Các chuyên đề đại số cho trung học cơ sở và trung học phổ thông.'),
    ('Toán - Tư duy', 'Hình học', 'Các chuyên đề hình học phẳng, hình học không gian và tư duy hình học.'),
    ('Toán - Tư duy', 'Xác suất - Thống kê', 'Kiến thức xác suất, thống kê và phân tích dữ liệu cơ bản.'),

    ('Ngôn ngữ - Văn học', 'Tiếng Việt', 'Tiếng Việt bậc tiểu học, đọc hiểu, chính tả và tập làm văn.'),
    ('Ngôn ngữ - Văn học', 'Ngữ văn', 'Ngữ văn trung học cơ sở và trung học phổ thông.'),
    ('Ngôn ngữ - Văn học', 'Viết sáng tạo', 'Rèn luyện diễn đạt, lập ý và viết bài có cấu trúc.'),

    ('Ngoại ngữ', 'Tiếng Anh', 'Tiếng Anh phổ thông và giao tiếp cơ bản.'),
    ('Ngoại ngữ', 'IELTS', 'Luyện thi IELTS theo bốn kỹ năng.'),
    ('Ngoại ngữ', 'TOEIC', 'Luyện thi TOEIC và tiếng Anh công việc.'),
    ('Ngoại ngữ', 'Tiếng Nhật', 'Tiếng Nhật cơ bản và luyện chứng chỉ JLPT.'),
    ('Ngoại ngữ', 'Tiếng Hàn', 'Tiếng Hàn cơ bản và luyện chứng chỉ TOPIK.'),
    ('Ngoại ngữ', 'Tiếng Trung', 'Tiếng Trung cơ bản và luyện chứng chỉ HSK.'),

    ('Khoa học tự nhiên', 'Vật lý', 'Vật lý trung học cơ sở và trung học phổ thông.'),
    ('Khoa học tự nhiên', 'Hóa học', 'Hóa học trung học cơ sở và trung học phổ thông.'),
    ('Khoa học tự nhiên', 'Sinh học', 'Sinh học trung học cơ sở và trung học phổ thông.'),
    ('Khoa học tự nhiên', 'Khoa học tự nhiên', 'Môn khoa học tự nhiên tích hợp ở bậc trung học cơ sở.'),

    ('Khoa học xã hội', 'Lịch sử', 'Lịch sử trung học cơ sở và trung học phổ thông.'),
    ('Khoa học xã hội', 'Địa lý', 'Địa lý trung học cơ sở và trung học phổ thông.'),
    ('Khoa học xã hội', 'Giáo dục công dân', 'Kiến thức công dân, pháp luật và đạo đức học đường.'),
    ('Khoa học xã hội', 'Lịch sử và Địa lý', 'Môn tích hợp Lịch sử và Địa lý ở bậc trung học cơ sở.'),

    ('Tin học - Lập trình', 'Tin học', 'Tin học phổ thông, kỹ năng máy tính và tư duy số.'),
    ('Tin học - Lập trình', 'Lập trình Scratch', 'Lập trình kéo thả và tư duy thuật toán cho học sinh nhỏ tuổi.'),
    ('Tin học - Lập trình', 'Lập trình Python', 'Lập trình Python cơ bản đến nâng cao.'),
    ('Tin học - Lập trình', 'Lập trình Web', 'HTML, CSS, JavaScript và nền tảng phát triển web.'),

    ('Luyện thi', 'Ôn thi vào lớp 10', 'Ôn tập kiến thức và luyện đề cho kỳ thi tuyển sinh lớp 10.'),
    ('Luyện thi', 'Ôn thi tốt nghiệp THPT', 'Ôn tập kiến thức và luyện đề cho kỳ thi tốt nghiệp THPT.'),
    ('Luyện thi', 'SAT', 'Luyện thi SAT và kỹ năng làm bài chuẩn hóa.'),

    ('Kỹ năng học tập', 'Phương pháp học tập', 'Kỹ năng lập kế hoạch, ghi nhớ và tự học hiệu quả.'),
    ('Kỹ năng học tập', 'Thuyết trình', 'Kỹ năng trình bày, diễn đạt và giao tiếp trước đám đông.'),
    ('Kỹ năng học tập', 'Quản lý thời gian', 'Kỹ năng tổ chức thời gian và duy trì tiến độ học tập.');

INSERT INTO subjects (category_id, name, description)
SELECT c.id, s.subject_name, s.description
FROM tmp_catalog_subjects s
JOIN categories c ON c.name = s.category_name;

DROP TEMPORARY TABLE tmp_catalog_subjects;
