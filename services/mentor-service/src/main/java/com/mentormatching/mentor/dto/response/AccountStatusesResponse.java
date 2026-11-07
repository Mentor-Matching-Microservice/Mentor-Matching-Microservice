package com.mentormatching.mentor.dto.response;

import java.util.List;

import com.mentormatching.mentor.dto.command.AccountStatusSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * Batch snapshot nhận từ auth; được service áp dụng validate trước khi ghi.
 *
 * @param accounts tối đa 100 snapshot không null; danh sách rỗng hợp lệ nếu không tìm thấy account
 */
@Builder
public record AccountStatusesResponse(
        @NotNull @Size(max = 100) List<@NotNull @Valid AccountStatusSnapshot> accounts) {
}
