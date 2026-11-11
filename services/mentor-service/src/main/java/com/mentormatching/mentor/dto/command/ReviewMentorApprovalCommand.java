package com.mentormatching.mentor.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewMentorApprovalCommand(
        @NotNull(message = "Review action is required")
        ReviewMentorApprovalAction action,
        @Size(max = 2000, message = "Rejection reason cannot exceed 2000 characters")
        String rejectionReason) {

    public ReviewMentorApprovalCommand {
        rejectionReason = rejectionReason != null ? rejectionReason.trim() : null;
    }
}
