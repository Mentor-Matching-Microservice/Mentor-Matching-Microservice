package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.entity.GradeLevelGroup;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.catalog.GetGradeDetailUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalCatalogController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class InternalCatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetGradeDetailUseCase gradeDetailUseCase;

    @Test
    void getGradeDetailIsAccessibleWithoutAuth() throws Exception {
        GradeDetails details = new GradeDetails(12L, "Grade 12", GradeLevelGroup.HIGH_SCHOOL);
        when(gradeDetailUseCase.execute(12L)).thenReturn(details);

        mvc.perform(get("/internal/v1/catalog/grades/{gradeId}", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(12)))
                .andExpect(jsonPath("$.data.name", is("Grade 12")));
    }
}

