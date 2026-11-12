package com.mentormatching.user.controller;

import java.util.UUID;

import com.mentormatching.user.dto.response.UserSummaryResponse;
import com.mentormatching.user.exception.ApiException;
import com.mentormatching.user.service.GetUserSummaryUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint nội bộ phục vụ tra cứu thông tin tóm tắt người dùng giữa các service.
 */
@RestController
@RequestMapping("/internal/v1/users")
public class InternalUserController {

    private final GetUserSummaryUseCase getUserSummaryUseCase;

    public InternalUserController(GetUserSummaryUseCase getUserSummaryUseCase) {
        this.getUserSummaryUseCase = getUserSummaryUseCase;
    }

    @GetMapping("/{userId}/summary")
    public UserSummaryResponse getUserSummary(@PathVariable String userId) {
        UUID parsedUserId;
        try {
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid user UUID format.");
        }
        return getUserSummaryUseCase.execute(parsedUserId);
    }
}
