package com.mentormatching.user.dto;

import com.mentormatching.user.entity.LearnerGender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Dữ liệu thay thế hồ sơ học viên; null xóa giá trị hiện tại của trường tương ứng.
 */
@Builder
public record UpsertCurrentLearnerProfileCommand(
        LearnerGender gender,
        @Min(1900) Integer birthYear,
        @Size(max = 255) String schoolName,
        @Positive Long gradeId,
        @Size(max = 2000) String learningGoal) {
}
