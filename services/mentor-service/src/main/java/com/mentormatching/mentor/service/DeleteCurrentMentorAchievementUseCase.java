package com.mentormatching.mentor.service;

import java.util.UUID;

import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorAchievementRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Xóa thành tích của mentor đang đăng nhập. */
@Service
@PreAuthorize("hasRole('MENTOR')")
public class DeleteCurrentMentorAchievementUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAchievementRepository mentorAchievementRepository;

    public DeleteCurrentMentorAchievementUseCase(MentorProfileRepository mentorProfileRepository,
                                                MentorAchievementRepository mentorAchievementRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAchievementRepository = mentorAchievementRepository;
    }

    /**
     * @param userId UUID tài khoản mentor đã xác thực
     * @param achievementId ID thành tích cần xóa
     * @throws ApiException 400 nếu tham số sai; 404 nếu không tìm thấy profile hoặc achievement
     */
    @Transactional
    public void execute(UUID userId, Long achievementId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }
        if (achievementId == null || achievementId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Achievement ID must be positive.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        var achievement = mentorAchievementRepository.findByIdAndMentorId(achievementId, profile.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_ACHIEVEMENT_NOT_FOUND",
                        "Mentor achievement not found."));

        mentorAchievementRepository.delete(achievement);
    }
}

