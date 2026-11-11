package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.query.GetSubjectGradeIdsQuery;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class GetSubjectGradeIdsUseCase {
    private final SubjectGradeRepository repository;
    private final Validator validator;

    @Transactional(readOnly = true)
    public List<Long> execute(GetSubjectGradeIdsQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_CATALOG_QUERY", "Invalid input data");
        }
        if (query.subjectId() != null) {
            return repository.findBySubjectId(query.subjectId()).stream()
                    .filter(sg -> query.gradeId() == null || query.gradeId().equals(sg.getGradeId()))
                    .map(SubjectGrade::getId)
                    .toList();
        }
        if (query.gradeId() != null) {
            return repository.findByGradeId(query.gradeId()).stream()
                    .map(SubjectGrade::getId)
                    .toList();
        }
        return List.of();
    }
}

