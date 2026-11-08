package com.mentormatching.mentor.dto.query;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

/**
 * Batch UUID gửi từ mentor-service sang auth-service.
 *
 * @param userIds UUID tài khoản cần đọc, caller giới hạn từ 1 đến 100 ID không null
 */
@Builder
public record AccountStatusesQuery(
        List<UUID> userIds) {
}
