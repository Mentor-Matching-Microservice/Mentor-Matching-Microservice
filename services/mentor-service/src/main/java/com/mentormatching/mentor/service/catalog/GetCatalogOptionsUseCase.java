package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.response.CatalogOptions;
import com.mentormatching.mentor.dto.catalog.response.CategoryDetails;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectGradeDetails;
import com.mentormatching.mentor.repository.CategoryRepository;
import com.mentormatching.mentor.repository.GradeRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCatalogOptionsUseCase {
    private final CategoryRepository categories;
    private final SubjectRepository subjects;
    private final GradeRepository grades;
    private final SubjectGradeRepository subjectGrades;

    @Transactional(readOnly = true)
    public CatalogOptions execute() {
        return CatalogOptions.builder()
                .categories(categories.findAll().stream().map(CategoryDetails::from).toList())
                .subjects(subjects.findAll().stream().map(SubjectDetails::from).toList())
                .grades(grades.findAll().stream().map(GradeDetails::from).toList())
                .subjectGrades(subjectGrades.findAll().stream().map(SubjectGradeDetails::from).toList())
                .build();
    }
}
