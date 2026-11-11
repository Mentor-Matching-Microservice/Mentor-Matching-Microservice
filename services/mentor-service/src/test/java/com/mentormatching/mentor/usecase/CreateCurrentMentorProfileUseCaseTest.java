package com.mentormatching.mentor.usecase;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.CreateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.response.MentorProfileDetails;
import com.mentormatching.mentor.entity.MentorGender;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
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
class CreateCurrentMentorProfileUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    CreateCurrentMentorProfileUseCase useCase;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CreateCurrentMentorProfileUseCase(mentorProfileRepository, validator);
    }

    @Test
    void execute_createsDraftProfileWhenNoneExists() {
        CreateCurrentMentorProfileCommand command = CreateCurrentMentorProfileCommand.builder()
                .gender(MentorGender.MALE)
                .headline("Math Teacher")
                .introduction("Hello there")
                .build();

        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(invocation -> {
            MentorProfile profile = invocation.getArgument(0);
            ReflectionTestUtils.setField(profile, "id", 100L);
            return profile;
        });

        MentorProfileDetails result = useCase.execute(userId, command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.headline()).isEqualTo("Math Teacher");
        verify(mentorProfileRepository).save(any(MentorProfile.class));
    }

    @Test
    void execute_throwsConflictWhenProfileAlreadyExists() {
        CreateCurrentMentorProfileCommand command = CreateCurrentMentorProfileCommand.builder().build();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(new MentorProfile()));

        assertThatThrownBy(() -> useCase.execute(userId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiEx.getCode()).isEqualTo("PROFILE_ALREADY_EXISTS");
                });
    }

    @Test
    void execute_throwsBadRequestWhenUserIdNull() {
        assertThatThrownBy(() -> useCase.execute(null, null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}

