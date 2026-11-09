package com.mentormatching.booking.controller;

import com.mentormatching.booking.config.SecurityConfiguration;
import com.mentormatching.booking.dto.request.CreateMentorAvailabilityRequest;
import com.mentormatching.booking.dto.response.MentorAvailabilityResponse;
import com.mentormatching.booking.entity.AvailabilityType;
import com.mentormatching.booking.exception.GlobalExceptionHandler;
import com.mentormatching.booking.exception.ProblemResponses;
import com.mentormatching.booking.exception.RequestIdFilter;
import com.mentormatching.booking.security.ProblemAccessDeniedHandler;
import com.mentormatching.booking.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.booking.service.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SchedulingController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class SchedulingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    SchedulingService schedulingService;

    @Test
    void getMyAvailabilitiesRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/scheduling/me/availabilities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyAvailabilitiesSucceedsWithJwt() throws Exception {
        MentorAvailabilityResponse item = MentorAvailabilityResponse.builder()
                .id(1L)
                .mentorId(10L)
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        when(schedulingService.getCurrentMentorAvailabilities(eq("user-sub-123"), any()))
                .thenReturn(List.of(item));

        mvc.perform(get("/api/v1/scheduling/me/availabilities")
                        .with(jwt().jwt(jwt -> jwt.subject("user-sub-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].dayOfWeek", is(1)));
    }

    @Test
    void createMyAvailabilitySucceedsWithJwt() throws Exception {
        MentorAvailabilityResponse created = MentorAvailabilityResponse.builder()
                .id(2L)
                .mentorId(10L)
                .availabilityType(AvailabilityType.RECURRING)
                .dayOfWeek(2)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .build();

        when(schedulingService.createCurrentMentorAvailability(eq("user-sub-123"), any(CreateMentorAvailabilityRequest.class)))
                .thenReturn(created);

        String json = """
                {
                    "availabilityType": "RECURRING",
                    "dayOfWeek": 2,
                    "startTime": "14:00:00",
                    "endTime": "16:00:00"
                }
                """;

        mvc.perform(post("/api/v1/scheduling/me/availabilities")
                        .with(jwt().jwt(jwt -> jwt.subject("user-sub-123")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(2)));
    }
}
