package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.entity.AccountStatus;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorGender;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import lombok.Builder;

@Builder
public record AdminMentorDetailResponse(
        Long id,
        UUID userId,
        String fullName,
        String avatarUrl,
        Long avatarMediaId,
        AccountStatus accountStatus,
        MentorGender gender,
        Long hometownCityId,
        Long currentDistrictId,
        String headline,
        String introduction,
        String teachingStyle,
        Integer experienceYears,
        String currentPosition,
        String workplace,
        String education,
        String major,
        MeetingType meetingType,
        MentorApprovalStatus approvalStatus,
        String rejectionReason,
        UUID approvedBy,
        Instant approvedAt,
        MentorVerificationStatus kycStatus,
        Long kycVerificationId,
        List<MentorSubjectDetails> subjects,
        List<MentorAchievementDetails> achievements,
        List<String> personalities,
        List<String> highlights,
        Instant createdAt,
        Instant updatedAt) {
}
