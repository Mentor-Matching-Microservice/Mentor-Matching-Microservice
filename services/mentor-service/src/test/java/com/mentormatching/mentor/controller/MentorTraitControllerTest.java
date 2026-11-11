package com.mentormatching.mentor.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.UpdateCurrentMentorTraitsCommand;
import com.mentormatching.mentor.dto.response.CurrentMentorTraitsResponse;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.GetCurrentMentorTraitsUseCase;
import com.mentormatching.mentor.service.UpdateCurrentMentorTraitsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MentorTraitController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class MentorTraitControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCurrentMentorTraitsUseCase getCurrentMentorTraitsUseCase;

    @MockitoBean
    UpdateCurrentMentorTraitsUseCase updateCurrentMentorTraitsUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getTraits_success() throws Exception {
        CurrentMentorTraitsResponse response = CurrentMentorTraitsResponse.builder()
                .personalityOptionIds(List.of(1L, 2L))
                .highlightOptionIds(List.of(10L))
                .build();

        when(getCurrentMentorTraitsUseCase.execute(userId)).thenReturn(response);

        mvc.perform(get("/api/v1/mentors/me/traits")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.personalityOptionIds", hasSize(2)))
                .andExpect(jsonPath("$.data.personalityOptionIds[0]", is(1)))
                .andExpect(jsonPath("$.data.highlightOptionIds", hasSize(1)));
    }

    @Test
    void updateTraits_success() throws Exception {
        CurrentMentorTraitsResponse response = CurrentMentorTraitsResponse.builder()
                .personalityOptionIds(List.of(1L))
                .highlightOptionIds(List.of())
                .build();

        when(updateCurrentMentorTraitsUseCase.execute(eq(userId), any(UpdateCurrentMentorTraitsCommand.class)))
                .thenReturn(response);

        mvc.perform(put("/api/v1/mentors/me/traits")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personalityOptionIds": [1],
                                    "highlightOptionIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.personalityOptionIds", hasSize(1)));
    }
}
