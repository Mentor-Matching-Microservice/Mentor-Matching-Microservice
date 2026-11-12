package com.mentormatching.mentor.service;

import com.mentormatching.mentor.repository.MentorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplyMentorRatingService {

    private final MentorProfileRepository mentorProfileRepository;

    @Transactional
    public boolean apply(Long mentorId, Double averageRating, Long totalReviews, long version) {
        if (mentorId == null || mentorId <= 0) {
            return false;
        }
        int updated = mentorProfileRepository.applyMentorRating(mentorId, averageRating, totalReviews, version);
        return updated > 0;
    }
}
