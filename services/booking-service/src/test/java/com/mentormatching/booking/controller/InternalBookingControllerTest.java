package com.mentormatching.booking.controller;

import com.mentormatching.booking.config.SecurityConfiguration;
import com.mentormatching.booking.dto.response.BookingPaymentContextResponse;
import com.mentormatching.booking.dto.response.BookingReviewEligibilityResponse;
import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.exception.GlobalExceptionHandler;
import com.mentormatching.booking.exception.ProblemResponses;
import com.mentormatching.booking.exception.RequestIdFilter;
import com.mentormatching.booking.security.ProblemAccessDeniedHandler;
import com.mentormatching.booking.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalBookingController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class InternalBookingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    BookingService bookingService;

    @Test
    void getPaymentContextIsPermittedWithoutAuth() throws Exception {
        BookingPaymentContextResponse context = BookingPaymentContextResponse.builder()
                .bookingId(10L)
                .studentUserId("student-1")
                .mentorId(5L)
                .mentorUserId("mentor-1")
                .totalAmount(new BigDecimal("350000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.getPaymentContext(10L)).thenReturn(context);

        mvc.perform(get("/internal/v1/bookings/10/payment-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(10)))
                .andExpect(jsonPath("$.data.totalAmount", is(350000.00)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    void getPaymentContextSupportsUuidPath() throws Exception {
        UUID uuid = new UUID(0L, 10L);
        BookingPaymentContextResponse context = BookingPaymentContextResponse.builder()
                .bookingId(10L)
                .studentUserId("student-1")
                .mentorId(5L)
                .mentorUserId("mentor-1")
                .totalAmount(new BigDecimal("350000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.getPaymentContext(10L)).thenReturn(context);

        mvc.perform(get("/internal/v1/bookings/" + uuid + "/payment-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(10)));
    }

    @Test
    void getReviewEligibilityIsPermittedWithoutAuth() throws Exception {
        BookingReviewEligibilityResponse response = BookingReviewEligibilityResponse.builder()
                .bookingId(10L)
                .studentUserId("student-1")
                .mentorId(5L)
                .status(BookingStatus.COMPLETED)
                .eligibleForReview(true)
                .build();

        when(bookingService.getReviewEligibility(10L)).thenReturn(response);

        mvc.perform(get("/internal/v1/bookings/10/review-eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eligibleForReview", is(true)));
    }
}
