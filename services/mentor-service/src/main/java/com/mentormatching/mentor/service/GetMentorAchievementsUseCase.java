package com.mentormatching.mentor.service;

import java.util.List;

import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorAchievementRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSearchSpecifications;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc thành tích của mentor công khai. */
@Service
@Transactional(readOnly = true)
public class GetMentorAchievementsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAchievementRepository mentorAchievementRepository;

    public GetMentorAchievementsUseCase(MentorProfileRepository mentorProfileRepository,
                                        MentorAchievementRepository mentorAchievementRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAchievementRepository = mentorAchievementRepository;
    }

    public List<MentorAchievementDetails> execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }
        mentorProfileRepository.findOne(MentorSearchSpecifications.visible()
                        .and((root, query, cb) -> cb.equal(root.get("id"), mentorId)))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        return mentorAchievementRepository.findAllByMentorIdOrderByIdAsc(mentorId).stream()
                .map(achievement -> MentorAchievementDetails.builder()
                        .id(achievement.getId())
                        .title(achievement.getTitle())
                        .description(achievement.getDescription())
                        .achievementType(achievement.getAchievementType())
                        .issuer(achievement.getIssuer())
                        .achievedAt(achievement.getAchievedAt())
                        .proofUrl(achievement.getProofUrl())
                        .verified(achievement.getVerified())
                        .build())
                .toList();
    }
}
