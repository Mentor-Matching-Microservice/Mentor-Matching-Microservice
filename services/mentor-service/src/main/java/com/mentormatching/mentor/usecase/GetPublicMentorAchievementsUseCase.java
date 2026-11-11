package com.mentormatching.mentor.usecase;

import java.util.List;

import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.entity.MentorAchievement;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorAchievementRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc danh sách thành tích của mentor để hiển thị công khai. */
@Service
@Transactional(readOnly = true)
public class GetPublicMentorAchievementsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAchievementRepository mentorAchievementRepository;

    public GetPublicMentorAchievementsUseCase(MentorProfileRepository mentorProfileRepository,
                                              MentorAchievementRepository mentorAchievementRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAchievementRepository = mentorAchievementRepository;
    }

    public List<MentorAchievementDetails> execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }
        if (!mentorProfileRepository.existsById(mentorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found.");
        }

        return mentorAchievementRepository.findAllByMentorIdOrderByIdAsc(mentorId).stream()
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

