package com.mentormatching.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.user.entity.UserProfile;
import com.mentormatching.user.entity.UserType;

/**
 * Một dòng trong danh sách người dùng cho Admin. {@code accountStatus} là bản sao từ auth-service
 * (null nếu chưa nhận sự kiện nào về tài khoản này).
 */
public record AdminUserItem(UUID userId, String fullName, String email, String phone, UserType userType,
                            String accountStatus, Instant createdAt) {

    public static AdminUserItem from(UserProfile p) {
        return new AdminUserItem(p.getUserId(), p.getFullName(), p.getEmail(), p.getPhone(), p.getUserType(),
                p.getAccountStatus(), p.getCreatedAt());
    }
}
