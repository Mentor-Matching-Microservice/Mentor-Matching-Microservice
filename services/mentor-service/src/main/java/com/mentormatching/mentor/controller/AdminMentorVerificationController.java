package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.ReviewMentorVerificationCommand;
import com.mentormatching.mentor.dto.query.GetAdminMentorVerificationsQuery;
import com.mentormatching.mentor.dto.response.AdminMentorVerificationDetailResponse;
import com.mentormatching.mentor.dto.response.AdminMentorVerificationItemResponse;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorVerificationReviewResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.usecase.GetAdminMentorVerificationDetailUseCase;
import com.mentormatching.mentor.usecase.GetAdminMentorVerificationsUseCase;
import com.mentormatching.mentor.usecase.ReviewMentorVerificationUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin tra cứu và review KYC mentor. */
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/mentor-verifications")
public class AdminMentorVerificationController {

    private final GetAdminMentorVerificationsUseCase getAdminMentorVerificationsUseCase;
    private final GetAdminMentorVerificationDetailUseCase getAdminMentorVerificationDetailUseCase;
    private final ReviewMentorVerificationUseCase reviewMentorVerificationUseCase;

    public AdminMentorVerificationController(GetAdminMentorVerificationsUseCase getAdminMentorVerificationsUseCase,
                                             GetAdminMentorVerificationDetailUseCase getAdminMentorVerificationDetailUseCase,
                                             ReviewMentorVerificationUseCase reviewMentorVerificationUseCase) {
        this.getAdminMentorVerificationsUseCase = getAdminMentorVerificationsUseCase;
        this.getAdminMentorVerificationDetailUseCase = getAdminMentorVerificationDetailUseCase;
        this.reviewMentorVerificationUseCase = reviewMentorVerificationUseCase;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminMentorVerificationItemResponse>> list(
            @Valid @ModelAttribute GetAdminMentorVerificationsQuery query) {
        return ApiResponse.success(getAdminMentorVerificationsUseCase.execute(query),
                "Get mentor verifications successfully");
    }

    @GetMapping("/{verificationId}")
    public ApiResponse<AdminMentorVerificationDetailResponse> detail(@PathVariable Long verificationId) {
        return ApiResponse.success(getAdminMentorVerificationDetailUseCase.execute(verificationId),
                "Get mentor verification detail successfully");
    }

    @PatchMapping("/{verificationId}")
    public ApiResponse<MentorVerificationReviewResponse> review(@AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable Long verificationId,
                                                                 @Valid @RequestBody ReviewMentorVerificationCommand command) {
        return ApiResponse.success(reviewMentorVerificationUseCase.execute(UUID.fromString(jwt.getSubject()), verificationId, command),
                "Review mentor verification successfully");
    }
}
