package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.catalog.query.GetSubjectGradeIdsQuery;
import com.mentormatching.mentor.dto.catalog.response.CatalogOptions;
import com.mentormatching.mentor.dto.catalog.response.CategoryDetails;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectGradeDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectGradeSummary;
import com.mentormatching.mentor.entity.GradeLevelGroup;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.catalog.GetCatalogOptionsUseCase;
import com.mentormatching.mentor.service.catalog.GetSubjectGradeIdsUseCase;
import com.mentormatching.mentor.service.catalog.GetSubjectGradeSummaryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class CatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCatalogOptionsUseCase optionsUseCase;
    @MockitoBean
    GetSubjectGradeIdsUseCase idsUseCase;
    @MockitoBean
    GetSubjectGradeSummaryUseCase summaryUseCase;

    @Test
    void getOptionsIsPubliclyAccessible() throws Exception {
        CatalogOptions options = CatalogOptions.builder()
                .categories(List.of(new CategoryDetails(1L, "Academics", "School subjects")))
                .subjects(List.of(new SubjectDetails(10L, 1L, "Math", "Math desc")))
                .grades(List.of(new GradeDetails(12L, "Grade 12", GradeLevelGroup.HIGH_SCHOOL)))
                .subjectGrades(List.of(new SubjectGradeDetails(100L, 10L, 12L)))
                .build();

        when(optionsUseCase.execute()).thenReturn(options);

        mvc.perform(get("/api/v1/catalog/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.categories", hasSize(1)))
                .andExpect(jsonPath("$.data.subjects", hasSize(1)))
                .andExpect(jsonPath("$.data.grades", hasSize(1)))
                .andExpect(jsonPath("$.data.subjectGrades", hasSize(1)));
    }

    @Test
    void getSubjectGradeIdsIsPubliclyAccessible() throws Exception {
        when(idsUseCase.execute(any(GetSubjectGradeIdsQuery.class))).thenReturn(List.of(100L, 101L));

        mvc.perform(get("/api/v1/catalog/subject-grades/ids")
                        .param("subjectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0]", is(100)));
    }

    @Test
    void getSubjectGradeSummaryIsPubliclyAccessible() throws Exception {
        SubjectGradeSummary summary = SubjectGradeSummary.builder()
                .subjectGradeId(100L)
                .subjectId(10L)
                .subjectName("Mathematics")
                .gradeId(12L)
                .gradeName("Grade 12")
                .build();

        when(summaryUseCase.execute(100L)).thenReturn(summary);

        mvc.perform(get("/api/v1/catalog/subject-grades/{id}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.subjectGradeId", is(100)))
                .andExpect(jsonPath("$.data.subjectName", is("Mathematics")));
    }
}
