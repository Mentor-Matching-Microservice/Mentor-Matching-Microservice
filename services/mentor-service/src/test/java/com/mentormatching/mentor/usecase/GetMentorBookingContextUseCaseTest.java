package com.mentormatching.mentor.usecase;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.dto.response.MentorBookingContextResponse;
import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorSubject;
import com.mentormatching.mentor.entity.Subject;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.GradeRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import com.mentormatching.mentor.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMentorBookingContextUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MentorSubjectRepository mentorSubjectRepository;

    @Mock
    SubjectGradeRepository subjectGradeRepository;

    @Mock
    SubjectRepository subjectRepository;

    @Mock
    GradeRepository gradeRepository;

    GetMentorBookingContextUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMentorBookingContextUseCase(
                mentorProfileRepository,
                mentorSubjectRepository,
                subjectGradeRepository,
                subjectRepository,
                gradeRepository
        );
    }

    @Test
    void execute_success() {
        UUID userId = UUID.randomUUID();
        MentorProfile profile = new MentorProfile();
        profile.setUserId(userId);
        profile.setFullName("John Doe");
        profile.setMeetingType(MeetingType.HYBRID);
        ReflectionTestUtils.setField(profile, "id", 1L);

        MentorSubject subject = new MentorSubject();
        subject.setMentorId(1L);
        subject.setSubjectGradeId(10L);
        subject.setPricePerHour(new BigDecimal("200000.00"));
        subject.setActive(true);
        ReflectionTestUtils.setField(subject, "id", 100L);

        SubjectGrade sg = new SubjectGrade(20L, 30L);
        ReflectionTestUtils.setField(sg, "id", 10L);

        Subject s = new Subject();
        s.setName("Math");
        ReflectionTestUtils.setField(s, "id", 20L);

        Grade g = new Grade();
        g.setName("Grade 12");
        g.setLevelGroup(com.mentormatching.mentor.entity.GradeLevelGroup.HIGH_SCHOOL);
        ReflectionTestUtils.setField(g, "id", 30L);

        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(mentorSubjectRepository.findByIdAndMentorId(100L, 1L)).thenReturn(Optional.of(subject));
        when(subjectGradeRepository.findById(10L)).thenReturn(Optional.of(sg));
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(s));
        when(gradeRepository.findById(30L)).thenReturn(Optional.of(g));

        MentorBookingContextResponse result = useCase.execute(1L, 100L);

        assertThat(result).isNotNull();
        assertThat(result.mentorId()).isEqualTo(1L);
        assertThat(result.mentorUserId()).isEqualTo(userId.toString());
        assertThat(result.mentorName()).isEqualTo("John Doe");
        assertThat(result.meetingType()).isEqualTo("HYBRID");
        assertThat(result.mentorSubjectId()).isEqualTo(100L);
        assertThat(result.subjectName()).isEqualTo("Math");
        assertThat(result.gradeName()).isEqualTo("Grade 12");
        assertThat(result.pricePerHour()).isEqualByComparingTo("200000.00");
        assertThat(result.active()).isTrue();
    }

    @Test
    void execute_mentorNotFound_throws404() {
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L, 100L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getCode()).isEqualTo("MENTOR_PROFILE_NOT_FOUND");
                });
    }

    @Test
    void execute_invalidParams_throws400() {
        assertThatThrownBy(() -> useCase.execute(-1L, 100L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
