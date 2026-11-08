package com.mentormatching.booking.controller;

import com.mentormatching.booking.dto.ApiResponse;
import com.mentormatching.booking.dto.request.CreateMentorAvailabilityRequest;
import com.mentormatching.booking.dto.request.UpdateMentorAvailabilityRequest;
import com.mentormatching.booking.dto.response.MentorAvailabilityResponse;
import com.mentormatching.booking.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduling")
@RequiredArgsConstructor
@Validated
public class SchedulingController {

    private final SchedulingService schedulingService;

    @GetMapping("/me/availabilities")
    public ResponseEntity<ApiResponse<List<MentorAvailabilityResponse>>> getCurrentMentorAvailabilities(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long mentorId) {
        String userId = resolveUserId(jwt);
        List<MentorAvailabilityResponse> responses = schedulingService.getCurrentMentorAvailabilities(userId, mentorId);
        return ResponseEntity.ok(ApiResponse.ok(responses, "Get current mentor availabilities successfully"));
    }

    @PostMapping("/me/availabilities")
    public ResponseEntity<ApiResponse<MentorAvailabilityResponse>> createCurrentMentorAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMentorAvailabilityRequest request) {
        String userId = resolveUserId(jwt);
        MentorAvailabilityResponse response = schedulingService.createCurrentMentorAvailability(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Create current mentor availability successfully"));
    }

    @PutMapping("/me/availabilities/{availabilityId}")
    public ResponseEntity<ApiResponse<Void>> updateCurrentMentorAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long availabilityId,
            @Valid @RequestBody UpdateMentorAvailabilityRequest request) {
        String userId = resolveUserId(jwt);
        schedulingService.updateCurrentMentorAvailability(userId, availabilityId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Update current mentor availability successfully"));
    }

    @DeleteMapping("/me/availabilities/{availabilityId}")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentMentorAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long availabilityId) {
        String userId = resolveUserId(jwt);
        schedulingService.deleteCurrentMentorAvailability(userId, availabilityId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Delete current mentor availability successfully"));
    }

    private String resolveUserId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "anonymous";
    }
}
