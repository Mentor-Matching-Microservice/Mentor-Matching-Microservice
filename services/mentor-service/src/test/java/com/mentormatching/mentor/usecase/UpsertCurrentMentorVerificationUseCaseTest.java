package com.mentormatching.mentor.usecase;

import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.client.MediaServiceClient;
import com.mentormatching.mentor.dto.command.UpsertCurrentMentorVerificationCommand;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpsertCurrentMentorVerificationUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MentorVerificationRepository mentorVerificationRepository;

    @Mock
    MediaServiceClient mediaServiceClient;

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    UpsertCurrentMentorVerificationUseCase useCase;

    final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpsertCurrentMentorVerificationUseCase(
                mentorProfileRepository,
                mentorVerificationRepository,
                mediaServiceClient,
                validator
        );
    }

    private MentorProfile createProfile() {
        var profile = new MentorProfile();
        try {
            var field = MentorProfile.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(profile, 10L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        profile.setUserId(userId);
        return profile;
    }

    @Test
    void execute_validMedia_success() {
        var profile = createProfile();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(mentorVerificationRepository.findByMentorId(profile.getId())).thenReturn(Optional.empty());
        when(mediaServiceClient.verifyAndAttach(eq("media-front"), eq(userId), any())).thenReturn(true);
        when(mediaServiceClient.verifyAndAttach(eq("media-back"), eq(userId), any())).thenReturn(true);
        when(mentorVerificationRepository.saveAndFlush(any(MentorVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = UpsertCurrentMentorVerificationCommand.builder()
                .fullName("Nguyen Van A")
                .idCardNumber("123456789")
                .idCardFrontUrl("https://cloudinary/front.jpg")
                .idCardFrontMediaId("media-front")
                .idCardBackUrl("https://cloudinary/back.jpg")
                .idCardBackMediaId("media-back")
                .build();

        var response = useCase.execute(userId, command);

        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.idCardFrontMediaId()).isEqualTo("media-front");
        assertThat(response.verificationStatus()).isEqualTo(MentorVerificationStatus.PENDING);
        verify(mediaServiceClient).verifyAndAttach(eq("media-front"), eq(userId), any());
    }

    @Test
    void execute_invalidMedia_throws400() {
        var profile = createProfile();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(mentorVerificationRepository.findByMentorId(profile.getId())).thenReturn(Optional.empty());
        when(mediaServiceClient.verifyAndAttach(eq("bad-media"), eq(userId), any())).thenReturn(false);

        var command = UpsertCurrentMentorVerificationCommand.builder()
                .fullName("Nguyen Van A")
                .idCardNumber("123456789")
                .idCardFrontUrl("https://cloudinary/front.jpg")
                .idCardFrontMediaId("bad-media")
                .idCardBackUrl("https://cloudinary/back.jpg")
                .build();

        assertThatThrownBy(() -> useCase.execute(userId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    var apiEx = (ApiException) e;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("INVALID_MEDIA");
                });
    }

    @Test
    void execute_alreadyVerified_throws409() {
        var profile = createProfile();
        var existing = new MentorVerification();
        existing.setVerificationStatus(MentorVerificationStatus.VERIFIED);

        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(mentorVerificationRepository.findByMentorId(profile.getId())).thenReturn(Optional.of(existing));

        var command = UpsertCurrentMentorVerificationCommand.builder()
                .fullName("Nguyen Van A")
                .idCardFrontUrl("https://cloudinary/front.jpg")
                .idCardBackUrl("https://cloudinary/back.jpg")
                .build();

        assertThatThrownBy(() -> useCase.execute(userId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
