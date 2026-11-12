package com.mentormatching.mentor.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.dto.response.MentorSearchItemResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.SubjectGrade;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorPriceProjection;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.SubjectGradeRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchMentorsUseCaseTest {

    @Mock
    MentorProfileRepository mentorProfileRepository;

    @Mock
    MentorSubjectRepository mentorSubjectRepository;

    @Mock
    SubjectGradeRepository subjectGradeRepository;

    @Mock
    Validator validator;

    SearchMentorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchMentorsUseCase(
                mentorProfileRepository,
                mentorSubjectRepository,
                subjectGradeRepository,
                validator
        );
    }

    @Test
    void execute_success_withSubjectAndGrade() {
        SearchMentorsQuery query = SearchMentorsQuery.builder()
                .search("John")
                .meetingType(MeetingType.ONLINE)
                .cityId(1L)
                .districtId(2L)
                .subjectId(10L)
                .gradeId(20L)
                .page(0)
                .size(10)
                .sortBy("averageRating")
                .sortDirection("DESC")
                .build();

        when(validator.validate(query)).thenReturn(Collections.emptySet());

        SubjectGrade sg = new SubjectGrade(10L, 20L);
        ReflectionTestUtils.setField(sg, "id", 100L);
        when(subjectGradeRepository.findBySubjectIdAndGradeId(10L, 20L)).thenReturn(Optional.of(sg));

        MentorProfile profile = new MentorProfile();
        profile.setUserId(UUID.randomUUID());
        profile.setFullName("John Doe");
        profile.setApprovalStatus(MentorApprovalStatus.APPROVED);
        profile.setMeetingType(MeetingType.ONLINE);
        profile.setAverageRating(4.85);
        profile.setTotalReviews(25L);
        ReflectionTestUtils.setField(profile, "createdAt", Instant.now());
        ReflectionTestUtils.setField(profile, "id", 1L);

        when(mentorProfileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(profile)));

        MentorPriceProjection priceProj = mock(MentorPriceProjection.class);
        when(priceProj.getMentorId()).thenReturn(1L);
        when(priceProj.getMinPrice()).thenReturn(new BigDecimal("150000.00"));
        when(mentorSubjectRepository.findMinimumActivePrices(List.of(1L))).thenReturn(List.of(priceProj));

        PageResponse<MentorSearchItemResponse> response = useCase.execute(query);

        assertThat(response).isNotNull();
        assertThat(response.data()).hasSize(1);
        MentorSearchItemResponse item = response.data().get(0);
        assertThat(item.fullName()).isEqualTo("John Doe");
        assertThat(item.averageRating()).isEqualTo(4.85);
        assertThat(item.totalReviews()).isEqualTo(25L);
        assertThat(item.minPrice()).isEqualByComparingTo("150000.00");
    }

    @Test
    void execute_invalidMinMaxPrice_throws400() {
        SearchMentorsQuery query = SearchMentorsQuery.builder()
                .minPrice(new BigDecimal("500000.00"))
                .maxPrice(new BigDecimal("100000.00"))
                .page(0)
                .size(10)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();

        when(validator.validate(query)).thenReturn(Collections.emptySet());

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid mentor search query");
    }
}
