package com.mentormatching.auth.dto.query;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;
import lombok.Builder;
/**
 * Yêu cầu đọc trạng thái nhiều tài khoản trong một lần gọi nội bộ.
 *
 * @param userIds danh sách từ 1 đến 100 UUID không null
 */
@Builder
public record AccountStatusesQuery(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> userIds) {}
