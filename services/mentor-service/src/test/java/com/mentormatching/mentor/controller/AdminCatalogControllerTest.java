package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.catalog.command.CreateGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.CreateSubjectCommand;
import com.mentormatching.mentor.dto.catalog.command.DeleteGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.DeleteSubjectCommand;
import com.mentormatching.mentor.dto.catalog.command.UpdateGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.UpdateSubjectCommand;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectDetails;
import com.mentormatching.mentor.entity.GradeLevelGroup;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.catalog.CreateGradeUseCase;
import com.mentormatching.mentor.service.catalog.CreateSubjectUseCase;
import com.mentormatching.mentor.service.catalog.DeleteGradeUseCase;
import com.mentormatching.mentor.service.catalog.DeleteSubjectUseCase;
import com.mentormatching.mentor.service.catalog.UpdateGradeUseCase;
import com.mentormatching.mentor.service.catalog.UpdateSubjectUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCatalogController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class AdminCatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    CreateSubjectUseCase createSubjectUseCase;
    @MockitoBean
    UpdateSubjectUseCase updateSubjectUseCase;
    @MockitoBean
    DeleteSubjectUseCase deleteSubjectUseCase;
    @MockitoBean
    CreateGradeUseCase createGradeUseCase;
    @MockitoBean
    UpdateGradeUseCase updateGradeUseCase;
    @MockitoBean
    DeleteGradeUseCase deleteGradeUseCase;

    @Test
    void anonymousCannotAccessAdminEndpoints() throws Exception {
        mvc.perform(post("/api/v1/admin/catalog/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "name": "Physics", "description": "Physics"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminGetsForbidden() throws Exception {
        mvc.perform(post("/api/v1/admin/catalog/subjects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MENTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "name": "Physics", "description": "Physics"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateSubject() throws Exception {
        SubjectDetails details = new SubjectDetails(10L, 1L, "Physics", "Physics desc");
        when(createSubjectUseCase.execute(any(CreateSubjectCommand.class))).thenReturn(details);

        mvc.perform(post("/api/v1/admin/catalog/subjects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "name": "Physics", "description": "Physics desc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(10)))
                .andExpect(jsonPath("$.data.name", is("Physics")));
    }

    @Test
    void adminDeleteSubjectReturns409WhenInUse() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "SUBJECT_IN_USE", "Subject cannot be deleted because it is assigned to mentors"))
                .when(deleteSubjectUseCase).execute(any(DeleteSubjectCommand.class));

        mvc.perform(delete("/api/v1/admin/catalog/subjects/{id}", 10L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SUBJECT_IN_USE")));
    }

    @Test
    void adminCanCreateGrade() throws Exception {
        GradeDetails details = new GradeDetails(12L, "Grade 12", GradeLevelGroup.HIGH_SCHOOL);
        when(createGradeUseCase.execute(any(CreateGradeCommand.class))).thenReturn(details);

        mvc.perform(post("/api/v1/admin/catalog/grades")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Grade 12", "levelGroup": "HIGH_SCHOOL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(12)))
                .andExpect(jsonPath("$.data.levelGroup", is("HIGH_SCHOOL")));
    }

    @Test
    void adminDeleteGradeReturns409WhenInUse() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "GRADE_IN_USE", "Grade cannot be deleted because it is assigned to mentors"))
                .when(deleteGradeUseCase).execute(any(DeleteGradeCommand.class));

        mvc.perform(delete("/api/v1/admin/catalog/grades/{id}", 12L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("GRADE_IN_USE")));
    }
}
