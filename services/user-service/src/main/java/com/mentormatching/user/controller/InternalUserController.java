package com.mentormatching.user.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.user.dto.request.BatchSummaryQuery;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
        return getUserSummaryUseCase.execute(parse(userId));
    }

    /** #66: cùng nội dung với {@code /summary}, theo đường dẫn ngắn {@code /internal/v1/users/{userId}}. */
    @GetMapping("/{userId}")
    public UserSummaryResponse getUser(@PathVariable String userId) {
        return getUserSummaryUseCase.execute(parse(userId));
    }

    /** #66: tóm tắt nhiều người dùng một lần (tối đa 100); id không tồn tại bị bỏ qua. */
    @PostMapping("/batch-summary")
    public List<UserSummaryResponse> batchSummary(@Valid @RequestBody BatchSummaryQuery query) {
        return getUserSummaryUseCase.executeAll(query.userIds());
    }

    private static UUID parse(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid user UUID format.");
        }
    }
}
