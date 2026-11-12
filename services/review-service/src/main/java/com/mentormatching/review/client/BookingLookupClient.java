package com.mentormatching.review.client;

import com.mentormatching.review.client.dto.BookingReviewEligibilityResponse;

public interface BookingLookupClient {
    BookingReviewEligibilityResponse getReviewEligibility(Long bookingId);
}
