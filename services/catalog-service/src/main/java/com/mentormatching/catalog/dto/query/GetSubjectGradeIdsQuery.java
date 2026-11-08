package com.mentormatching.catalog.dto.query;

import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record GetSubjectGradeIdsQuery(@Positive Long subjectId, @Positive Long gradeId) {
}
