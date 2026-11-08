package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.exception.CatalogWriteErrors;
import com.mentormatching.catalog.dto.command.CreateGradeCommand;
import com.mentormatching.catalog.dto.response.GradeDetails;
import com.mentormatching.catalog.entity.Grade;
import com.mentormatching.catalog.repository.GradeRepository;
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
            // Detect this unique-constraint violation before leaving the use case transaction.
            return GradeDetails.from(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException error) {
            throw CatalogWriteErrors.duplicateName(error, "uk_grades_name",
                    "GRADE_NAME_ALREADY_EXISTS", "Grade name already exists");
        }
    }
}
