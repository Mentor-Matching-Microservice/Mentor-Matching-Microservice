package com.mentormatching.mentor.usecase;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.ReviewMentorApprovalAction;
import com.mentormatching.mentor.dto.command.ReviewMentorApprovalCommand;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.entity.OutboxEvent;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import com.mentormatching.mentor.repository.OutboxEventRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReviewMentorApprovalUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MentorVerificationRepository mentorVerificationRepository;

    @Mock
    OutboxEventRepository outboxEventRepository;

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    ObjectMapper objectMapper = new ObjectMapper();

    AdminReviewMentorApprovalUseCase useCase;

    final UUID adminId = UUID.randomUUID();
    final UUID mentorUserId = UUID.randomUUID();
    final Long mentorId = 1L;

    @BeforeEach
    void setUp() {
        useCase = new AdminReviewMentorApprovalUseCase(
                mentorProfileRepository,
                mentorVerificationRepository,
                outboxEventRepository,
                validator,
                objectMapper
        );
    }

    private MentorProfile createProfile(MentorApprovalStatus status) throws Exception {
        var profile = new MentorProfile();
        Field idField = MentorProfile.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(profile, mentorId);
        profile.setUserId(mentorUserId);
        profile.setApprovalStatus(status);
        return profile;
    }

    private MentorVerification createVerification(MentorVerificationStatus status) {
        var verification = new MentorVerification();
        verification.setMentorId(mentorId);
        verification.setVerificationStatus(status);
        return verification;
    }

    @Test
    void approve_pendingWithKycVerified_successAndPublishesOutbox() throws Exception {
        var profile = createProfile(MentorApprovalStatus.PENDING);
        var verification = createVerification(MentorVerificationStatus.VERIFIED);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));
        when(mentorVerificationRepository.findByMentorId(mentorId)).thenReturn(Optional.of(verification));
        when(mentorProfileRepository.saveAndFlush(any(MentorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.APPROVE, null);
        var response = useCase.execute(adminId, mentorId, command);

        assertThat(response.approvalStatus()).isEqualTo(MentorApprovalStatus.APPROVED);
        assertThat(response.approvedBy()).isEqualTo(adminId);
        assertThat(response.approvedAt()).isNotNull();

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        var outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventType()).isEqualTo("MentorApproved");
        assertThat(outbox.getAggregateId()).isEqualTo(mentorId.toString());
        assertThat(outbox.getPayload()).contains(mentorUserId.toString());
    }

    @Test
    void approve_pendingWithKycPending_failsHardRule_throws400() throws Exception {
        var profile = createProfile(MentorApprovalStatus.PENDING);
        var verification = createVerification(MentorVerificationStatus.PENDING);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));
        when(mentorVerificationRepository.findByMentorId(mentorId)).thenReturn(Optional.of(verification));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.APPROVE, null);

        assertThatThrownBy(() -> useCase.execute(adminId, mentorId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    var apiEx = (ApiException) e;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("KYC_NOT_VERIFIED");
                });
    }

    @Test
    void approve_pendingWithNoKyc_failsHardRule_throws400() throws Exception {
        var profile = createProfile(MentorApprovalStatus.PENDING);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));
        when(mentorVerificationRepository.findByMentorId(mentorId)).thenReturn(Optional.empty());

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.APPROVE, null);

        assertThatThrownBy(() -> useCase.execute(adminId, mentorId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    var apiEx = (ApiException) e;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("KYC_NOT_VERIFIED");
                });
    }

    @Test
    void approve_notPending_throws409() throws Exception {
        var profile = createProfile(MentorApprovalStatus.DRAFT);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.APPROVE, null);

        assertThatThrownBy(() -> useCase.execute(adminId, mentorId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reject_pendingWithReason_successAndPublishesOutbox() throws Exception {
        var profile = createProfile(MentorApprovalStatus.PENDING);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));
        when(mentorProfileRepository.saveAndFlush(any(MentorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.REJECT, "Incomplete documentation");
        var response = useCase.execute(adminId, mentorId, command);

        assertThat(response.approvalStatus()).isEqualTo(MentorApprovalStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Incomplete documentation");

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        var outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventType()).isEqualTo("MentorRejected");
        assertThat(outbox.getPayload()).contains("Incomplete documentation");
    }

    @Test
    void reject_missingReason_throws400() throws Exception {
        var profile = createProfile(MentorApprovalStatus.PENDING);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.REJECT, "   ");

        assertThatThrownBy(() -> useCase.execute(adminId, mentorId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    var apiEx = (ApiException) e;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("REJECTION_REASON_REQUIRED");
                });
    }

    @Test
    void suspend_approvedMentor_success() throws Exception {
        var profile = createProfile(MentorApprovalStatus.APPROVED);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));
        when(mentorProfileRepository.saveAndFlush(any(MentorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.SUSPEND, null);
        var response = useCase.execute(adminId, mentorId, command);

        assertThat(response.approvalStatus()).isEqualTo(MentorApprovalStatus.SUSPENDED);
    }

    @Test
    void reactivate_suspendedMentor_success() throws Exception {
        var profile = createProfile(MentorApprovalStatus.SUSPENDED);

        when(mentorProfileRepository.findByIdForReview(mentorId)).thenReturn(Optional.of(profile));
        when(mentorProfileRepository.saveAndFlush(any(MentorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new ReviewMentorApprovalCommand(ReviewMentorApprovalAction.REACTIVATE, null);
        var response = useCase.execute(adminId, mentorId, command);

        assertThat(response.approvalStatus()).isEqualTo(MentorApprovalStatus.APPROVED);
    }
}
