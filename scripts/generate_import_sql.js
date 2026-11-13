const fs = require('fs');

function main() {
    const v5Content = fs.readFileSync('Mentor-Matching/backend/src/main/resources/db/migration/V5__seed_demo_users.sql', 'utf8');
    const v6Content = fs.readFileSync('Mentor-Matching/backend/src/main/resources/db/migration/V6__seed_demo_learner_profiles.sql', 'utf8');
    const v8Content = fs.readFileSync('Mentor-Matching/backend/src/main/resources/db/migration/V8__seed_demo_mentor_profiles.sql', 'utf8');
    const v11Content = fs.readFileSync('Mentor-Matching/backend/src/main/resources/db/migration/V11__seed_demo_mentor_subjects.sql', 'utf8');

    // Parse V5
    const v5Match = v5Content.match(/INSERT INTO tmp_demo_users[\s\S]*?VALUES([\s\S]*?);/i);
    const userRows = [];
    if (v5Match) {
        const lines = v5Match[1].trim().split('\n').map(l => l.trim().replace(/,$/, '')).filter(Boolean);
        for (const line of lines) {
            if (line.startsWith('(')) {
                userRows.push(line.replace(/,$/, ''));
            }
        }
    }
    console.log(`Parsed ${userRows.length} users from V5.`);

    // Parse V6
    const v6Match = v6Content.match(/INSERT INTO tmp_demo_learner_profiles[\s\S]*?VALUES([\s\S]*?);/i);
    const learnerRows = [];
    if (v6Match) {
        const lines = v6Match[1].trim().split('\n').map(l => l.trim().replace(/,$/, '')).filter(Boolean);
        for (const line of lines) {
            if (line.startsWith('(')) {
                learnerRows.push(line.replace(/,$/, ''));
            }
        }
    }
    console.log(`Parsed ${learnerRows.length} learner profiles from V6.`);

    // Map email to full_name from V5
    const emailToFullName = {};
    const parsedUsers = [];
    for (const r of userRows) {
        // ('Admin Test', 'admin@test.com', '$2a$10$...', '0901100001', 'ADMIN', 'WORKING_ADULT', 'ACTIVE')
        const content = r.trim().slice(1, -1);
        const tokens = content.split("', '").map(s => s.replace(/'/g, '').trim());
        const [fullName, email, origPasswordHash, phone, role, userType, rawStatus] = tokens;
        const status = rawStatus === 'INACTIVE' ? 'DISABLED' : rawStatus;
        emailToFullName[email] = fullName;

        let uuidHex;
        if (email.startsWith('admin')) {
            const m = email.match(/\d+/);
            const num = m ? parseInt(m[0], 10) : 1;
            uuidHex = `00000000-0000-0000-0000-${String(num).padStart(12, '0')}`;
        } else if (email.startsWith('manager')) {
            const m = email.match(/\d+/);
            const num = m ? parseInt(m[0], 10) + 1 : 2;
            uuidHex = `00000000-0000-0000-0000-${String(num).padStart(12, '0')}`;
        } else if (email.startsWith('mentor')) {
            const m = email.match(/\d+/);
            const num = parseInt(m[0], 10);
            uuidHex = `00000000-0000-0000-0001-${String(num).padStart(12, '0')}`;
        } else if (email.startsWith('learner')) {
            const m = email.match(/\d+/);
            const num = parseInt(m[0], 10);
            uuidHex = `00000000-0000-0000-0002-${String(num).padStart(12, '0')}`;
        } else {
            uuidHex = `00000000-0000-0000-0009-000000000001`;
        }

        parsedUsers.push({
            uuidHex,
            fullName,
            email,
            phone,
            role,
            userType,
            status
        });
    }

    const v8Match = v8Content.match(/INSERT INTO tmp_demo_mentor_profiles[\s\S]*?VALUES([\s\S]*?);/i);
    const mentorVals = v8Match[1].trim();

    const v11Match = v11Content.match(/INSERT INTO tmp_demo_mentor_subjects[\s\S]*?VALUES([\s\S]*?);/i);
    const subjectVals = v11Match[1].trim();

    const out = [];
    out.push(`-- ==============================================================================
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
`);

    const userInsertLines = parsedUsers.map(u => 
        `('${u.fullName}', '${u.email}', '${u.phone}', '${u.role}', '${u.userType}', '${u.status}', '${u.uuidHex}')`
    );
    out.push(userInsertLines.join(',\n') + ';\n\n');

    out.push(`-- 1.1 Insert or update accounts
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
`);

    const userLinesForUserDb = parsedUsers.map(u => 
        `('${u.uuidHex}', '${u.fullName}', '${u.email}', '${u.phone}', '${u.userType}', '${u.status}')`
    );
    out.push(userLinesForUserDb.join(',\n') + ';\n\n');

    out.push(`-- 2.1 Insert/update user_profiles
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
`);

    const gradeMap = {
        'Lớp 1': 1, 'Lớp 2': 2, 'Lớp 3': 3, 'Lớp 4': 4,
        'Lớp 5': 5, 'Lớp 6': 6, 'Lớp 7': 7, 'Lớp 8': 8,
        'Lớp 9': 9, 'Lớp 10': 10, 'Lớp 11': 11, 'Lớp 12': 12
    };

    const learnerInsertLines = [];
    for (const lr of learnerRows) {
        const lrClean = lr.trim().slice(1, -1);
        const tokens = lrClean.split(', ');
        const email = tokens[0].trim().replace(/'/g, '');
        const num = parseInt(email.match(/\d+/)[0], 10);
        const gender = tokens[1];
        const birthYear = tokens[2];
        const schoolName = tokens[3];
        const gradeRaw = tokens[4].replace(/'/g, '').trim();
        const gradeId = gradeMap[gradeRaw] || parseInt(gradeRaw.replace(/\D+/g, ''), 10);
        const learningGoal = tokens.slice(5).join(', ');
        learnerInsertLines.push(`(${num}, ${gender}, ${birthYear}, ${schoolName}, ${gradeId}, ${learningGoal})`);
    }
    out.push(learnerInsertLines.join(',\n') + ';\n\n');

    out.push(`INSERT INTO learner_profiles (id, user_id, gender, birth_year, school_name, grade_id, learning_goal, created_at, updated_at, version)
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
`);

    const transformedMentorVals = mentorVals.replace(/\(\s*'([^']+@mentor-matching\.local)',\s*/g, (match, email) => {
        const num = parseInt(email.match(/\d+/)[0], 10);
        const fullName = emailToFullName[email] || `Mentor ${String(num).padStart(2, '0')}`;
        return `(${num}, '${fullName}', `;
    });

    out.push(transformedMentorVals + ';\n\n');

    out.push(`-- 3.3 Insert/update mentor_profiles
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
`);

    const transformedSubjectVals = subjectVals.replace(/\(\s*'mentor(\d+)@mentor-matching\.local',\s*/g, (match, numStr) => {
        return `(${parseInt(numStr, 10)}, `;
    });
    out.push(transformedSubjectVals + ';\n\n');

    out.push(`INSERT INTO mentor_subjects (
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
`);

    fs.writeFileSync('scripts/import_monolith_data.sql', out.join(''), 'utf8');
    console.log('Successfully written scripts/import_monolith_data.sql');
}

main();
