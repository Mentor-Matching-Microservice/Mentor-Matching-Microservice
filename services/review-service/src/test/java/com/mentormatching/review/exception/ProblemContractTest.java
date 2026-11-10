package com.mentormatching.review.exception;

import com.mentormatching.review.config.CorsConfig;
import com.mentormatching.review.config.SecurityConfiguration;
import com.mentormatching.review.security.ProblemAccessDeniedHandler;
import com.mentormatching.review.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.review.controller.ReviewController;
import com.mentormatching.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProblemErrorController.class, ReviewController.class})
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class, CorsConfig.class})
class ProblemContractTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    ReviewService reviewService;

    @Test
    void unauthenticatedReturnsRfc7807ProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/reviews/me")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type", is("urn:mentor-matching:problem:authentication-required")))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.requestId").exists());
    }
}
