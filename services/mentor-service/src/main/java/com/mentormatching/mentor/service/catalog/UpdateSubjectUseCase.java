package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.command.UpdateSubjectCommand;
import com.mentormatching.mentor.dto.catalog.response.SubjectDetails;
import com.mentormatching.mentor.entity.Subject;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.exception.CatalogWriteErrors;
import com.mentormatching.mentor.repository.CategoryRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
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
public class UpdateSubjectUseCase {
    private final SubjectRepository repository;
    private final Validator validator;
    private final CategoryRepository categories;

    @Transactional
    public SubjectDetails execute(UpdateSubjectCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_SUBJECT", "Invalid input data");
        }
        var entity = repository.findById(command.id())
                .orElseThrow(() -> new ApiException(NOT_FOUND, "SUBJECT_NOT_FOUND", "Subject not found"));
        if (repository.existsByNameIgnoreCaseAndIdNot(command.name(), command.id())) {
            throw new ApiException(CONFLICT, "SUBJECT_NAME_ALREADY_EXISTS", "Subject name already exists");
        }
        if (!categories.existsById(command.categoryId())) {
            throw new ApiException(NOT_FOUND, "CATEGORY_NOT_FOUND", "Category not found");
        }
        entity.setName(command.name());
        entity.setCategoryId(command.categoryId());
        entity.setDescription(command.description());
        try {
            return SubjectDetails.from(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException error) {
            throw CatalogWriteErrors.duplicateName(error, "uk_subjects_name",
                    "SUBJECT_NAME_ALREADY_EXISTS", "Subject name already exists");
        }
    }
}

