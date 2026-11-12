package com.mentormatching.mentor.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.AccountStatusSnapshot;
import com.mentormatching.mentor.dto.response.AccountStatusesResponse;
import com.mentormatching.mentor.dto.response.MentorBookingContextResponse;
import com.mentormatching.mentor.entity.AccountStatus;
import com.mentormatching.mentor.entity.MentorProfile;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.repository.MentorProfileRepository;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.ApplyAccountStatusesService;
import com.mentormatching.mentor.service.ApplyMentorRatingService;
import com.mentormatching.mentor.service.ApplyUserProfileService;
import com.mentormatching.mentor.usecase.GetMentorBookingContextUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalMentorController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class InternalMentorControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetMentorBookingContextUseCase getMentorBookingContextUseCase;

    @MockitoBean
    MentorProfileRepository mentorProfileRepository;

    @MockitoBean
    ApplyUserProfileService applyUserProfileService;

    @MockitoBean
    ApplyAccountStatusesService applyAccountStatusesService;

    @MockitoBean
    ApplyMentorRatingService applyMentorRatingService;

    @Test
    void getBookingContext_success() throws Exception {
        MentorBookingContextResponse response = MentorBookingContextResponse.builder()
                .mentorId(1L)
                .mentorUserId("123e4567-e89b-12d3-a456-426614174000")
                .mentorName("John Doe")
                .meetingType("ONLINE")
                .mentorSubjectId(10L)
                .subjectName("Toán")
                .gradeName("Lớp 12")
                .pricePerHour(new BigDecimal("200000.00"))
                .currency("VND")
                .eligible(true)
                .active(true)
                .build();

        when(getMentorBookingContextUseCase.execute(1L, 10L)).thenReturn(response);

        mvc.perform(get("/internal/v1/mentors/{mentorId}/booking-context", 1L)
                        .param("mentorSubjectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentorId", is(1)))
                .andExpect(jsonPath("$.mentorUserId", is("123e4567-e89b-12d3-a456-426614174000")))
                .andExpect(jsonPath("$.mentorName", is("John Doe")))
                .andExpect(jsonPath("$.currency", is("VND")))
                .andExpect(jsonPath("$.eligible", is(true)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void updateRating_success() throws Exception {
        mvc.perform(put("/internal/v1/mentors/{mentorId}/rating", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "averageRating": 4.75,
                                    "totalReviews": 12,
                                    "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Mentor rating updated successfully")));

        verify(applyMentorRatingService).apply(1L, 4.75, 12L, 3L);
    }

    @Test
    void getMentorByUserId_success() throws Exception {
        UUID userId = UUID.randomUUID();
        MentorProfile profile = new MentorProfile();
        profile.setUserId(userId);
        profile.setFullName("John Doe");
        ReflectionTestUtils.setField(profile, "id", 1L);

        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        mvc.perform(get("/internal/v1/mentors/by-user/{userId}", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentorId", is(1)))
                .andExpect(jsonPath("$.mentorUserId", is(userId.toString())))
                .andExpect(jsonPath("$.fullName", is("John Doe")));
    }

    @Test
    void getMentorByUserId_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        mvc.perform(get("/internal/v1/mentors/by-user/{userId}", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("MENTOR_PROFILE_NOT_FOUND")));
    }

    @Test
    void getMentorByUserId_invalidUuid_returns400() throws Exception {
        mvc.perform(get("/internal/v1/mentors/by-user/{userId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")));
    }

    @Test
    void syncUserProfile_success() throws Exception {
        UUID userId = UUID.randomUUID();
        when(applyUserProfileService.execute(eq(userId), eq("New Name"), eq("https://avatar.png"), eq(55L)))
                .thenReturn(1);

        mvc.perform(put("/internal/v1/mentors/users/{userId}/profile-sync", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "New Name",
                                    "avatarUrl": "https://avatar.png",
                                    "avatarMediaId": 55
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(1)));
    }

    @Test
    void syncAccountStatuses_success() throws Exception {
        when(applyAccountStatusesService.execute(any(AccountStatusesResponse.class))).thenReturn(1);

        mvc.perform(put("/internal/v1/mentors/account-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "accounts": [
                                        {
                                            "userId": "123e4567-e89b-12d3-a456-426614174000",
                                            "status": "ACTIVE",
                                            "version": 1
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(1)));
    }
}
