package com.mentormatching.mentor.usecase;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.dto.response.SubmitMentorProfileResponse;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
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
public class SubmitMentorProfileUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final MentorVerificationRepository mentorVerificationRepository;

    public SubmitMentorProfileUseCase(MentorProfileRepository mentorProfileRepository,
                                     MentorSubjectRepository mentorSubjectRepository,
                                     MentorVerificationRepository mentorVerificationRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
    }

    @Transactional
    public SubmitMentorProfileResponse execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        if (profile.getApprovalStatus() == MentorApprovalStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_SUBMITTED",
                    "Mentor profile is already submitted and pending review.");
        }
        if (profile.getApprovalStatus() == MentorApprovalStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROFILE_ALREADY_APPROVED",
                    "Mentor profile is already approved.");
        }
        if (profile.getApprovalStatus() != MentorApprovalStatus.DRAFT
                && profile.getApprovalStatus() != MentorApprovalStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "CANNOT_SUBMIT",
                    "Mentor profile cannot be submitted in current state.");
        }

        boolean isBasicCompleted = profile.getHeadline() != null && !profile.getHeadline().isBlank()
                && profile.getIntroduction() != null && !profile.getIntroduction().isBlank()
                && profile.getMeetingType() != null;
        if (!isBasicCompleted) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INCOMPLETE_PROFILE",
                    "Basic profile information (headline, introduction, meeting type) must be completed.");
        }

        long activeSubjects = mentorSubjectRepository.countByMentorIdAndActiveTrue(profile.getId());
        if (activeSubjects < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_ACTIVE_SUBJECTS",
                    "At least one active teaching subject is required before submission.");
        }

        var verificationOpt = mentorVerificationRepository.findByMentorId(profile.getId());
        if (verificationOpt.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "KYC_REQUIRED",
                    "KYC verification documents must be submitted before profile submission.");
        }

        profile.setApprovalStatus(MentorApprovalStatus.PENDING);
        profile.setRejectionReason(null);
        var saved = mentorProfileRepository.save(profile);

        return SubmitMentorProfileResponse.builder()
                .mentorId(saved.getId())
                .approvalStatus(saved.getApprovalStatus())
                .submittedAt(Instant.now())
                .message("Mentor profile submitted successfully for admin review.")
                .build();
    }
}
