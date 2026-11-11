package com.mentormatching.mentor.usecase;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorSubjectCommand;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorSubject;
import com.mentormatching.mentor.entity.ProficiencyLevel;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpsertCurrentMentorSubjectUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MentorSubjectRepository mentorSubjectRepository;

    @Mock
    SubjectGradeRepository subjectGradeRepository;

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    UpsertCurrentMentorSubjectUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpsertCurrentMentorSubjectUseCase(
                mentorProfileRepository,
                mentorSubjectRepository,
                subjectGradeRepository,
                validator
        );
    }

    @Test
    void execute_createsSubject_whenValid() {
        UpsertCurrentMentorSubjectCommand command = UpsertCurrentMentorSubjectCommand.builder()
                .subjectGradeId(10L)
                .proficiencyLevel(ProficiencyLevel.INTERMEDIATE)
                .pricePerHour(new BigDecimal("200000.00"))
                .active(true)
                .build();

        MentorProfile profile = new MentorProfile();
        ReflectionTestUtils.setField(profile, "id", 1L);

        when(subjectGradeRepository.existsById(10L)).thenReturn(true);
        when(mentorProfileRepository.findByUserIdForSubjectUpdate(userId)).thenReturn(Optional.of(profile));
        when(mentorSubjectRepository.existsByMentorIdAndSubjectGradeId(1L, 10L)).thenReturn(false);
        when(mentorSubjectRepository.save(any(MentorSubject.class))).thenAnswer(inv -> {
            MentorSubject ms = inv.getArgument(0);
            ReflectionTestUtils.setField(ms, "id", 50L);
            return ms;
        });

        MentorSubjectDetails details = useCase.execute(userId, command);

        assertThat(details).isNotNull();
        assertThat(details.id()).isEqualTo(50L);
        assertThat(details.subjectGradeId()).isEqualTo(10L);
        assertThat(details.pricePerHour()).isEqualByComparingTo("200000.00");
        verify(mentorSubjectRepository).save(any(MentorSubject.class));
    }

    @Test
    void execute_throwsNotFound_whenSubjectGradeDoesNotExist() {
        UpsertCurrentMentorSubjectCommand command = UpsertCurrentMentorSubjectCommand.builder()
                .subjectGradeId(999L)
                .proficiencyLevel(ProficiencyLevel.INTERMEDIATE)
                .pricePerHour(new BigDecimal("200000.00"))
                .active(true)
                .build();

        when(subjectGradeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(userId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getCode()).isEqualTo("SUBJECT_GRADE_NOT_FOUND");
                });
    }

    @Test
    void execute_throwsBadRequest_whenPricePerHourIsZeroOrNegative() {
        UpsertCurrentMentorSubjectCommand command = UpsertCurrentMentorSubjectCommand.builder()
                .subjectGradeId(10L)
                .proficiencyLevel(ProficiencyLevel.INTERMEDIATE)
                .pricePerHour(BigDecimal.ZERO)
                .active(true)
                .build();

        assertThatThrownBy(() -> useCase.execute(userId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("INVALID_PRICE");
                });
    }
}
