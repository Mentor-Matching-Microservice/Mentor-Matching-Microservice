package com.mentormatching.mentor.dto.command;

import java.util.Comparator;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

/**
 * Hai danh sách là trạng thái mong muốn sau cập nhật; danh sách rỗng bỏ hết lựa chọn.
 */
@Builder
public record UpdateCurrentMentorTraitsCommand(
        @NotNull List<@NotNull @Positive Long> personalityOptionIds,
        @NotNull List<@NotNull @Positive Long> highlightOptionIds) {

    public UpdateCurrentMentorTraitsCommand {
        personalityOptionIds = normalize(personalityOptionIds);
        highlightOptionIds = normalize(highlightOptionIds);
    }

    private static List<Long> normalize(List<Long> ids) {
        return ids == null ? null : ids.stream()
                .distinct()
                .sorted(Comparator.nullsFirst(Long::compareTo))
                .toList();
    }
}
