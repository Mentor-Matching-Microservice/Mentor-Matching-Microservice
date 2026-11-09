package com.mentormatching.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForceCancelBookingRequest {

    @NotBlank(message = "Cancel reason is required")
    @Size(max = 1000, message = "Cancel reason must not exceed 1000 characters")
    private String cancelReason;
}
