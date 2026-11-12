package com.mentormatching.mentor.usecase;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.entity.MentorAchievement;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorAchievementRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc thành tích của mentor đang đăng nhập, kể cả thành tích chưa xác minh.
 */
@Service
@PreAuthorize("hasRole('MENTOR')")
@Transactional(readOnly = true)
public class GetCurrentMentorAchievementsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAchievementRepository mentorAchievementRepository;

    public GetCurrentMentorAchievementsUseCase(MentorProfileRepository mentorProfileRepository,
                                               MentorAchievementRepository mentorAchievementRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAchievementRepository = mentorAchievementRepository;
    }

    /**
     * Caller lấy userId từ JWT sub, không nhận mentorId từ request.
     *
     * @param userId UUID tài khoản mentor đã xác thực
     * @return thành tích theo ID tăng dần, hoặc danh sách rỗng nếu chưa có
     * @throws ApiException 400 nếu userId null; 404 nếu chưa có profile mentor
     */
    public List<MentorAchievementDetails> execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        return mentorAchievementRepository.findAllByMentorIdOrderByIdAsc(profile.getId()).stream()
                .map(this::toDetails)
                .toList();
    }

    private MentorAchievementDetails toDetails(MentorAchievement achievement) {
        return MentorAchievementDetails.builder()
                .id(achievement.getId())
                .title(achievement.getTitle())
                .description(achievement.getDescription())
                .achievementType(achievement.getAchievementType())
                .issuer(achievement.getIssuer())
                .achievedAt(achievement.getAchievedAt())
                .proofUrl(achievement.getProofUrl())
                .proofMediaId(achievement.getProofMediaId())
                .verified(achievement.getVerified())
                .build();
    }
}
