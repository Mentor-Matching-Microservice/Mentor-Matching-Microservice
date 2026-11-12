package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.ReviewMentorApprovalCommand;
import com.mentormatching.mentor.dto.query.GetAdminMentorsQuery;
import com.mentormatching.mentor.dto.response.AdminMentorApprovalResponse;
import com.mentormatching.mentor.dto.response.AdminMentorDetailResponse;
import com.mentormatching.mentor.dto.response.AdminMentorSummaryResponse;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.usecase.AdminReviewMentorApprovalUseCase;
import com.mentormatching.mentor.usecase.GetAdminMentorDetailUseCase;
import com.mentormatching.mentor.usecase.GetAdminMentorsUseCase;
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

/** Admin tra cứu và xét duyệt hồ sơ Mentor. */
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/mentors")
public class AdminMentorController {

    private final GetAdminMentorsUseCase getAdminMentorsUseCase;
    private final GetAdminMentorDetailUseCase getAdminMentorDetailUseCase;
    private final AdminReviewMentorApprovalUseCase adminReviewMentorApprovalUseCase;

    public AdminMentorController(GetAdminMentorsUseCase getAdminMentorsUseCase,
                                 GetAdminMentorDetailUseCase getAdminMentorDetailUseCase,
                                 AdminReviewMentorApprovalUseCase adminReviewMentorApprovalUseCase) {
        this.getAdminMentorsUseCase = getAdminMentorsUseCase;
        this.getAdminMentorDetailUseCase = getAdminMentorDetailUseCase;
        this.adminReviewMentorApprovalUseCase = adminReviewMentorApprovalUseCase;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminMentorSummaryResponse>> list(
            @Valid @ModelAttribute GetAdminMentorsQuery query) {
        return ApiResponse.success(getAdminMentorsUseCase.execute(query),
                "Get mentors successfully");
    }

    @GetMapping("/{mentorId}")
    public ApiResponse<AdminMentorDetailResponse> detail(@PathVariable Long mentorId) {
        return ApiResponse.success(getAdminMentorDetailUseCase.execute(mentorId),
                "Get mentor detail successfully");
    }

    @PatchMapping("/{mentorId}/approval")
    public ApiResponse<AdminMentorApprovalResponse> reviewApproval(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long mentorId,
            @Valid @RequestBody ReviewMentorApprovalCommand command) {
        return ApiResponse.success(
                adminReviewMentorApprovalUseCase.execute(UUID.fromString(jwt.getSubject()), mentorId, command),
                "Review mentor approval successfully");
    }
}
