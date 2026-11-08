package com.mentormatching.booking.client;

import com.mentormatching.booking.client.dto.UserSummaryResponse;

public interface UserLookupClient {
    UserSummaryResponse getUserSummary(String userId);
}
