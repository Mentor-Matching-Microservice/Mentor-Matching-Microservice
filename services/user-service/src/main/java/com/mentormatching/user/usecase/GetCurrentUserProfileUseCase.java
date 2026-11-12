package com.mentormatching.user.usecase;

import java.util.Objects;
import java.util.UUID;

import com.mentormatching.user.dto.UserProfileDetails;
import com.mentormatching.user.exception.ApiException;
import com.mentormatching.user.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc hồ sơ chung của người dùng hiện tại.
 */
@Service
@PreAuthorize("hasRole('LEANER')")
@Transactional(readOnly = true)
public class GetCurrentUserProfileUseCase {

    private final UserProfileRepository repository;

    public GetCurrentUserProfileUseCase(UserProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Caller cung cấp userId từ danh tính đã xác thực, không lấy từ request body.
     */
    public UserProfileDetails execute(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        var profile = repository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_PROFILE_NOT_FOUND",
                        "User profile not found")
                );
        return UserProfileDetails.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .userType(profile.getUserType())
                .build();
    }
}
