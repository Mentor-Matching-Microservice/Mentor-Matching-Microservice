package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.UpsertCurrentMentorVerificationCommand;
import com.mentormatching.mentor.dto.response.CurrentMentorVerificationResponse;
import com.mentormatching.mentor.dto.response.CurrentMentorVerificationStatusResponse;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.GetCurrentMentorVerificationStatusUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorVerificationUseCase;
import com.mentormatching.mentor.usecase.UpsertCurrentMentorVerificationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MentorVerificationController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class MentorVerificationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCurrentMentorVerificationStatusUseCase getCurrentMentorVerificationStatusUseCase;

    @MockitoBean
    GetCurrentMentorVerificationUseCase getCurrentMentorVerificationUseCase;

    @MockitoBean
    UpsertCurrentMentorVerificationUseCase upsertCurrentMentorVerificationUseCase;

    final UUID userId = UUID.randomUUID();

    @Test
    void getStatus_authenticated_success() throws Exception {
        var statusResponse = CurrentMentorVerificationStatusResponse.builder()
                .verificationStatus(MentorVerificationStatus.PENDING)
                .build();

        when(getCurrentMentorVerificationStatusUseCase.execute(userId)).thenReturn(statusResponse);

        mvc.perform(get("/api/v1/mentors/me/verification/status")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.verificationStatus", is("PENDING")));
    }

    @Test
    void getVerification_authenticated_success() throws Exception {
        var response = CurrentMentorVerificationResponse.builder()
                .id(1L)
                .mentorId(10L)
                .fullName("Nguyen Van A")
                .idCardNumber("123456789")
                .idCardFrontUrl("https://cloudinary/front.jpg")
                .idCardFrontMediaId("media-front-1")
                .idCardBackUrl("https://cloudinary/back.jpg")
                .idCardBackMediaId("media-back-1")
                .verificationStatus(MentorVerificationStatus.PENDING)
                .build();

        when(getCurrentMentorVerificationUseCase.execute(userId)).thenReturn(response);

        mvc.perform(get("/api/v1/mentors/me/verification")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.idCardFrontMediaId", is("media-front-1")))
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")));
    }

    @Test
    void putVerification_validCommand_success() throws Exception {
        var response = CurrentMentorVerificationResponse.builder()
                .id(1L)
                .mentorId(10L)
                .fullName("Nguyen Van A")
                .idCardNumber("123456789")
                .idCardFrontUrl("https://cloudinary/front.jpg")
                .idCardFrontMediaId("media-front-1")
                .idCardBackUrl("https://cloudinary/back.jpg")
                .idCardBackMediaId("media-back-1")
                .verificationStatus(MentorVerificationStatus.PENDING)
                .build();

        when(upsertCurrentMentorVerificationUseCase.execute(eq(userId), any(UpsertCurrentMentorVerificationCommand.class)))
                .thenReturn(response);

        mvc.perform(put("/api/v1/mentors/me/verification")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "Nguyen Van A",
                                    "idCardNumber": "123456789",
                                    "idCardFrontUrl": "https://cloudinary/front.jpg",
                                    "idCardFrontMediaId": "media-front-1",
                                    "idCardBackUrl": "https://cloudinary/back.jpg",
                                    "idCardBackMediaId": "media-back-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")));
    }
}
