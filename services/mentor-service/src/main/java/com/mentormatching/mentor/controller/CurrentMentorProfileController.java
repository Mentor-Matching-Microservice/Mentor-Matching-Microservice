package com.mentormatching.mentor.controller;

import java.util.UUID;

import com.mentormatching.mentor.dto.command.CreateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.command.UpdateCurrentMentorProfileCommand;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorProfileDetails;
import com.mentormatching.mentor.usecase.CreateCurrentMentorProfileUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorProfileUseCase;
import com.mentormatching.mentor.usecase.UpdateCurrentMentorProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hồ sơ của mentor đang đăng nhập. */
@RestController
@RequestMapping("/api/v1/mentors/me")
public class CurrentMentorProfileController {

    private final CreateCurrentMentorProfileUseCase createCurrentMentorProfileUseCase;
    private final GetCurrentMentorProfileUseCase getCurrentMentorProfileUseCase;
    private final UpdateCurrentMentorProfileUseCase updateCurrentMentorProfileUseCase;

    public CurrentMentorProfileController(CreateCurrentMentorProfileUseCase createCurrentMentorProfileUseCase,
                                          GetCurrentMentorProfileUseCase getCurrentMentorProfileUseCase,
                                          UpdateCurrentMentorProfileUseCase updateCurrentMentorProfileUseCase) {
        this.createCurrentMentorProfileUseCase = createCurrentMentorProfileUseCase;
        this.getCurrentMentorProfileUseCase = getCurrentMentorProfileUseCase;
        this.updateCurrentMentorProfileUseCase = updateCurrentMentorProfileUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MentorProfileDetails>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) CreateCurrentMentorProfileCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(createCurrentMentorProfileUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                        "Create current mentor profile successfully"));
    }

    @GetMapping
    public ApiResponse<MentorProfileDetails> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorProfileUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get current mentor profile successfully");
    }

    @PutMapping
    public ApiResponse<MentorProfileDetails> update(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody UpdateCurrentMentorProfileCommand command) {
        return ApiResponse.success(updateCurrentMentorProfileUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                "Save current mentor profile successfully");
    }
}
