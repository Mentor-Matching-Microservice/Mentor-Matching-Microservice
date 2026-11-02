package com.mentormatching.user.controller;

import java.util.UUID;

import com.mentormatching.user.dto.ApiResponse;
import com.mentormatching.user.dto.UserProfileDetails;
import com.mentormatching.user.dto.UpsertCurrentUserProfileCommand;
import com.mentormatching.user.usecase.GetCurrentUserProfileUseCase;
import com.mentormatching.user.usecase.UpsertCurrentUserProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API hồ sơ user của người dùng đang đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final GetCurrentUserProfileUseCase getProfile;
    private final UpsertCurrentUserProfileUseCase upsertProfile;

    public UserProfileController(GetCurrentUserProfileUseCase getProfile,
                                 UpsertCurrentUserProfileUseCase upsertProfile) {
        this.getProfile = getProfile;
        this.upsertProfile = upsertProfile;
    }

    @GetMapping
    public ApiResponse<UserProfileDetails> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<UserProfileDetails>builder()
                .status(200)
                .code("SUCCESS")
                .success(true)
                .message("Get user profile successfully")
                .data(getProfile.execute(UUID.fromString(jwt.getSubject())))
                .build();
    }

    /**
     * PUT thay thế các trường trong command, trả 200 cho cả tạo mới và cập nhật.
     */
    @PutMapping
    public ApiResponse<UserProfileDetails> upsert(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody UpsertCurrentUserProfileCommand command) {
        return ApiResponse.<UserProfileDetails>builder()
                .status(200)
                .code("SUCCESS")
                .success(true)
                .message("Save user profile successfully")
                .data(upsertProfile.execute(UUID.fromString(jwt.getSubject()), command))
                .build();
    }
}
