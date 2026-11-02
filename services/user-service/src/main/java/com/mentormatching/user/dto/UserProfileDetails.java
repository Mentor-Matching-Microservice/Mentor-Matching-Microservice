package com.mentormatching.user.dto;

import lombok.Builder;

import java.util.UUID;

import com.mentormatching.user.entity.UserType;

/** Dữ liệu hồ sơ chung thuộc user-service. */
@Builder
public record UserProfileDetails(UUID userId, String fullName, String phone, UserType userType) {
}
