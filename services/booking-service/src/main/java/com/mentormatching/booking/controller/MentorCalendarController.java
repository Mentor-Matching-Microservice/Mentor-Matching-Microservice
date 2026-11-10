package com.mentormatching.booking.controller;

import com.mentormatching.booking.dto.ApiResponse;
import com.mentormatching.booking.dto.response.MentorAvailabilityResponse;
import com.mentormatching.booking.dto.response.MentorCalendarResponse;
import com.mentormatching.booking.service.MentorCalendarQueryService;
import com.mentormatching.booking.service.SchedulingService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mentors")
@RequiredArgsConstructor
@Validated
public class MentorCalendarController {

    private final MentorCalendarQueryService mentorCalendarQueryService;
    private final SchedulingService schedulingService;

    @GetMapping("/{mentorId}/calendar-booking")
    public ResponseEntity<ApiResponse<MentorCalendarResponse>> getMentorCalendar(
            @PathVariable @Min(1) Long mentorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        MentorCalendarResponse response = mentorCalendarQueryService.getMentorCalendar(mentorId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get mentor calendar successfully"));
    }

    @GetMapping("/{mentorId}/availabilities")
    public ResponseEntity<ApiResponse<List<MentorAvailabilityResponse>>> getMentorAvailabilities(
            @PathVariable @Min(1) Long mentorId) {
        List<MentorAvailabilityResponse> response = schedulingService.getMentorAvailabilities(mentorId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Get mentor availabilities successfully"));
    }
}

