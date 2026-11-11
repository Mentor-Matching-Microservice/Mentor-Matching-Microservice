package com.mentormatching.mentor.dto.command;

import java.time.LocalDate;

import com.mentormatching.mentor.entity.AchievementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** ID null tạo thành tích; ID có giá trị thay thế các trường mentor được phép khai báo. */
@Builder
public record UpsertCurrentMentorAchievementCommand(
        @Positive Long id,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull AchievementType achievementType,
        @Size(max = 255) String issuer,
        LocalDate achievedAt,
        String proofUrl,
        Long proofMediaId) {

    public UpsertCurrentMentorAchievementCommand {
        title = title == null ? null : title.trim();
    }

    public UpsertCurrentMentorAchievementCommand withId(Long newId) {
        return new UpsertCurrentMentorAchievementCommand(newId, title, description, achievementType, issuer, achievedAt, proofUrl, proofMediaId);
    }

    @Override
    public String toString() {
        return "UpsertCurrentMentorAchievementCommand[redacted]";
    }
}
