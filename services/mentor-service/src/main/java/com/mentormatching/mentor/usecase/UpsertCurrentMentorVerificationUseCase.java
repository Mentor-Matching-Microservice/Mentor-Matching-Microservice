package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorVerificationCommand;
import com.mentormatching.mentor.dto.response.CurrentMentorVerificationResponse;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhận hồ sơ xác minh và tạo profile nháp trước khi ứng viên được cấp role MENTOR.
 */
@Service
@PreAuthorize("isAuthenticated()")
public class UpsertCurrentMentorVerificationUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorVerificationRepository mentorVerificationRepository;
    private final Validator validator;

    public UpsertCurrentMentorVerificationUseCase(MentorProfileRepository mentorProfileRepository,
                                                  MentorVerificationRepository mentorVerificationRepository, Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
        this.validator = validator;
    }

    /**
     * Tạo profile nếu chưa có; đặt trạng thái PENDING khi nộp hoặc nộp lại verification bị từ chối.
     * Profile và verification được ghi trong cùng transaction.
     *
     * @param userId  UUID từ JWT sub, không lấy từ request body
     * @param command thông tin giấy tờ cần xác minh
     * @return bản ghi xác minh của chính ứng viên
     * @throws ApiException 400 nếu đầu vào không hợp lệ; 409 nếu đang chờ hoặc đã xác minh
     */
    @Transactional
    public CurrentMentorVerificationResponse execute(UUID userId, UpsertCurrentMentorVerificationCommand command) {
        if (userId == null || command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor verification.");
        }

        // CREATE DRAFT PROFILE
        var profile = mentorProfileRepository.findByUserId(userId).orElseGet(() -> {
            var draft = new MentorProfile();
            draft.setUserId(userId);
            draft.setAvatarUrl("");
            draft.setApprovalStatus(MentorApprovalStatus.DRAFT);
            return mentorProfileRepository.saveAndFlush(draft);
        });

        var existing = mentorVerificationRepository.findByMentorId(profile.getId());
        if (existing.isPresent() && existing.get().getVerificationStatus() != MentorVerificationStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "VERIFICATION_NOT_EDITABLE", "Mentor verification cannot be updated.");
        }
        var verification = existing.orElseGet(() -> {
            var submitted = new MentorVerification();
            submitted.setMentorId(profile.getId());
            return submitted;
        });

        verification.setFullName(command.fullName());
        verification.setIdCardNumber(command.idCardNumber());
        verification.setIdCardFrontUrl(command.idCardFrontUrl());
        verification.setIdCardBackUrl(command.idCardBackUrl());
        verification.setSelfieWithIdUrl(command.selfieWithIdUrl());
        verification.setIdCardFrontMediaId(null);
        verification.setIdCardBackMediaId(null);
        verification.setSelfieWithIdMediaId(null);
        verification.setVerificationStatus(MentorVerificationStatus.PENDING);
        verification.setVerifiedBy(null);
        verification.setVerifiedAt(null);
        verification.setRejectionReason(null);
        profile.setApprovalStatus(MentorApprovalStatus.PENDING);
        profile.setApprovedBy(null);
        profile.setApprovedAt(null);
        profile.setApprovalNote(null);

        mentorProfileRepository.save(profile);
        var saved = mentorVerificationRepository.saveAndFlush(verification);

        return CurrentMentorVerificationResponse.builder()
                .id(saved.getId())
                .mentorId(saved.getMentorId())
                .fullName(saved.getFullName())
                .idCardNumber(saved.getIdCardNumber())
                .idCardFrontUrl(saved.getIdCardFrontUrl())
                .idCardBackUrl(saved.getIdCardBackUrl())
                .selfieWithIdUrl(saved.getSelfieWithIdUrl())
                .verificationStatus(saved.getVerificationStatus())
                .rejectionReason(saved.getRejectionReason())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
