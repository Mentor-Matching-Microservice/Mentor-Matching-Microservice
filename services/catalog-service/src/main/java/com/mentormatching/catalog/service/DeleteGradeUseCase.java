package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.dto.command.DeleteGradeCommand;
import com.mentormatching.catalog.entity.Grade;
import com.mentormatching.catalog.repository.GradeRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DeleteGradeUseCase {
    private final GradeRepository repository;
    private final Validator validator;

    @Transactional
    public void execute(DeleteGradeCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_GRADE", "Invalid input data");
        }
        var entity = repository.findById(command.id())
                .orElseThrow(() -> new ApiException(NOT_FOUND, "GRADE_NOT_FOUND", "Grade not found"));
        repository.delete(entity);
    }
}
