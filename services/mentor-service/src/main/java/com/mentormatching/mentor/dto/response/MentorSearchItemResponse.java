package com.mentormatching.mentor.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorGender;
import lombok.Builder;

/**
 * Dữ liệu hiển thị một mentor trong kết quả tìm kiếm, không chứa entity JPA.
 *
 * @param id ID hồ sơ mentor
 * @param userId UUID tài khoản do auth cấp
 * @param fullName bản sao tên từ user-service, có thể null khi chưa đồng bộ
 * @param avatarUrl URL ảnh đại diện
 * @param gender giới tính
 * @param headline tiêu đề giới thiệu
 * @param experienceYears số năm kinh nghiệm
 * @param currentPosition vị trí công việc
 * @param workplace nơi làm việc
 * @param education trình độ học vấn
 * @param major chuyên ngành
 * @param meetingType hình thức học
 * @param hometownCityId ID thành phố quê quán
 * @param currentDistrictId ID quận/huyện hiện tại
 * @param minPrice giá thấp nhất không null của tất cả môn active; null nếu không có giá
 * @param averageRating điểm đánh giá trung bình
 * @param totalReviews tổng số đánh giá
 * @param createdAt thời điểm tạo hồ sơ UTC
 */
@Builder
public record MentorSearchItemResponse(
        Long id,
        UUID userId,
        String fullName,
        String avatarUrl,
        MentorGender gender,
        String headline,
        Integer experienceYears,
        String currentPosition,
        String workplace,
        String education,
        String major,
        MeetingType meetingType,
        Long hometownCityId,
        Long currentDistrictId,
        BigDecimal minPrice,
        Double averageRating,
        Long totalReviews,
        Instant createdAt) {
}
