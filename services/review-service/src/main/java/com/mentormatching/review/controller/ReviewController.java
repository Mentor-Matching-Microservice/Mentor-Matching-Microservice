package com.mentormatching.review.controller;

import com.mentormatching.review.dto.ApiResponse;
import com.mentormatching.review.dto.request.CreateReviewRequest;
import com.mentormatching.review.dto.request.UpdateReviewRequest;
import com.mentormatching.review.dto.response.MentorRatingSummaryResponse;
import com.mentormatching.review.dto.response.ReviewDetailResponse;
import com.mentormatching.review.dto.response.ReviewTagOptionResponse;
import com.mentormatching.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReviewRequest request) {
        String studentUserId = jwt != null ? jwt.getSubject() : null;
        ReviewDetailResponse response = reviewService.createReview(studentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Create review successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        String studentUserId = jwt != null ? jwt.getSubject() : null;
        ReviewDetailResponse response = reviewService.updateReview(studentUserId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Update review successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        String currentUserId = jwt != null ? jwt.getSubject() : null;
        boolean isAdminOrManager = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        reviewService.deleteReview(currentUserId, id, isAdminOrManager);
        return ResponseEntity.ok(ApiResponse.ok(null, "Delete review successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReviewDetailResponse>>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt) {
        String studentUserId = jwt != null ? jwt.getSubject() : null;
        List<ReviewDetailResponse> reviews = reviewService.getMyReviews(studentUserId);
        return ResponseEntity.ok(ApiResponse.ok(reviews, "Get my reviews successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDetailResponse>> getReviewDetail(@PathVariable Long id) {
        ReviewDetailResponse response = reviewService.getReviewDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get review detail successfully"));
    }

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<ApiResponse<Page<ReviewDetailResponse>>> getMentorReviews(
            @PathVariable Long mentorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), sort);
        Page<ReviewDetailResponse> reviews = reviewService.getMentorReviews(mentorId, pageRequest);
        return ResponseEntity.ok(ApiResponse.ok(reviews, "Get mentor reviews successfully"));
    }

    @GetMapping("/mentor/{mentorId}/summary")
    public ResponseEntity<ApiResponse<MentorRatingSummaryResponse>> getMentorRatingSummary(
            @PathVariable Long mentorId) {
        MentorRatingSummaryResponse response = reviewService.calculateMentorRatingSummary(mentorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get mentor rating summary successfully"));
    }

    @GetMapping("/tag-options")
    public ResponseEntity<ApiResponse<List<ReviewTagOptionResponse>>> getTagOptions() {
        List<ReviewTagOptionResponse> options = reviewService.getTagOptions();
        return ResponseEntity.ok(ApiResponse.ok(options, "Get review tag options successfully"));
    }
}
