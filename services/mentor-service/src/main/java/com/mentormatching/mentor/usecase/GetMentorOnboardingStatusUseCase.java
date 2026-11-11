package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.response.MentorOnboardingStatusResponse;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class GetMentorOnboardingStatusUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final MentorVerificationRepository mentorVerificationRepository;

    public GetMentorOnboardingStatusUseCase(MentorProfileRepository mentorProfileRepository,
                                           MentorSubjectRepository mentorSubjectRepository,
                                           MentorVerificationRepository mentorVerificationRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
    }

    public MentorOnboardingStatusResponse execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var profileOpt = mentorProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return MentorOnboardingStatusResponse.builder()
                    .approvalStatus(MentorApprovalStatus.DRAFT)
                    .isBasicProfileCompleted(false)
                    .activeSubjectsCount(0)
                    .hasSubmittedKyc(false)
                    .kycStatus(null)
                    .canSubmit(false)
                    .rejectionReason(null)
                    .build();
        }

        var profile = profileOpt.get();
        boolean isBasicCompleted = profile.getHeadline() != null && !profile.getHeadline().isBlank()
                && profile.getIntroduction() != null && !profile.getIntroduction().isBlank()
                && profile.getMeetingType() != null;

        int activeSubjects = (int) mentorSubjectRepository.countByMentorIdAndActiveTrue(profile.getId());

        var verificationOpt = mentorVerificationRepository.findByMentorId(profile.getId());
        boolean hasKyc = verificationOpt.isPresent();
        MentorVerificationStatus kycStatus = verificationOpt.map(v -> v.getVerificationStatus()).orElse(null);

        MentorApprovalStatus approvalStatus = profile.getApprovalStatus() != null
                ? profile.getApprovalStatus()
                : MentorApprovalStatus.DRAFT;

        boolean canSubmit = isBasicCompleted
                && activeSubjects >= 1
                && hasKyc
                && (approvalStatus == MentorApprovalStatus.DRAFT || approvalStatus == MentorApprovalStatus.REJECTED);

        return MentorOnboardingStatusResponse.builder()
                .approvalStatus(approvalStatus)
                .isBasicProfileCompleted(isBasicCompleted)
                .activeSubjectsCount(activeSubjects)
                .hasSubmittedKyc(hasKyc)
                .kycStatus(kycStatus)
                .canSubmit(canSubmit)
                .rejectionReason(profile.getRejectionReason())
                .build();
    }
}
