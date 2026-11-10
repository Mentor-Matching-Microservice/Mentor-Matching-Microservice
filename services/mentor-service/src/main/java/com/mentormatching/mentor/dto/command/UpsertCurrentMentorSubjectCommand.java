package com.mentormatching.mentor.dto.command;

import java.math.BigDecimal;

import com.mentormatching.mentor.entity.ProficiencyLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

/** ID null tạo môn dạy mới; ID có giá trị thay thế toàn bộ trường được quản lý của môn đó. */
@Builder
public record UpsertCurrentMentorSubjectCommand(
        @Positive Long id,
        @NotNull @Positive Long subjectGradeId,
        @NotNull ProficiencyLevel proficiencyLevel,
        String teachingNote,
        @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal pricePerHour,
        @NotNull Boolean active) {
}
