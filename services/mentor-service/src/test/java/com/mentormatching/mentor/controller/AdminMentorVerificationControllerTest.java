package com.mentormatching.mentor.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.ReviewMentorVerificationAction;
import com.mentormatching.mentor.dto.command.ReviewMentorVerificationCommand;
import com.mentormatching.mentor.dto.response.AdminMentorVerificationDetailResponse;
import com.mentormatching.mentor.dto.response.AdminMentorVerificationItemResponse;
import com.mentormatching.mentor.dto.response.MentorVerificationReviewResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.usecase.GetAdminMentorVerificationDetailUseCase;
import com.mentormatching.mentor.usecase.GetAdminMentorVerificationsUseCase;
import com.mentormatching.mentor.usecase.ReviewMentorVerificationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminMentorVerificationController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class AdminMentorVerificationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetAdminMentorVerificationsUseCase getAdminMentorVerificationsUseCase;

    @MockitoBean
    GetAdminMentorVerificationDetailUseCase getAdminMentorVerificationDetailUseCase;

    @MockitoBean
    ReviewMentorVerificationUseCase reviewMentorVerificationUseCase;

    final UUID adminId = UUID.randomUUID();

    @Test
    void listVerifications_asAdmin_success() throws Exception {
        var item = AdminMentorVerificationItemResponse.builder()
                .id(1L)
                .mentorId(10L)
                .fullName("Nguyen Van A")
                .verificationStatus(MentorVerificationStatus.PENDING)
                .build();

        var pageResponse = PageResponse.<AdminMentorVerificationItemResponse>builder()
                .page(1)
                .pageSize(20)
                .totalItems(1)
                .totalPages(1)
                .data(List.of(item))
                .build();

        when(getAdminMentorVerificationsUseCase.execute(any())).thenReturn(pageResponse);

        mvc.perform(get("/api/v1/admin/mentor-verifications")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].fullName", is("Nguyen Van A")));
    }

    @Test
    void getVerificationDetail_asAdmin_success() throws Exception {
        var detail = AdminMentorVerificationDetailResponse.builder()
                .id(1L)
                .mentorId(10L)
                .userId(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .idCardNumber("012345678901")
                .idCardFrontUrl("https://cloudinary/front.jpg")
                .idCardFrontMediaId("media-front-123")
                .idCardBackUrl("https://cloudinary/back.jpg")
                .idCardBackMediaId("media-back-123")
                .verificationStatus(MentorVerificationStatus.PENDING)
                .build();

        when(getAdminMentorVerificationDetailUseCase.execute(1L)).thenReturn(detail);

        mvc.perform(get("/api/v1/admin/mentor-verifications/1")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.idCardFrontMediaId", is("media-front-123")))
                .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")));
    }

    @Test
    void reviewVerification_asAdmin_success() throws Exception {
        var response = MentorVerificationReviewResponse.builder()
                .id(1L)
                .mentorId(10L)
                .status(MentorVerificationStatus.VERIFIED)
                .verifiedBy(adminId)
                .build();

        when(reviewMentorVerificationUseCase.execute(eq(adminId), eq(1L), any(ReviewMentorVerificationCommand.class)))
                .thenReturn(response);

        mvc.perform(patch("/api/v1/admin/mentor-verifications/1")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "action": "VERIFY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("VERIFIED")));
    }

    @Test
    void listVerifications_asNonAdmin_returns403() throws Exception {
        mvc.perform(get("/api/v1/admin/mentor-verifications")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_MENTOR"))))
                .andExpect(status().isForbidden());
    }
}
