package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorGender;
import lombok.Builder;

/**
 * Hồ sơ mentor công khai.
 *
 * @param id ID hồ sơ mentor
 * @param userId UUID tài khoản mentor
 * @param fullName tên hiển thị đã đồng bộ từ user-service
 * @param avatarUrl URL ảnh đại diện
 * @param gender giới tính
 * @param hometownCityId ID thành phố quê quán
 * @param currentDistrictId ID quận/huyện hiện tại
 * @param headline tiêu đề giới thiệu
 * @param introduction phần giới thiệu
 * @param teachingStyle phong cách dạy
 * @param experienceYears số năm kinh nghiệm
 * @param currentPosition vị trí công việc
 * @param workplace nơi làm việc
 * @param education trình độ học vấn
 * @param major chuyên ngành
 * @param meetingType hình thức học
 * @param averageRating điểm đánh giá trung bình
 * @param totalReviews tổng số đánh giá
 * @param createdAt thời điểm tạo hồ sơ
 * @param updatedAt thời điểm cập nhật hồ sơ
 */
@Builder
public record MentorDetailResponse(
        Long id,
        UUID userId,
        String fullName,
        String avatarUrl,
        MentorGender gender,
        Long hometownCityId,
        Long currentDistrictId,
        String headline,
        String introduction,
        String teachingStyle,
        Integer experienceYears,
        String currentPosition,
        String workplace,
        String education,
        String major,
        MeetingType meetingType,
        Double averageRating,
        Long totalReviews,
        Instant createdAt,
        Instant updatedAt) {
}
