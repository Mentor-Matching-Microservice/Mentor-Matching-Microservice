package com.mentormatching.user.controller;

import java.util.UUID;

import com.mentormatching.user.dto.ApiResponse;
import com.mentormatching.user.dto.LearnerProfileDetails;
import com.mentormatching.user.dto.UpsertCurrentLearnerProfileCommand;
import com.mentormatching.user.usecase.GetCurrentLearnerProfileUseCase;
import com.mentormatching.user.usecase.UpsertCurrentLearnerProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API hồ sơ learner của người dùng đang đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/users/me/learner-profile")
public class LearnerProfileController {

    private final GetCurrentLearnerProfileUseCase getProfile;
    private final UpsertCurrentLearnerProfileUseCase upsertProfile;

    public LearnerProfileController(GetCurrentLearnerProfileUseCase getProfile,
                                    UpsertCurrentLearnerProfileUseCase upsertProfile) {
        this.getProfile = getProfile;
        this.upsertProfile = upsertProfile;
    }

    @GetMapping
    public ApiResponse<LearnerProfileDetails> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<LearnerProfileDetails>builder()
                .status(200)
                .code("SUCCESS")
                .success(true)
                .message("Get learner profile successfully")
                .data(getProfile.execute(UUID.fromString(jwt.getSubject())))
                .build();
    }

    /**
     * PUT thay thế các trường trong command, trả 200 cho cả tạo mới và cập nhật.
     */
    @PutMapping
    public ApiResponse<LearnerProfileDetails> upsert(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody UpsertCurrentLearnerProfileCommand command) {
        return ApiResponse.<LearnerProfileDetails>builder()
                .status(200)
                .code("SUCCESS")
                .success(true)
                .message("Save learner profile successfully")
                .data(upsertProfile.execute(UUID.fromString(jwt.getSubject()), command))
                .build();
    }
}
