package com.mentormatching.user.repository;

import java.util.UUID;

import com.mentormatching.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Truy cập và quản lý dữ liệu hồ sơ người dùng.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID>, JpaSpecificationExecutor<UserProfile> {

    /**
     * Kiểm tra số điện thoại đã được người dùng khác sử dụng hay chưa.
     */
    boolean existsByPhoneAndUserIdNot(String phone, UUID userId);
}
