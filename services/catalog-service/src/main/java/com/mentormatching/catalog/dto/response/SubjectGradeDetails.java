package com.mentormatching.catalog.dto.response;

import com.mentormatching.catalog.entity.SubjectGrade;
import lombok.Builder;

@Builder
public record SubjectGradeDetails(Long id, Long subjectId, Long gradeId) {
    public static SubjectGradeDetails from(SubjectGrade entity) {
        return SubjectGradeDetails.builder()
                .id(entity.getId())
                .subjectId(entity.getSubjectId())
                .gradeId(entity.getGradeId())
                .build();
    }
}
