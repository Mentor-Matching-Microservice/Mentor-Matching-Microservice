package com.mentormatching.mentor.usecase;

import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitMentorProfileUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MentorSubjectRepository mentorSubjectRepository;

    @Mock
    MentorVerificationRepository mentorVerificationRepository;

    SubmitMentorProfileUseCase useCase;

    final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SubmitMentorProfileUseCase(
                mentorProfileRepository,
                mentorSubjectRepository,
                mentorVerificationRepository
        );
    }

    private MentorProfile createValidProfile() {
        var profile = new MentorProfile();
        profile.setUserId(userId);
        profile.setHeadline("Math Expert");
        profile.setIntroduction("Experienced tutor with 5 years experience");
        profile.setMeetingType(MeetingType.HYBRID);
        profile.setApprovalStatus(MentorApprovalStatus.DRAFT);
        return profile;
    }

    @Test
    void submit_eligible_success() {
        var profile = createValidProfile();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(mentorSubjectRepository.countByMentorIdAndActiveTrue(profile.getId())).thenReturn(2L);
        when(mentorVerificationRepository.findByMentorId(profile.getId())).thenReturn(Optional.of(new MentorVerification()));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = useCase.execute(userId);

        assertThat(response.approvalStatus()).isEqualTo(MentorApprovalStatus.PENDING);
        assertThat(profile.getApprovalStatus()).isEqualTo(MentorApprovalStatus.PENDING);
        verify(mentorProfileRepository).save(profile);
    }

    @Test
    void submit_missingHeadline_throws400() {
        var profile = createValidProfile();
        profile.setHeadline(null);
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void submit_noActiveSubjects_throws400() {
        var profile = createValidProfile();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(mentorSubjectRepository.countByMentorIdAndActiveTrue(profile.getId())).thenReturn(0L);

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void submit_noKyc_throws400() {
        var profile = createValidProfile();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(mentorSubjectRepository.countByMentorIdAndActiveTrue(profile.getId())).thenReturn(1L);
        when(mentorVerificationRepository.findByMentorId(profile.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void submit_alreadyPending_throws409() {
        var profile = createValidProfile();
        profile.setApprovalStatus(MentorApprovalStatus.PENDING);
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void submit_alreadyApproved_throws409() {
        var profile = createValidProfile();
        profile.setApprovalStatus(MentorApprovalStatus.APPROVED);
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> useCase.execute(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
