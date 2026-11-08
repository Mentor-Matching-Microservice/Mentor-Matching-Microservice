package com.mentormatching.user.usecase;

import java.util.Objects;
import java.util.UUID;

import com.mentormatching.user.dto.UpsertCurrentUserProfileCommand;
import com.mentormatching.user.dto.UserProfileDetails;
import com.mentormatching.user.entity.UserProfile;
import com.mentormatching.user.exception.ApiException;
import com.mentormatching.user.repository.UserProfileRepository;
import jakarta.validation.Validator;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Tạo hoặc cập nhật hồ sơ chung bằng UUID tài khoản do auth-service cấp.
 */
@Service
@PreAuthorize("hasRole('LEANER')")
public class UpsertCurrentUserProfileUseCase {

    private final UserProfileRepository users;
    private final Validator validator;

    public UpsertCurrentUserProfileUseCase(UserProfileRepository users, Validator validator) {
        this.users = users;
        this.validator = validator;
    }

    /**
     * Caller phải lấy userId từ danh tính đã xác thực. Chưa tra cứu account qua auth-service.
     * Phone chỉ trim, chưa quy đổi giữa định dạng nội địa và quốc tế.
     */
    @Transactional
    public UserProfileDetails execute(UUID userId, UpsertCurrentUserProfileCommand command) {
        Objects.requireNonNull(userId, "userId is required");
        if (command == null || !validator.validate(command).isEmpty()) {
            throw invalidProfile();
        }

        // Pre-check phone
        if (users.existsByPhoneAndUserIdNot(command.phone(), userId)) {
            throw phoneAlreadyExists();
        }

        var profile = users.findById(userId).orElseGet(() -> {
            var created = new UserProfile();
            created.setUserId(userId);
            return created;
        });
        profile.setFullName(command.fullName());
        profile.setPhone(command.phone());
        profile.setUserType(command.userType());

        UserProfile saved;
        try {
            saved = users.saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            // Unique constraint bảo vệ cả trường hợp hai transaction vượt qua pre-check.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uk_user_profiles_phone".equals(violation.getConstraintName())) {
                    throw phoneAlreadyExists();
                }
            }
            throw exception;
        }
        return UserProfileDetails.builder()
                .userId(saved.getUserId())
                .fullName(saved.getFullName())
                .phone(saved.getPhone())
                .userType(saved.getUserType())
                .build();
    }

    private ApiException invalidProfile() {
        return new ApiException(BAD_REQUEST, "INVALID_USER_PROFILE", "Invalid user profile data");
    }

    private ApiException phoneAlreadyExists() {
        return new ApiException(CONFLICT, "PHONE_ALREADY_EXISTS", "Phone already exists");
    }
}
