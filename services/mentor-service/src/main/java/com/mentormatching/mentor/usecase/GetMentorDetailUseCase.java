package com.mentormatching.mentor.usecase;

import com.mentormatching.mentor.dto.response.MentorDetailResponse;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSearchSpecifications;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lấy hồ sơ mentor đủ điều kiện hiển thị công khai.
 */
@Service
@Transactional(readOnly = true)
public class GetMentorDetailUseCase {

    private final MentorProfileRepository mentorProfileRepository;

    public GetMentorDetailUseCase(MentorProfileRepository mentorProfileRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
    }

    /**
     * Tìm hồ sơ theo ID; hồ sơ chưa công khai được trả về như không tồn tại.
     *
     * @param mentorId ID hồ sơ mentor, phải dương
     * @return các trường hồ sơ được phép hiển thị công khai
     * @throws ApiException 400 nếu ID không hợp lệ, 404 nếu không có mentor công khai
     */
    public MentorDetailResponse execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }

        var profile = mentorProfileRepository.findOne(MentorSearchSpecifications.visible()
                        .and((root, query, cb) -> cb.equal(root.get("id"), mentorId)))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        return MentorDetailResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
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
                .averageRating(profile.getAverageRating())
                .totalReviews(profile.getTotalReviews())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
