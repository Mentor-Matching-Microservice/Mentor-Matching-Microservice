package com.mentormatching.mentor.usecase;

import java.util.List;

import com.mentormatching.mentor.dto.response.AdminMentorDetailResponse;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.entity.MentorHighlight;
import com.mentormatching.mentor.entity.MentorHighlightOption;
import com.mentormatching.mentor.entity.MentorPersonality;
import com.mentormatching.mentor.entity.MentorVerification;
import com.mentormatching.mentor.entity.PersonalityOption;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorAchievementRepository;
import com.mentormatching.mentor.repository.MentorHighlightOptionRepository;
import com.mentormatching.mentor.repository.MentorHighlightRepository;
import com.mentormatching.mentor.repository.MentorPersonalityRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSubjectRepository;
import com.mentormatching.mentor.repository.MentorVerificationRepository;
import com.mentormatching.mentor.repository.PersonalityOptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class GetAdminMentorDetailUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorVerificationRepository mentorVerificationRepository;
    private final MentorSubjectRepository mentorSubjectRepository;
    private final MentorAchievementRepository mentorAchievementRepository;
    private final MentorPersonalityRepository mentorPersonalityRepository;
    private final PersonalityOptionRepository personalityOptionRepository;
    private final MentorHighlightRepository mentorHighlightRepository;
    private final MentorHighlightOptionRepository mentorHighlightOptionRepository;

    public GetAdminMentorDetailUseCase(MentorProfileRepository mentorProfileRepository,
                                       MentorVerificationRepository mentorVerificationRepository,
                                       MentorSubjectRepository mentorSubjectRepository,
                                       MentorAchievementRepository mentorAchievementRepository,
                                       MentorPersonalityRepository mentorPersonalityRepository,
                                       PersonalityOptionRepository personalityOptionRepository,
                                       MentorHighlightRepository mentorHighlightRepository,
                                       MentorHighlightOptionRepository mentorHighlightOptionRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorVerificationRepository = mentorVerificationRepository;
        this.mentorSubjectRepository = mentorSubjectRepository;
        this.mentorAchievementRepository = mentorAchievementRepository;
        this.mentorPersonalityRepository = mentorPersonalityRepository;
        this.personalityOptionRepository = personalityOptionRepository;
        this.mentorHighlightRepository = mentorHighlightRepository;
        this.mentorHighlightOptionRepository = mentorHighlightOptionRepository;
    }

    public AdminMentorDetailResponse execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }

        var profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        var verificationOpt = mentorVerificationRepository.findByMentorId(mentorId);

        List<MentorSubjectDetails> subjects = mentorSubjectRepository.findAllByMentorIdOrderByIdAsc(mentorId).stream()
                .map(s -> MentorSubjectDetails.builder()
                        .id(s.getId())
                        .mentorId(s.getMentorId())
                        .subjectGradeId(s.getSubjectGradeId())
                        .proficiencyLevel(s.getProficiencyLevel())
                        .teachingNote(s.getTeachingNote())
                        .pricePerHour(s.getPricePerHour())
                        .active(s.getActive())
                        .build())
                .toList();

        List<MentorAchievementDetails> achievements = mentorAchievementRepository.findAllByMentorIdOrderByIdAsc(mentorId).stream()
                .map(a -> MentorAchievementDetails.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .achievementType(a.getAchievementType())
                        .issuer(a.getIssuer())
                        .achievedAt(a.getAchievedAt())
                        .proofUrl(a.getProofUrl())
                        .proofMediaId(a.getProofMediaId())
                        .verified(a.getVerified())
                        .build())
                .toList();

        List<Long> personalityOptionIds = mentorPersonalityRepository.findAllByMentorId(mentorId).stream()
                .map(MentorPersonality::getPersonalityOptionId)
                .toList();
        List<String> personalities = personalityOptionRepository.findAllById(personalityOptionIds).stream()
                .map(PersonalityOption::getName)
                .toList();

        List<Long> highlightOptionIds = mentorHighlightRepository.findAllByMentorId(mentorId).stream()
                .map(MentorHighlight::getHighlightOptionId)
                .toList();
        List<String> highlights = mentorHighlightOptionRepository.findAllById(highlightOptionIds).stream()
                .map(MentorHighlightOption::getName)
                .toList();

        return AdminMentorDetailResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .avatarMediaId(profile.getAvatarMediaId())
                .accountStatus(profile.getAccountStatus())
                .gender(profile.getGender())
                .hometownCityId(profile.getHometownCityId())
                .currentDistrictId(profile.getCurrentDistrictId())
                .headline(profile.getHeadline())
                .introduction(profile.getIntroduction())
                .teachingStyle(profile.getTeachingStyle())
                .experienceYears(profile.getExperienceYears())
                .currentPosition(profile.getCurrentPosition())
                .workplace(profile.getWorkplace())
                .education(profile.getEducation())
                .major(profile.getMajor())
                .meetingType(profile.getMeetingType())
                .approvalStatus(profile.getApprovalStatus())
                .rejectionReason(profile.getRejectionReason())
                .approvedBy(profile.getApprovedBy())
                .approvedAt(profile.getApprovedAt())
                .kycStatus(verificationOpt.map(MentorVerification::getVerificationStatus).orElse(null))
                .kycVerificationId(verificationOpt.map(MentorVerification::getId).orElse(null))
                .subjects(subjects)
                .achievements(achievements)
                .personalities(personalities)
                .highlights(highlights)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
