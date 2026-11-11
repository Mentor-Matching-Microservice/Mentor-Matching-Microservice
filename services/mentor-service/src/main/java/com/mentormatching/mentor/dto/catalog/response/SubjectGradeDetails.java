package com.mentormatching.mentor.dto.catalog.response;

import com.mentormatching.mentor.entity.SubjectGrade;
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

