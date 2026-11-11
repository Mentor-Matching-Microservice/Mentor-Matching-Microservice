package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GetGradeDetailUseCase {
    private final GradeRepository repository;

    @Transactional(readOnly = true)
    public GradeDetails execute(Long gradeId) {
        if (gradeId == null || gradeId <= 0) {
            throw new ApiException(BAD_REQUEST, "INVALID_ID", "A positive ID is required");
        }
        return repository.findById(gradeId)
                .map(GradeDetails::from)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "GRADE_NOT_FOUND", "Grade not found"));
    }
}

