package com.mentormatching.catalog.service;

import com.mentormatching.catalog.dto.response.GradeDetails;
import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.repository.GradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class GetGradeDetailUseCase {

    private final GradeRepository gradeRepository;

    public GetGradeDetailUseCase(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    @Transactional(readOnly = true)
    public GradeDetails execute(Long gradeId) {
        if (gradeId == null || gradeId <= 0) {
            throw new ApiException(BAD_REQUEST, "INVALID_ID", "A positive ID is required");
        }
        return gradeRepository.findById(gradeId)
                .map(GradeDetails::from)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "GRADE_NOT_FOUND", "Grade not found"));
    }
}
