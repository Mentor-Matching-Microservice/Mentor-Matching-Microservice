package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.exception.CatalogWriteErrors;
import com.mentormatching.catalog.dto.command.CreateSubjectCommand;
import com.mentormatching.catalog.dto.response.SubjectDetails;
import com.mentormatching.catalog.entity.Category;
import com.mentormatching.catalog.entity.Subject;
import com.mentormatching.catalog.repository.CategoryRepository;
import com.mentormatching.catalog.repository.SubjectRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CreateSubjectUseCase {
    private final SubjectRepository repository;
    private final Validator validator;
    private final CategoryRepository categories;

    @Transactional
    public SubjectDetails execute(CreateSubjectCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_SUBJECT", "Invalid input data");
        }
        if (repository.existsByNameIgnoreCase(command.name())) {
            throw new ApiException(CONFLICT, "SUBJECT_NAME_ALREADY_EXISTS", "Subject name already exists");
        }
        if (!categories.existsById(command.categoryId())) {
            throw new ApiException(NOT_FOUND, "CATEGORY_NOT_FOUND", "Category not found");
        }
        var entity = new Subject();
        entity.setName(command.name());
        entity.setCategoryId(command.categoryId());
        entity.setDescription(command.description());
        try {
            // Detect this unique-constraint violation before leaving the use case transaction.
            return SubjectDetails.from(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException error) {
            throw CatalogWriteErrors.duplicateName(error, "uk_subjects_name",
                    "SUBJECT_NAME_ALREADY_EXISTS", "Subject name already exists");
        }
    }
}
