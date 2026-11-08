package com.mentormatching.booking.dto.request;

import com.mentormatching.booking.entity.MeetingType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateBookingRequest {

    @NotNull(message = "Mentor id is required")
    private Long mentorId;

    @NotNull(message = "Mentor subject id is required")
    private Long mentorSubjectId;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date must not be in the past")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Meeting type is required")
    private MeetingType meetingType;

    @Size(max = 1000, message = "Booking note must not exceed 1000 characters")
    private String note;
}

