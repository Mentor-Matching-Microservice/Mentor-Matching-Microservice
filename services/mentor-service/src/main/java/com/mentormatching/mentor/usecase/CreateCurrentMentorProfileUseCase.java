package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.CreateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.response.MentorProfileDetails;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo hồ sơ mentor nháp cho tài khoản chưa có hồ sơ.
 */
@Service
@PreAuthorize("isAuthenticated()")
public class CreateCurrentMentorProfileUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final Validator validator;

    public CreateCurrentMentorProfileUseCase(MentorProfileRepository mentorProfileRepository, Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.validator = validator;
    }

    /**
     * @param userId UUID tài khoản lấy từ JWT sub
     * @param command thông tin khởi tạo hồ sơ
     * @return hồ sơ vừa tạo
     * @throws ApiException 400 nếu dữ liệu không hợp lệ; 409 nếu tài khoản đã có hồ sơ mentor
     */
    @Transactional
    public MentorProfileDetails execute(UUID userId, CreateCurrentMentorProfileCommand command) {
        if (userId == null || (command != null && !validator.validate(command).isEmpty())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor profile.");
        }

        if (mentorProfileRepository.findByUserId(userId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "PROFILE_ALREADY_EXISTS", "Mentor profile already exists.");
        }

        var profile = new MentorProfile();
        profile.setUserId(userId);
        profile.setAvatarUrl(command != null && command.avatarUrl() != null ? command.avatarUrl() : "");
        if (command != null) {
            profile.setAvatarMediaId(command.avatarMediaId());
            profile.setGender(command.gender());
            profile.setHometownCityId(command.hometownCityId());
            profile.setCurrentDistrictId(command.currentDistrictId());
            profile.setHeadline(command.headline());
            profile.setIntroduction(command.introduction());
            profile.setTeachingStyle(command.teachingStyle());
            profile.setExperienceYears(command.experienceYears());
            profile.setCurrentPosition(command.currentPosition());
            profile.setWorkplace(command.workplace());
            profile.setEducation(command.education());
            profile.setMajor(command.major());
            profile.setMeetingType(command.meetingType());
        }

        var saved = mentorProfileRepository.save(profile);

        return MentorProfileDetails.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .accountStatus(saved.getAccountStatus())
                .fullName(saved.getFullName())
                .avatarUrl(saved.getAvatarUrl())
                .avatarMediaId(saved.getAvatarMediaId())
                .gender(saved.getGender())
                .hometownCityId(saved.getHometownCityId())
                .currentDistrictId(saved.getCurrentDistrictId())
                .headline(saved.getHeadline())
                .introduction(saved.getIntroduction())
                .teachingStyle(saved.getTeachingStyle())
                .experienceYears(saved.getExperienceYears())
                .currentPosition(saved.getCurrentPosition())
                .workplace(saved.getWorkplace())
                .education(saved.getEducation())
                .major(saved.getMajor())
                .meetingType(saved.getMeetingType())
                .approvalStatus(saved.getApprovalStatus())
                .rejectionReason(saved.getRejectionReason())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}

