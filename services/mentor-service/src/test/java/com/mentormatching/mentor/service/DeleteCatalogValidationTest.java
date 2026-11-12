package com.mentormatching.mentor.service;

import com.mentormatching.mentor.dto.catalog.command.DeleteGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.DeleteSubjectCommand;
import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.entity.GradeLevelGroup;
import com.mentormatching.mentor.entity.Subject;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.GradeRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
import com.mentormatching.mentor.service.catalog.DeleteGradeUseCase;
import com.mentormatching.mentor.service.catalog.DeleteSubjectUseCase;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCatalogValidationTest {

    @Mock
    SubjectRepository subjectRepository;
    @Mock
    GradeRepository gradeRepository;
    @Mock
    SubjectGradeRepository subjectGradeRepository;
    @Mock
    MentorSubjectRepository mentorSubjectRepository;

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    DeleteSubjectUseCase deleteSubjectUseCase;
    DeleteGradeUseCase deleteGradeUseCase;

    @BeforeEach
    void setUp() {
        deleteSubjectUseCase = new DeleteSubjectUseCase(
                subjectRepository, subjectGradeRepository, mentorSubjectRepository, validator);
        deleteGradeUseCase = new DeleteGradeUseCase(
                gradeRepository, subjectGradeRepository, mentorSubjectRepository, validator);
    }

    @Test
    void deleteSubjectThrows409ConflictWhenAssignedToMentors() {
        Subject subject = new Subject();
        ReflectionTestUtils.setField(subject, "id", 10L);
        subject.setName("Mathematics");

        SubjectGrade sg = new SubjectGrade();
        ReflectionTestUtils.setField(sg, "id", 101L);
        sg.setSubjectId(10L);
        sg.setGradeId(1L);

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(subjectGradeRepository.findBySubjectId(10L)).thenReturn(List.of(sg));
        when(mentorSubjectRepository.existsBySubjectGradeIdIn(List.of(101L))).thenReturn(true);

        assertThatThrownBy(() -> deleteSubjectUseCase.execute(new DeleteSubjectCommand(10L)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getCode()).isEqualTo("SUBJECT_IN_USE");
                });

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteSubjectDeletesWhenNotAssignedToMentors() {
        Subject subject = new Subject();
        ReflectionTestUtils.setField(subject, "id", 10L);
        subject.setName("Mathematics");

        SubjectGrade sg = new SubjectGrade();
        ReflectionTestUtils.setField(sg, "id", 101L);
        sg.setSubjectId(10L);
        sg.setGradeId(1L);

        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(subjectGradeRepository.findBySubjectId(10L)).thenReturn(List.of(sg));
        when(mentorSubjectRepository.existsBySubjectGradeIdIn(List.of(101L))).thenReturn(false);

        deleteSubjectUseCase.execute(new DeleteSubjectCommand(10L));

        verify(subjectGradeRepository).deleteAll(List.of(sg));
        verify(subjectRepository).delete(subject);
    }

    @Test
    void deleteGradeThrows409ConflictWhenAssignedToMentors() {
        Grade grade = new Grade();
        ReflectionTestUtils.setField(grade, "id", 12L);
        grade.setName("Grade 12");
        grade.setLevelGroup(GradeLevelGroup.HIGH_SCHOOL);

        SubjectGrade sg = new SubjectGrade();
        ReflectionTestUtils.setField(sg, "id", 201L);
        sg.setSubjectId(5L);
        sg.setGradeId(12L);

        when(gradeRepository.findById(12L)).thenReturn(Optional.of(grade));
        when(subjectGradeRepository.findByGradeId(12L)).thenReturn(List.of(sg));
        when(mentorSubjectRepository.existsBySubjectGradeIdIn(List.of(201L))).thenReturn(true);

        assertThatThrownBy(() -> deleteGradeUseCase.execute(new DeleteGradeCommand(12L)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getCode()).isEqualTo("GRADE_IN_USE");
                });

        verify(gradeRepository, never()).delete(any());
    }

    @Test
    void deleteGradeDeletesWhenNotAssignedToMentors() {
        Grade grade = new Grade();
        ReflectionTestUtils.setField(grade, "id", 12L);
        grade.setName("Grade 12");
        grade.setLevelGroup(GradeLevelGroup.HIGH_SCHOOL);

        SubjectGrade sg = new SubjectGrade();
        ReflectionTestUtils.setField(sg, "id", 201L);
        sg.setSubjectId(5L);
        sg.setGradeId(12L);

        when(gradeRepository.findById(12L)).thenReturn(Optional.of(grade));
        when(subjectGradeRepository.findByGradeId(12L)).thenReturn(List.of(sg));
        when(mentorSubjectRepository.existsBySubjectGradeIdIn(List.of(201L))).thenReturn(false);

        deleteGradeUseCase.execute(new DeleteGradeCommand(12L));

        verify(subjectGradeRepository).deleteAll(List.of(sg));
        verify(gradeRepository).delete(grade);
    }
}
