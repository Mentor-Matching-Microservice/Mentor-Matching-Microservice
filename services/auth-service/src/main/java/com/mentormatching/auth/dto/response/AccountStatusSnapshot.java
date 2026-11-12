package com.mentormatching.auth.dto.response;
import java.util.UUID;
import com.mentormatching.auth.entity.AccountStatus;
import lombok.Builder;
/**
 * Snapshot chỉ chứa dữ liệu cần cho đồng bộ trạng thái tài khoản.
 *
 * @param userId UUID của account
 * @param status trạng thái do auth sở hữu
 * @param version version hiện tại của account trong auth, không phải version hồ sơ mentor
 */
@Builder
public record AccountStatusSnapshot(
        UUID userId,
        AccountStatus status,
        long version) {}
