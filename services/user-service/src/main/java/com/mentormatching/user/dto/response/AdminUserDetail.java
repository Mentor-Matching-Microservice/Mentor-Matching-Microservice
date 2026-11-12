package com.mentormatching.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.user.dto.LearnerProfileDetails;
import com.mentormatching.user.entity.UserType;

/**
 * Chi tiết một người dùng cho Admin: hồ sơ, hồ sơ học viên (null nếu chưa có) và trạng thái tài khoản.
 */
public record AdminUserDetail(UUID userId, String fullName, String email, String phone, UserType userType,
                              String accountStatus, Long accountStatusVersion, Instant createdAt, Instant updatedAt,
                              LearnerProfileDetails learnerProfile) {
}
