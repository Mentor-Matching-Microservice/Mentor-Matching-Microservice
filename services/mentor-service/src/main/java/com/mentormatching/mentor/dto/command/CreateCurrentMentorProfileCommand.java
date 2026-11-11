package com.mentormatching.mentor.dto.command;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorGender;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Lệnh khởi tạo hồ sơ mentor nháp cho tài khoản.
 */
@Builder
public record CreateCurrentMentorProfileCommand(
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
        MeetingType meetingType,
        String avatarUrl,
        Long avatarMediaId
) {
}

