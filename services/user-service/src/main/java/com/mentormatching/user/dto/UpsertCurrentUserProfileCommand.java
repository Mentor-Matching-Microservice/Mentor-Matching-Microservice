package com.mentormatching.user.dto;

import com.mentormatching.user.entity.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/** Dữ liệu hoàn thiện hoặc thay thế hồ sơ chung của người dùng. */
@Builder
public record UpsertCurrentUserProfileCommand(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 20) @Pattern(regexp = "[\\x20-\\x7E]+") String phone,
        @NotNull UserType userType) {

    /** Chuẩn hóa khi tạo command qua constructor hoặc builder; giữ null để validator xử lý. */
    public UpsertCurrentUserProfileCommand {
        fullName = fullName == null ? null : fullName.trim();
        phone = phone == null ? null : phone.trim();
    }
}
