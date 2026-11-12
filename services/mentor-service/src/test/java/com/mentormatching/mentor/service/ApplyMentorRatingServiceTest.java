package com.mentormatching.mentor.service;

import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyMentorRatingServiceTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @InjectMocks
    ApplyMentorRatingService service;

    @Test
    void apply_success() {
        when(mentorProfileRepository.applyMentorRating(1L, 4.8, 15L, 2L)).thenReturn(1);

        boolean result = service.apply(1L, 4.8, 15L, 2L);

        assertThat(result).isTrue();
        verify(mentorProfileRepository).applyMentorRating(1L, 4.8, 15L, 2L);
    }

    @Test
    void apply_olderVersion_returnsFalse() {
        when(mentorProfileRepository.applyMentorRating(1L, 4.8, 15L, 2L)).thenReturn(0);

        boolean result = service.apply(1L, 4.8, 15L, 2L);

        assertThat(result).isFalse();
    }

    @Test
    void apply_invalidMentorId_returnsFalseWithoutQuery() {
        assertThat(service.apply(null, 4.8, 15L, 2L)).isFalse();
        assertThat(service.apply(0L, 4.8, 15L, 2L)).isFalse();
        assertThat(service.apply(-5L, 4.8, 15L, 2L)).isFalse();
        verifyNoInteractions(mentorProfileRepository);
    }
}
