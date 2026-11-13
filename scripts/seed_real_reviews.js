const fs = require('fs');

// Learners
const learners = [
    { id: '00000000-0000-0000-0002-000000000001', name: 'Learner Kim Ngan' },
    { id: '00000000-0000-0000-0002-000000000002', name: 'Learner An Nhi' },
    { id: '00000000-0000-0000-0002-000000000003', name: 'Learner Hoang Long' },
    { id: '00000000-0000-0000-0002-000000000004', name: 'Learner My Linh' },
    { id: '00000000-0000-0000-0002-000000000005', name: 'Learner Quang Huy' },
    { id: '00000000-0000-0000-0002-000000000006', name: 'Learner Diem My' }, // Parent
    { id: '00000000-0000-0000-0002-000000000007', name: 'Learner Phuoc An' },
    { id: '00000000-0000-0000-0002-000000000008', name: 'Learner Tram Lam' },
    { id: '00000000-0000-0000-0002-000000000009', name: 'Learner Anh Kiet' },
    { id: '00000000-0000-0000-0002-000000000010', name: 'Learner Thanh Ha' }
];

// Review tag IDs:
// 1: Nhiệt tình, 2: Đúng giờ, 3: Dễ hiểu, 4: Chuyên môn cao, 5: Tài liệu hữu ích

// Mentor reviews dataset
const mentorReviewData = {
    // Demo Mentors (IDs 9..45)
    9: { // Mentor Minh Anh - Toán THCS
        subjectId: 10, subjectName: 'Toán', gradeName: 'Lớp 9', price: 220000,
        reviews: [
            { learnerIdx: 0, rating: 5, tags: [1, 3], comment: 'Cô dạy rất kiên nhẫn và tận tình. Em từng mất gốc hình học lớp 9 mà sau 5 buổi đã tự tin làm được bài thi thử tuyển sinh. Cô chữa bài tập về nhà rất kỹ.', date: '2026-04-12' },
            { learnerIdx: 1, rating: 5, tags: [2, 4], comment: 'Phương pháp giải đại số của cô Minh Anh rất mạch lạc, dễ nhớ. Cô luôn đúng giờ và chuẩn bị phiếu bài tập phân dạng rất khoa học.', date: '2026-04-28' },
            { learnerIdx: 2, rating: 4, tags: [3, 5], comment: 'Buổi học vui vẻ, giải thích bài dễ hiểu. Cô cho nhiều bài tập tự luyện sát với đề thi vào 10 của Sở.', date: '2026-05-15' }
        ]
    },
    10: { // Mentor Quoc Huy - Vật lý
        subjectId: 14, subjectName: 'Vật lý', gradeName: 'Lớp 10', price: 250000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [3, 4], comment: 'Thầy Huy giảng phần động lực học chất điểm cực kỳ trực quan, liên hệ nhiều hiện tượng thực tế nên em hiểu bản chất rất nhanh.', date: '2026-04-10' },
            { learnerIdx: 4, rating: 5, tags: [1, 2], comment: 'Thầy dạy nhiệt tình, hay giải đáp thắc mắc bài tập trên lớp ngoài giờ học. Tài liệu tóm tắt công thức của thầy rất ngắn gọn và dễ tra cứu.', date: '2026-05-02' },
            { learnerIdx: 7, rating: 5, tags: [4, 5], comment: 'Chuyên môn rất vững, cách thầy phân tích hiện tượng vật lý giúp em tư duy giải bài tập trắc nghiệm nhanh hơn hẳn.', date: '2026-05-20' }
        ]
    },
    11: { // Mentor Ngoc Mai - Tiếng Anh / IELTS
        subjectId: 19, subjectName: 'IELTS', gradeName: null, price: 350000,
        reviews: [
            { learnerIdx: 7, rating: 5, tags: [4, 5], comment: 'Cô Mai sửa bài Writing Task 2 siêu chi tiết, chỉ ra từng lỗi dùng từ chưa tự nhiên và cách phát triển ý theo chuẩn band 7.0+. Rất đáng tiền!', date: '2026-04-15' },
            { learnerIdx: 3, rating: 5, tags: [1, 3], comment: 'Phần Speaking cô luyện phản xạ rất tốt, giúp mình tự tin hơn hẳn khi trả lời câu hỏi Part 2 và Part 3.', date: '2026-05-08' },
            { learnerIdx: 0, rating: 4, tags: [2, 3], comment: 'Cô phát âm chuẩn, nhiệt tình hướng dẫn các mẹo làm bài Listening và Reading. Nhịp học hơi nhanh một chút nhưng rất hiệu quả.', date: '2026-05-25' }
        ]
    },
    12: { // Mentor Thanh Tung - Lập trình Python / Web
        subjectId: 21, subjectName: 'Lập trình Python', gradeName: 'Lớp 10', price: 300000,
        reviews: [
            { learnerIdx: 2, rating: 5, tags: [3, 4], comment: 'Anh Tùng dạy code rất cuốn, từ cú pháp cơ bản đến tự build project game nhỏ. Khi gặp bug anh hướng dẫn cách đọc traceback và debug rất bài bản.', date: '2026-04-18' },
            { learnerIdx: 4, rating: 5, tags: [1, 5], comment: 'Tài liệu thực hành phong phú, có repo GitHub để theo dõi code sau mỗi buổi. Anh giảng chậm rãi và trả lời mọi thắc mắc.', date: '2026-05-12' }
        ]
    },
    13: { // Mentor Bao Vy - Ngữ văn
        subjectId: 23, subjectName: 'Ngữ văn', gradeName: 'Lớp 9', price: 220000,
        reviews: [
            { learnerIdx: 0, rating: 5, tags: [1, 3], comment: 'Cô Vy giúp em không còn sợ viết văn nghị luận xã hội nữa. Cô hướng dẫn cách lập dàn ý logic, dùng dẫn chứng thời sự sắc bén.', date: '2026-04-20' },
            { learnerIdx: 9, rating: 5, tags: [3, 5], comment: 'Lời văn của cô rất truyền cảm, bài giảng phân tích tác phẩm sâu sắc mà không hề bị sáo rỗng hay học vẹt.', date: '2026-05-18' },
            { learnerIdx: 1, rating: 5, tags: [2, 4], comment: 'Cô sửa bài văn rất kỹ từng câu chữ và lỗi diễn đạt. Điểm thi thử môn Văn của em đã tăng từ 6.5 lên 8.0.', date: '2026-06-01' }
        ]
    },
    14: { // Mentor Hoang Yen - Hóa học / Sinh học
        subjectId: 26, subjectName: 'Hóa học', gradeName: 'Lớp 9', price: 240000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [3, 4], comment: 'Cô Yến dạy Hóa bằng sơ đồ tư duy chuỗi phản ứng rất dễ thuộc. Phần bài tập tính toán nồng độ và khối lượng cô chỉ mẹo giải cực nhanh.', date: '2026-04-22' },
            { learnerIdx: 2, rating: 4, tags: [1, 2], comment: 'Cô rất nhiệt tình và hiền, không hiểu chỗ nào cô giảng lại ngay. Bài tập sau buổi học vừa sức và sát kiến thức trên lớp.', date: '2026-05-14' }
        ]
    },
    15: { // Mentor Duc Tri - Toán nâng cao
        subjectId: 10, subjectName: 'Toán', gradeName: 'Lớp 10', price: 250000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Thầy Trí có tư duy giải toán rất độc đáo, chuyên trị các bài bất đẳng thức và hình học giải tích nâng cao.', date: '2026-04-05' },
            { learnerIdx: 2, rating: 4, tags: [2, 4], comment: 'Bài giảng chất lượng cao dành cho học sinh khá giỏi. Thầy yêu cầu tư duy độc lập trước khi đưa ra gợi ý.', date: '2026-05-06' }
        ]
    },
    16: { // Mentor Gia Bao - Lập trình Scratch / Tin học
        subjectId: 33, subjectName: 'Lập trình Scratch', gradeName: 'Lớp 4', price: 220000,
        reviews: [
            { learnerIdx: 5, rating: 5, tags: [1, 3], comment: 'Phụ huynh đánh giá: Thầy Bảo dạy bé 9 tuổi nhà mình rất kiên nhẫn. Bé rất hào hứng chờ đến giờ học để tự làm phim hoạt hình và game mê cung trên Scratch.', date: '2026-04-14' },
            { learnerIdx: 8, rating: 5, tags: [1, 5], comment: 'Em học thầy Bảo làm được 3 mini game rồi. Thầy chỉ dẫn vui tính và dễ hiểu lắm ạ.', date: '2026-05-16' }
        ]
    },
    17: { // Mentor Khanh Linh - Tiếng Anh giao tiếp & THCS
        subjectId: 35, subjectName: 'Tiếng Anh', gradeName: 'Lớp 6', price: 180000,
        reviews: [
            { learnerIdx: 6, rating: 5, tags: [1, 3], comment: 'Cô Linh rất thân thiện, giọng phát âm chuẩn. Em từ một đứa ngại nói tiếng Anh giờ đã dám tự tin nói chuyện cả buổi.', date: '2026-04-25' },
            { learnerIdx: 1, rating: 5, tags: [2, 5], comment: 'Giáo trình sinh động, nhiều trò chơi tương tác từ vựng giúp em nhớ từ mới rất lâu.', date: '2026-05-22' }
        ]
    },
    18: { // Mentor Tuan Minh - Toán & Vật lý
        subjectId: 38, subjectName: 'Toán', gradeName: 'Lớp 10', price: 250000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [2, 4], comment: 'Thầy Minh có phong cách dạy kỷ luật và rõ ràng. Bài test 10 phút đầu giờ giúp em không bị quên kiến thức cũ.', date: '2026-04-16' },
            { learnerIdx: 3, rating: 4, tags: [1, 4], comment: 'Thầy giảng chắc lý thuyết rồi mới cho áp dụng bài tập. Điểm kiểm tra 1 tiết của em đạt 8.5.', date: '2026-05-19' }
        ]
    },
    19: { // Mentor Phuong Nhi - Lịch sử & Địa lý
        subjectId: 41, subjectName: 'Lịch sử', gradeName: 'Lớp 8', price: 180000,
        reviews: [
            { learnerIdx: 2, rating: 5, tags: [1, 3], comment: 'Cô Nhi kể chuyện lịch sử hấp dẫn như xem phim tài liệu, không hề có cảm giác khô khan hay phải học vẹt mốc thời gian.', date: '2026-04-27' },
            { learnerIdx: 0, rating: 5, tags: [3, 5], comment: 'Sơ đồ timeline sự kiện cô gửi sau buổi học cực kỳ trực quan và dễ nhớ.', date: '2026-05-24' }
        ]
    },
    20: { // Mentor Duy Anh - Lập trình Web
        subjectId: 44, subjectName: 'Lập trình Web', gradeName: 'Lớp 10', price: 300000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Anh Duy Anh dạy HTML, CSS và JavaScript thực chiến. Học 1 tháng là tự làm được trang portfolio cá nhân.', date: '2026-04-11' },
            { learnerIdx: 2, rating: 5, tags: [1, 4], comment: 'Hướng dẫn responsive và animation rất kỹ. Mentor giải đáp nhiệt tình kể cả ngoài giờ học.', date: '2026-05-09' }
        ]
    },
    21: { // Mentor Ngoc Han - Tiếng Việt tiểu học
        subjectId: 47, subjectName: 'Tiếng Việt', gradeName: 'Lớp 2', price: 170000,
        reviews: [
            { learnerIdx: 5, rating: 5, tags: [1, 3], comment: 'Phụ huynh nhận xét: Cô Hân rèn chữ và chính tả cho con rất kiên trì. Con tiến bộ rõ rệt sau 2 tháng học.', date: '2026-04-19' },
            { learnerIdx: 8, rating: 5, tags: [1, 2], comment: 'Cô dạy nhẹ nhàng, con thích học đọc truyện và viết văn miêu tả cùng cô.', date: '2026-05-21' }
        ]
    },
    22: { // Mentor Minh Quan - SAT Math & Xác suất
        subjectId: 50, subjectName: 'SAT', gradeName: null, price: 420000,
        reviews: [
            { learnerIdx: 7, rating: 5, tags: [4, 5], comment: 'Thầy Quân dạy chiến thuật làm bài SAT Math cực đỉnh, mẹo dùng máy tính Desmos giúp tiết kiệm một nửa thời gian làm bài.', date: '2026-04-14' },
            { learnerIdx: 3, rating: 5, tags: [2, 4], comment: 'Điểm SAT Math của mình tăng từ 680 lên 770 nhờ lộ trình luyện đề chuyên sâu của thầy.', date: '2026-05-18' }
        ]
    },
    23: { // Mentor Bao Chau - Sinh học THPT
        subjectId: 54, subjectName: 'Sinh học', gradeName: 'Lớp 11', price: 270000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [3, 4], comment: 'Chị Châu là sinh viên Y khoa nên kiến thức sinh học di truyền và sinh thái nắm siêu chắc. Cách chị liên hệ bệnh học thực tế làm bài học rất sinh động.', date: '2026-04-29' },
            { learnerIdx: 2, rating: 5, tags: [1, 5], comment: 'Tài liệu tóm tắt chu trình sinh học rất đẹp và dễ hiểu, ôn thi học kỳ cực kỳ nhàn.', date: '2026-05-26' }
        ]
    },
    24: { // Mentor Hoang Nam - Toán tiểu học & Toán tư duy
        subjectId: 58, subjectName: 'Toán tư duy', gradeName: 'Lớp 4', price: 210000,
        reviews: [
            { learnerIdx: 5, rating: 5, tags: [1, 3], comment: 'Thầy Nam dạy toán tư duy kích thích sự sáng tạo của con, không ép giải theo khuôn mẫu. Rất hài lòng!', date: '2026-04-17' },
            { learnerIdx: 8, rating: 5, tags: [2, 3], comment: 'Thầy đúng giờ, giọng nói ấm áp và luôn động viên học sinh khi gặp bài toán khó.', date: '2026-05-13' }
        ]
    },
    25: { // Mentor Thao Nguyen - Tiếng Anh THPT / TOEIC / IELTS
        subjectId: 61, subjectName: 'IELTS', gradeName: null, price: 340000,
        reviews: [
            { learnerIdx: 7, rating: 5, tags: [4, 5], comment: 'Cô Thảo Nguyên có phát âm cực hay và phương pháp dạy IELTS rất hệ thống. Bộ đề forecast cô cung cấp trúng nhiều chủ đề trong kỳ thi thật.', date: '2026-04-21' },
            { learnerIdx: 3, rating: 5, tags: [1, 3], comment: 'Luyện speaking với cô giúp mình bớt ngắc ngứ và phản xạ trôi chảy hơn rất nhiều.', date: '2026-05-17' }
        ]
    },
    26: { // Mentor Thanh Lam - Hóa học
        subjectId: 26, subjectName: 'Hóa học', gradeName: 'Lớp 9', price: 240000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Thầy Lam có phòng thí nghiệm thực tế nên giải thích hiện tượng hóa học rất thuyết phục.', date: '2026-04-12' },
            { learnerIdx: 2, rating: 4, tags: [2, 3], comment: 'Bài giảng chuyên sâu, phù hợp với học sinh định hướng thi chuyên Hóa.', date: '2026-05-11' }
        ]
    },
    27: { // Mentor Hai Dang - Địa lý
        subjectId: 66, subjectName: 'Địa lý', gradeName: 'Lớp 12', price: 250000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [3, 4], comment: 'Thầy Đăng dạy đọc Atlat Địa lý siêu nhanh và chuẩn xác. Nhờ thầy mà phần trắc nghiệm Atlat em không sai câu nào.', date: '2026-04-26' },
            { learnerIdx: 7, rating: 5, tags: [1, 5], comment: 'Thầy nhiệt tình và có nguồn tài liệu số liệu kinh tế - xã hội cập nhật mới nhất.', date: '2026-05-23' }
        ]
    },
    28: { // Mentor Minh Chau - IELTS
        subjectId: 68, subjectName: 'IELTS', gradeName: null, price: 380000,
        reviews: [
            { learnerIdx: 7, rating: 5, tags: [4, 5], comment: 'Chị Châu chấm và sửa bài IELTS Writing cực kỳ nghiêm túc, feedback chi tiết từng tiêu chí Lexical Resource và Coherence.', date: '2026-04-15' },
            { learnerIdx: 3, rating: 5, tags: [1, 4], comment: 'Sau khóa học 2 tháng mình đã tự tin đi thi và đạt overall 7.5 IELTS. Cảm ơn chị Châu rất nhiều!', date: '2026-05-20' }
        ]
    },
    29: { // Mentor Gia Han - Toán THCS
        subjectId: 71, subjectName: 'Toán', gradeName: 'Lớp 6', price: 200000,
        reviews: [
            { learnerIdx: 1, rating: 5, tags: [1, 3], comment: 'Cô Gia Hân dạy toán lớp 6 rất dễ hiểu, giúp em bỡ ngỡ bước đầu chuyển từ cấp 1 lên cấp 2.', date: '2026-04-18' },
            { learnerIdx: 6, rating: 5, tags: [2, 3], comment: 'Cô chuẩn bị giáo án chu đáo, luôn kiểm tra xem học sinh đã hiểu bài chưa trước khi chuyển dạng mới.', date: '2026-05-15' }
        ]
    },
    30: { // Mentor Nam Phong - Khoa học tự nhiên
        subjectId: 74, subjectName: 'Khoa học tự nhiên', gradeName: 'Lớp 6', price: 190000,
        reviews: [
            { learnerIdx: 2, rating: 5, tags: [3, 5], comment: 'Thầy Phong dạy môn KHTN tích hợp Vật lý, Hóa học và Sinh học rất hài hòa và thú vị.', date: '2026-04-23' },
            { learnerIdx: 6, rating: 5, tags: [1, 3], comment: 'Thầy hiền lành, giải thích cặn kẽ các hiện tượng tự nhiên quanh đời sống.', date: '2026-05-19' }
        ]
    },
    31: { // Mentor Tue Lam - Tiếng Trung
        subjectId: 77, subjectName: 'Tiếng Trung', gradeName: null, price: 260000,
        reviews: [
            { learnerIdx: 7, rating: 5, tags: [3, 4], comment: 'Cô Tuệ Lâm phát âm chuẩn Bắc Kinh, chỉnh thanh điệu cho người mới bắt đầu rất kỹ lưỡng.', date: '2026-04-10' },
            { learnerIdx: 3, rating: 5, tags: [1, 5], comment: 'Học tiếng Trung với cô rất vui, cô chia sẻ thêm nhiều văn hóa và âm nhạc Trung Quốc.', date: '2026-05-08' }
        ]
    },
    32: { // Mentor Bao Ngoc - Kỹ năng học tập & Thuyết trình
        subjectId: 79, subjectName: 'Phương pháp học tập', gradeName: null, price: 260000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [3, 5], comment: 'Chị Ngọc chia sẻ phương pháp ghi chép Cornell và kỹ thuật ghi nhớ Spaced Repetition thay đổi hoàn toàn cách tự học của mình.', date: '2026-04-24' },
            { learnerIdx: 7, rating: 5, tags: [1, 4], comment: 'Kỹ năng thuyết trình của mình cải thiện vượt bậc, biết cách kiểm soát ngôn ngữ cơ thể và thiết kế slide ấn tượng.', date: '2026-05-22' }
        ]
    },
    33: { // Mentor Quynh Anh - Tiếng Nhật
        subjectId: 82, subjectName: 'Tiếng Nhật', gradeName: null, price: 280000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [1, 3], comment: 'Cô Quỳnh Anh dạy bảng chữ cái và ngữ pháp N5 rất chắc. Phương pháp nhớ Kanji bằng hình ảnh của cô rất thú vị.', date: '2026-04-16' },
            { learnerIdx: 7, rating: 4, tags: [2, 5], comment: 'Cô chuẩn bị nhiều tài liệu luyện nghe thực tế từ anime và tin tức NHK.', date: '2026-05-17' }
        ]
    },
    34: { // Mentor Viet Anh - Toán THPT
        subjectId: 86, subjectName: 'Toán', gradeName: 'Lớp 12', price: 350000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Thầy Việt Anh là giáo viên trường chuyên nên các chuyên đề hàm số và tích phân thầy dạy cực kỳ đỉnh cao.', date: '2026-04-20' },
            { learnerIdx: 7, rating: 5, tags: [2, 4], comment: 'Đề thi thử thầy biên soạn sát với ma trận đề thi tốt nghiệp của Bộ Giáo Dục.', date: '2026-05-25' }
        ]
    },
    35: { // Mentor Ha My - Tiểu học
        subjectId: 87, subjectName: 'Toán', gradeName: 'Lớp 5', price: 190000,
        reviews: [
            { learnerIdx: 5, rating: 5, tags: [1, 3], comment: 'Phụ huynh đánh giá: Cô Hà My kèm con ôn thi vào trường cấp 2 chất lượng cao rất sát sao. Bé đạt điểm 9 môn Toán.', date: '2026-04-13' },
            { learnerIdx: 8, rating: 5, tags: [1, 2], comment: 'Cô dạy dễ hiểu và thường xuyên thưởng sao khuyến khích khi con làm bài đúng.', date: '2026-05-14' }
        ]
    },
    36: { // Mentor Minh Khoi - Data & Python
        subjectId: 91, subjectName: 'Lập trình Python', gradeName: 'Lớp 11', price: 330000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Anh Khôi làm Data Engineer nên dạy các thư viện Pandas, NumPy và Matplotlib rất thực tế.', date: '2026-04-22' },
            { learnerIdx: 2, rating: 5, tags: [1, 4], comment: 'Anh hướng dẫn em làm project phân tích dữ liệu điểm thi rất hay và hữu ích.', date: '2026-05-21' }
        ]
    },
    37: { // Mentor Khanh Vy - Thuyết trình & Tranh biện
        subjectId: 93, subjectName: 'Thuyết trình', gradeName: null, price: 260000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [1, 3], comment: 'Chị Vy truyền cảm hứng rất tốt. Nhờ chị mà em đã tự tin tham gia câu lạc bộ tranh biện của trường.', date: '2026-04-18' },
            { learnerIdx: 0, rating: 5, tags: [3, 5], comment: 'Bài tập thực hành thuyết trình có ghi âm và nhận xét cụ thể từng cử chỉ, ngữ điệu.', date: '2026-05-19' }
        ]
    },
    38: { // Mentor Nhat Minh - Hóa học / Sinh học
        subjectId: 96, subjectName: 'Hóa học', gradeName: 'Lớp 12', price: 340000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Thầy Nhật Minh ôn thi đại học khối B cực kỳ tâm huyết. Bộ câu hỏi phân loại 9+ Hóa học của thầy rất đáng giá.', date: '2026-04-25' },
            { learnerIdx: 7, rating: 5, tags: [2, 4], comment: 'Thầy đúng giờ và giải đáp mọi thắc mắc bài tập hóc búa đến tận khuya.', date: '2026-05-27' }
        ]
    },
    39: { // Mentor Phuong Linh - Ngữ văn THPT
        subjectId: 99, subjectName: 'Ngữ văn', gradeName: 'Lớp 12', price: 300000,
        reviews: [
            { learnerIdx: 0, rating: 5, tags: [1, 3], comment: 'Cô Linh chữa bài văn mẫu rất tỉ mỉ, giúp em biết cách mở rộng vốn từ và nâng cao chiều sâu tư tưởng bài viết.', date: '2026-04-17' },
            { learnerIdx: 3, rating: 5, tags: [3, 5], comment: 'Cẩm nang sơ đồ tác phẩm văn học của cô ngắn gọn, giúp ôn tập nước rút cực kỳ hiệu quả.', date: '2026-05-20' }
        ]
    },
    40: { // Mentor Bao Long - Tin học văn phòng
        subjectId: 102, subjectName: 'Tin học', gradeName: 'Lớp 7', price: 200000,
        reviews: [
            { learnerIdx: 2, rating: 5, tags: [1, 3], comment: 'Thầy Long dạy các hàm Excel và kỹ năng Word rất thực tế, em làm bài tập thực hành trên lớp điểm 10.', date: '2026-04-21' },
            { learnerIdx: 4, rating: 4, tags: [2, 5], comment: 'Thầy nhiệt tình, có nhiều bài tập thực tế để luyện gõ phím và phím tắt.', date: '2026-05-23' }
        ]
    },
    41: { // Mentor Thien An - Tiếng Hàn
        subjectId: 105, subjectName: 'Tiếng Hàn', gradeName: null, price: 280000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [1, 3], comment: 'Cô Thiên An dạy bảng Hangeul và phát âm chuẩn Seoul. Cách cô giải thích cấu trúc kính ngữ rất dễ hiểu.', date: '2026-04-11' },
            { learnerIdx: 7, rating: 5, tags: [3, 5], comment: 'Học tiếng Hàn với cô rất thoải mái, cô chia sẻ nhiều kinh nghiệm xin học bổng du học Hàn Quốc.', date: '2026-05-16' }
        ]
    },
    42: { // Mentor Ngoc Diep - Tiểu học
        subjectId: 107, subjectName: 'Toán', gradeName: 'Lớp 2', price: 170000,
        reviews: [
            { learnerIdx: 5, rating: 5, tags: [1, 3], comment: 'Phụ huynh: Cô Điệp rất yêu trẻ và tâm lý. Bé nhà mình trước đây sợ học toán giờ đã tự giác làm bài tập.', date: '2026-04-19' },
            { learnerIdx: 8, rating: 5, tags: [1, 2], comment: 'Cô thưởng sticker khen ngợi sau mỗi buổi học, con rất thích học với cô.', date: '2026-05-22' }
        ]
    },
    43: { // Mentor Thanh Phuc - Luyện thi vào lớp 10 môn Toán
        subjectId: 111, subjectName: 'Toán', gradeName: 'Lớp 9', price: 260000,
        reviews: [
            { learnerIdx: 0, rating: 5, tags: [4, 5], comment: 'Thầy Phúc luyện đề tuyển sinh vào lớp 10 rất chuẩn. Thầy chỉ rõ từng lỗi trình bày hay bị trừ điểm oan.', date: '2026-04-14' },
            { learnerIdx: 1, rating: 5, tags: [2, 4], comment: 'Nhờ thầy mà em nắm chắc dạng bài toán thực tế và hình học đường tròn.', date: '2026-05-18' }
        ]
    },
    44: { // Mentor Anh Thu - Tiếng Anh mất gốc
        subjectId: 114, subjectName: 'Tiếng Anh', gradeName: 'Lớp 6', price: 190000,
        reviews: [
            { learnerIdx: 6, rating: 5, tags: [1, 3], comment: 'Cô Thư dạy rất chậm và chắc. Em bị mất gốc tiếng Anh mà sau 1 tháng đã bắt kịp các bạn trên lớp.', date: '2026-04-20' },
            { learnerIdx: 1, rating: 5, tags: [1, 5], comment: 'Cô gửi bài nghe và flashcard ôn từ vựng mỗi ngày, chăm sóc học sinh cực kỳ chu đáo.', date: '2026-05-24' }
        ]
    },
    45: { // Mentor Gia Khang - Toán tư duy & logic
        subjectId: 117, subjectName: 'Toán tư duy', gradeName: 'Lớp 3', price: 220000,
        reviews: [
            { learnerIdx: 5, rating: 5, tags: [3, 4], comment: 'Phụ huynh: Thầy Khang có các câu đố toán học logic và trò chơi tư duy rất bổ ích, rèn phản xạ cho bé rất tốt.', date: '2026-04-26' },
            { learnerIdx: 8, rating: 5, tags: [1, 3], comment: 'Thầy dạy vui tính, khuyến khích con tự tìm nhiều cách giải khác nhau.', date: '2026-05-28' }
        ]
    },

    // Sample Mentors (IDs 1..8)
    1: {
        subjectId: 1, subjectName: 'Toán', gradeName: 'Lớp 10', price: 200000,
        reviews: [
            { learnerIdx: 0, rating: 5, tags: [1, 3], comment: 'Thầy An dạy rất nhiệt tình và bài bản, giúp em nắm vững kiến thức nền tảng.', date: '2026-04-10' },
            { learnerIdx: 2, rating: 5, tags: [2, 4], comment: 'Thầy đúng giờ và phương pháp giảng dạy dễ hiểu.', date: '2026-05-12' }
        ]
    },
    2: {
        subjectId: 2, subjectName: 'Vật lý', gradeName: 'Lớp 11', price: 220000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [3, 4], comment: 'Thầy Bình giải thích phần mạch điện xoay chiều rất sáng rõ và dễ hiểu.', date: '2026-04-15' },
            { learnerIdx: 4, rating: 5, tags: [1, 5], comment: 'Nhiều bài tập áp dụng thực tế rất hữu ích.', date: '2026-05-18' }
        ]
    },
    3: {
        subjectId: 3, subjectName: 'Hóa học', gradeName: 'Lớp 12', price: 230000,
        reviews: [
            { learnerIdx: 4, rating: 5, tags: [4, 5], comment: 'Cô Chi dạy phần Hóa hữu cơ rất logic, giúp em làm trắc nghiệm nhanh hơn.', date: '2026-04-20' },
            { learnerIdx: 7, rating: 5, tags: [2, 3], comment: 'Cô giảng bài nhiệt tình, chuẩn bị tài liệu rất công phu.', date: '2026-05-22' }
        ]
    },
    4: {
        subjectId: 4, subjectName: 'Tiếng Anh', gradeName: 'Lớp 10', price: 210000,
        reviews: [
            { learnerIdx: 1, rating: 5, tags: [1, 3], comment: 'Thầy Dũng phát âm rất chuẩn, buổi học có nhiều hoạt động tương tác.', date: '2026-04-25' },
            { learnerIdx: 9, rating: 4, tags: [2, 5], comment: 'Thầy đúng giờ và bài tập về nhà rất vừa sức.', date: '2026-05-25' }
        ]
    },
    5: {
        subjectId: 5, subjectName: 'Ngữ văn', gradeName: 'Lớp 9', price: 200000,
        reviews: [
            { learnerIdx: 0, rating: 5, tags: [1, 3], comment: 'Cô Hà dạy văn rất cảm xúc và giúp em cải thiện kỹ năng diễn đạt.', date: '2026-04-18' },
            { learnerIdx: 1, rating: 5, tags: [3, 5], comment: 'Dàn ý tác phẩm của cô rất chi tiết, dễ nhớ khi đi thi.', date: '2026-05-16' }
        ]
    },
    6: {
        subjectId: 6, subjectName: 'Sinh học', gradeName: 'Lớp 11', price: 220000,
        reviews: [
            { learnerIdx: 2, rating: 5, tags: [3, 4], comment: 'Thầy Khang giảng giải cơ chế di truyền phân tử rất dễ hiểu.', date: '2026-04-22' },
            { learnerIdx: 4, rating: 5, tags: [1, 5], comment: 'Tài liệu hình ảnh sinh học trực quan và sinh động.', date: '2026-05-19' }
        ]
    },
    7: {
        subjectId: 7, subjectName: 'Toán', gradeName: 'Lớp 12', price: 250000,
        reviews: [
            { learnerIdx: 3, rating: 5, tags: [4, 5], comment: 'Cô Linh có nhiều phương pháp giải nhanh trắc nghiệm rất hay.', date: '2026-04-28' },
            { learnerIdx: 7, rating: 5, tags: [2, 4], comment: 'Cô chữa bài tập đề thi thử rất cẩn thận từng câu một.', date: '2026-05-27' }
        ]
    },
    8: {
        subjectId: 8, subjectName: 'Lập trình Python', gradeName: 'Lớp 10', price: 280000,
        reviews: [
            { learnerIdx: 2, rating: 5, tags: [3, 4], comment: 'Thầy Nam dạy lập trình rất thực chiến và nhiệt tình giải đáp thắc mắc.', date: '2026-04-12' },
            { learnerIdx: 4, rating: 5, tags: [1, 5], comment: 'Các bài tập dự án nhỏ giúp em tự làm được sản phẩm chạy được.', date: '2026-05-14' }
        ]
    }
};

function main() {
    // Generate SQL for mm_booking and mm_review (MSSQL)
    const mssqlLines = [];
    mssqlLines.push(`-- ==============================================================================`);
    mssqlLines.push(`-- Seed Real Bookings and Reviews for Mentors in MSSQL`);
    mssqlLines.push(`-- Databases: mm_booking, mm_review`);
    mssqlLines.push(`-- ==============================================================================`);
    mssqlLines.push(``);
    mssqlLines.push(`USE mm_booking;`);
    mssqlLines.push(`SET NOCOUNT ON;`);
    mssqlLines.push(``);

    // Prepare bookings
    let bookingIdCounter = 1;
    const reviewRecords = []; // { reviewId, bookingId, studentUserId, studentName, mentorId, rating, comment, createdAt, tags }

    // Clear existing test bookings and reviews to ensure clean idempotent insert
    mssqlLines.push(`-- Clean up existing data in mm_booking and mm_review`);
    mssqlLines.push(`DELETE FROM mm_review.dbo.review_tags;`);
    mssqlLines.push(`DELETE FROM mm_review.dbo.reviews;`);
    mssqlLines.push(`DELETE FROM mm_booking.dbo.bookings;`);
    mssqlLines.push(``);
    mssqlLines.push(`SET IDENTITY_INSERT mm_booking.dbo.bookings ON;`);
    mssqlLines.push(``);

    const bookingInsertValues = [];

    for (const [mentorIdStr, mentorData] of Object.entries(mentorReviewData)) {
        const mentorId = parseInt(mentorIdStr, 10);
        // Mentor User ID
        let mentorUserId;
        if (mentorId >= 9) {
            const num = mentorId - 8;
            mentorUserId = `00000000-0000-0000-0001-${String(num).padStart(12, '0')}`;
        } else {
            mentorUserId = `00000000-0000-0000-0000-${String(mentorId + 21).padStart(12, '0')}`;
        }

        for (const rev of mentorData.reviews) {
            const learner = learners[rev.learnerIdx];
            const bookingId = bookingIdCounter++;
            const totalAmount = mentorData.price * 2; // 2 hours
            const gradeVal = mentorData.gradeName ? `N'${mentorData.gradeName}'` : `NULL`;

            bookingInsertValues.push(`(
    ${bookingId},
    N'${learner.id}', N'${learner.name}',
    ${mentorId}, N'${mentorUserId}', N'Mentor',
    ${mentorData.subjectId}, N'${mentorData.subjectName}', ${gradeVal},
    '${rev.date}', '08:00:00', '10:00:00',
    ${mentorData.price}.00, ${totalAmount}.00,
    N'ONLINE', N'https://meet.google.com/abc-defg-hij', NULL,
    N'COMPLETED', N'Buổi học hoàn thành xuất sắc', NULL, NULL,
    '${rev.date} 07:00:00', '${rev.date} 10:00:00', NULL
)`);

            reviewRecords.push({
                bookingId,
                studentUserId: learner.id,
                studentName: learner.name,
                mentorId,
                rating: rev.rating,
                comment: rev.comment,
                createdAt: `${rev.date} 11:00:00 +00:00`,
                tags: rev.tags
            });
        }
    }

    // Split booking inserts into batches
    const batchSize = 25;
    for (let i = 0; i < bookingInsertValues.length; i += batchSize) {
        const batch = bookingInsertValues.slice(i, i + batchSize);
        mssqlLines.push(`INSERT INTO mm_booking.dbo.bookings (`);
        mssqlLines.push(`    id, student_user_id, student_name, mentor_id, mentor_user_id, mentor_name,`);
        mssqlLines.push(`    mentor_subject_id, subject_name, grade_name, booking_date, start_time, end_time,`);
        mssqlLines.push(`    price_per_hour, total_amount, meeting_type, meeting_link, meeting_address,`);
        mssqlLines.push(`    status, note, cancelled_by, cancel_reason, created_at, updated_at, hold_expires_at`);
        mssqlLines.push(`) VALUES`);
        mssqlLines.push(batch.join(',\n') + ';');
        mssqlLines.push(``);
    }

    mssqlLines.push(`SET IDENTITY_INSERT mm_booking.dbo.bookings OFF;`);
    mssqlLines.push(``);

    // Insert reviews into mm_review
    mssqlLines.push(`USE mm_review;`);
    mssqlLines.push(``);
    mssqlLines.push(`SET IDENTITY_INSERT mm_review.dbo.reviews ON;`);
    mssqlLines.push(``);

    const reviewInsertValues = [];
    let reviewIdCounter = 1;
    const tagInsertValues = [];

    for (const r of reviewRecords) {
        const reviewId = reviewIdCounter++;
        reviewInsertValues.push(`(
    ${reviewId}, ${r.bookingId}, N'${r.studentUserId}', N'${r.studentName}',
    ${r.mentorId}, ${r.rating}, N'${r.comment.replace(/'/g, "''")}',
    '${r.createdAt}', '${r.createdAt}'
)`);

        for (const tagId of r.tags) {
            tagInsertValues.push(`(${reviewId}, ${tagId}, '${r.createdAt}')`);
        }
    }

    for (let i = 0; i < reviewInsertValues.length; i += batchSize) {
        const batch = reviewInsertValues.slice(i, i + batchSize);
        mssqlLines.push(`INSERT INTO mm_review.dbo.reviews (`);
        mssqlLines.push(`    id, booking_id, student_user_id, student_name, mentor_id, rating, comment, created_at, updated_at`);
        mssqlLines.push(`) VALUES`);
        mssqlLines.push(batch.join(',\n') + ';');
        mssqlLines.push(``);
    }

    mssqlLines.push(`SET IDENTITY_INSERT mm_review.dbo.reviews OFF;`);
    mssqlLines.push(``);

    // Insert review tags
    for (let i = 0; i < tagInsertValues.length; i += 40) {
        const batch = tagInsertValues.slice(i, i + 40);
        mssqlLines.push(`INSERT INTO mm_review.dbo.review_tags (`);
        mssqlLines.push(`    review_id, review_tag_option_id, created_at`);
        mssqlLines.push(`) VALUES`);
        mssqlLines.push(batch.join(',\n') + ';');
        mssqlLines.push(``);
    }

    fs.writeFileSync('scripts/seed_bookings_reviews.sql', mssqlLines.join('\n'), 'utf8');
    console.log(`Generated scripts/seed_bookings_reviews.sql with ${reviewRecords.length} reviews and bookings.`);

    // Calculate rating summaries for MySQL update
    const mentorStats = {};
    for (const r of reviewRecords) {
        if (!mentorStats[r.mentorId]) {
            mentorStats[r.mentorId] = { total: 0, sum: 0 };
        }
        mentorStats[r.mentorId].total++;
        mentorStats[r.mentorId].sum += r.rating;
    }

    const mysqlLines = [];
    mysqlLines.push(`-- Update mentor_profiles average_rating and total_reviews matching mm_review`);
    mysqlLines.push(`USE mm_mentor;`);
    mysqlLines.push(`SET NAMES utf8mb4;`);
    mysqlLines.push(``);

    for (const [mIdStr, stats] of Object.entries(mentorStats)) {
        const avg = (stats.sum / stats.total).toFixed(1);
        mysqlLines.push(`UPDATE mentor_profiles SET average_rating = ${avg}, total_reviews = ${stats.total} WHERE id = ${mIdStr};`);
    }

    fs.writeFileSync('scripts/update_mentor_ratings.sql', mysqlLines.join('\n'), 'utf8');
    console.log(`Generated scripts/update_mentor_ratings.sql for ${Object.keys(mentorStats).length} mentors.`);
}

main();

