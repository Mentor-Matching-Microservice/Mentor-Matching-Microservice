package com.mentormatching.booking.controller;

import com.mentormatching.booking.config.SecurityConfiguration;
import com.mentormatching.booking.dto.request.CreateBookingRequest;
import com.mentormatching.booking.dto.response.CreateBookingResponse;
import com.mentormatching.booking.entity.BookingStatus;
import com.mentormatching.booking.entity.MeetingType;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

@WebMvcTest(controllers = BookingController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class BookingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    BookingService bookingService;

    @Test
    void createBookingRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBookingSucceedsWithJwt() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);

        CreateBookingResponse mockResponse = CreateBookingResponse.builder()
                .bookingId(101L)
                .studentUserId("student-sub-123")
                .studentName("Student Name")
                .mentorId(10L)
                .mentorName("Mentor Name")
                .mentorSubjectId(5L)
                .subjectName("Mathematics")
                .gradeName("Grade 12")
                .bookingDate(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .pricePerHour(new BigDecimal("100000.00"))
                .totalAmount(new BigDecimal("200000.00"))
                .meetingType(MeetingType.ONLINE)
                .status(BookingStatus.PENDING)
                .holdExpiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingService.createBooking(eq("student-sub-123"), any(CreateBookingRequest.class)))
                .thenReturn(mockResponse);

        String json = String.format("""
                {
                    "mentorId": 10,
                    "mentorSubjectId": 5,
                    "bookingDate": "%s",
                    "startTime": "10:00:00",
                    "endTime": "12:00:00",
                    "meetingType": "ONLINE",
                    "note": "Test booking"
                }
                """, date);

        mvc.perform(post("/api/v1/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("student-sub-123")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(101)))
                .andExpect(jsonPath("$.data.totalAmount", is(200000.00)))
                .andExpect(jsonPath("$.data.status", is("PENDING")));
    }

    @Test
    void rejectBookingSucceedsWithMentorRole() throws Exception {
        mvc.perform(patch("/api/v1/bookings/50/reject")
                        .with(jwt().jwt(jwt -> jwt.subject("mentor-uuid-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MENTOR")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"Emergency schedule change\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(bookingService).rejectBookingByMentor("mentor-uuid-1", 50L, "Emergency schedule change");
    }

    @Test
    void completeBookingSucceedsWithMentorRole() throws Exception {
        mvc.perform(patch("/api/v1/bookings/60/complete")
                        .with(jwt().jwt(jwt -> jwt.subject("mentor-uuid-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MENTOR")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(bookingService).completeBookingByMentor("mentor-uuid-1", 60L);
    }

    @Test
    void forceCancelBookingSucceedsWithAdminRole() throws Exception {
        mvc.perform(patch("/api/v1/bookings/70/force-cancel")
                        .with(jwt().jwt(jwt -> jwt.subject("admin-uuid-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"Policy violation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(bookingService).forceCancelBooking("admin-uuid-1", 70L, "Policy violation");
    }

    @Test
    void forceCancelBookingForbiddenForLearner() throws Exception {
        mvc.perform(patch("/api/v1/bookings/70/force-cancel")
                        .with(jwt().jwt(jwt -> jwt.subject("learner-uuid-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_LEARNER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"I want to cancel\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAdminBookingsSucceedsForAdmin() throws Exception {
        when(bookingService.getBookings(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/v1/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("admin-uuid-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
