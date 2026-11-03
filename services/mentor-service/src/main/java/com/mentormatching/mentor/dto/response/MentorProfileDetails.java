package com.mentormatching.mentor.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorGender;
import lombok.Builder;

/** Current mentor profile data owned by mentor-service. */
@Builder
public record MentorProfileDetails(
        Long id,
        UUID userId,
        String fullName,
        String avatarUrl,
        Long avatarMediaId,
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
        String approvalNote,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt) {
}
