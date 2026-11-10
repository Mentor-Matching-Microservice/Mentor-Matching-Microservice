package com.mentormatching.mentor.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mentormatching.mentor.dto.command.UpdateCurrentMentorTraitsCommand;
import com.mentormatching.mentor.dto.response.CurrentMentorTraitsResponse;
import com.mentormatching.mentor.entity.MentorHighlight;
import com.mentormatching.mentor.entity.MentorPersonality;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.MentorHighlightOptionRepository;
import com.mentormatching.mentor.repository.MentorHighlightRepository;
import com.mentormatching.mentor.repository.MentorPersonalityRepository;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.repository.PersonalityOptionRepository;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thay tập tính cách và điểm nổi bật của mentor đang đăng nhập.
 */
@Service
@PreAuthorize("hasRole('MENTOR')")
public class UpdateCurrentMentorTraitsUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final PersonalityOptionRepository personalityOptionRepository;
    private final MentorHighlightOptionRepository mentorHighlightOptionRepository;
    private final MentorPersonalityRepository mentorPersonalityRepository;
    private final MentorHighlightRepository mentorHighlightRepository;
    private final Validator validator;

    public UpdateCurrentMentorTraitsUseCase(MentorProfileRepository mentorProfileRepository,
                                            PersonalityOptionRepository personalityOptionRepository,
                                            MentorHighlightOptionRepository mentorHighlightOptionRepository,
                                            MentorPersonalityRepository mentorPersonalityRepository,
                                            MentorHighlightRepository mentorHighlightRepository,
                                            Validator validator) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.personalityOptionRepository = personalityOptionRepository;
        this.mentorHighlightOptionRepository = mentorHighlightOptionRepository;
        this.mentorPersonalityRepository = mentorPersonalityRepository;
        this.mentorHighlightRepository = mentorHighlightRepository;
        this.validator = validator;
    }

    /**
     * Caller lấy userId từ JWT sub. Cả hai danh sách đều bắt buộc; danh sách rỗng xóa hết
     * lựa chọn của nhóm tương ứng. Các ID được loại trùng và sắp tăng trong command.
     *
     * @param userId  UUID tài khoản mentor đã xác thực
     * @param command hai tập option ID mong muốn
     * @return hai tập option ID đã lưu
     * @throws ApiException 400 nếu đầu vào hoặc option ID sai; 404 nếu thiếu profile
     */
    @Transactional
    public CurrentMentorTraitsResponse execute(UUID userId, UpdateCurrentMentorTraitsCommand command) {
        if (userId == null || command == null || !validator.validate(command).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid mentor traits.");
        }

        var profile = mentorProfileRepository.findByUserIdForTraitsUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MENTOR_PROFILE_NOT_FOUND",
                        "Mentor profile not found."));

        validateOptionIds(
                command.personalityOptionIds(),
                personalityOptionRepository.findAllById(command.personalityOptionIds())
                        .stream()
                        .map(option -> option.getId())
                        .collect(Collectors.toSet()),
                "INVALID_PERSONALITY_OPTION"
        );

        validateOptionIds(
                command.highlightOptionIds(),
                mentorHighlightOptionRepository.findAllById(command.highlightOptionIds())
                        .stream()
                        .map(option -> option.getId())
                        .collect(Collectors.toSet()),
                "INVALID_HIGHLIGHT_OPTION"
        );

        var mentorId = profile.getId();
        updatePersonalities(mentorId, command.personalityOptionIds());
        updateHighlights(mentorId, command.highlightOptionIds());

        return CurrentMentorTraitsResponse.builder()
                .personalityOptionIds(command.personalityOptionIds())
                .highlightOptionIds(command.highlightOptionIds())
                .build();
    }

    private void validateOptionIds(List<Long> requested, Set<Long> found, String code) {
        if (!found.containsAll(requested)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "One or more mentor options are invalid.");
        }
    }

    private void updatePersonalities(Long mentorId, List<Long> requested) {
        // Tìm tất cả mentor personality hiện có
        var existing = mentorPersonalityRepository.findAllByMentorId(mentorId);
        // Lưu các ID hiện có thành vào Set
        var existingIds = existing.stream().map(MentorPersonality::getPersonalityOptionId).collect(Collectors.toSet());
        var requestedIds = Set.copyOf(requested);
        // Lấy danh sách cần xoá
        var removed = existing.stream()
                .filter(link -> !requestedIds.contains(link.getPersonalityOptionId()))
                .toList();
        if (!removed.isEmpty()) {
            mentorPersonalityRepository.deleteAllInBatch(removed);
        }
        var added = requested.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> {
                    var link = new MentorPersonality();
                    link.setMentorId(mentorId);
                    link.setPersonalityOptionId(id);
                    return link;
                })
                .toList();
        if (!added.isEmpty()) {
            mentorPersonalityRepository.saveAll(added);
        }
    }

    private void updateHighlights(Long mentorId, List<Long> requested) {
        var existing = mentorHighlightRepository.findAllByMentorId(mentorId);
        var existingIds = existing.stream().map(MentorHighlight::getHighlightOptionId).collect(Collectors.toSet());
        var requestedIds = Set.copyOf(requested);
        var removed = existing.stream()
                .filter(link -> !requestedIds.contains(link.getHighlightOptionId()))
                .toList();
        if (!removed.isEmpty()) {
            mentorHighlightRepository.deleteAllInBatch(removed); // Xoá tất cả thay vì deleteAll (duyệt qua for each)
        }
        var added = requested.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> {
                    var link = new MentorHighlight();
                    link.setMentorId(mentorId);
                    link.setHighlightOptionId(id);
                    return link;
                })
                .toList();
        if (!added.isEmpty()) {
            mentorHighlightRepository.saveAll(added);
        }
    }
}
