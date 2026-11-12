package com.mentormatching.mentor.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.command.UpsertCurrentMentorSubjectCommand;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.DeleteCurrentMentorSubjectUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorSubjectsUseCase;
import com.mentormatching.mentor.usecase.UpsertCurrentMentorSubjectUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MentorSubjectController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class MentorSubjectControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCurrentMentorSubjectsUseCase getCurrentMentorSubjectsUseCase;

    @MockitoBean
    UpsertCurrentMentorSubjectUseCase upsertCurrentMentorSubjectUseCase;

    @MockitoBean
    DeleteCurrentMentorSubjectUseCase deleteCurrentMentorSubjectUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getSubjects_success() throws Exception {
        MentorSubjectDetails details = MentorSubjectDetails.builder()
                .id(1L)
                .subjectGradeId(10L)
                .pricePerHour(new BigDecimal("200000.00"))
                .active(true)
                .build();

        when(getCurrentMentorSubjectsUseCase.execute(userId)).thenReturn(List.of(details));

        mvc.perform(get("/api/v1/mentors/me/subjects")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].subjectGradeId", is(10)));
    }

    @Test
    void upsertSubject_success() throws Exception {
        MentorSubjectDetails details = MentorSubjectDetails.builder()
                .id(1L)
                .subjectGradeId(10L)
                .pricePerHour(new BigDecimal("250000.00"))
                .active(true)
                .build();

        when(upsertCurrentMentorSubjectUseCase.execute(eq(userId), any(UpsertCurrentMentorSubjectCommand.class)))
                .thenReturn(details);

        mvc.perform(put("/api/v1/mentors/me/subjects")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "subjectGradeId": 10,
                                    "proficiencyLevel": "INTERMEDIATE",
                                    "pricePerHour": 250000.00,
                                    "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.pricePerHour", is(250000.00)));
    }

    @Test
    void upsertSubject_notFoundSubjectGrade_returns404() throws Exception {
        when(upsertCurrentMentorSubjectUseCase.execute(eq(userId), any(UpsertCurrentMentorSubjectCommand.class)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "SUBJECT_GRADE_NOT_FOUND", "Subject grade not found."));

        mvc.perform(put("/api/v1/mentors/me/subjects")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "subjectGradeId": 999,
                                    "proficiencyLevel": "INTERMEDIATE",
                                    "pricePerHour": 250000.00,
                                    "active": true
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("SUBJECT_GRADE_NOT_FOUND")));
    }

    @Test
    void upsertSubject_invalidPrice_returns400() throws Exception {
        mvc.perform(put("/api/v1/mentors/me/subjects")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "subjectGradeId": 10,
                                    "pricePerHour": -100.00,
                                    "active": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteSubject_success() throws Exception {
        doNothing().when(deleteCurrentMentorSubjectUseCase).execute(userId, 1L);

        mvc.perform(delete("/api/v1/mentors/me/subjects/{subjectId}", 1L)
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}

