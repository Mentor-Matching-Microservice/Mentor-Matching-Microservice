package com.mentormatching.mentor.usecase;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.ReviewMentorApprovalAction;
import com.mentormatching.mentor.dto.command.ReviewMentorApprovalCommand;
import com.mentormatching.mentor.dto.response.AdminMentorApprovalResponse;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.entity.OutboxEvent;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import com.mentormatching.mentor.repository.OutboxEventRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Admin xét duyệt hồ sơ Mentor và lưu sự kiện vào outbox_events.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewMentorApprovalUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorVerificationRepository mentorVerificationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public AdminReviewMentorApprovalUseCase(MentorProfileRepository mentorProfileRepository,
                                           MentorVerificationRepository mentorVerificationRepository,
                                           OutboxEventRepository outboxEventRepository,
                                           Validator validator,
                                           ObjectMapper objectMapper) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AdminMentorApprovalResponse execute(UUID reviewerId, Long mentorId, ReviewMentorApprovalCommand command) {
        if (reviewerId == null || mentorId == null || mentorId <= 0 || command == null
                || !validator.validate(command).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor approval review.");
        }

        var profile = mentorProfileRepository.findByIdForReview(mentorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        var verificationOpt = mentorVerificationRepository.findByMentorId(mentorId);

        Instant now = Instant.now();

        switch (command.action()) {
            case APPROVE -> {
                if (profile.getApprovalStatus() != MentorApprovalStatus.PENDING) {
                    throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE",
                            "Only pending mentor profile can be approved.");
                }
                // Hard Rule: Admin can ONLY APPROVE when KYC verification is VERIFIED
                if (verificationOpt.isEmpty()
                        || verificationOpt.get().getVerificationStatus() != MentorVerificationStatus.VERIFIED) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "KYC_NOT_VERIFIED",
                            "Cannot approve mentor profile before KYC verification is verified.");
                }

                profile.setApprovalStatus(MentorApprovalStatus.APPROVED);
                profile.setRejectionReason(null);
                profile.setApprovedBy(reviewerId);
                profile.setApprovedAt(now);

                String payload = createPayload(Map.of(
                        "mentorId", profile.getId(),
                        "userId", profile.getUserId().toString(),
                        "approvedBy", reviewerId.toString(),
                        "approvedAt", now.toString(),
                        "timestamp", now.toString()
                ));
                outboxEventRepository.save(new OutboxEvent("MentorApproved", "MENTOR", profile.getId().toString(), payload));
            }
            case REJECT -> {
                if (profile.getApprovalStatus() != MentorApprovalStatus.PENDING) {
                    throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE",
                            "Only pending mentor profile can be rejected.");
                }
                if (command.rejectionReason() == null || command.rejectionReason().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "REJECTION_REASON_REQUIRED",
                            "Rejection reason is required when rejecting a mentor profile.");
                }

                profile.setApprovalStatus(MentorApprovalStatus.REJECTED);
                profile.setRejectionReason(command.rejectionReason());
                profile.setApprovedBy(reviewerId);
                profile.setApprovedAt(now);

                String payload = createPayload(Map.of(
                        "mentorId", profile.getId(),
                        "userId", profile.getUserId().toString(),
                        "rejectionReason", command.rejectionReason(),
                        "rejectedBy", reviewerId.toString(),
                        "rejectedAt", now.toString(),
                        "timestamp", now.toString()
                ));
                outboxEventRepository.save(new OutboxEvent("MentorRejected", "MENTOR", profile.getId().toString(), payload));
            }
            case SUSPEND -> {
                if (profile.getApprovalStatus() != MentorApprovalStatus.APPROVED) {
                    throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE",
                            "Only approved mentor profile can be suspended.");
                }
                profile.setApprovalStatus(MentorApprovalStatus.SUSPENDED);
            }
            case REACTIVATE -> {
                if (profile.getApprovalStatus() != MentorApprovalStatus.SUSPENDED) {
                    throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE",
                            "Only suspended mentor profile can be reactivated.");
                }
                profile.setApprovalStatus(MentorApprovalStatus.APPROVED);
            }
        }

        var saved = mentorProfileRepository.saveAndFlush(profile);

        return AdminMentorApprovalResponse.builder()
                .mentorId(saved.getId())
                .approvalStatus(saved.getApprovalStatus())
                .approvedBy(saved.getApprovedBy())
                .approvedAt(saved.getApprovedAt())
                .rejectionReason(saved.getRejectionReason())
                .build();
    }

    private String createPayload(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (var entry : data.entrySet()) {
                if (i++ > 0) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof Number) {
                    sb.append(entry.getValue());
                } else {
                    sb.append("\"").append(entry.getValue()).append("\"");
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
