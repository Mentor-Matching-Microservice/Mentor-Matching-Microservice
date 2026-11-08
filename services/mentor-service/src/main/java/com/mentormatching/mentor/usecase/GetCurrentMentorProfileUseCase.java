package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.response.MentorProfileDetails;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('MENTOR')")
@Transactional(readOnly = true)
public class GetCurrentMentorProfileUseCase {
    
    private final MentorProfileRepository mentorProfileRepository;

    public GetCurrentMentorProfileUseCase(MentorProfileRepository mentorProfileRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
    }

    /**
     * Caller must supply userId from the authenticated JWT subject, never from client input.
     * Role authorization does not establish ownership of an arbitrary UUID passed here.
     */
    public MentorProfileDetails execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }
        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        return MentorProfileDetails.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .accountStatus(profile.getAccountStatus())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .avatarMediaId(profile.getAvatarMediaId())
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
                .approvalNote(profile.getApprovalNote())
                .approvedAt(profile.getApprovedAt())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
