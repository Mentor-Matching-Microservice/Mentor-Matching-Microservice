package com.mentormatching.booking.dto.response;

import com.mentormatching.booking.entity.Booking;
import com.mentormatching.booking.entity.BookingStatus;
import lombok.Builder;

@Builder
public record BookingReviewEligibilityResponse(
        Long bookingId,
        String studentUserId,
        Long mentorId,
        BookingStatus status,
        boolean eligibleForReview
) {
    public static BookingReviewEligibilityResponse from(Booking booking) {
        return BookingReviewEligibilityResponse.builder()
                .bookingId(booking.getId())
                .studentUserId(booking.getStudentUserId())
                .mentorId(booking.getMentorId())
                .status(booking.getStatus())
                .eligibleForReview(booking.getStatus() == BookingStatus.COMPLETED)
                .build();
    }
}
