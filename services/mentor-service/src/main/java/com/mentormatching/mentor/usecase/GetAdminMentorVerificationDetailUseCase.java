package com.mentormatching.mentor.usecase;

import com.mentormatching.mentor.dto.response.AdminMentorVerificationDetailResponse;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc chi tiết verification và hồ sơ mentor liên quan để admin review.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class GetAdminMentorVerificationDetailUseCase {

    private final MentorVerificationRepository mentorVerificationRepository;
    private final MentorProfileRepository mentorProfileRepository;

    public GetAdminMentorVerificationDetailUseCase(MentorVerificationRepository mentorVerificationRepository,
                                                   MentorProfileRepository mentorProfileRepository) {
        this.mentorVerificationRepository = mentorVerificationRepository;
        this.mentorProfileRepository = mentorProfileRepository;
    }

    /**
     * @param verificationId ID bản ghi xác minh cần xem
     * @return thông tin giấy tờ và trạng thái duyệt của mentor liên quan
     * @throws ApiException 400 nếu ID không hợp lệ; 404 nếu không tìm thấy dữ liệu
     */
    public AdminMentorVerificationDetailResponse execute(Long verificationId) {
        if (verificationId == null || verificationId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Verification ID must be positive.");
        }

        var verification = mentorVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_VERIFICATION_NOT_FOUND",
                        "Mentor verification not found."));
        var profile = mentorProfileRepository.findById(verification.getMentorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        return AdminMentorVerificationDetailResponse.builder()
                .id(verification.getId())
                .mentorId(verification.getMentorId())
                .userId(profile.getUserId())
                .accountFullName(profile.getFullName())
                .fullName(verification.getFullName())
                .idCardNumber(verification.getIdCardNumber())
                .idCardFrontUrl(verification.getIdCardFrontUrl())
                .idCardBackUrl(verification.getIdCardBackUrl())
                .selfieWithIdUrl(verification.getSelfieWithIdUrl())
                .verificationStatus(verification.getVerificationStatus())
                .verifiedBy(verification.getVerifiedBy())
                .verifiedAt(verification.getVerifiedAt())
                .rejectionReason(verification.getRejectionReason())
                .createdAt(verification.getCreatedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
    }
}
