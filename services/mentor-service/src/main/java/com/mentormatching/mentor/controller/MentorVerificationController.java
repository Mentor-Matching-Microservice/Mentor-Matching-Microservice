package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorVerificationCommand;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.CurrentMentorVerificationResponse;
import com.mentormatching.mentor.dto.response.CurrentMentorVerificationStatusResponse;
import com.mentormatching.mentor.service.GetCurrentMentorVerificationStatusUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorVerificationUseCase;
import com.mentormatching.mentor.usecase.UpsertCurrentMentorVerificationUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** KYC của ứng viên hoặc mentor đang đăng nhập. */
@RestController
@RequestMapping("/api/v1/mentors/me/verification")
public class MentorVerificationController {

    private final GetCurrentMentorVerificationStatusUseCase getCurrentMentorVerificationStatusUseCase;
    private final GetCurrentMentorVerificationUseCase getCurrentMentorVerificationUseCase;
    private final UpsertCurrentMentorVerificationUseCase upsertCurrentMentorVerificationUseCase;

    public MentorVerificationController(GetCurrentMentorVerificationStatusUseCase getCurrentMentorVerificationStatusUseCase,
                                        GetCurrentMentorVerificationUseCase getCurrentMentorVerificationUseCase,
                                        UpsertCurrentMentorVerificationUseCase upsertCurrentMentorVerificationUseCase) {
        this.getCurrentMentorVerificationStatusUseCase = getCurrentMentorVerificationStatusUseCase;
        this.getCurrentMentorVerificationUseCase = getCurrentMentorVerificationUseCase;
        this.upsertCurrentMentorVerificationUseCase = upsertCurrentMentorVerificationUseCase;
    }

    @GetMapping("/status")
    public ApiResponse<CurrentMentorVerificationStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorVerificationStatusUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get mentor verification status successfully");
    }

    @GetMapping
    public ApiResponse<CurrentMentorVerificationResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorVerificationUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get current mentor verification successfully");
    }

    @PutMapping
    public ApiResponse<CurrentMentorVerificationResponse> upsert(@AuthenticationPrincipal Jwt jwt,
                                                                  @Valid @RequestBody UpsertCurrentMentorVerificationCommand command) {
        return ApiResponse.success(upsertCurrentMentorVerificationUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                "Save current mentor verification successfully");
    }
}
