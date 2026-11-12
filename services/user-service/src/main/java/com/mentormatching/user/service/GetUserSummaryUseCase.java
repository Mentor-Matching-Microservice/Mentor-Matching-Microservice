package com.mentormatching.user.service;

import java.util.UUID;

import com.mentormatching.user.dto.response.UserSummaryResponse;
import com.mentormatching.user.exception.ApiException;
import com.mentormatching.user.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông tin tóm tắt một người dùng cho service nội bộ ({@code GET /internal/v1/users/{userId}/summary}).
 * {@code email} là bản sao từ {@code AccountRegistered}; có thể null nếu hồ sơ được tạo trước khi nhận sự kiện.
 */
@Service
@Transactional(readOnly = true)
public class GetUserSummaryUseCase {

    private final UserProfileRepository profiles;

    public GetUserSummaryUseCase(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    public UserSummaryResponse execute(UUID userId) {
        return profiles.findById(userId)
                .map(p -> UserSummaryResponse.builder()
                        .userId(p.getUserId().toString())
                        .fullName(p.getFullName())
                        .email(p.getEmail())
                        .phone(p.getPhone())
                        .build())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_PROFILE_NOT_FOUND", "User profile not found"));
    }
}
