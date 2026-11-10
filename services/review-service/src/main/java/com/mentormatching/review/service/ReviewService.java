package com.mentormatching.review.service;

import com.mentormatching.review.client.BookingLookupClient;
import com.mentormatching.review.client.UserLookupClient;
import com.mentormatching.review.client.dto.BookingReviewEligibilityResponse;
import com.mentormatching.review.dto.request.CreateReviewRequest;
import com.mentormatching.review.dto.request.UpdateReviewRequest;
import com.mentormatching.review.dto.response.MentorRatingSummaryResponse;
import com.mentormatching.review.dto.response.ReviewDetailResponse;
import com.mentormatching.review.dto.response.ReviewTagOptionResponse;
import com.mentormatching.review.entity.Review;
import com.mentormatching.review.entity.ReviewTag;
import com.mentormatching.review.entity.ReviewTagOption;
import com.mentormatching.review.exception.ApiException;
import com.mentormatching.review.repository.ReviewRepository;
import com.mentormatching.review.repository.ReviewTagOptionRepository;
import com.mentormatching.review.repository.ReviewTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewTagOptionRepository reviewTagOptionRepository;
    private final ReviewTagRepository reviewTagRepository;
    private final BookingLookupClient bookingLookupClient;
    private final UserLookupClient userLookupClient;
    private final ReviewOutboxService reviewOutboxService;
    private final Clock clock;

    @Transactional
    public ReviewDetailResponse createReview(String currentUserId, CreateReviewRequest request) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw ApiException.badRequest("Authentication required");
        }

        validateRating(request.getRating());

        // Check if booking has already been reviewed
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw ApiException.conflict("This booking has already been reviewed");
        }

        // Verify booking eligibility via internal booking-service client
        BookingReviewEligibilityResponse eligibility = bookingLookupClient.getReviewEligibility(request.getBookingId());
        if (eligibility == null || !eligibility.eligibleForReview()) {
            throw ApiException.badRequest("Only completed bookings can be reviewed");
        }

        if (!currentUserId.equals(eligibility.studentUserId())) {
            throw ApiException.forbidden("You are not authorized to review this booking");
        }

        String studentName = userLookupClient.getUserFullName(currentUserId);

        Review review = Review.builder()
                .bookingId(request.getBookingId())
                .studentUserId(currentUserId)
                .studentName(studentName)
                .mentorId(eligibility.mentorId())
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .build();

        Review savedReview = reviewRepository.save(review);

        // Save selected tags
        List<ReviewTagOptionResponse> tagResponses = saveTags(savedReview.getId(), request.getTagOptionIds());

        // Recalculate mentor rating summary and emit Outbox events
        MentorRatingSummaryResponse summary = calculateMentorRatingSummary(savedReview.getMentorId());
        reviewOutboxService.recordReviewCreated(
                savedReview.getId(),
                savedReview.getBookingId(),
                savedReview.getStudentUserId(),
                savedReview.getMentorId(),
                savedReview.getRating()
        );
        reviewOutboxService.recordMentorRatingUpdated(
                savedReview.getMentorId(),
                summary.averageRating(),
                summary.totalReviews()
        );

        return ReviewDetailResponse.from(savedReview, tagResponses);
    }

    @Transactional
    public ReviewDetailResponse updateReview(String currentUserId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        if (!review.getStudentUserId().equals(currentUserId)) {
            throw ApiException.forbidden("You are not authorized to update this review");
        }

        // Author can only update within 30 days of creation
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (review.getCreatedAt() != null && review.getCreatedAt().plusDays(30).isBefore(now)) {
            throw ApiException.badRequest("Reviews can only be updated within 30 days of creation");
        }

        validateRating(request.getRating());

        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);
        Review updatedReview = reviewRepository.save(review);

        // Update tags
        if (request.getTagOptionIds() != null) {
            reviewTagRepository.deleteByReviewId(reviewId);
        }
        List<ReviewTagOptionResponse> tagResponses = saveTags(reviewId, request.getTagOptionIds());

        // Recalculate summary and emit Outbox event
        MentorRatingSummaryResponse summary = calculateMentorRatingSummary(updatedReview.getMentorId());
        reviewOutboxService.recordMentorRatingUpdated(
                updatedReview.getMentorId(),
                summary.averageRating(),
                summary.totalReviews()
        );

        return ReviewDetailResponse.from(updatedReview, tagResponses);
    }

    @Transactional
    public void deleteReview(String currentUserId, Long reviewId, boolean isAdminOrManager) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        if (!isAdminOrManager && !review.getStudentUserId().equals(currentUserId)) {
            throw ApiException.forbidden("You do not have permission to delete this review");
        }

        Long mentorId = review.getMentorId();
        reviewTagRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);

        // Recalculate summary and emit Outbox event
        MentorRatingSummaryResponse summary = calculateMentorRatingSummary(mentorId);
        reviewOutboxService.recordMentorRatingUpdated(
                mentorId,
                summary.averageRating(),
                summary.totalReviews()
        );
    }

    @Transactional(readOnly = true)
    public ReviewDetailResponse getReviewDetail(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));

        List<ReviewTagOptionResponse> tags = getTagsForReview(reviewId);
        return ReviewDetailResponse.from(review, tags);
    }

    @Transactional(readOnly = true)
    public List<ReviewDetailResponse> getMyReviews(String studentUserId) {
        List<Review> reviews = reviewRepository.findByStudentUserIdOrderByCreatedAtDesc(studentUserId);
        return mapReviewsWithTags(reviews);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDetailResponse> getMentorReviews(Long mentorId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByMentorId(mentorId, pageable);
        List<Long> reviewIds = reviewPage.getContent().stream().map(Review::getId).toList();
        Map<Long, List<ReviewTagOptionResponse>> tagsByReview = getTagsForReviews(reviewIds);

        return reviewPage.map(r -> ReviewDetailResponse.from(r, tagsByReview.getOrDefault(r.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public MentorRatingSummaryResponse calculateMentorRatingSummary(Long mentorId) {
        List<Review> reviews = reviewRepository.findByMentorId(mentorId);

        long totalReviews = reviews.size();
        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        double roundedAverage = Math.round(average * 10.0) / 10.0;

        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        Map<Integer, Long> fullDistribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            fullDistribution.put(i, distribution.getOrDefault(i, 0L));
        }

        return MentorRatingSummaryResponse.builder()
                .mentorId(mentorId)
                .averageRating(roundedAverage)
                .totalReviews(totalReviews)
                .ratingDistribution(fullDistribution)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReviewTagOptionResponse> getTagOptions() {
        return reviewTagOptionRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(ReviewTagOptionResponse::from)
                .toList();
    }

    private List<ReviewTagOptionResponse> saveTags(Long reviewId, List<Long> tagOptionIds) {
        if (tagOptionIds == null || tagOptionIds.isEmpty()) {
            return List.of();
        }

        List<ReviewTagOption> validOptions = reviewTagOptionRepository.findAllById(tagOptionIds);
        List<ReviewTag> tagsToSave = validOptions.stream()
                .map(opt -> ReviewTag.builder()
                        .reviewId(reviewId)
                        .reviewTagOptionId(opt.getId())
                        .build())
                .toList();

        reviewTagRepository.saveAll(tagsToSave);
        return validOptions.stream().map(ReviewTagOptionResponse::from).toList();
    }

    private List<ReviewTagOptionResponse> getTagsForReview(Long reviewId) {
        List<ReviewTag> tags = reviewTagRepository.findByReviewId(reviewId);
        List<Long> optionIds = tags.stream().map(ReviewTag::getReviewTagOptionId).toList();
        return reviewTagOptionRepository.findAllById(optionIds).stream()
                .map(ReviewTagOptionResponse::from)
                .toList();
    }

    private Map<Long, List<ReviewTagOptionResponse>> getTagsForReviews(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        List<ReviewTag> tags = reviewTagRepository.findByReviewIdIn(reviewIds);
        Set<Long> optionIds = tags.stream().map(ReviewTag::getReviewTagOptionId).collect(Collectors.toSet());
        Map<Long, ReviewTagOptionResponse> optionMap = reviewTagOptionRepository.findAllById(optionIds).stream()
                .collect(Collectors.toMap(ReviewTagOption::getId, ReviewTagOptionResponse::from));

        Map<Long, List<ReviewTagOptionResponse>> result = new HashMap<>();
        for (ReviewTag tag : tags) {
            ReviewTagOptionResponse opt = optionMap.get(tag.getReviewTagOptionId());
            if (opt != null) {
                result.computeIfAbsent(tag.getReviewId(), k -> new ArrayList<>()).add(opt);
            }
        }
        return result;
    }

    private List<ReviewDetailResponse> mapReviewsWithTags(List<Review> reviews) {
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewTagOptionResponse>> tagsByReview = getTagsForReviews(reviewIds);

        return reviews.stream()
                .map(r -> ReviewDetailResponse.from(r, tagsByReview.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    private void validateRating(Integer rating) {
        if (rating == null) {
            throw ApiException.badRequest("Rating is required");
        }
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("Rating must be between 1 and 5");
        }
    }
}
