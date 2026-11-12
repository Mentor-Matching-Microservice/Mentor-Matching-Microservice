package com.mentormatching.mentor.dto.catalog.response;

import lombok.Builder;

@Builder
public record SubjectGradeSummary(
        Long subjectGradeId,
        Long subjectId,
        String subjectName,
        Long gradeId,
        String gradeName
) {
}

