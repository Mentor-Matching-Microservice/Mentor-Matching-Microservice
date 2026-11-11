package com.mentormatching.mentor.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorAchievementCommand;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.service.UpsertCurrentMentorAchievementUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorAchievementsUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thành tích của mentor đang đăng nhập. */
@RestController
@RequestMapping("/api/v1/mentors/me/achievements")
public class MentorAchievementController {

    private final GetCurrentMentorAchievementsUseCase getCurrentMentorAchievementsUseCase;
    private final UpsertCurrentMentorAchievementUseCase upsertCurrentMentorAchievementUseCase;

    public MentorAchievementController(GetCurrentMentorAchievementsUseCase getCurrentMentorAchievementsUseCase,
                                       UpsertCurrentMentorAchievementUseCase upsertCurrentMentorAchievementUseCase) {
        this.getCurrentMentorAchievementsUseCase = getCurrentMentorAchievementsUseCase;
        this.upsertCurrentMentorAchievementUseCase = upsertCurrentMentorAchievementUseCase;
    }

    @GetMapping
    public ApiResponse<List<MentorAchievementDetails>> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorAchievementsUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get current mentor achievements successfully");
    }

    @PutMapping
    public ApiResponse<MentorAchievementDetails> upsert(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody UpsertCurrentMentorAchievementCommand command) {
        return ApiResponse.success(upsertCurrentMentorAchievementUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                "Save current mentor achievement successfully");
    }
}
