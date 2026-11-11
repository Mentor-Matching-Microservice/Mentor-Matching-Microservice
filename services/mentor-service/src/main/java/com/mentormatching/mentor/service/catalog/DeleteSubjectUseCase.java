package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.command.DeleteSubjectCommand;
import com.mentormatching.mentor.entity.Subject;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DeleteSubjectUseCase {
    private final SubjectRepository repository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final Validator validator;

    @Transactional
    public void execute(DeleteSubjectCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_SUBJECT", "Invalid input data");
        }
        var entity = repository.findById(command.id())
                .orElseThrow(() -> new ApiException(NOT_FOUND, "SUBJECT_NOT_FOUND", "Subject not found"));

        List<SubjectGrade> subjectGrades = subjectGradeRepository.findBySubjectId(entity.getId());
        if (!subjectGrades.isEmpty()) {
            List<Long> subjectGradeIds = subjectGrades.stream().map(SubjectGrade::getId).toList();
            if (mentorSubjectRepository.existsBySubjectGradeIdIn(subjectGradeIds)) {
                throw new ApiException(CONFLICT, "SUBJECT_IN_USE", "Subject cannot be deleted because it is assigned to mentors");
            }
            subjectGradeRepository.deleteAll(subjectGrades);
        }

        repository.delete(entity);
    }
}

