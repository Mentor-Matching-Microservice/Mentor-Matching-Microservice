package com.mentormatching.mentor.service;

import java.util.Objects;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorAchievementCommand;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.entity.MentorAchievement;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorAchievementRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tạo hoặc thay thế thành tích của mentor đang đăng nhập. */
@Service
@PreAuthorize("hasRole('MENTOR')")
public class UpsertCurrentMentorAchievementUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAchievementRepository mentorAchievementRepository;
    private final Validator validator;

    public UpsertCurrentMentorAchievementUseCase(MentorProfileRepository mentorProfileRepository,
                                                 MentorAchievementRepository mentorAchievementRepository,
                                                 Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAchievementRepository = mentorAchievementRepository;
        this.validator = validator;
    }

    /**
     * Caller lấy userId từ JWT sub. ID null tạo thành tích mới; ID có giá trị chỉ cập nhật
     * bản ghi thuộc mentor hiện tại. Khi nội dung thay đổi, xác minh cũ hết hiệu lực.
     *
     * @param userId UUID tài khoản mentor đã xác thực
     * @param command các trường thành tích được mentor khai báo
     * @return thành tích sau khi lưu
     * @throws ApiException 400 nếu đầu vào sai; 404 nếu thiếu profile hoặc thành tích không thuộc mentor
     */
    @Transactional
    public MentorAchievementDetails execute(UUID userId, UpsertCurrentMentorAchievementCommand command) {
        if (userId == null || command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor achievement.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        MentorAchievement achievement;
        if (command.id() == null) {
            achievement = new MentorAchievement();
            achievement.setMentorId(profile.getId());
            achievement.setVerified(false);
        } else {
            achievement = mentorAchievementRepository.findByIdAndMentorId(command.id(), profile.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_ACHIEVEMENT_NOT_FOUND",
                            "Mentor achievement not found."));
            if (hasChanges(achievement, command)) {
                achievement.setVerified(false);
                achievement.setVerifiedBy(null);
                achievement.setVerifiedAt(null);
            }
        }

        achievement.setTitle(command.title());
        achievement.setDescription(command.description());
        achievement.setAchievementType(command.achievementType());
        achievement.setIssuer(command.issuer());
        achievement.setAchievedAt(command.achievedAt());
        achievement.setProofUrl(command.proofUrl());

        var saved = mentorAchievementRepository.save(achievement);
        return MentorAchievementDetails.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .achievementType(saved.getAchievementType())
                .issuer(saved.getIssuer())
                .achievedAt(saved.getAchievedAt())
                .proofUrl(saved.getProofUrl())
                .verified(saved.getVerified())
                .build();
    }

    private boolean hasChanges(MentorAchievement achievement, UpsertCurrentMentorAchievementCommand command) {
        return !Objects.equals(achievement.getTitle(), command.title())
                || !Objects.equals(achievement.getDescription(), command.description())
                || !Objects.equals(achievement.getAchievementType(), command.achievementType())
                || !Objects.equals(achievement.getIssuer(), command.issuer())
                || !Objects.equals(achievement.getAchievedAt(), command.achievedAt())
                || !Objects.equals(achievement.getProofUrl(), command.proofUrl());
    }
}
