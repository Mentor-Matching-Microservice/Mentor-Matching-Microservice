package com.mentormatching.mentor.usecase;

import java.util.ArrayList;
import java.util.List;

import com.mentormatching.mentor.client.MediaServiceClient;
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
    private final MediaServiceClient mediaServiceClient;

    public GetAdminMentorVerificationDetailUseCase(MentorVerificationRepository mentorVerificationRepository,
                                                   MentorProfileRepository mentorProfileRepository,
                                                   MediaServiceClient mediaServiceClient) {
        this.mentorVerificationRepository = mentorVerificationRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mediaServiceClient = mediaServiceClient;
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

        List<String> mediaIds = new ArrayList<>();
        if (verification.getIdCardFrontMediaId() != null) mediaIds.add(verification.getIdCardFrontMediaId());
        if (verification.getIdCardBackMediaId() != null) mediaIds.add(verification.getIdCardBackMediaId());
        if (verification.getSelfieWithIdMediaId() != null) mediaIds.add(verification.getSelfieWithIdMediaId());

        var signedUrls = mediaServiceClient.getPrivateDeliveryUrls(mediaIds);

        String frontUrl = verification.getIdCardFrontMediaId() != null
                ? signedUrls.getOrDefault(verification.getIdCardFrontMediaId(), verification.getIdCardFrontUrl())
                : verification.getIdCardFrontUrl();
        String backUrl = verification.getIdCardBackMediaId() != null
                ? signedUrls.getOrDefault(verification.getIdCardBackMediaId(), verification.getIdCardBackUrl())
                : verification.getIdCardBackUrl();
        String selfieUrl = verification.getSelfieWithIdMediaId() != null
                ? signedUrls.getOrDefault(verification.getSelfieWithIdMediaId(), verification.getSelfieWithIdUrl())
                : verification.getSelfieWithIdUrl();

        return AdminMentorVerificationDetailResponse.builder()
                .id(verification.getId())
                .mentorId(verification.getMentorId())
                .userId(profile.getUserId())
                .accountFullName(profile.getFullName())
                .fullName(verification.getFullName())
                .idCardNumber(verification.getIdCardNumber())
                .idCardFrontUrl(frontUrl)
                .idCardFrontMediaId(verification.getIdCardFrontMediaId())
                .idCardBackUrl(backUrl)
                .idCardBackMediaId(verification.getIdCardBackMediaId())
                .selfieWithIdUrl(selfieUrl)
                .selfieWithIdMediaId(verification.getSelfieWithIdMediaId())
                .verificationStatus(verification.getVerificationStatus())
                .verifiedBy(verification.getVerifiedBy())
                .verifiedAt(verification.getVerifiedAt())
                .rejectionReason(verification.getRejectionReason())
                .createdAt(verification.getCreatedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
    }
}
