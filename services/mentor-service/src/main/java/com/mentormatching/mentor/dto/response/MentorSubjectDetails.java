package com.mentormatching.mentor.dto.response;

import java.math.BigDecimal;

import com.mentormatching.mentor.entity.ProficiencyLevel;
import lombok.Builder;

/** Dữ liệu môn dạy do mentor-service sở hữu; tên môn/lớp thuộc catalog-service. */
@Builder
public record MentorSubjectDetails(
        Long id,
        Long mentorId,
        Long subjectGradeId,
        ProficiencyLevel proficiencyLevel,
        String teachingNote,
        BigDecimal pricePerHour,
        Boolean active) {
}
