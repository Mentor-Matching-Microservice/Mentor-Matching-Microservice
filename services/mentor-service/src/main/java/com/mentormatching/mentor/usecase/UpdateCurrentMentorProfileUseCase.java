package com.mentormatching.mentor.usecase;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpdateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.response.MentorProfileDetails;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thay thế các trường hồ sơ do mentor đang đăng nhập quản lý.
 */
@Service
@PreAuthorize("hasRole('MENTOR')")
public class UpdateCurrentMentorProfileUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final Validator validator;

    public UpdateCurrentMentorProfileUseCase(MentorProfileRepository mentorProfileRepository, Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.validator = validator;
    }

    /**
     * Caller phải lấy userId từ JWT sub; không nhận ID chủ hồ sơ từ body.
     * Hồ sơ phải được tạo ở bước xác minh trước khi tài khoản được cấp role MENTOR.
     * Lệnh cập nhật thay thế toàn bộ trường trong command, kể cả khi trường nhận null.
     * Không thay đổi avatar, tên đồng bộ hoặc trạng thái tài khoản.
     *
     * @param userId  UUID tài khoản mentor đã xác thực
     * @param command các trường hồ sơ mentor được phép sửa
     * @return trạng thái hồ sơ sau khi lưu
     * @throws ApiException 400 nếu đầu vào không hợp lệ; 404 nếu chưa có hồ sơ
     */
    @Transactional
    public MentorProfileDetails execute(UUID userId, UpdateCurrentMentorProfileCommand command) {
        if (userId == null || command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor profile.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

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

        var saved = mentorProfileRepository.saveAndFlush(profile);

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
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
