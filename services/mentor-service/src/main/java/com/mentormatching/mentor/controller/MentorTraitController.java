package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpdateCurrentMentorTraitsCommand;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.CurrentMentorTraitsResponse;
import com.mentormatching.mentor.service.GetCurrentMentorTraitsUseCase;
import com.mentormatching.mentor.service.UpdateCurrentMentorTraitsUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Các tính cách và điểm nổi bật của mentor đang đăng nhập. */
@RestController
@RequestMapping("/api/v1/mentors/me/traits")
public class MentorTraitController {

    private final GetCurrentMentorTraitsUseCase getCurrentMentorTraitsUseCase;
    private final UpdateCurrentMentorTraitsUseCase updateCurrentMentorTraitsUseCase;

    public MentorTraitController(GetCurrentMentorTraitsUseCase getCurrentMentorTraitsUseCase,
                                 UpdateCurrentMentorTraitsUseCase updateCurrentMentorTraitsUseCase) {
        this.getCurrentMentorTraitsUseCase = getCurrentMentorTraitsUseCase;
        this.updateCurrentMentorTraitsUseCase = updateCurrentMentorTraitsUseCase;
    }

    @GetMapping
    public ApiResponse<CurrentMentorTraitsResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorTraitsUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get current mentor traits successfully");
    }

    @PutMapping
    public ApiResponse<CurrentMentorTraitsResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody UpdateCurrentMentorTraitsCommand command) {
        return ApiResponse.success(updateCurrentMentorTraitsUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                "Save current mentor traits successfully");
    }
}
