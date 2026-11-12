package com.mentormatching.mentor.service;

import com.mentormatching.mentor.dto.response.MentorOptionResponse;
import com.mentormatching.mentor.dto.response.MentorTraitsDetailResponse;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorHighlightOptionRepository;
import com.mentormatching.mentor.repository.MentorHighlightRepository;
import com.mentormatching.mentor.repository.MentorPersonalityRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.MentorSearchSpecifications;
import com.mentormatching.mentor.repository.PersonalityOptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc tên và mô tả các traits của mentor công khai. */
@Service
@Transactional(readOnly = true)
public class GetMentorTraitsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorPersonalityRepository mentorPersonalityRepository;
    private final MentorHighlightRepository mentorHighlightRepository;
    private final PersonalityOptionRepository personalityOptionRepository;
    private final MentorHighlightOptionRepository mentorHighlightOptionRepository;

    public GetMentorTraitsUseCase(MentorProfileRepository mentorProfileRepository,
                                  MentorPersonalityRepository mentorPersonalityRepository,
                                  MentorHighlightRepository mentorHighlightRepository,
                                  PersonalityOptionRepository personalityOptionRepository,
                                  MentorHighlightOptionRepository mentorHighlightOptionRepository) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorPersonalityRepository = mentorPersonalityRepository;
        this.mentorHighlightRepository = mentorHighlightRepository;
        this.personalityOptionRepository = personalityOptionRepository;
        this.mentorHighlightOptionRepository = mentorHighlightOptionRepository;
    }

    public MentorTraitsDetailResponse execute(Long mentorId) {
        if (mentorId == null || mentorId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Mentor ID must be positive.");
        }
        mentorProfileRepository.findOne(MentorSearchSpecifications.visible()
                        .and((root, query, cb) -> cb.equal(root.get("id"), mentorId)))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND", "Mentor profile not found."));

        var personalityIds = mentorPersonalityRepository.findAllByMentorId(mentorId).stream()
                .map(link -> link.getPersonalityOptionId()).toList();
        var highlightIds = mentorHighlightRepository.findAllByMentorId(mentorId).stream()
                .map(link -> link.getHighlightOptionId()).toList();

        var personalities = personalityOptionRepository.findAllById(personalityIds).stream()
                .map(option -> MentorOptionResponse.builder()
                        .id(option.getId()).name(option.getName()).description(option.getDescription()).build())
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
        var highlights = mentorHighlightOptionRepository.findAllById(highlightIds).stream()
                .map(option -> MentorOptionResponse.builder()
                        .id(option.getId()).name(option.getName()).description(option.getDescription()).build())
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
        return MentorTraitsDetailResponse.builder()
                .personalities(personalities)
                .highlights(highlights)
                .build();
    }
}
