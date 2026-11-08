package com.mentormatching.booking.client;

import com.mentormatching.booking.client.dto.MentorBookingContextResponse;

public interface MentorLookupClient {
    MentorBookingContextResponse getBookingContext(Long mentorId, Long mentorSubjectId);
}

