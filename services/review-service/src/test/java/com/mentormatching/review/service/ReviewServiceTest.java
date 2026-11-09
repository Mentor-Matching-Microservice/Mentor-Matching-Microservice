package com.mentormatching.review.service;

import com.mentormatching.review.client.BookingLookupClient;
import com.mentormatching.review.client.UserLookupClient;
import com.mentormatching.review.client.dto.BookingReviewEligibilityResponse;
import com.mentormatching.review.dto.request.CreateReviewRequest;
import com.mentormatching.review.dto.request.UpdateReviewRequest;
import com.mentormatching.review.dto.response.MentorRatingSummaryResponse;
import com.mentormatching.review.dto.response.ReviewDetailResponse;
import com.mentormatching.review.entity.Review;
import com.mentormatching.review.entity.ReviewTagOption;
import com.mentormatching.review.exception.ApiException;
import com.mentormatching.review.repository.ReviewRepository;
import com.mentormatching.review.repository.ReviewTagOptionRepository;
import com.mentormatching.review.repository.ReviewTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewTagOptionRepository reviewTagOptionRepository;
    private ReviewTagRepository reviewTagRepository;
    private BookingLookupClient bookingLookupClient;
    private UserLookupClient userLookupClient;
    private ReviewOutboxService reviewOutboxService;
    private ReviewService reviewService;
    private Clock fixedClock;

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        reviewTagOptionRepository = mock(ReviewTagOptionRepository.class);
        reviewTagRepository = mock(ReviewTagRepository.class);
        bookingLookupClient = mock(BookingLookupClient.class);
        userLookupClient = mock(UserLookupClient.class);
        reviewOutboxService = mock(ReviewOutboxService.class);
        fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);

        reviewService = new ReviewService(
                reviewRepository,
                reviewTagOptionRepository,
                reviewTagRepository,
                bookingLookupClient,
                userLookupClient,
                reviewOutboxService,
                fixedClock
        );
    }

    @Test
    void createReviewSucceedsForEligibleCompletedBooking() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(10L)
                .rating(5)
                .comment("Excellent session!")
                .tagOptionIds(List.of(1L, 2L))
                .build();

        BookingReviewEligibilityResponse eligibility = BookingReviewEligibilityResponse.builder()
                .bookingId(10L)
                .studentUserId("student-123")
                .mentorId(5L)
                .status("COMPLETED")
                .eligibleForReview(true)
                .build();

        when(reviewRepository.existsByBookingId(10L)).thenReturn(false);
        when(bookingLookupClient.getReviewEligibility(10L)).thenReturn(eligibility);
        when(userLookupClient.getUserFullName("student-123")).thenReturn("Nguyen Van A");

        ReviewTagOption tag1 = ReviewTagOption.builder().id(1L).name("Nhiệt tình").build();
        ReviewTagOption tag2 = ReviewTagOption.builder().id(2L).name("Đúng giờ").build();
        when(reviewTagOptionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(tag1, tag2));

        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(OffsetDateTime.now(fixedClock));
            r.setUpdatedAt(OffsetDateTime.now(fixedClock));
            return r;
        });

        ReviewDetailResponse response = reviewService.createReview("student-123", request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(5, response.rating());
        assertEquals("Nguyen Van A", response.studentName());
        assertEquals(2, response.tags().size());

        verify(reviewOutboxService).recordReviewCreated(eq(100L), eq(10L), eq("student-123"), eq(5L), eq(5));
        verify(reviewOutboxService).recordMentorRatingUpdated(eq(5L), anyDouble(), anyLong());
    }

    @Test
    void createReviewFailsWhenBookingNotCompleted() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(11L)
                .rating(4)
                .build();

        BookingReviewEligibilityResponse eligibility = BookingReviewEligibilityResponse.builder()
                .bookingId(11L)
                .studentUserId("student-123")
                .status("PENDING")
                .eligibleForReview(false)
                .build();

        when(reviewRepository.existsByBookingId(11L)).thenReturn(false);
        when(bookingLookupClient.getReviewEligibility(11L)).thenReturn(eligibility);

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview("student-123", request));
        assertEquals("Only completed bookings can be reviewed", ex.getMessage());
    }

    @Test
    void createReviewFailsWhenUserNotAuthorized() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(12L)
                .rating(4)
                .build();

        BookingReviewEligibilityResponse eligibility = BookingReviewEligibilityResponse.builder()
                .bookingId(12L)
                .studentUserId("different-student")
                .eligibleForReview(true)
                .build();

        when(reviewRepository.existsByBookingId(12L)).thenReturn(false);
        when(bookingLookupClient.getReviewEligibility(12L)).thenReturn(eligibility);

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview("student-123", request));
        assertEquals("You are not authorized to review this booking", ex.getMessage());
    }

    @Test
    void createReviewFailsWhenAlreadyReviewed() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(13L)
                .rating(5)
                .build();

        when(reviewRepository.existsByBookingId(13L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview("student-123", request));
        assertEquals("This booking has already been reviewed", ex.getMessage());
    }

    @Test
    void createReviewFailsWhenRatingInvalid() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(14L)
                .rating(6)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.createReview("student-123", request));
        assertEquals("Rating must be between 1 and 5", ex.getMessage());
    }

    @Test
    void updateReviewSucceedsWithin30Days() {
        Review review = Review.builder()
                .id(200L)
                .bookingId(20L)
                .studentUserId("student-123")
                .mentorId(5L)
                .rating(4)
                .comment("Good")
                .createdAt(OffsetDateTime.now(fixedClock).minusDays(10))
                .build();

        when(reviewRepository.findById(200L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .rating(5)
                .comment("Actually excellent!")
                .build();

        ReviewDetailResponse response = reviewService.updateReview("student-123", 200L, request);

        assertEquals(5, response.rating());
        assertEquals("Actually excellent!", response.comment());
        verify(reviewOutboxService).recordMentorRatingUpdated(eq(5L), anyDouble(), anyLong());
    }

    @Test
    void updateReviewFailsAfter30Days() {
        Review review = Review.builder()
                .id(201L)
                .studentUserId("student-123")
                .mentorId(5L)
                .createdAt(OffsetDateTime.now(fixedClock).minusDays(31))
                .build();

        when(reviewRepository.findById(201L)).thenReturn(Optional.of(review));

        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .rating(5)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.updateReview("student-123", 201L, request));
        assertEquals("Reviews can only be updated within 30 days of creation", ex.getMessage());
    }

    @Test
    void updateReviewFailsForNonOwner() {
        Review review = Review.builder()
                .id(202L)
                .studentUserId("original-owner")
                .createdAt(OffsetDateTime.now(fixedClock).minusDays(5))
                .build();

        when(reviewRepository.findById(202L)).thenReturn(Optional.of(review));

        UpdateReviewRequest request = UpdateReviewRequest.builder()
                .rating(5)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.updateReview("hacker", 202L, request));
        assertEquals("You are not authorized to update this review", ex.getMessage());
    }

    @Test
    void deleteReviewSucceedsForOwner() {
        Review review = Review.builder()
                .id(300L)
                .studentUserId("student-123")
                .mentorId(5L)
                .build();

        when(reviewRepository.findById(300L)).thenReturn(Optional.of(review));

        assertDoesNotThrow(() -> reviewService.deleteReview("student-123", 300L, false));

        verify(reviewTagRepository).deleteByReviewId(300L);
        verify(reviewRepository).delete(review);
        verify(reviewOutboxService).recordMentorRatingUpdated(eq(5L), anyDouble(), anyLong());
    }

    @Test
    void deleteReviewSucceedsForAdmin() {
        Review review = Review.builder()
                .id(301L)
                .studentUserId("student-123")
                .mentorId(5L)
                .build();

        when(reviewRepository.findById(301L)).thenReturn(Optional.of(review));

        assertDoesNotThrow(() -> reviewService.deleteReview("admin-user", 301L, true));

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReviewFailsForUnauthorizedUser() {
        Review review = Review.builder()
                .id(302L)
                .studentUserId("student-123")
                .build();

        when(reviewRepository.findById(302L)).thenReturn(Optional.of(review));

        ApiException ex = assertThrows(ApiException.class, () ->
                reviewService.deleteReview("different-user", 302L, false));
        assertEquals("You do not have permission to delete this review", ex.getMessage());
    }

    @Test
    void calculateMentorRatingSummaryRoundsToOneDecimalAndBuildsDistribution() {
        Review r1 = Review.builder().rating(5).build();
        Review r2 = Review.builder().rating(4).build();
        Review r3 = Review.builder().rating(5).build();

        when(reviewRepository.findByMentorId(5L)).thenReturn(List.of(r1, r2, r3));

        MentorRatingSummaryResponse summary = reviewService.calculateMentorRatingSummary(5L);

        assertEquals(5L, summary.mentorId());
        assertEquals(3L, summary.totalReviews());
        // Average is (5 + 4 + 5) / 3 = 4.666... -> rounded to 4.7
        assertEquals(4.7, summary.averageRating());
        assertEquals(2L, summary.ratingDistribution().get(5));
        assertEquals(1L, summary.ratingDistribution().get(4));
        assertEquals(0L, summary.ratingDistribution().get(3));
        assertEquals(0L, summary.ratingDistribution().get(2));
        assertEquals(0L, summary.ratingDistribution().get(1));
    }
}
