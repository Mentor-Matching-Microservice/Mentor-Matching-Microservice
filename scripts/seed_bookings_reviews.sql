-- ==============================================================================
-- Seed Real Bookings and Reviews for Mentors in MSSQL
-- Databases: mm_booking, mm_review
-- ==============================================================================

USE mm_booking;
SET NOCOUNT ON;

-- Clean up existing data in mm_booking and mm_review
DELETE FROM mm_review.dbo.review_tags;
DELETE FROM mm_review.dbo.reviews;
DELETE FROM mm_booking.dbo.bookings;

SET IDENTITY_INSERT mm_booking.dbo.bookings ON;

INSERT INTO mm_booking.dbo.bookings (
    id, student_user_id, student_name, mentor_id, mentor_user_id, mentor_name,
    mentor_subject_id, subject_name, grade_name, booking_date, start_time, end_time,
    price_per_hour, total_amount, meeting_type, meeting_link, meeting_address,
    status, note, cancelled_by, cancel_reason, created_at, updated_at, hold_expires_at
) VALUES
(
    1,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    1, N'00000000-0000-0000-0000-000000000022', N'Mentor',
    1, N'Toán', N'Lớp 10',
    '2026-04-10', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-10 07:00:00', '2026-04-10 10:00:00', NULL
),
(
    2,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    1, N'00000000-0000-0000-0000-000000000022', N'Mentor',
    1, N'Toán', N'Lớp 10',
    '2026-05-12', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-12 07:00:00', '2026-05-12 10:00:00', NULL
),
(
    3,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    2, N'00000000-0000-0000-0000-000000000023', N'Mentor',
    2, N'Vật lý', N'Lớp 11',
    '2026-04-15', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-15 07:00:00', '2026-04-15 10:00:00', NULL
),
(
    4,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    2, N'00000000-0000-0000-0000-000000000023', N'Mentor',
    2, N'Vật lý', N'Lớp 11',
    '2026-05-18', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-18 07:00:00', '2026-05-18 10:00:00', NULL
),
(
    5,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    3, N'00000000-0000-0000-0000-000000000024', N'Mentor',
    3, N'Hóa học', N'Lớp 12',
    '2026-04-20', '08:00:00', '10:00:00',
    230000.00, 460000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-20 07:00:00', '2026-04-20 10:00:00', NULL
),
(
    6,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    3, N'00000000-0000-0000-0000-000000000024', N'Mentor',
    3, N'Hóa học', N'Lớp 12',
    '2026-05-22', '08:00:00', '10:00:00',
    230000.00, 460000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-22 07:00:00', '2026-05-22 10:00:00', NULL
),
(
    7,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    4, N'00000000-0000-0000-0000-000000000025', N'Mentor',
    4, N'Tiếng Anh', N'Lớp 10',
    '2026-04-25', '08:00:00', '10:00:00',
    210000.00, 420000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-25 07:00:00', '2026-04-25 10:00:00', NULL
),
(
    8,
    N'00000000-0000-0000-0002-000000000010', N'Learner Thanh Ha',
    4, N'00000000-0000-0000-0000-000000000025', N'Mentor',
    4, N'Tiếng Anh', N'Lớp 10',
    '2026-05-25', '08:00:00', '10:00:00',
    210000.00, 420000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-25 07:00:00', '2026-05-25 10:00:00', NULL
),
(
    9,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    5, N'00000000-0000-0000-0000-000000000026', N'Mentor',
    5, N'Ngữ văn', N'Lớp 9',
    '2026-04-18', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-18 07:00:00', '2026-04-18 10:00:00', NULL
),
(
    10,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    5, N'00000000-0000-0000-0000-000000000026', N'Mentor',
    5, N'Ngữ văn', N'Lớp 9',
    '2026-05-16', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-16 07:00:00', '2026-05-16 10:00:00', NULL
),
(
    11,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    6, N'00000000-0000-0000-0000-000000000027', N'Mentor',
    6, N'Sinh học', N'Lớp 11',
    '2026-04-22', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-22 07:00:00', '2026-04-22 10:00:00', NULL
),
(
    12,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    6, N'00000000-0000-0000-0000-000000000027', N'Mentor',
    6, N'Sinh học', N'Lớp 11',
    '2026-05-19', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-19 07:00:00', '2026-05-19 10:00:00', NULL
),
(
    13,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    7, N'00000000-0000-0000-0000-000000000028', N'Mentor',
    7, N'Toán', N'Lớp 12',
    '2026-04-28', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-28 07:00:00', '2026-04-28 10:00:00', NULL
),
(
    14,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    7, N'00000000-0000-0000-0000-000000000028', N'Mentor',
    7, N'Toán', N'Lớp 12',
    '2026-05-27', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-27 07:00:00', '2026-05-27 10:00:00', NULL
),
(
    15,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    8, N'00000000-0000-0000-0000-000000000029', N'Mentor',
    8, N'Lập trình Python', N'Lớp 10',
    '2026-04-12', '08:00:00', '10:00:00',
    280000.00, 560000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-12 07:00:00', '2026-04-12 10:00:00', NULL
),
(
    16,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    8, N'00000000-0000-0000-0000-000000000029', N'Mentor',
    8, N'Lập trình Python', N'Lớp 10',
    '2026-05-14', '08:00:00', '10:00:00',
    280000.00, 560000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-14 07:00:00', '2026-05-14 10:00:00', NULL
),
(
    17,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    9, N'00000000-0000-0000-0001-000000000001', N'Mentor',
    10, N'Toán', N'Lớp 9',
    '2026-04-12', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-12 07:00:00', '2026-04-12 10:00:00', NULL
),
(
    18,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    9, N'00000000-0000-0000-0001-000000000001', N'Mentor',
    10, N'Toán', N'Lớp 9',
    '2026-04-28', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-28 07:00:00', '2026-04-28 10:00:00', NULL
),
(
    19,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    9, N'00000000-0000-0000-0001-000000000001', N'Mentor',
    10, N'Toán', N'Lớp 9',
    '2026-05-15', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-15 07:00:00', '2026-05-15 10:00:00', NULL
),
(
    20,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    10, N'00000000-0000-0000-0001-000000000002', N'Mentor',
    14, N'Vật lý', N'Lớp 10',
    '2026-04-10', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-10 07:00:00', '2026-04-10 10:00:00', NULL
),
(
    21,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    10, N'00000000-0000-0000-0001-000000000002', N'Mentor',
    14, N'Vật lý', N'Lớp 10',
    '2026-05-02', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-02 07:00:00', '2026-05-02 10:00:00', NULL
),
(
    22,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    10, N'00000000-0000-0000-0001-000000000002', N'Mentor',
    14, N'Vật lý', N'Lớp 10',
    '2026-05-20', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-20 07:00:00', '2026-05-20 10:00:00', NULL
),
(
    23,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    11, N'00000000-0000-0000-0001-000000000003', N'Mentor',
    19, N'IELTS', NULL,
    '2026-04-15', '08:00:00', '10:00:00',
    350000.00, 700000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-15 07:00:00', '2026-04-15 10:00:00', NULL
),
(
    24,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    11, N'00000000-0000-0000-0001-000000000003', N'Mentor',
    19, N'IELTS', NULL,
    '2026-05-08', '08:00:00', '10:00:00',
    350000.00, 700000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-08 07:00:00', '2026-05-08 10:00:00', NULL
),
(
    25,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    11, N'00000000-0000-0000-0001-000000000003', N'Mentor',
    19, N'IELTS', NULL,
    '2026-05-25', '08:00:00', '10:00:00',
    350000.00, 700000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-25 07:00:00', '2026-05-25 10:00:00', NULL
);

INSERT INTO mm_booking.dbo.bookings (
    id, student_user_id, student_name, mentor_id, mentor_user_id, mentor_name,
    mentor_subject_id, subject_name, grade_name, booking_date, start_time, end_time,
    price_per_hour, total_amount, meeting_type, meeting_link, meeting_address,
    status, note, cancelled_by, cancel_reason, created_at, updated_at, hold_expires_at
) VALUES
(
    26,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    12, N'00000000-0000-0000-0001-000000000004', N'Mentor',
    21, N'Lập trình Python', N'Lớp 10',
    '2026-04-18', '08:00:00', '10:00:00',
    300000.00, 600000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-18 07:00:00', '2026-04-18 10:00:00', NULL
),
(
    27,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    12, N'00000000-0000-0000-0001-000000000004', N'Mentor',
    21, N'Lập trình Python', N'Lớp 10',
    '2026-05-12', '08:00:00', '10:00:00',
    300000.00, 600000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-12 07:00:00', '2026-05-12 10:00:00', NULL
),
(
    28,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    13, N'00000000-0000-0000-0001-000000000005', N'Mentor',
    23, N'Ngữ văn', N'Lớp 9',
    '2026-04-20', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-20 07:00:00', '2026-04-20 10:00:00', NULL
),
(
    29,
    N'00000000-0000-0000-0002-000000000010', N'Learner Thanh Ha',
    13, N'00000000-0000-0000-0001-000000000005', N'Mentor',
    23, N'Ngữ văn', N'Lớp 9',
    '2026-05-18', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-18 07:00:00', '2026-05-18 10:00:00', NULL
),
(
    30,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    13, N'00000000-0000-0000-0001-000000000005', N'Mentor',
    23, N'Ngữ văn', N'Lớp 9',
    '2026-06-01', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-06-01 07:00:00', '2026-06-01 10:00:00', NULL
),
(
    31,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    14, N'00000000-0000-0000-0001-000000000006', N'Mentor',
    26, N'Hóa học', N'Lớp 9',
    '2026-04-22', '08:00:00', '10:00:00',
    240000.00, 480000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-22 07:00:00', '2026-04-22 10:00:00', NULL
),
(
    32,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    14, N'00000000-0000-0000-0001-000000000006', N'Mentor',
    26, N'Hóa học', N'Lớp 9',
    '2026-05-14', '08:00:00', '10:00:00',
    240000.00, 480000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-14 07:00:00', '2026-05-14 10:00:00', NULL
),
(
    33,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    15, N'00000000-0000-0000-0001-000000000007', N'Mentor',
    10, N'Toán', N'Lớp 10',
    '2026-04-05', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-05 07:00:00', '2026-04-05 10:00:00', NULL
),
(
    34,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    15, N'00000000-0000-0000-0001-000000000007', N'Mentor',
    10, N'Toán', N'Lớp 10',
    '2026-05-06', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-06 07:00:00', '2026-05-06 10:00:00', NULL
),
(
    35,
    N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    16, N'00000000-0000-0000-0001-000000000008', N'Mentor',
    33, N'Lập trình Scratch', N'Lớp 4',
    '2026-04-14', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-14 07:00:00', '2026-04-14 10:00:00', NULL
),
(
    36,
    N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    16, N'00000000-0000-0000-0001-000000000008', N'Mentor',
    33, N'Lập trình Scratch', N'Lớp 4',
    '2026-05-16', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-16 07:00:00', '2026-05-16 10:00:00', NULL
),
(
    37,
    N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    17, N'00000000-0000-0000-0001-000000000009', N'Mentor',
    35, N'Tiếng Anh', N'Lớp 6',
    '2026-04-25', '08:00:00', '10:00:00',
    180000.00, 360000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-25 07:00:00', '2026-04-25 10:00:00', NULL
),
(
    38,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    17, N'00000000-0000-0000-0001-000000000009', N'Mentor',
    35, N'Tiếng Anh', N'Lớp 6',
    '2026-05-22', '08:00:00', '10:00:00',
    180000.00, 360000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-22 07:00:00', '2026-05-22 10:00:00', NULL
),
(
    39,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    18, N'00000000-0000-0000-0001-000000000010', N'Mentor',
    38, N'Toán', N'Lớp 10',
    '2026-04-16', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-16 07:00:00', '2026-04-16 10:00:00', NULL
),
(
    40,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    18, N'00000000-0000-0000-0001-000000000010', N'Mentor',
    38, N'Toán', N'Lớp 10',
    '2026-05-19', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-19 07:00:00', '2026-05-19 10:00:00', NULL
),
(
    41,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    19, N'00000000-0000-0000-0001-000000000011', N'Mentor',
    41, N'Lịch sử', N'Lớp 8',
    '2026-04-27', '08:00:00', '10:00:00',
    180000.00, 360000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-27 07:00:00', '2026-04-27 10:00:00', NULL
),
(
    42,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    19, N'00000000-0000-0000-0001-000000000011', N'Mentor',
    41, N'Lịch sử', N'Lớp 8',
    '2026-05-24', '08:00:00', '10:00:00',
    180000.00, 360000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-24 07:00:00', '2026-05-24 10:00:00', NULL
),
(
    43,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    20, N'00000000-0000-0000-0001-000000000012', N'Mentor',
    44, N'Lập trình Web', N'Lớp 10',
    '2026-04-11', '08:00:00', '10:00:00',
    300000.00, 600000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-11 07:00:00', '2026-04-11 10:00:00', NULL
),
(
    44,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    20, N'00000000-0000-0000-0001-000000000012', N'Mentor',
    44, N'Lập trình Web', N'Lớp 10',
    '2026-05-09', '08:00:00', '10:00:00',
    300000.00, 600000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-09 07:00:00', '2026-05-09 10:00:00', NULL
),
(
    45,
    N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    21, N'00000000-0000-0000-0001-000000000013', N'Mentor',
    47, N'Tiếng Việt', N'Lớp 2',
    '2026-04-19', '08:00:00', '10:00:00',
    170000.00, 340000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-19 07:00:00', '2026-04-19 10:00:00', NULL
),
(
    46,
    N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    21, N'00000000-0000-0000-0001-000000000013', N'Mentor',
    47, N'Tiếng Việt', N'Lớp 2',
    '2026-05-21', '08:00:00', '10:00:00',
    170000.00, 340000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-21 07:00:00', '2026-05-21 10:00:00', NULL
),
(
    47,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    22, N'00000000-0000-0000-0001-000000000014', N'Mentor',
    50, N'SAT', NULL,
    '2026-04-14', '08:00:00', '10:00:00',
    420000.00, 840000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-14 07:00:00', '2026-04-14 10:00:00', NULL
),
(
    48,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    22, N'00000000-0000-0000-0001-000000000014', N'Mentor',
    50, N'SAT', NULL,
    '2026-05-18', '08:00:00', '10:00:00',
    420000.00, 840000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-18 07:00:00', '2026-05-18 10:00:00', NULL
),
(
    49,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    23, N'00000000-0000-0000-0001-000000000015', N'Mentor',
    54, N'Sinh học', N'Lớp 11',
    '2026-04-29', '08:00:00', '10:00:00',
    270000.00, 540000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-29 07:00:00', '2026-04-29 10:00:00', NULL
),
(
    50,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    23, N'00000000-0000-0000-0001-000000000015', N'Mentor',
    54, N'Sinh học', N'Lớp 11',
    '2026-05-26', '08:00:00', '10:00:00',
    270000.00, 540000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-26 07:00:00', '2026-05-26 10:00:00', NULL
);

INSERT INTO mm_booking.dbo.bookings (
    id, student_user_id, student_name, mentor_id, mentor_user_id, mentor_name,
    mentor_subject_id, subject_name, grade_name, booking_date, start_time, end_time,
    price_per_hour, total_amount, meeting_type, meeting_link, meeting_address,
    status, note, cancelled_by, cancel_reason, created_at, updated_at, hold_expires_at
) VALUES
(
    51,
    N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    24, N'00000000-0000-0000-0001-000000000016', N'Mentor',
    58, N'Toán tư duy', N'Lớp 4',
    '2026-04-17', '08:00:00', '10:00:00',
    210000.00, 420000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-17 07:00:00', '2026-04-17 10:00:00', NULL
),
(
    52,
    N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    24, N'00000000-0000-0000-0001-000000000016', N'Mentor',
    58, N'Toán tư duy', N'Lớp 4',
    '2026-05-13', '08:00:00', '10:00:00',
    210000.00, 420000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-13 07:00:00', '2026-05-13 10:00:00', NULL
),
(
    53,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    25, N'00000000-0000-0000-0001-000000000017', N'Mentor',
    61, N'IELTS', NULL,
    '2026-04-21', '08:00:00', '10:00:00',
    340000.00, 680000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-21 07:00:00', '2026-04-21 10:00:00', NULL
),
(
    54,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    25, N'00000000-0000-0000-0001-000000000017', N'Mentor',
    61, N'IELTS', NULL,
    '2026-05-17', '08:00:00', '10:00:00',
    340000.00, 680000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-17 07:00:00', '2026-05-17 10:00:00', NULL
),
(
    55,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    26, N'00000000-0000-0000-0001-000000000018', N'Mentor',
    26, N'Hóa học', N'Lớp 9',
    '2026-04-12', '08:00:00', '10:00:00',
    240000.00, 480000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-12 07:00:00', '2026-04-12 10:00:00', NULL
),
(
    56,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    26, N'00000000-0000-0000-0001-000000000018', N'Mentor',
    26, N'Hóa học', N'Lớp 9',
    '2026-05-11', '08:00:00', '10:00:00',
    240000.00, 480000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-11 07:00:00', '2026-05-11 10:00:00', NULL
),
(
    57,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    27, N'00000000-0000-0000-0001-000000000019', N'Mentor',
    66, N'Địa lý', N'Lớp 12',
    '2026-04-26', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-26 07:00:00', '2026-04-26 10:00:00', NULL
),
(
    58,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    27, N'00000000-0000-0000-0001-000000000019', N'Mentor',
    66, N'Địa lý', N'Lớp 12',
    '2026-05-23', '08:00:00', '10:00:00',
    250000.00, 500000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-23 07:00:00', '2026-05-23 10:00:00', NULL
),
(
    59,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    28, N'00000000-0000-0000-0001-000000000020', N'Mentor',
    68, N'IELTS', NULL,
    '2026-04-15', '08:00:00', '10:00:00',
    380000.00, 760000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-15 07:00:00', '2026-04-15 10:00:00', NULL
),
(
    60,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    28, N'00000000-0000-0000-0001-000000000020', N'Mentor',
    68, N'IELTS', NULL,
    '2026-05-20', '08:00:00', '10:00:00',
    380000.00, 760000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-20 07:00:00', '2026-05-20 10:00:00', NULL
),
(
    61,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    29, N'00000000-0000-0000-0001-000000000021', N'Mentor',
    71, N'Toán', N'Lớp 6',
    '2026-04-18', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-18 07:00:00', '2026-04-18 10:00:00', NULL
),
(
    62,
    N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    29, N'00000000-0000-0000-0001-000000000021', N'Mentor',
    71, N'Toán', N'Lớp 6',
    '2026-05-15', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-15 07:00:00', '2026-05-15 10:00:00', NULL
),
(
    63,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    30, N'00000000-0000-0000-0001-000000000022', N'Mentor',
    74, N'Khoa học tự nhiên', N'Lớp 6',
    '2026-04-23', '08:00:00', '10:00:00',
    190000.00, 380000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-23 07:00:00', '2026-04-23 10:00:00', NULL
),
(
    64,
    N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    30, N'00000000-0000-0000-0001-000000000022', N'Mentor',
    74, N'Khoa học tự nhiên', N'Lớp 6',
    '2026-05-19', '08:00:00', '10:00:00',
    190000.00, 380000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-19 07:00:00', '2026-05-19 10:00:00', NULL
),
(
    65,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    31, N'00000000-0000-0000-0001-000000000023', N'Mentor',
    77, N'Tiếng Trung', NULL,
    '2026-04-10', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-10 07:00:00', '2026-04-10 10:00:00', NULL
),
(
    66,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    31, N'00000000-0000-0000-0001-000000000023', N'Mentor',
    77, N'Tiếng Trung', NULL,
    '2026-05-08', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-08 07:00:00', '2026-05-08 10:00:00', NULL
),
(
    67,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    32, N'00000000-0000-0000-0001-000000000024', N'Mentor',
    79, N'Phương pháp học tập', NULL,
    '2026-04-24', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-24 07:00:00', '2026-04-24 10:00:00', NULL
),
(
    68,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    32, N'00000000-0000-0000-0001-000000000024', N'Mentor',
    79, N'Phương pháp học tập', NULL,
    '2026-05-22', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-22 07:00:00', '2026-05-22 10:00:00', NULL
),
(
    69,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    33, N'00000000-0000-0000-0001-000000000025', N'Mentor',
    82, N'Tiếng Nhật', NULL,
    '2026-04-16', '08:00:00', '10:00:00',
    280000.00, 560000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-16 07:00:00', '2026-04-16 10:00:00', NULL
),
(
    70,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    33, N'00000000-0000-0000-0001-000000000025', N'Mentor',
    82, N'Tiếng Nhật', NULL,
    '2026-05-17', '08:00:00', '10:00:00',
    280000.00, 560000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-17 07:00:00', '2026-05-17 10:00:00', NULL
),
(
    71,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    34, N'00000000-0000-0000-0001-000000000026', N'Mentor',
    86, N'Toán', N'Lớp 12',
    '2026-04-20', '08:00:00', '10:00:00',
    350000.00, 700000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-20 07:00:00', '2026-04-20 10:00:00', NULL
),
(
    72,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    34, N'00000000-0000-0000-0001-000000000026', N'Mentor',
    86, N'Toán', N'Lớp 12',
    '2026-05-25', '08:00:00', '10:00:00',
    350000.00, 700000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-25 07:00:00', '2026-05-25 10:00:00', NULL
),
(
    73,
    N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    35, N'00000000-0000-0000-0001-000000000027', N'Mentor',
    87, N'Toán', N'Lớp 5',
    '2026-04-13', '08:00:00', '10:00:00',
    190000.00, 380000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-13 07:00:00', '2026-04-13 10:00:00', NULL
),
(
    74,
    N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    35, N'00000000-0000-0000-0001-000000000027', N'Mentor',
    87, N'Toán', N'Lớp 5',
    '2026-05-14', '08:00:00', '10:00:00',
    190000.00, 380000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-14 07:00:00', '2026-05-14 10:00:00', NULL
),
(
    75,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    36, N'00000000-0000-0000-0001-000000000028', N'Mentor',
    91, N'Lập trình Python', N'Lớp 11',
    '2026-04-22', '08:00:00', '10:00:00',
    330000.00, 660000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-22 07:00:00', '2026-04-22 10:00:00', NULL
);

INSERT INTO mm_booking.dbo.bookings (
    id, student_user_id, student_name, mentor_id, mentor_user_id, mentor_name,
    mentor_subject_id, subject_name, grade_name, booking_date, start_time, end_time,
    price_per_hour, total_amount, meeting_type, meeting_link, meeting_address,
    status, note, cancelled_by, cancel_reason, created_at, updated_at, hold_expires_at
) VALUES
(
    76,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    36, N'00000000-0000-0000-0001-000000000028', N'Mentor',
    91, N'Lập trình Python', N'Lớp 11',
    '2026-05-21', '08:00:00', '10:00:00',
    330000.00, 660000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-21 07:00:00', '2026-05-21 10:00:00', NULL
),
(
    77,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    37, N'00000000-0000-0000-0001-000000000029', N'Mentor',
    93, N'Thuyết trình', NULL,
    '2026-04-18', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-18 07:00:00', '2026-04-18 10:00:00', NULL
),
(
    78,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    37, N'00000000-0000-0000-0001-000000000029', N'Mentor',
    93, N'Thuyết trình', NULL,
    '2026-05-19', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-19 07:00:00', '2026-05-19 10:00:00', NULL
),
(
    79,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    38, N'00000000-0000-0000-0001-000000000030', N'Mentor',
    96, N'Hóa học', N'Lớp 12',
    '2026-04-25', '08:00:00', '10:00:00',
    340000.00, 680000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-25 07:00:00', '2026-04-25 10:00:00', NULL
),
(
    80,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    38, N'00000000-0000-0000-0001-000000000030', N'Mentor',
    96, N'Hóa học', N'Lớp 12',
    '2026-05-27', '08:00:00', '10:00:00',
    340000.00, 680000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-27 07:00:00', '2026-05-27 10:00:00', NULL
),
(
    81,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    39, N'00000000-0000-0000-0001-000000000031', N'Mentor',
    99, N'Ngữ văn', N'Lớp 12',
    '2026-04-17', '08:00:00', '10:00:00',
    300000.00, 600000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-17 07:00:00', '2026-04-17 10:00:00', NULL
),
(
    82,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    39, N'00000000-0000-0000-0001-000000000031', N'Mentor',
    99, N'Ngữ văn', N'Lớp 12',
    '2026-05-20', '08:00:00', '10:00:00',
    300000.00, 600000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-20 07:00:00', '2026-05-20 10:00:00', NULL
),
(
    83,
    N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    40, N'00000000-0000-0000-0001-000000000032', N'Mentor',
    102, N'Tin học', N'Lớp 7',
    '2026-04-21', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-21 07:00:00', '2026-04-21 10:00:00', NULL
),
(
    84,
    N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    40, N'00000000-0000-0000-0001-000000000032', N'Mentor',
    102, N'Tin học', N'Lớp 7',
    '2026-05-23', '08:00:00', '10:00:00',
    200000.00, 400000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-23 07:00:00', '2026-05-23 10:00:00', NULL
),
(
    85,
    N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    41, N'00000000-0000-0000-0001-000000000033', N'Mentor',
    105, N'Tiếng Hàn', NULL,
    '2026-04-11', '08:00:00', '10:00:00',
    280000.00, 560000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-11 07:00:00', '2026-04-11 10:00:00', NULL
),
(
    86,
    N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    41, N'00000000-0000-0000-0001-000000000033', N'Mentor',
    105, N'Tiếng Hàn', NULL,
    '2026-05-16', '08:00:00', '10:00:00',
    280000.00, 560000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-16 07:00:00', '2026-05-16 10:00:00', NULL
),
(
    87,
    N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    42, N'00000000-0000-0000-0001-000000000034', N'Mentor',
    107, N'Toán', N'Lớp 2',
    '2026-04-19', '08:00:00', '10:00:00',
    170000.00, 340000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-19 07:00:00', '2026-04-19 10:00:00', NULL
),
(
    88,
    N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    42, N'00000000-0000-0000-0001-000000000034', N'Mentor',
    107, N'Toán', N'Lớp 2',
    '2026-05-22', '08:00:00', '10:00:00',
    170000.00, 340000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-22 07:00:00', '2026-05-22 10:00:00', NULL
),
(
    89,
    N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    43, N'00000000-0000-0000-0001-000000000035', N'Mentor',
    111, N'Toán', N'Lớp 9',
    '2026-04-14', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-14 07:00:00', '2026-04-14 10:00:00', NULL
),
(
    90,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    43, N'00000000-0000-0000-0001-000000000035', N'Mentor',
    111, N'Toán', N'Lớp 9',
    '2026-05-18', '08:00:00', '10:00:00',
    260000.00, 520000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-18 07:00:00', '2026-05-18 10:00:00', NULL
),
(
    91,
    N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    44, N'00000000-0000-0000-0001-000000000036', N'Mentor',
    114, N'Tiếng Anh', N'Lớp 6',
    '2026-04-20', '08:00:00', '10:00:00',
    190000.00, 380000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-20 07:00:00', '2026-04-20 10:00:00', NULL
),
(
    92,
    N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    44, N'00000000-0000-0000-0001-000000000036', N'Mentor',
    114, N'Tiếng Anh', N'Lớp 6',
    '2026-05-24', '08:00:00', '10:00:00',
    190000.00, 380000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-24 07:00:00', '2026-05-24 10:00:00', NULL
),
(
    93,
    N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    45, N'00000000-0000-0000-0001-000000000037', N'Mentor',
    117, N'Toán tư duy', N'Lớp 3',
    '2026-04-26', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-04-26 07:00:00', '2026-04-26 10:00:00', NULL
),
(
    94,
    N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    45, N'00000000-0000-0000-0001-000000000037', N'Mentor',
    117, N'Toán tư duy', N'Lớp 3',
    '2026-05-28', '08:00:00', '10:00:00',
    220000.00, 440000.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '2026-05-28 07:00:00', '2026-05-28 10:00:00', NULL
);

SET IDENTITY_INSERT mm_booking.dbo.bookings OFF;

USE mm_review;

SET IDENTITY_INSERT mm_review.dbo.reviews ON;

INSERT INTO mm_review.dbo.reviews (
    id, booking_id, student_user_id, student_name, mentor_id, rating, comment, created_at, updated_at
) VALUES
(
    1, 1, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    1, 5, N'Thầy An dạy rất nhiệt tình và bài bản, giúp em nắm vững kiến thức nền tảng.',
    '2026-04-10 11:00:00 +00:00', '2026-04-10 11:00:00 +00:00'
),
(
    2, 2, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    1, 5, N'Thầy đúng giờ và phương pháp giảng dạy dễ hiểu.',
    '2026-05-12 11:00:00 +00:00', '2026-05-12 11:00:00 +00:00'
),
(
    3, 3, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    2, 5, N'Thầy Bình giải thích phần mạch điện xoay chiều rất sáng rõ và dễ hiểu.',
    '2026-04-15 11:00:00 +00:00', '2026-04-15 11:00:00 +00:00'
),
(
    4, 4, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    2, 5, N'Nhiều bài tập áp dụng thực tế rất hữu ích.',
    '2026-05-18 11:00:00 +00:00', '2026-05-18 11:00:00 +00:00'
),
(
    5, 5, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    3, 5, N'Cô Chi dạy phần Hóa hữu cơ rất logic, giúp em làm trắc nghiệm nhanh hơn.',
    '2026-04-20 11:00:00 +00:00', '2026-04-20 11:00:00 +00:00'
),
(
    6, 6, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    3, 5, N'Cô giảng bài nhiệt tình, chuẩn bị tài liệu rất công phu.',
    '2026-05-22 11:00:00 +00:00', '2026-05-22 11:00:00 +00:00'
),
(
    7, 7, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    4, 5, N'Thầy Dũng phát âm rất chuẩn, buổi học có nhiều hoạt động tương tác.',
    '2026-04-25 11:00:00 +00:00', '2026-04-25 11:00:00 +00:00'
),
(
    8, 8, N'00000000-0000-0000-0002-000000000010', N'Learner Thanh Ha',
    4, 4, N'Thầy đúng giờ và bài tập về nhà rất vừa sức.',
    '2026-05-25 11:00:00 +00:00', '2026-05-25 11:00:00 +00:00'
),
(
    9, 9, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    5, 5, N'Cô Hà dạy văn rất cảm xúc và giúp em cải thiện kỹ năng diễn đạt.',
    '2026-04-18 11:00:00 +00:00', '2026-04-18 11:00:00 +00:00'
),
(
    10, 10, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    5, 5, N'Dàn ý tác phẩm của cô rất chi tiết, dễ nhớ khi đi thi.',
    '2026-05-16 11:00:00 +00:00', '2026-05-16 11:00:00 +00:00'
),
(
    11, 11, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    6, 5, N'Thầy Khang giảng giải cơ chế di truyền phân tử rất dễ hiểu.',
    '2026-04-22 11:00:00 +00:00', '2026-04-22 11:00:00 +00:00'
),
(
    12, 12, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    6, 5, N'Tài liệu hình ảnh sinh học trực quan và sinh động.',
    '2026-05-19 11:00:00 +00:00', '2026-05-19 11:00:00 +00:00'
),
(
    13, 13, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    7, 5, N'Cô Linh có nhiều phương pháp giải nhanh trắc nghiệm rất hay.',
    '2026-04-28 11:00:00 +00:00', '2026-04-28 11:00:00 +00:00'
),
(
    14, 14, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    7, 5, N'Cô chữa bài tập đề thi thử rất cẩn thận từng câu một.',
    '2026-05-27 11:00:00 +00:00', '2026-05-27 11:00:00 +00:00'
),
(
    15, 15, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    8, 5, N'Thầy Nam dạy lập trình rất thực chiến và nhiệt tình giải đáp thắc mắc.',
    '2026-04-12 11:00:00 +00:00', '2026-04-12 11:00:00 +00:00'
),
(
    16, 16, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    8, 5, N'Các bài tập dự án nhỏ giúp em tự làm được sản phẩm chạy được.',
    '2026-05-14 11:00:00 +00:00', '2026-05-14 11:00:00 +00:00'
),
(
    17, 17, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    9, 5, N'Cô dạy rất kiên nhẫn và tận tình. Em từng mất gốc hình học lớp 9 mà sau 5 buổi đã tự tin làm được bài thi thử tuyển sinh. Cô chữa bài tập về nhà rất kỹ.',
    '2026-04-12 11:00:00 +00:00', '2026-04-12 11:00:00 +00:00'
),
(
    18, 18, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    9, 5, N'Phương pháp giải đại số của cô Minh Anh rất mạch lạc, dễ nhớ. Cô luôn đúng giờ và chuẩn bị phiếu bài tập phân dạng rất khoa học.',
    '2026-04-28 11:00:00 +00:00', '2026-04-28 11:00:00 +00:00'
),
(
    19, 19, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    9, 4, N'Buổi học vui vẻ, giải thích bài dễ hiểu. Cô cho nhiều bài tập tự luyện sát với đề thi vào 10 của Sở.',
    '2026-05-15 11:00:00 +00:00', '2026-05-15 11:00:00 +00:00'
),
(
    20, 20, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    10, 5, N'Thầy Huy giảng phần động lực học chất điểm cực kỳ trực quan, liên hệ nhiều hiện tượng thực tế nên em hiểu bản chất rất nhanh.',
    '2026-04-10 11:00:00 +00:00', '2026-04-10 11:00:00 +00:00'
),
(
    21, 21, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    10, 5, N'Thầy dạy nhiệt tình, hay giải đáp thắc mắc bài tập trên lớp ngoài giờ học. Tài liệu tóm tắt công thức của thầy rất ngắn gọn và dễ tra cứu.',
    '2026-05-02 11:00:00 +00:00', '2026-05-02 11:00:00 +00:00'
),
(
    22, 22, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    10, 5, N'Chuyên môn rất vững, cách thầy phân tích hiện tượng vật lý giúp em tư duy giải bài tập trắc nghiệm nhanh hơn hẳn.',
    '2026-05-20 11:00:00 +00:00', '2026-05-20 11:00:00 +00:00'
),
(
    23, 23, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    11, 5, N'Cô Mai sửa bài Writing Task 2 siêu chi tiết, chỉ ra từng lỗi dùng từ chưa tự nhiên và cách phát triển ý theo chuẩn band 7.0+. Rất đáng tiền!',
    '2026-04-15 11:00:00 +00:00', '2026-04-15 11:00:00 +00:00'
),
(
    24, 24, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    11, 5, N'Phần Speaking cô luyện phản xạ rất tốt, giúp mình tự tin hơn hẳn khi trả lời câu hỏi Part 2 và Part 3.',
    '2026-05-08 11:00:00 +00:00', '2026-05-08 11:00:00 +00:00'
),
(
    25, 25, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    11, 4, N'Cô phát âm chuẩn, nhiệt tình hướng dẫn các mẹo làm bài Listening và Reading. Nhịp học hơi nhanh một chút nhưng rất hiệu quả.',
    '2026-05-25 11:00:00 +00:00', '2026-05-25 11:00:00 +00:00'
);

INSERT INTO mm_review.dbo.reviews (
    id, booking_id, student_user_id, student_name, mentor_id, rating, comment, created_at, updated_at
) VALUES
(
    26, 26, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    12, 5, N'Anh Tùng dạy code rất cuốn, từ cú pháp cơ bản đến tự build project game nhỏ. Khi gặp bug anh hướng dẫn cách đọc traceback và debug rất bài bản.',
    '2026-04-18 11:00:00 +00:00', '2026-04-18 11:00:00 +00:00'
),
(
    27, 27, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    12, 5, N'Tài liệu thực hành phong phú, có repo GitHub để theo dõi code sau mỗi buổi. Anh giảng chậm rãi và trả lời mọi thắc mắc.',
    '2026-05-12 11:00:00 +00:00', '2026-05-12 11:00:00 +00:00'
),
(
    28, 28, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    13, 5, N'Cô Vy giúp em không còn sợ viết văn nghị luận xã hội nữa. Cô hướng dẫn cách lập dàn ý logic, dùng dẫn chứng thời sự sắc bén.',
    '2026-04-20 11:00:00 +00:00', '2026-04-20 11:00:00 +00:00'
),
(
    29, 29, N'00000000-0000-0000-0002-000000000010', N'Learner Thanh Ha',
    13, 5, N'Lời văn của cô rất truyền cảm, bài giảng phân tích tác phẩm sâu sắc mà không hề bị sáo rỗng hay học vẹt.',
    '2026-05-18 11:00:00 +00:00', '2026-05-18 11:00:00 +00:00'
),
(
    30, 30, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    13, 5, N'Cô sửa bài văn rất kỹ từng câu chữ và lỗi diễn đạt. Điểm thi thử môn Văn của em đã tăng từ 6.5 lên 8.0.',
    '2026-06-01 11:00:00 +00:00', '2026-06-01 11:00:00 +00:00'
),
(
    31, 31, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    14, 5, N'Cô Yến dạy Hóa bằng sơ đồ tư duy chuỗi phản ứng rất dễ thuộc. Phần bài tập tính toán nồng độ và khối lượng cô chỉ mẹo giải cực nhanh.',
    '2026-04-22 11:00:00 +00:00', '2026-04-22 11:00:00 +00:00'
),
(
    32, 32, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    14, 4, N'Cô rất nhiệt tình và hiền, không hiểu chỗ nào cô giảng lại ngay. Bài tập sau buổi học vừa sức và sát kiến thức trên lớp.',
    '2026-05-14 11:00:00 +00:00', '2026-05-14 11:00:00 +00:00'
),
(
    33, 33, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    15, 5, N'Thầy Trí có tư duy giải toán rất độc đáo, chuyên trị các bài bất đẳng thức và hình học giải tích nâng cao.',
    '2026-04-05 11:00:00 +00:00', '2026-04-05 11:00:00 +00:00'
),
(
    34, 34, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    15, 4, N'Bài giảng chất lượng cao dành cho học sinh khá giỏi. Thầy yêu cầu tư duy độc lập trước khi đưa ra gợi ý.',
    '2026-05-06 11:00:00 +00:00', '2026-05-06 11:00:00 +00:00'
),
(
    35, 35, N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    16, 5, N'Phụ huynh đánh giá: Thầy Bảo dạy bé 9 tuổi nhà mình rất kiên nhẫn. Bé rất hào hứng chờ đến giờ học để tự làm phim hoạt hình và game mê cung trên Scratch.',
    '2026-04-14 11:00:00 +00:00', '2026-04-14 11:00:00 +00:00'
),
(
    36, 36, N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    16, 5, N'Em học thầy Bảo làm được 3 mini game rồi. Thầy chỉ dẫn vui tính và dễ hiểu lắm ạ.',
    '2026-05-16 11:00:00 +00:00', '2026-05-16 11:00:00 +00:00'
),
(
    37, 37, N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    17, 5, N'Cô Linh rất thân thiện, giọng phát âm chuẩn. Em từ một đứa ngại nói tiếng Anh giờ đã dám tự tin nói chuyện cả buổi.',
    '2026-04-25 11:00:00 +00:00', '2026-04-25 11:00:00 +00:00'
),
(
    38, 38, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    17, 5, N'Giáo trình sinh động, nhiều trò chơi tương tác từ vựng giúp em nhớ từ mới rất lâu.',
    '2026-05-22 11:00:00 +00:00', '2026-05-22 11:00:00 +00:00'
),
(
    39, 39, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    18, 5, N'Thầy Minh có phong cách dạy kỷ luật và rõ ràng. Bài test 10 phút đầu giờ giúp em không bị quên kiến thức cũ.',
    '2026-04-16 11:00:00 +00:00', '2026-04-16 11:00:00 +00:00'
),
(
    40, 40, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    18, 4, N'Thầy giảng chắc lý thuyết rồi mới cho áp dụng bài tập. Điểm kiểm tra 1 tiết của em đạt 8.5.',
    '2026-05-19 11:00:00 +00:00', '2026-05-19 11:00:00 +00:00'
),
(
    41, 41, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    19, 5, N'Cô Nhi kể chuyện lịch sử hấp dẫn như xem phim tài liệu, không hề có cảm giác khô khan hay phải học vẹt mốc thời gian.',
    '2026-04-27 11:00:00 +00:00', '2026-04-27 11:00:00 +00:00'
),
(
    42, 42, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    19, 5, N'Sơ đồ timeline sự kiện cô gửi sau buổi học cực kỳ trực quan và dễ nhớ.',
    '2026-05-24 11:00:00 +00:00', '2026-05-24 11:00:00 +00:00'
),
(
    43, 43, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    20, 5, N'Anh Duy Anh dạy HTML, CSS và JavaScript thực chiến. Học 1 tháng là tự làm được trang portfolio cá nhân.',
    '2026-04-11 11:00:00 +00:00', '2026-04-11 11:00:00 +00:00'
),
(
    44, 44, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    20, 5, N'Hướng dẫn responsive và animation rất kỹ. Mentor giải đáp nhiệt tình kể cả ngoài giờ học.',
    '2026-05-09 11:00:00 +00:00', '2026-05-09 11:00:00 +00:00'
),
(
    45, 45, N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    21, 5, N'Phụ huynh nhận xét: Cô Hân rèn chữ và chính tả cho con rất kiên trì. Con tiến bộ rõ rệt sau 2 tháng học.',
    '2026-04-19 11:00:00 +00:00', '2026-04-19 11:00:00 +00:00'
),
(
    46, 46, N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    21, 5, N'Cô dạy nhẹ nhàng, con thích học đọc truyện và viết văn miêu tả cùng cô.',
    '2026-05-21 11:00:00 +00:00', '2026-05-21 11:00:00 +00:00'
),
(
    47, 47, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    22, 5, N'Thầy Quân dạy chiến thuật làm bài SAT Math cực đỉnh, mẹo dùng máy tính Desmos giúp tiết kiệm một nửa thời gian làm bài.',
    '2026-04-14 11:00:00 +00:00', '2026-04-14 11:00:00 +00:00'
),
(
    48, 48, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    22, 5, N'Điểm SAT Math của mình tăng từ 680 lên 770 nhờ lộ trình luyện đề chuyên sâu của thầy.',
    '2026-05-18 11:00:00 +00:00', '2026-05-18 11:00:00 +00:00'
),
(
    49, 49, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    23, 5, N'Chị Châu là sinh viên Y khoa nên kiến thức sinh học di truyền và sinh thái nắm siêu chắc. Cách chị liên hệ bệnh học thực tế làm bài học rất sinh động.',
    '2026-04-29 11:00:00 +00:00', '2026-04-29 11:00:00 +00:00'
),
(
    50, 50, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    23, 5, N'Tài liệu tóm tắt chu trình sinh học rất đẹp và dễ hiểu, ôn thi học kỳ cực kỳ nhàn.',
    '2026-05-26 11:00:00 +00:00', '2026-05-26 11:00:00 +00:00'
);

INSERT INTO mm_review.dbo.reviews (
    id, booking_id, student_user_id, student_name, mentor_id, rating, comment, created_at, updated_at
) VALUES
(
    51, 51, N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    24, 5, N'Thầy Nam dạy toán tư duy kích thích sự sáng tạo của con, không ép giải theo khuôn mẫu. Rất hài lòng!',
    '2026-04-17 11:00:00 +00:00', '2026-04-17 11:00:00 +00:00'
),
(
    52, 52, N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    24, 5, N'Thầy đúng giờ, giọng nói ấm áp và luôn động viên học sinh khi gặp bài toán khó.',
    '2026-05-13 11:00:00 +00:00', '2026-05-13 11:00:00 +00:00'
),
(
    53, 53, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    25, 5, N'Cô Thảo Nguyên có phát âm cực hay và phương pháp dạy IELTS rất hệ thống. Bộ đề forecast cô cung cấp trúng nhiều chủ đề trong kỳ thi thật.',
    '2026-04-21 11:00:00 +00:00', '2026-04-21 11:00:00 +00:00'
),
(
    54, 54, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    25, 5, N'Luyện speaking với cô giúp mình bớt ngắc ngứ và phản xạ trôi chảy hơn rất nhiều.',
    '2026-05-17 11:00:00 +00:00', '2026-05-17 11:00:00 +00:00'
),
(
    55, 55, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    26, 5, N'Thầy Lam có phòng thí nghiệm thực tế nên giải thích hiện tượng hóa học rất thuyết phục.',
    '2026-04-12 11:00:00 +00:00', '2026-04-12 11:00:00 +00:00'
),
(
    56, 56, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    26, 4, N'Bài giảng chuyên sâu, phù hợp với học sinh định hướng thi chuyên Hóa.',
    '2026-05-11 11:00:00 +00:00', '2026-05-11 11:00:00 +00:00'
),
(
    57, 57, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    27, 5, N'Thầy Đăng dạy đọc Atlat Địa lý siêu nhanh và chuẩn xác. Nhờ thầy mà phần trắc nghiệm Atlat em không sai câu nào.',
    '2026-04-26 11:00:00 +00:00', '2026-04-26 11:00:00 +00:00'
),
(
    58, 58, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    27, 5, N'Thầy nhiệt tình và có nguồn tài liệu số liệu kinh tế - xã hội cập nhật mới nhất.',
    '2026-05-23 11:00:00 +00:00', '2026-05-23 11:00:00 +00:00'
),
(
    59, 59, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    28, 5, N'Chị Châu chấm và sửa bài IELTS Writing cực kỳ nghiêm túc, feedback chi tiết từng tiêu chí Lexical Resource và Coherence.',
    '2026-04-15 11:00:00 +00:00', '2026-04-15 11:00:00 +00:00'
),
(
    60, 60, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    28, 5, N'Sau khóa học 2 tháng mình đã tự tin đi thi và đạt overall 7.5 IELTS. Cảm ơn chị Châu rất nhiều!',
    '2026-05-20 11:00:00 +00:00', '2026-05-20 11:00:00 +00:00'
),
(
    61, 61, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    29, 5, N'Cô Gia Hân dạy toán lớp 6 rất dễ hiểu, giúp em bỡ ngỡ bước đầu chuyển từ cấp 1 lên cấp 2.',
    '2026-04-18 11:00:00 +00:00', '2026-04-18 11:00:00 +00:00'
),
(
    62, 62, N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    29, 5, N'Cô chuẩn bị giáo án chu đáo, luôn kiểm tra xem học sinh đã hiểu bài chưa trước khi chuyển dạng mới.',
    '2026-05-15 11:00:00 +00:00', '2026-05-15 11:00:00 +00:00'
),
(
    63, 63, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    30, 5, N'Thầy Phong dạy môn KHTN tích hợp Vật lý, Hóa học và Sinh học rất hài hòa và thú vị.',
    '2026-04-23 11:00:00 +00:00', '2026-04-23 11:00:00 +00:00'
),
(
    64, 64, N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    30, 5, N'Thầy hiền lành, giải thích cặn kẽ các hiện tượng tự nhiên quanh đời sống.',
    '2026-05-19 11:00:00 +00:00', '2026-05-19 11:00:00 +00:00'
),
(
    65, 65, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    31, 5, N'Cô Tuệ Lâm phát âm chuẩn Bắc Kinh, chỉnh thanh điệu cho người mới bắt đầu rất kỹ lưỡng.',
    '2026-04-10 11:00:00 +00:00', '2026-04-10 11:00:00 +00:00'
),
(
    66, 66, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    31, 5, N'Học tiếng Trung với cô rất vui, cô chia sẻ thêm nhiều văn hóa và âm nhạc Trung Quốc.',
    '2026-05-08 11:00:00 +00:00', '2026-05-08 11:00:00 +00:00'
),
(
    67, 67, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    32, 5, N'Chị Ngọc chia sẻ phương pháp ghi chép Cornell và kỹ thuật ghi nhớ Spaced Repetition thay đổi hoàn toàn cách tự học của mình.',
    '2026-04-24 11:00:00 +00:00', '2026-04-24 11:00:00 +00:00'
),
(
    68, 68, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    32, 5, N'Kỹ năng thuyết trình của mình cải thiện vượt bậc, biết cách kiểm soát ngôn ngữ cơ thể và thiết kế slide ấn tượng.',
    '2026-05-22 11:00:00 +00:00', '2026-05-22 11:00:00 +00:00'
),
(
    69, 69, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    33, 5, N'Cô Quỳnh Anh dạy bảng chữ cái và ngữ pháp N5 rất chắc. Phương pháp nhớ Kanji bằng hình ảnh của cô rất thú vị.',
    '2026-04-16 11:00:00 +00:00', '2026-04-16 11:00:00 +00:00'
),
(
    70, 70, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    33, 4, N'Cô chuẩn bị nhiều tài liệu luyện nghe thực tế từ anime và tin tức NHK.',
    '2026-05-17 11:00:00 +00:00', '2026-05-17 11:00:00 +00:00'
),
(
    71, 71, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    34, 5, N'Thầy Việt Anh là giáo viên trường chuyên nên các chuyên đề hàm số và tích phân thầy dạy cực kỳ đỉnh cao.',
    '2026-04-20 11:00:00 +00:00', '2026-04-20 11:00:00 +00:00'
),
(
    72, 72, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    34, 5, N'Đề thi thử thầy biên soạn sát với ma trận đề thi tốt nghiệp của Bộ Giáo Dục.',
    '2026-05-25 11:00:00 +00:00', '2026-05-25 11:00:00 +00:00'
),
(
    73, 73, N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    35, 5, N'Phụ huynh đánh giá: Cô Hà My kèm con ôn thi vào trường cấp 2 chất lượng cao rất sát sao. Bé đạt điểm 9 môn Toán.',
    '2026-04-13 11:00:00 +00:00', '2026-04-13 11:00:00 +00:00'
),
(
    74, 74, N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    35, 5, N'Cô dạy dễ hiểu và thường xuyên thưởng sao khuyến khích khi con làm bài đúng.',
    '2026-05-14 11:00:00 +00:00', '2026-05-14 11:00:00 +00:00'
),
(
    75, 75, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    36, 5, N'Anh Khôi làm Data Engineer nên dạy các thư viện Pandas, NumPy và Matplotlib rất thực tế.',
    '2026-04-22 11:00:00 +00:00', '2026-04-22 11:00:00 +00:00'
);

INSERT INTO mm_review.dbo.reviews (
    id, booking_id, student_user_id, student_name, mentor_id, rating, comment, created_at, updated_at
) VALUES
(
    76, 76, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    36, 5, N'Anh hướng dẫn em làm project phân tích dữ liệu điểm thi rất hay và hữu ích.',
    '2026-05-21 11:00:00 +00:00', '2026-05-21 11:00:00 +00:00'
),
(
    77, 77, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    37, 5, N'Chị Vy truyền cảm hứng rất tốt. Nhờ chị mà em đã tự tin tham gia câu lạc bộ tranh biện của trường.',
    '2026-04-18 11:00:00 +00:00', '2026-04-18 11:00:00 +00:00'
),
(
    78, 78, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    37, 5, N'Bài tập thực hành thuyết trình có ghi âm và nhận xét cụ thể từng cử chỉ, ngữ điệu.',
    '2026-05-19 11:00:00 +00:00', '2026-05-19 11:00:00 +00:00'
),
(
    79, 79, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    38, 5, N'Thầy Nhật Minh ôn thi đại học khối B cực kỳ tâm huyết. Bộ câu hỏi phân loại 9+ Hóa học của thầy rất đáng giá.',
    '2026-04-25 11:00:00 +00:00', '2026-04-25 11:00:00 +00:00'
),
(
    80, 80, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    38, 5, N'Thầy đúng giờ và giải đáp mọi thắc mắc bài tập hóc búa đến tận khuya.',
    '2026-05-27 11:00:00 +00:00', '2026-05-27 11:00:00 +00:00'
),
(
    81, 81, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    39, 5, N'Cô Linh chữa bài văn mẫu rất tỉ mỉ, giúp em biết cách mở rộng vốn từ và nâng cao chiều sâu tư tưởng bài viết.',
    '2026-04-17 11:00:00 +00:00', '2026-04-17 11:00:00 +00:00'
),
(
    82, 82, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    39, 5, N'Cẩm nang sơ đồ tác phẩm văn học của cô ngắn gọn, giúp ôn tập nước rút cực kỳ hiệu quả.',
    '2026-05-20 11:00:00 +00:00', '2026-05-20 11:00:00 +00:00'
),
(
    83, 83, N'00000000-0000-0000-0002-000000000003', N'Learner Hoang Long',
    40, 5, N'Thầy Long dạy các hàm Excel và kỹ năng Word rất thực tế, em làm bài tập thực hành trên lớp điểm 10.',
    '2026-04-21 11:00:00 +00:00', '2026-04-21 11:00:00 +00:00'
),
(
    84, 84, N'00000000-0000-0000-0002-000000000005', N'Learner Quang Huy',
    40, 4, N'Thầy nhiệt tình, có nhiều bài tập thực tế để luyện gõ phím và phím tắt.',
    '2026-05-23 11:00:00 +00:00', '2026-05-23 11:00:00 +00:00'
),
(
    85, 85, N'00000000-0000-0000-0002-000000000004', N'Learner My Linh',
    41, 5, N'Cô Thiên An dạy bảng Hangeul và phát âm chuẩn Seoul. Cách cô giải thích cấu trúc kính ngữ rất dễ hiểu.',
    '2026-04-11 11:00:00 +00:00', '2026-04-11 11:00:00 +00:00'
),
(
    86, 86, N'00000000-0000-0000-0002-000000000008', N'Learner Tram Lam',
    41, 5, N'Học tiếng Hàn với cô rất thoải mái, cô chia sẻ nhiều kinh nghiệm xin học bổng du học Hàn Quốc.',
    '2026-05-16 11:00:00 +00:00', '2026-05-16 11:00:00 +00:00'
),
(
    87, 87, N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    42, 5, N'Phụ huynh: Cô Điệp rất yêu trẻ và tâm lý. Bé nhà mình trước đây sợ học toán giờ đã tự giác làm bài tập.',
    '2026-04-19 11:00:00 +00:00', '2026-04-19 11:00:00 +00:00'
),
(
    88, 88, N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    42, 5, N'Cô thưởng sticker khen ngợi sau mỗi buổi học, con rất thích học với cô.',
    '2026-05-22 11:00:00 +00:00', '2026-05-22 11:00:00 +00:00'
),
(
    89, 89, N'00000000-0000-0000-0002-000000000001', N'Learner Kim Ngan',
    43, 5, N'Thầy Phúc luyện đề tuyển sinh vào lớp 10 rất chuẩn. Thầy chỉ rõ từng lỗi trình bày hay bị trừ điểm oan.',
    '2026-04-14 11:00:00 +00:00', '2026-04-14 11:00:00 +00:00'
),
(
    90, 90, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    43, 5, N'Nhờ thầy mà em nắm chắc dạng bài toán thực tế và hình học đường tròn.',
    '2026-05-18 11:00:00 +00:00', '2026-05-18 11:00:00 +00:00'
),
(
    91, 91, N'00000000-0000-0000-0002-000000000007', N'Learner Phuoc An',
    44, 5, N'Cô Thư dạy rất chậm và chắc. Em bị mất gốc tiếng Anh mà sau 1 tháng đã bắt kịp các bạn trên lớp.',
    '2026-04-20 11:00:00 +00:00', '2026-04-20 11:00:00 +00:00'
),
(
    92, 92, N'00000000-0000-0000-0002-000000000002', N'Learner An Nhi',
    44, 5, N'Cô gửi bài nghe và flashcard ôn từ vựng mỗi ngày, chăm sóc học sinh cực kỳ chu đáo.',
    '2026-05-24 11:00:00 +00:00', '2026-05-24 11:00:00 +00:00'
),
(
    93, 93, N'00000000-0000-0000-0002-000000000006', N'Learner Diem My',
    45, 5, N'Phụ huynh: Thầy Khang có các câu đố toán học logic và trò chơi tư duy rất bổ ích, rèn phản xạ cho bé rất tốt.',
    '2026-04-26 11:00:00 +00:00', '2026-04-26 11:00:00 +00:00'
),
(
    94, 94, N'00000000-0000-0000-0002-000000000009', N'Learner Anh Kiet',
    45, 5, N'Thầy dạy vui tính, khuyến khích con tự tìm nhiều cách giải khác nhau.',
    '2026-05-28 11:00:00 +00:00', '2026-05-28 11:00:00 +00:00'
);

SET IDENTITY_INSERT mm_review.dbo.reviews OFF;

INSERT INTO mm_review.dbo.review_tags (
    review_id, review_tag_option_id, created_at
) VALUES
(1, 1, '2026-04-10 11:00:00 +00:00'),
(1, 3, '2026-04-10 11:00:00 +00:00'),
(2, 2, '2026-05-12 11:00:00 +00:00'),
(2, 4, '2026-05-12 11:00:00 +00:00'),
(3, 3, '2026-04-15 11:00:00 +00:00'),
(3, 4, '2026-04-15 11:00:00 +00:00'),
(4, 1, '2026-05-18 11:00:00 +00:00'),
(4, 5, '2026-05-18 11:00:00 +00:00'),
(5, 4, '2026-04-20 11:00:00 +00:00'),
(5, 5, '2026-04-20 11:00:00 +00:00'),
(6, 2, '2026-05-22 11:00:00 +00:00'),
(6, 3, '2026-05-22 11:00:00 +00:00'),
(7, 1, '2026-04-25 11:00:00 +00:00'),
(7, 3, '2026-04-25 11:00:00 +00:00'),
(8, 2, '2026-05-25 11:00:00 +00:00'),
(8, 5, '2026-05-25 11:00:00 +00:00'),
(9, 1, '2026-04-18 11:00:00 +00:00'),
(9, 3, '2026-04-18 11:00:00 +00:00'),
(10, 3, '2026-05-16 11:00:00 +00:00'),
(10, 5, '2026-05-16 11:00:00 +00:00'),
(11, 3, '2026-04-22 11:00:00 +00:00'),
(11, 4, '2026-04-22 11:00:00 +00:00'),
(12, 1, '2026-05-19 11:00:00 +00:00'),
(12, 5, '2026-05-19 11:00:00 +00:00'),
(13, 4, '2026-04-28 11:00:00 +00:00'),
(13, 5, '2026-04-28 11:00:00 +00:00'),
(14, 2, '2026-05-27 11:00:00 +00:00'),
(14, 4, '2026-05-27 11:00:00 +00:00'),
(15, 3, '2026-04-12 11:00:00 +00:00'),
(15, 4, '2026-04-12 11:00:00 +00:00'),
(16, 1, '2026-05-14 11:00:00 +00:00'),
(16, 5, '2026-05-14 11:00:00 +00:00'),
(17, 1, '2026-04-12 11:00:00 +00:00'),
(17, 3, '2026-04-12 11:00:00 +00:00'),
(18, 2, '2026-04-28 11:00:00 +00:00'),
(18, 4, '2026-04-28 11:00:00 +00:00'),
(19, 3, '2026-05-15 11:00:00 +00:00'),
(19, 5, '2026-05-15 11:00:00 +00:00'),
(20, 3, '2026-04-10 11:00:00 +00:00'),
(20, 4, '2026-04-10 11:00:00 +00:00');

INSERT INTO mm_review.dbo.review_tags (
    review_id, review_tag_option_id, created_at
) VALUES
(21, 1, '2026-05-02 11:00:00 +00:00'),
(21, 2, '2026-05-02 11:00:00 +00:00'),
(22, 4, '2026-05-20 11:00:00 +00:00'),
(22, 5, '2026-05-20 11:00:00 +00:00'),
(23, 4, '2026-04-15 11:00:00 +00:00'),
(23, 5, '2026-04-15 11:00:00 +00:00'),
(24, 1, '2026-05-08 11:00:00 +00:00'),
(24, 3, '2026-05-08 11:00:00 +00:00'),
(25, 2, '2026-05-25 11:00:00 +00:00'),
(25, 3, '2026-05-25 11:00:00 +00:00'),
(26, 3, '2026-04-18 11:00:00 +00:00'),
(26, 4, '2026-04-18 11:00:00 +00:00'),
(27, 1, '2026-05-12 11:00:00 +00:00'),
(27, 5, '2026-05-12 11:00:00 +00:00'),
(28, 1, '2026-04-20 11:00:00 +00:00'),
(28, 3, '2026-04-20 11:00:00 +00:00'),
(29, 3, '2026-05-18 11:00:00 +00:00'),
(29, 5, '2026-05-18 11:00:00 +00:00'),
(30, 2, '2026-06-01 11:00:00 +00:00'),
(30, 4, '2026-06-01 11:00:00 +00:00'),
(31, 3, '2026-04-22 11:00:00 +00:00'),
(31, 4, '2026-04-22 11:00:00 +00:00'),
(32, 1, '2026-05-14 11:00:00 +00:00'),
(32, 2, '2026-05-14 11:00:00 +00:00'),
(33, 4, '2026-04-05 11:00:00 +00:00'),
(33, 5, '2026-04-05 11:00:00 +00:00'),
(34, 2, '2026-05-06 11:00:00 +00:00'),
(34, 4, '2026-05-06 11:00:00 +00:00'),
(35, 1, '2026-04-14 11:00:00 +00:00'),
(35, 3, '2026-04-14 11:00:00 +00:00'),
(36, 1, '2026-05-16 11:00:00 +00:00'),
(36, 5, '2026-05-16 11:00:00 +00:00'),
(37, 1, '2026-04-25 11:00:00 +00:00'),
(37, 3, '2026-04-25 11:00:00 +00:00'),
(38, 2, '2026-05-22 11:00:00 +00:00'),
(38, 5, '2026-05-22 11:00:00 +00:00'),
(39, 2, '2026-04-16 11:00:00 +00:00'),
(39, 4, '2026-04-16 11:00:00 +00:00'),
(40, 1, '2026-05-19 11:00:00 +00:00'),
(40, 4, '2026-05-19 11:00:00 +00:00');

INSERT INTO mm_review.dbo.review_tags (
    review_id, review_tag_option_id, created_at
) VALUES
(41, 1, '2026-04-27 11:00:00 +00:00'),
(41, 3, '2026-04-27 11:00:00 +00:00'),
(42, 3, '2026-05-24 11:00:00 +00:00'),
(42, 5, '2026-05-24 11:00:00 +00:00'),
(43, 4, '2026-04-11 11:00:00 +00:00'),
(43, 5, '2026-04-11 11:00:00 +00:00'),
(44, 1, '2026-05-09 11:00:00 +00:00'),
(44, 4, '2026-05-09 11:00:00 +00:00'),
(45, 1, '2026-04-19 11:00:00 +00:00'),
(45, 3, '2026-04-19 11:00:00 +00:00'),
(46, 1, '2026-05-21 11:00:00 +00:00'),
(46, 2, '2026-05-21 11:00:00 +00:00'),
(47, 4, '2026-04-14 11:00:00 +00:00'),
(47, 5, '2026-04-14 11:00:00 +00:00'),
(48, 2, '2026-05-18 11:00:00 +00:00'),
(48, 4, '2026-05-18 11:00:00 +00:00'),
(49, 3, '2026-04-29 11:00:00 +00:00'),
(49, 4, '2026-04-29 11:00:00 +00:00'),
(50, 1, '2026-05-26 11:00:00 +00:00'),
(50, 5, '2026-05-26 11:00:00 +00:00'),
(51, 1, '2026-04-17 11:00:00 +00:00'),
(51, 3, '2026-04-17 11:00:00 +00:00'),
(52, 2, '2026-05-13 11:00:00 +00:00'),
(52, 3, '2026-05-13 11:00:00 +00:00'),
(53, 4, '2026-04-21 11:00:00 +00:00'),
(53, 5, '2026-04-21 11:00:00 +00:00'),
(54, 1, '2026-05-17 11:00:00 +00:00'),
(54, 3, '2026-05-17 11:00:00 +00:00'),
(55, 4, '2026-04-12 11:00:00 +00:00'),
(55, 5, '2026-04-12 11:00:00 +00:00'),
(56, 2, '2026-05-11 11:00:00 +00:00'),
(56, 3, '2026-05-11 11:00:00 +00:00'),
(57, 3, '2026-04-26 11:00:00 +00:00'),
(57, 4, '2026-04-26 11:00:00 +00:00'),
(58, 1, '2026-05-23 11:00:00 +00:00'),
(58, 5, '2026-05-23 11:00:00 +00:00'),
(59, 4, '2026-04-15 11:00:00 +00:00'),
(59, 5, '2026-04-15 11:00:00 +00:00'),
(60, 1, '2026-05-20 11:00:00 +00:00'),
(60, 4, '2026-05-20 11:00:00 +00:00');

INSERT INTO mm_review.dbo.review_tags (
    review_id, review_tag_option_id, created_at
) VALUES
(61, 1, '2026-04-18 11:00:00 +00:00'),
(61, 3, '2026-04-18 11:00:00 +00:00'),
(62, 2, '2026-05-15 11:00:00 +00:00'),
(62, 3, '2026-05-15 11:00:00 +00:00'),
(63, 3, '2026-04-23 11:00:00 +00:00'),
(63, 5, '2026-04-23 11:00:00 +00:00'),
(64, 1, '2026-05-19 11:00:00 +00:00'),
(64, 3, '2026-05-19 11:00:00 +00:00'),
(65, 3, '2026-04-10 11:00:00 +00:00'),
(65, 4, '2026-04-10 11:00:00 +00:00'),
(66, 1, '2026-05-08 11:00:00 +00:00'),
(66, 5, '2026-05-08 11:00:00 +00:00'),
(67, 3, '2026-04-24 11:00:00 +00:00'),
(67, 5, '2026-04-24 11:00:00 +00:00'),
(68, 1, '2026-05-22 11:00:00 +00:00'),
(68, 4, '2026-05-22 11:00:00 +00:00'),
(69, 1, '2026-04-16 11:00:00 +00:00'),
(69, 3, '2026-04-16 11:00:00 +00:00'),
(70, 2, '2026-05-17 11:00:00 +00:00'),
(70, 5, '2026-05-17 11:00:00 +00:00'),
(71, 4, '2026-04-20 11:00:00 +00:00'),
(71, 5, '2026-04-20 11:00:00 +00:00'),
(72, 2, '2026-05-25 11:00:00 +00:00'),
(72, 4, '2026-05-25 11:00:00 +00:00'),
(73, 1, '2026-04-13 11:00:00 +00:00'),
(73, 3, '2026-04-13 11:00:00 +00:00'),
(74, 1, '2026-05-14 11:00:00 +00:00'),
(74, 2, '2026-05-14 11:00:00 +00:00'),
(75, 4, '2026-04-22 11:00:00 +00:00'),
(75, 5, '2026-04-22 11:00:00 +00:00'),
(76, 1, '2026-05-21 11:00:00 +00:00'),
(76, 4, '2026-05-21 11:00:00 +00:00'),
(77, 1, '2026-04-18 11:00:00 +00:00'),
(77, 3, '2026-04-18 11:00:00 +00:00'),
(78, 3, '2026-05-19 11:00:00 +00:00'),
(78, 5, '2026-05-19 11:00:00 +00:00'),
(79, 4, '2026-04-25 11:00:00 +00:00'),
(79, 5, '2026-04-25 11:00:00 +00:00'),
(80, 2, '2026-05-27 11:00:00 +00:00'),
(80, 4, '2026-05-27 11:00:00 +00:00');

INSERT INTO mm_review.dbo.review_tags (
    review_id, review_tag_option_id, created_at
) VALUES
(81, 1, '2026-04-17 11:00:00 +00:00'),
(81, 3, '2026-04-17 11:00:00 +00:00'),
(82, 3, '2026-05-20 11:00:00 +00:00'),
(82, 5, '2026-05-20 11:00:00 +00:00'),
(83, 1, '2026-04-21 11:00:00 +00:00'),
(83, 3, '2026-04-21 11:00:00 +00:00'),
(84, 2, '2026-05-23 11:00:00 +00:00'),
(84, 5, '2026-05-23 11:00:00 +00:00'),
(85, 1, '2026-04-11 11:00:00 +00:00'),
(85, 3, '2026-04-11 11:00:00 +00:00'),
(86, 3, '2026-05-16 11:00:00 +00:00'),
(86, 5, '2026-05-16 11:00:00 +00:00'),
(87, 1, '2026-04-19 11:00:00 +00:00'),
(87, 3, '2026-04-19 11:00:00 +00:00'),
(88, 1, '2026-05-22 11:00:00 +00:00'),
(88, 2, '2026-05-22 11:00:00 +00:00'),
(89, 4, '2026-04-14 11:00:00 +00:00'),
(89, 5, '2026-04-14 11:00:00 +00:00'),
(90, 2, '2026-05-18 11:00:00 +00:00'),
(90, 4, '2026-05-18 11:00:00 +00:00'),
(91, 1, '2026-04-20 11:00:00 +00:00'),
(91, 3, '2026-04-20 11:00:00 +00:00'),
(92, 1, '2026-05-24 11:00:00 +00:00'),
(92, 5, '2026-05-24 11:00:00 +00:00'),
(93, 3, '2026-04-26 11:00:00 +00:00'),
(93, 4, '2026-04-26 11:00:00 +00:00'),
(94, 1, '2026-05-28 11:00:00 +00:00'),
(94, 3, '2026-05-28 11:00:00 +00:00');
