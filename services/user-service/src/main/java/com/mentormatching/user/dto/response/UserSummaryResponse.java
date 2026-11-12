package com.mentormatching.user.dto.response;

import com.mentormatching.user.entity.UserType;
import lombok.Builder;

@Builder
public record UserSummaryResponse(
        String userId,
        String fullName,
        String email,
        String phone,
        // #66: loại người dùng (STUDENT, PARENT, ...); null nếu chưa khai báo.
        UserType userType
) {
}
