package com.mentormatching.auth.dto.response;
import java.util.List;
import lombok.Builder;
/**
 * Kết quả đọc snapshot tài khoản theo batch.
 *
 * @param accounts các account tìm thấy, không bảo đảm thứ tự theo request; không chứa account không tồn tại
 */
@Builder
public record AccountStatusesResponse(
        List<AccountStatusSnapshot> accounts) {}
