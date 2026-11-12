package com.mentormatching.mentor.usecase;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.dto.response.MentorDetailResponse;
import com.mentormatching.mentor.entity.AccountStatus;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMentorDetailUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    GetMentorDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMentorDetailUseCase(mentorProfileRepository);
    }

    @Test
    void execute_success() {
        MentorProfile profile = new MentorProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setFullName("Jane Doe");
        profile.setApprovalStatus(MentorApprovalStatus.APPROVED);
        profile.setAccountStatus(AccountStatus.ACTIVE);
        profile.setMeetingType(MeetingType.ONLINE);
        profile.setAverageRating(4.9);
        profile.setTotalReviews(10L);
        ReflectionTestUtils.setField(profile, "createdAt", Instant.now());
        ReflectionTestUtils.setField(profile, "id", 1L);

        when(mentorProfileRepository.findOne(any(Specification.class))).thenReturn(Optional.of(profile));

        MentorDetailResponse response = useCase.execute(1L);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.averageRating()).isEqualTo(4.9);
        assertThat(response.totalReviews()).isEqualTo(10L);
    }

    @Test
    void execute_notFound_throws404() {
        when(mentorProfileRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiEx.getCode()).isEqualTo("MENTOR_PROFILE_NOT_FOUND");
                });
    }

    @Test
    void execute_invalidId_throws400() {
        assertThatThrownBy(() -> useCase.execute(-1L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
