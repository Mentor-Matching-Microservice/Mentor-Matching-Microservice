package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.command.CreateGradeCommand;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.exception.CatalogWriteErrors;
import com.mentormatching.mentor.repository.GradeRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CreateGradeUseCase {
    private final GradeRepository repository;
    private final Validator validator;

    @Transactional
    public GradeDetails execute(CreateGradeCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_GRADE", "Invalid input data");
        }
        if (repository.existsByNameIgnoreCase(command.name())) {
            throw new ApiException(CONFLICT, "GRADE_NAME_ALREADY_EXISTS", "Grade name already exists");
        }
        var entity = new Grade();
        entity.setName(command.name());
        entity.setLevelGroup(command.levelGroup());
        try {
            return GradeDetails.from(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException error) {
            throw CatalogWriteErrors.duplicateName(error, "uk_grades_name",
                    "GRADE_NAME_ALREADY_EXISTS", "Grade name already exists");
        }
    }
}

