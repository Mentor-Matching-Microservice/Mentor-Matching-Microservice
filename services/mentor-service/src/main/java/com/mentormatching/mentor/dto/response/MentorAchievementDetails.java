package com.mentormatching.mentor.dto.response;

import java.time.LocalDate;

import com.mentormatching.mentor.entity.AchievementType;
import lombok.Builder;

/** Thông tin thành tích mentor đã khai cùng trạng thái xác minh. */
@Builder
public record MentorAchievementDetails(
        Long id,
        String title,
        String description,
        AchievementType achievementType,
        String issuer,
        LocalDate achievedAt,
        String proofUrl,
        Boolean verified) {

    @Override
    public String toString() {
        return "MentorAchievementDetails[redacted]";
    }
}
