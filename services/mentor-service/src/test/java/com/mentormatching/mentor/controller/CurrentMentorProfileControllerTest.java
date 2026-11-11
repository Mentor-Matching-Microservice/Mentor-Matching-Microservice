package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.CreateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.command.UpdateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.response.MentorProfileDetails;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.usecase.CreateCurrentMentorProfileUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorProfileUseCase;
import com.mentormatching.mentor.usecase.UpdateCurrentMentorProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurrentMentorProfileController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class CurrentMentorProfileControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCurrentMentorProfileUseCase getCurrentMentorProfileUseCase;

    @MockitoBean
    CreateCurrentMentorProfileUseCase createCurrentMentorProfileUseCase;

    @MockitoBean
    UpdateCurrentMentorProfileUseCase updateCurrentMentorProfileUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void postCurrentMentorProfile_success() throws Exception {
        MentorProfileDetails response = MentorProfileDetails.builder()
                .id(1L)
                .userId(userId)
                .fullName("John Doe")
                .meetingType(MeetingType.ONLINE)
                .headline("Experienced Tutor")
                .introduction("Intro content")
                .build();

        when(createCurrentMentorProfileUseCase.execute(eq(userId), any(CreateCurrentMentorProfileCommand.class)))
                .thenReturn(response);

        mvc.perform(post("/api/v1/mentors/me")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "headline": "Experienced Tutor",
                                    "introduction": "Intro content",
                                    "meetingType": "ONLINE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    void postCurrentMentorProfile_conflictWhenAlreadyExists() throws Exception {
        when(createCurrentMentorProfileUseCase.execute(eq(userId), any(CreateCurrentMentorProfileCommand.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "PROFILE_ALREADY_EXISTS", "Mentor profile already exists."));

        mvc.perform(post("/api/v1/mentors/me")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "headline": "Experienced Tutor"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PROFILE_ALREADY_EXISTS")));
    }

    @Test
    void getCurrentMentorProfile_success() throws Exception {
        MentorProfileDetails response = MentorProfileDetails.builder()
                .id(1L)
                .userId(userId)
                .fullName("John Doe")
                .build();

        when(getCurrentMentorProfileUseCase.execute(userId)).thenReturn(response);

        mvc.perform(get("/api/v1/mentors/me")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    void putCurrentMentorProfile_success() throws Exception {
        MentorProfileDetails response = MentorProfileDetails.builder()
                .id(1L)
                .userId(userId)
                .fullName("John Updated")
                .meetingType(MeetingType.HYBRID)
                .build();

        when(updateCurrentMentorProfileUseCase.execute(eq(userId), any(UpdateCurrentMentorProfileCommand.class)))
                .thenReturn(response);

        mvc.perform(put("/api/v1/mentors/me")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "meetingType": "HYBRID",
                                    "headline": "Updated headline"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("John Updated")));
    }

    @Test
    void getCurrentMentorProfile_unauthenticatedReturns401() throws Exception {
        mvc.perform(get("/api/v1/mentors/me"))
                .andExpect(status().isUnauthorized());
    }
}
