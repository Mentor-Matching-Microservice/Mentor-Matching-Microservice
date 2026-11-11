package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.response.CurrentMentorVerificationResponse;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc hồ sơ xác minh của chính tài khoản đang đăng nhập, kể cả khi chưa có role MENTOR.
 */
@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class GetCurrentMentorVerificationUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorVerificationRepository mentorVerificationRepository;

    public GetCurrentMentorVerificationUseCase(MentorProfileRepository mentorProfileRepository,
                                               MentorVerificationRepository mentorVerificationRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
    }

    /**
     * Caller lấy userId từ JWT sub; không nhận mentorId từ request để tránh đọc giấy tờ người khác.
     *
     * @param userId UUID tài khoản đã xác thực
     * @return dữ liệu xác minh và trạng thái hiện tại của chính tài khoản đó
     * @throws ApiException 400 nếu userId null; 404 nếu chưa có profile hoặc verification
     */
    public CurrentMentorVerificationResponse execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));
        var verification = mentorVerificationRepository.findByMentorId(profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_VERIFICATION_NOT_FOUND",
                        "Mentor verification not found."));

        return CurrentMentorVerificationResponse.builder()
                .id(verification.getId())
                .mentorId(verification.getMentorId())
                .fullName(verification.getFullName())
                .idCardNumber(verification.getIdCardNumber())
                .idCardFrontUrl(verification.getIdCardFrontUrl())
                .idCardFrontMediaId(verification.getIdCardFrontMediaId())
                .idCardBackUrl(verification.getIdCardBackUrl())
                .idCardBackMediaId(verification.getIdCardBackMediaId())
                .selfieWithIdUrl(verification.getSelfieWithIdUrl())
                .selfieWithIdMediaId(verification.getSelfieWithIdMediaId())
                .verificationStatus(verification.getVerificationStatus())
                .rejectionReason(verification.getRejectionReason())
                .createdAt(verification.getCreatedAt())
                .updatedAt(verification.getUpdatedAt())
                .build();
    }
}
