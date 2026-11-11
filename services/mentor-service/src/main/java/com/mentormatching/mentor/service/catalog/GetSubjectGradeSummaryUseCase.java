package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.response.SubjectGradeSummary;
import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.GradeRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GetSubjectGradeSummaryUseCase {
    private final SubjectGradeRepository repository;
    private final SubjectRepository subjects;
    private final GradeRepository grades;

    @Transactional(readOnly = true)
    public SubjectGradeSummary execute(Long subjectGradeId) {
        if (subjectGradeId == null || subjectGradeId <= 0) {
            throw new ApiException(BAD_REQUEST, "INVALID_ID", "A positive ID is required");
        }
        var mapping = repository.findById(subjectGradeId)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "SUBJECT_GRADE_NOT_FOUND", "Subject grade not found"));
        var subject = subjects.findById(mapping.getSubjectId())
                .orElseThrow(() -> new ApiException(NOT_FOUND, "SUBJECT_NOT_FOUND", "Subject not found"));
        var gradeName = mapping.getGradeId() == null ? null : grades.findById(mapping.getGradeId())
                .map(Grade::getName).orElse(null);
        return SubjectGradeSummary.builder()
                .subjectGradeId(mapping.getId())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .gradeId(mapping.getGradeId())
                .gradeName(gradeName)
                .build();
    }
}
