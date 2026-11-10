package com.mentormatching.mentor.dto.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Quyết định review verification của admin.
 *
 * @param action duyệt hoặc từ chối
 * @param rejectionReason lý do bắt buộc khi từ chối; không dùng khi duyệt
 */
@Builder
public record ReviewMentorVerificationCommand(
        @NotNull ReviewMentorVerificationAction action,
        String rejectionReason) {

    public ReviewMentorVerificationCommand {
        rejectionReason = rejectionReason == null ? null : rejectionReason.trim();
    }
}
