package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.command.DeleteGradeCommand;
import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.GradeRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
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
public class DeleteGradeUseCase {
    private final GradeRepository repository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final Validator validator;

    @Transactional
    public void execute(DeleteGradeCommand command) {
        if (command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_GRADE", "Invalid input data");
        }
        var entity = repository.findById(command.id())
                .orElseThrow(() -> new ApiException(NOT_FOUND, "GRADE_NOT_FOUND", "Grade not found"));

        List<SubjectGrade> subjectGrades = subjectGradeRepository.findByGradeId(entity.getId());
        if (!subjectGrades.isEmpty()) {
            List<Long> subjectGradeIds = subjectGrades.stream().map(SubjectGrade::getId).toList();
            if (mentorSubjectRepository.existsBySubjectGradeIdIn(subjectGradeIds)) {
                throw new ApiException(CONFLICT, "GRADE_IN_USE", "Grade cannot be deleted because it is assigned to mentors");
            }
            subjectGradeRepository.deleteAll(subjectGrades);
        }

        repository.delete(entity);
    }
}
