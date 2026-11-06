package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.dto.command.DeleteSubjectCommand;
import com.mentormatching.catalog.entity.Subject;
import com.mentormatching.catalog.repository.SubjectRepository;
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
public class DeleteSubjectUseCase {
    private final SubjectRepository repository;
    private final Validator validator;

    @Transactional
    public void execute(DeleteSubjectCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_SUBJECT", "Invalid input data");
        }
        var entity = repository.findById(command.id())
                .orElseThrow(() -> new ApiException(NOT_FOUND, "SUBJECT_NOT_FOUND", "Subject not found"));
        repository.delete(entity);
    }
}
