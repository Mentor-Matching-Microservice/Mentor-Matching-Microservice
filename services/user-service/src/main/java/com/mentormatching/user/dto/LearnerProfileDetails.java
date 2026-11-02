package com.mentormatching.user.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.user.entity.LearnerGender;

/** Dữ liệu hồ sơ học viên; các trường ngoài userId có thể null khi chưa tạo hồ sơ. */
@Builder
public record LearnerProfileDetails(UUID id, UUID userId, LearnerGender gender, Integer birthYear,
                                    String schoolName, Long gradeId, String learningGoal,
                                    Instant createdAt, Instant updatedAt) {
}
