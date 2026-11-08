package com.mentormatching.booking.client;

import com.mentormatching.booking.client.dto.MentorBookingContextResponse;
import com.mentormatching.booking.client.dto.MentorProfileSummary;

public interface MentorLookupClient {
    MentorBookingContextResponse getBookingContext(Long mentorId, Long mentorSubjectId);

    MentorProfileSummary getMentorByUserId(String userId);
}
