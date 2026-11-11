package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.MentorRatingSyncRequest;
import com.mentormatching.mentor.dto.command.UserProfileSyncRequest;
import com.mentormatching.mentor.dto.response.AccountStatusesResponse;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorBookingContextResponse;
import com.mentormatching.mentor.dto.response.MentorProfileSummary;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.service.ApplyAccountStatusesService;
import com.mentormatching.mentor.service.ApplyMentorRatingService;
import com.mentormatching.mentor.service.ApplyUserProfileService;
import com.mentormatching.mentor.usecase.GetMentorBookingContextUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/mentors")
public class InternalMentorController {

    private final GetMentorBookingContextUseCase getMentorBookingContextUseCase;
    private final MentorProfileRepository mentorProfileRepository;
    private final ApplyUserProfileService applyUserProfileService;
    private final ApplyAccountStatusesService applyAccountStatusesService;
    private final ApplyMentorRatingService applyMentorRatingService;

    @PutMapping("/{mentorId}/rating")
    public ApiResponse<Void> updateRating(
            @PathVariable Long mentorId,
            @Valid @RequestBody MentorRatingSyncRequest request) {
        applyMentorRatingService.apply(mentorId, request.averageRating(), request.totalReviews(), request.version());
        return ApiResponse.success(null, "Mentor rating updated successfully");
    }

    @GetMapping("/{mentorId}/booking-context")
    public MentorBookingContextResponse getBookingContext(
            @PathVariable Long mentorId,
            @RequestParam Long mentorSubjectId) {
        return getMentorBookingContextUseCase.execute(mentorId, mentorSubjectId);
    }

    @GetMapping("/by-user/{userId}")
    public MentorProfileSummary getMentorByUserId(@PathVariable String userId) {
        UUID parsedUserId;
        try {
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid user UUID format.");
        }

        MentorProfile profile = mentorProfileRepository.findByUserId(parsedUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        return MentorProfileSummary.builder()
                .mentorId(profile.getId())
                .mentorUserId(profile.getUserId().toString())
                .fullName(profile.getFullName())
                .build();
    }

    @PutMapping("/users/{userId}/profile-sync")
    public ApiResponse<Integer> syncUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody UserProfileSyncRequest request) {
        UUID parsedUserId;
        try {
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid user UUID format.");
        }

        int updated = applyUserProfileService.execute(
                parsedUserId,
                request.fullName(),
                request.avatarUrl(),
                request.avatarMediaId()
        );
        return ApiResponse.success(updated, "Profile sync completed successfully");
    }

    @PutMapping("/account-statuses")
    public ApiResponse<Integer> syncAccountStatuses(@Valid @RequestBody AccountStatusesResponse request) {
        int updated = applyAccountStatusesService.execute(request);
        return ApiResponse.success(updated, "Account statuses updated successfully");
    }
}

