package com.mentormatching.mentor.usecase;

import java.util.List;

import com.mentormatching.mentor.dto.response.MentorOptionResponse;
import com.mentormatching.mentor.dto.response.MentorTraitsDetailResponse;
import com.mentormatching.mentor.entity.MentorHighlight;
import com.mentormatching.mentor.entity.MentorPersonality;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorHighlightOptionRepository;
import com.mentormatching.mentor.repository.MentorHighlightRepository;
import com.mentormatching.mentor.repository.MentorPersonalityRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.PersonalityOptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc tính cách và điểm nổi bật chi tiết của mentor để hiển thị công khai. */
@Service
@Transactional(readOnly = true)
public class GetPublicMentorTraitsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorPersonalityRepository mentorPersonalityRepository;
    private final PersonalityOptionRepository personalityOptionRepository;
    private final MentorHighlightRepository mentorHighlightRepository;
    private final MentorHighlightOptionRepository mentorHighlightOptionRepository;

    public GetPublicMentorTraitsUseCase(MentorProfileRepository mentorProfileRepository,
                                        MentorPersonalityRepository mentorPersonalityRepository,
                                        PersonalityOptionRepository personalityOptionRepository,
                                        MentorHighlightRepository mentorHighlightRepository,
                                        MentorHighlightOptionRepository mentorHighlightOptionRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorPersonalityRepository = mentorPersonalityRepository;
        this.personalityOptionRepository = personalityOptionRepository;
        this.mentorHighlightRepository = mentorHighlightRepository;
        this.mentorHighlightOptionRepository = mentorHighlightOptionRepository;
    }

    public MentorTraitsDetailResponse execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }
        if (!mentorProfileRepository.existsById(mentorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found.");
        }

        List<Long> personalityIds = mentorPersonalityRepository.findAllByMentorId(mentorId).stream()
                .map(MentorPersonality::getPersonalityOptionId)
                .toList();
        List<MentorOptionResponse> personalities = personalityOptionRepository.findAllById(personalityIds).stream()
                .map(p -> MentorOptionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .build())
                .toList();

        List<Long> highlightIds = mentorHighlightRepository.findAllByMentorId(mentorId).stream()
                .map(MentorHighlight::getHighlightOptionId)
                .toList();
        List<MentorOptionResponse> highlights = mentorHighlightOptionRepository.findAllById(highlightIds).stream()
                .map(h -> MentorOptionResponse.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .description(h.getDescription())
                        .build())
                .toList();

        return MentorTraitsDetailResponse.builder()
                .personalities(personalities)
                .highlights(highlights)
                .build();
    }
}

