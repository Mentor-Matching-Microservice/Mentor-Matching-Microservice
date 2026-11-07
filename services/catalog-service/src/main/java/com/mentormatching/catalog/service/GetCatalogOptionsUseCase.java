package com.mentormatching.catalog.service;

import com.mentormatching.catalog.dto.response.CatalogOptions;
import com.mentormatching.catalog.dto.response.CategoryDetails;
import com.mentormatching.catalog.dto.response.GradeDetails;
import com.mentormatching.catalog.dto.response.SubjectDetails;
import com.mentormatching.catalog.dto.response.SubjectGradeDetails;
import com.mentormatching.catalog.repository.CategoryRepository;
import com.mentormatching.catalog.repository.GradeRepository;
import com.mentormatching.catalog.repository.SubjectGradeRepository;
import com.mentormatching.catalog.repository.SubjectRepository;
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
