package com.mentormatching.mentor.dto.command;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorGender;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Các trường mentor được phép thay thế trong hồ sơ của mình.
 * Giá trị null xóa giá trị cũ của trường tương ứng khi cập nhật.
 *
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
 */
@Builder
public record UpdateCurrentMentorProfileCommand(
        MentorGender gender,
        @Positive Long hometownCityId,
        @Positive Long currentDistrictId,
        @Size(max = 255) String headline,
        String introduction,
        String teachingStyle,
        @PositiveOrZero Integer experienceYears,
        @Size(max = 255) String currentPosition,
        @Size(max = 255) String workplace,
        @Size(max = 255) String education,
        @Size(max = 255) String major,
        MeetingType meetingType) {
}
