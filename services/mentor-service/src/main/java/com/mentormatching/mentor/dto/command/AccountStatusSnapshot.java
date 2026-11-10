package com.mentormatching.mentor.dto.command;

import java.util.UUID;

import com.mentormatching.mentor.entity.AccountStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * Snapshot trạng thái nguồn để cập nhật bản sao trong mentor-service.
 *
 * @param userId UUID do auth cấp
 * @param status trạng thái hiện tại do auth sở hữu
 * @param version version account nguồn, không âm; dùng để loại snapshot cũ hoặc trùng
 */
@Builder
public record AccountStatusSnapshot(
        @NotNull UUID userId,
        @NotNull AccountStatus status,
        @NotNull @PositiveOrZero Long version) {
}
