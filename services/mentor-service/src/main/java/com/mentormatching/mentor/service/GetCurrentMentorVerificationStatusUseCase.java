package com.mentormatching.mentor.service;

import java.util.UUID;

import com.mentormatching.mentor.dto.response.CurrentMentorVerificationStatusResponse;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc trạng thái KYC của ứng viên hoặc mentor đang đăng nhập. */
@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class GetCurrentMentorVerificationStatusUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorVerificationRepository mentorVerificationRepository;

    public GetCurrentMentorVerificationStatusUseCase(MentorProfileRepository mentorProfileRepository,
                                                      MentorVerificationRepository mentorVerificationRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
    }

    /**
     * Caller lấy userId từ JWT sub. Chưa nộp KYC được trả về với status null.
     *
     * @param userId UUID tài khoản đã xác thực
     * @return trạng thái KYC và lý do từ chối nếu có
     * @throws ApiException 400 nếu userId null
     */
    public CurrentMentorVerificationStatusResponse execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var verification = mentorProfileRepository.findByUserId(userId)
                .flatMap(profile -> mentorVerificationRepository.findByMentorId(profile.getId()));

        if (verification.isEmpty()) {
            return CurrentMentorVerificationStatusResponse.builder().build();
        }

        var current = verification.get();
        return CurrentMentorVerificationStatusResponse.builder()
                .verificationStatus(current.getVerificationStatus())
                .rejectionReason(current.getVerificationStatus() == MentorVerificationStatus.REJECTED
                        ? current.getRejectionReason() : null)
                .build();
    }
}
