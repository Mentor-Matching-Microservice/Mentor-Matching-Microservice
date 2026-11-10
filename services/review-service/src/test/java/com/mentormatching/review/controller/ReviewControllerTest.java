package com.mentormatching.review.controller;

import com.mentormatching.review.config.CorsConfig;
import com.mentormatching.review.config.SecurityConfiguration;
import com.mentormatching.review.dto.request.CreateReviewRequest;
import com.mentormatching.review.dto.response.MentorRatingSummaryResponse;
import com.mentormatching.review.dto.response.ReviewDetailResponse;
import com.mentormatching.review.dto.response.ReviewTagOptionResponse;
import com.mentormatching.review.exception.GlobalExceptionHandler;
import com.mentormatching.review.exception.ProblemResponses;
import com.mentormatching.review.exception.RequestIdFilter;
import com.mentormatching.review.security.ProblemAccessDeniedHandler;
import com.mentormatching.review.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class, CorsConfig.class})
class ReviewControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    ReviewService reviewService;

    @Test
    void getTagOptionsIsPublic() throws Exception {
        ReviewTagOptionResponse opt = ReviewTagOptionResponse.builder()
                .id(1L)
                .name("Nhiệt tình")
                .description("Rất nhiệt tình")
                .build();

        when(reviewService.getTagOptions()).thenReturn(List.of(opt));

        mvc.perform(get("/api/v1/reviews/tag-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Nhiệt tình")));
    }

    @Test
    void getMentorRatingSummaryIsPublic() throws Exception {
        MentorRatingSummaryResponse summary = MentorRatingSummaryResponse.builder()
                .mentorId(5L)
                .averageRating(4.8)
                .totalReviews(10L)
                .ratingDistribution(Map.of(5, 8L, 4, 2L, 3, 0L, 2, 0L, 1, 0L))
                .build();

        when(reviewService.calculateMentorRatingSummary(5L)).thenReturn(summary);

        mvc.perform(get("/api/v1/reviews/mentor/5/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.averageRating", is(4.8)))
                .andExpect(jsonPath("$.data.totalReviews", is(10)));
    }

    @Test
    void createReviewRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\": 1, \"rating\": 5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReviewSucceedsWithJwt() throws Exception {
        ReviewDetailResponse created = ReviewDetailResponse.builder()
                .id(100L)
                .bookingId(10L)
                .studentUserId("student-sub-1")
                .rating(5)
                .comment("Great!")
                .createdAt(OffsetDateTime.now())
                .build();

        when(reviewService.createReview(eq("student-sub-1"), any(CreateReviewRequest.class)))
                .thenReturn(created);

        String json = """
                {
                    "bookingId": 10,
                    "rating": 5,
                    "comment": "Great!"
                }
                """;

        mvc.perform(post("/api/v1/reviews")
                        .with(jwt().jwt(j -> j.subject("student-sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(100)))
                .andExpect(jsonPath("$.data.rating", is(5)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReviewSucceedsForAdmin() throws Exception {
        mvc.perform(delete("/api/v1/reviews/100")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(reviewService).deleteReview(any(), eq(100L), eq(true));
    }
}
