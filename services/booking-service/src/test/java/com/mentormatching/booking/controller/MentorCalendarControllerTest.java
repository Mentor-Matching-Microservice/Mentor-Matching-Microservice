package com.mentormatching.booking.controller;

import com.mentormatching.booking.config.SecurityConfiguration;
import com.mentormatching.booking.dto.response.MentorCalendarDateResponse;
import com.mentormatching.booking.dto.response.MentorCalendarResponse;
import com.mentormatching.booking.dto.response.MentorCalendarWindowResponse;
import com.mentormatching.booking.exception.GlobalExceptionHandler;
import com.mentormatching.booking.exception.ProblemResponses;
import com.mentormatching.booking.exception.RequestIdFilter;
import com.mentormatching.booking.security.ProblemAccessDeniedHandler;
import com.mentormatching.booking.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.booking.service.MentorCalendarQueryService;
import com.mentormatching.booking.service.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MentorCalendarController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class MentorCalendarControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    MentorCalendarQueryService mentorCalendarQueryService;

    @MockitoBean
    SchedulingService schedulingService;

    @Test
    void getMentorCalendarIsPubliclyAccessible() throws Exception {
        LocalDate from = LocalDate.of(2026, 6, 15);
        LocalDate to = LocalDate.of(2026, 6, 16);

        MentorCalendarResponse mockResponse = MentorCalendarResponse.builder()
                .mentorId(1L)
                .from(from)
                .to(to)
                .dates(List.of(
                        MentorCalendarDateResponse.builder()
                                .date(from)
                                .windows(List.of(
                                        MentorCalendarWindowResponse.builder()
                                                .startTime(LocalTime.of(9, 0))
                                                .endTime(LocalTime.of(11, 0))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(mentorCalendarQueryService.getMentorCalendar(1L, from, to)).thenReturn(mockResponse);

        mvc.perform(get("/api/v1/mentors/1/calendar-booking?from=2026-06-15&to=2026-06-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.mentorId", is(1)))
                .andExpect(jsonPath("$.data.dates[0].windows[0].startTime", is("09:00:00")));
    }
}
