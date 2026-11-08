package com.mentormatching.mentor.usecase;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.ReviewMentorVerificationAction;
import com.mentormatching.mentor.dto.command.ReviewMentorVerificationCommand;
import com.mentormatching.mentor.dto.response.MentorVerificationReviewResponse;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin duyệt hoặc từ chối giấy tờ xác minh mentor đang chờ.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class ReviewMentorVerificationUseCase {

    private final MentorVerificationRepository repository;
    private final Validator validator;

    public ReviewMentorVerificationUseCase(MentorVerificationRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Chỉ review bản PENDING; reviewerId phải lấy từ JWT sub của admin.
     * Khóa bản ghi trong transaction để tránh hai admin cùng review thành công.
     * Việc cấp role MENTOR tại auth-service thuộc bước tích hợp riêng.
     *
     * @param reviewerId     UUID admin đã xác thực
     * @param verificationId ID bản ghi cần review
     * @param command        quyết định và lý do từ chối nếu có
     * @return trạng thái sau review
     * @throws ApiException 400 nếu đầu vào sai, 404 nếu không tìm thấy, 409 nếu đã review
     */
    @Transactional
    public MentorVerificationReviewResponse execute(UUID reviewerId, Long verificationId,
                                                    ReviewMentorVerificationCommand command) {
        if (reviewerId == null || verificationId == null || verificationId <= 0
                || command == null || !validator.validate(command).isEmpty()
                || command.action() == ReviewMentorVerificationAction.REJECT
                && (command.rejectionReason() == null || command.rejectionReason().isBlank())
                || command.action() == ReviewMentorVerificationAction.VERIFY && command.rejectionReason() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor verification review.");
        }

        var verification = repository.findForReviewById(verificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_VERIFICATION_NOT_FOUND",
                        "Mentor verification not found."));
        if (verification.getVerificationStatus() != MentorVerificationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "VERIFICATION_NOT_PENDING",
                    "Only pending mentor verification can be reviewed.");
        }

        // VERIFY
        if (command.action() == ReviewMentorVerificationAction.VERIFY) {
            verification.setVerificationStatus(MentorVerificationStatus.VERIFIED);
            verification.setVerifiedBy(reviewerId);
            verification.setVerifiedAt(Instant.now());
            verification.setRejectionReason(null);
        } else {
            // REJECTED
            verification.setVerificationStatus(MentorVerificationStatus.REJECTED);
            verification.setVerifiedBy(null);
            verification.setVerifiedAt(null);
            verification.setRejectionReason(command.rejectionReason());
        }

        var saved = repository.saveAndFlush(verification);

        return MentorVerificationReviewResponse.builder()
                .id(saved.getId())
                .mentorId(saved.getMentorId())
                .status(saved.getVerificationStatus())
                .verifiedBy(saved.getVerifiedBy())
                .verifiedAt(saved.getVerifiedAt())
                .rejectionReason(saved.getRejectionReason())
                .build();
    }
}
