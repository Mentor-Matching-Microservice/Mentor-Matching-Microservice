package com.mentormatching.catalog.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record CatalogOptions(List<CategoryDetails> categories, List<SubjectDetails> subjects, List<GradeDetails> grades, List<SubjectGradeDetails> subjectGrades) {
}
