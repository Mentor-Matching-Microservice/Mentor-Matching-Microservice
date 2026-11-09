package com.mentormatching.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import com.mentormatching.booking.config.CorsConfig;
import com.mentormatching.booking.config.SecurityConfiguration;
import com.mentormatching.booking.controller.BookingPaymentSummaryController;
import com.mentormatching.booking.domain.BookingRepository;
import com.mentormatching.booking.exception.GlobalExceptionHandler;
import com.mentormatching.booking.exception.ProblemResponses;
import com.mentormatching.booking.exception.RequestIdFilter;
import com.mentormatching.booking.security.ProblemAccessDeniedHandler;
import com.mentormatching.booking.security.ProblemAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingPaymentSummaryController.class)
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class, CorsConfig.class})
class BookingProblemDetailTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    BookingRepository bookings;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void notFoundBookingUsesRfc7807ProblemDetail() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookings.findById(bookingId)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/bookings/{id}/payment-summary", bookingId)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:mentor-matching:problem:resource-not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Booking not found"))
                .andExpect(jsonPath("$.instance").value("/api/v1/bookings/" + bookingId + "/payment-summary"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
