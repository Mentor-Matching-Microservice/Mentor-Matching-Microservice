package com.mentormatching.mentor.usecase;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.client.MediaServiceClient;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminMentorVerificationDetailUseCaseTest {

    @Mock
    MentorVerificationRepository mentorVerificationRepository;

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MediaServiceClient mediaServiceClient;

    GetAdminMentorVerificationDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAdminMentorVerificationDetailUseCase(
                mentorVerificationRepository,
                mentorProfileRepository,
                mediaServiceClient
        );
    }

    @Test
    void execute_resolvesPrivateDeliveryUrls() {
        var verification = new MentorVerification();
        verification.setMentorId(10L);
        verification.setFullName("Nguyen Van A");
        verification.setIdCardFrontUrl("https://cloudinary/original-front.jpg");
        verification.setIdCardFrontMediaId("media-front-123");
        verification.setIdCardBackUrl("https://cloudinary/original-back.jpg");
        verification.setIdCardBackMediaId("media-back-123");
        verification.setVerificationStatus(MentorVerificationStatus.PENDING);

        var profile = new MentorProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setFullName("Nguyen Van A");

        when(mentorVerificationRepository.findById(1L)).thenReturn(Optional.of(verification));
        when(mentorProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(mediaServiceClient.getPrivateDeliveryUrls(anyList())).thenReturn(Map.of(
                "media-front-123", "https://cloudinary/signed-front.jpg",
                "media-back-123", "https://cloudinary/signed-back.jpg"
        ));

        var response = useCase.execute(1L);

        assertThat(response.idCardFrontUrl()).isEqualTo("https://cloudinary/signed-front.jpg");
        assertThat(response.idCardBackUrl()).isEqualTo("https://cloudinary/signed-back.jpg");
        assertThat(response.idCardFrontMediaId()).isEqualTo("media-front-123");
    }
}
