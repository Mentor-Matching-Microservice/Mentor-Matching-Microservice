package com.mentormatching.mentor.service;

import java.util.UUID;

import com.mentormatching.mentor.dto.response.CurrentMentorTraitsResponse;
import com.mentormatching.mentor.entity.MentorHighlight;
import com.mentormatching.mentor.entity.MentorPersonality;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorHighlightRepository;
import com.mentormatching.mentor.repository.MentorPersonalityRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc các tính cách và điểm nổi bật của mentor đang đăng nhập. */
@Service
@PreAuthorize("hasRole('MENTOR')")
@Transactional(readOnly = true)
public class GetCurrentMentorTraitsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorPersonalityRepository mentorPersonalityRepository;
    private final MentorHighlightRepository mentorHighlightRepository;

    public GetCurrentMentorTraitsUseCase(MentorProfileRepository mentorProfileRepository,
                                         MentorPersonalityRepository mentorPersonalityRepository,
                                         MentorHighlightRepository mentorHighlightRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorPersonalityRepository = mentorPersonalityRepository;
        this.mentorHighlightRepository = mentorHighlightRepository;
    }

    /**
     * Caller lấy userId từ JWT sub, không nhận mentorId từ request.
     *
     * @param userId UUID tài khoản mentor đã xác thực
     * @return hai danh sách option ID tăng dần; rỗng nếu chưa chọn
     * @throws ApiException 400 nếu userId null; 404 nếu chưa có profile mentor
     */
    public CurrentMentorTraitsResponse execute(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User ID is required.");
        }

        var profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));
        var mentorId = profile.getId();

        return CurrentMentorTraitsResponse.builder()
                .personalityOptionIds(mentorPersonalityRepository.findAllByMentorId(mentorId).stream()
                        .map(MentorPersonality::getPersonalityOptionId)
                        .sorted()
                        .toList())
                .highlightOptionIds(mentorHighlightRepository.findAllByMentorId(mentorId).stream()
                        .map(MentorHighlight::getHighlightOptionId)
                        .sorted()
                        .toList())
                .build();
    }
}
