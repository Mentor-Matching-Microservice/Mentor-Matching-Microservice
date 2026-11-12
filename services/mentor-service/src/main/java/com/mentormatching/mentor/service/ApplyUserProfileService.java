package com.mentormatching.mentor.service;

import java.util.UUID;

import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Áp dụng bản sao hồ sơ người dùng (fullName, avatarUrl) từ user-service vào hồ sơ mentor.
 */
@Service
public class ApplyUserProfileService {

    private final MentorProfileRepository mentorProfileRepository;

    public ApplyUserProfileService(MentorProfileRepository mentorProfileRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
    }

    @Transactional
    public int execute(UUID userId, String fullName, String avatarUrl, Long avatarMediaId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }
        return mentorProfileRepository.applyUserProfile(userId, fullName, avatarUrl, avatarMediaId);
    }
}

