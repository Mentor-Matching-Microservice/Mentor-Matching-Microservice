package com.mentormatching.booking.dto.request;

import com.mentormatching.booking.entity.AvailabilityType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMentorAvailabilityRequest {

    private Long mentorId;

    @NotNull
    private AvailabilityType availabilityType;

    @Min(1)
    @Max(7)
    private Integer dayOfWeek;

    private LocalDate availableDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}

