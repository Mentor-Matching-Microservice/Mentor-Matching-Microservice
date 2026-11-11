package com.mentormatching.mentor.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.ReviewMentorApprovalAction;
import com.mentormatching.mentor.dto.command.ReviewMentorApprovalCommand;
import com.mentormatching.mentor.dto.response.AdminMentorApprovalResponse;
import com.mentormatching.mentor.dto.response.AdminMentorDetailResponse;
import com.mentormatching.mentor.dto.response.AdminMentorSummaryResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.entity.AccountStatus;
import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorApprovalStatus;
import com.mentormatching.mentor.entity.MentorVerificationStatus;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.usecase.AdminReviewMentorApprovalUseCase;
import com.mentormatching.mentor.usecase.GetAdminMentorDetailUseCase;
import com.mentormatching.mentor.usecase.GetAdminMentorsUseCase;
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

@WebMvcTest(controllers = AdminMentorController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class AdminMentorControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetAdminMentorsUseCase getAdminMentorsUseCase;

    @MockitoBean
    GetAdminMentorDetailUseCase getAdminMentorDetailUseCase;

    @MockitoBean
    AdminReviewMentorApprovalUseCase adminReviewMentorApprovalUseCase;

    final UUID adminId = UUID.randomUUID();

    @Test
    void listMentors_asAdmin_success() throws Exception {
        var summary = AdminMentorSummaryResponse.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .fullName("Jane Doe")
                .headline("Math Mentor")
                .approvalStatus(MentorApprovalStatus.PENDING)
                .accountStatus(AccountStatus.ACTIVE)
                .kycStatus(MentorVerificationStatus.VERIFIED)
                .activeSubjectsCount(2)
                .build();

        var pageResponse = PageResponse.<AdminMentorSummaryResponse>builder()
                .page(1)
                .pageSize(20)
                .totalItems(1)
                .totalPages(1)
                .data(List.of(summary))
                .build();

        when(getAdminMentorsUseCase.execute(any())).thenReturn(pageResponse);

        mvc.perform(get("/api/v1/admin/mentors")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.data", hasSize(1)))
                .andExpect(jsonPath("$.data.data[0].fullName", is("Jane Doe")));
    }

    @Test
    void listMentors_asNonAdmin_returns403() throws Exception {
        mvc.perform(get("/api/v1/admin/mentors")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_MENTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMentors_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/v1/admin/mentors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMentorDetail_asAdmin_success() throws Exception {
        var detail = AdminMentorDetailResponse.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .fullName("Jane Doe")
                .meetingType(MeetingType.ONLINE)
                .approvalStatus(MentorApprovalStatus.PENDING)
                .kycStatus(MentorVerificationStatus.VERIFIED)
                .kycVerificationId(10L)
                .subjects(List.of())
                .achievements(List.of())
                .personalities(List.of("Friendly"))
                .highlights(List.of("Top Rated"))
                .build();

        when(getAdminMentorDetailUseCase.execute(1L)).thenReturn(detail);

        mvc.perform(get("/api/v1/admin/mentors/1")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Jane Doe")))
                .andExpect(jsonPath("$.data.kycStatus", is("VERIFIED")));
    }

    @Test
    void reviewApproval_asAdmin_success() throws Exception {
        var response = AdminMentorApprovalResponse.builder()
                .mentorId(1L)
                .approvalStatus(MentorApprovalStatus.APPROVED)
                .approvedBy(adminId)
                .build();

        when(adminReviewMentorApprovalUseCase.execute(eq(adminId), eq(1L), any(ReviewMentorApprovalCommand.class)))
                .thenReturn(response);

        mvc.perform(patch("/api/v1/admin/mentors/1/approval")
                        .with(jwt().jwt(j -> j.subject(adminId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "action": "APPROVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")));
    }
}
