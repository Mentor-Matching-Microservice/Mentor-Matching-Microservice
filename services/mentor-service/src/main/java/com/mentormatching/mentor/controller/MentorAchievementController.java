package com.mentormatching.mentor.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorAchievementCommand;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.service.DeleteCurrentMentorAchievementUseCase;
import com.mentormatching.mentor.service.UpsertCurrentMentorAchievementUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorAchievementsUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final DeleteCurrentMentorAchievementUseCase deleteCurrentMentorAchievementUseCase;

    public MentorAchievementController(GetCurrentMentorAchievementsUseCase getCurrentMentorAchievementsUseCase,
                                       UpsertCurrentMentorAchievementUseCase upsertCurrentMentorAchievementUseCase,
                                       DeleteCurrentMentorAchievementUseCase deleteCurrentMentorAchievementUseCase) {
        this.getCurrentMentorAchievementsUseCase = getCurrentMentorAchievementsUseCase;
        this.upsertCurrentMentorAchievementUseCase = upsertCurrentMentorAchievementUseCase;
        this.deleteCurrentMentorAchievementUseCase = deleteCurrentMentorAchievementUseCase;
    }

    @GetMapping
    public ApiResponse<List<MentorAchievementDetails>> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorAchievementsUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get current mentor achievements successfully");
    }

    @PostMapping
    public ApiResponse<MentorAchievementDetails> create(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody UpsertCurrentMentorAchievementCommand command) {
        return ApiResponse.success(upsertCurrentMentorAchievementUseCase.execute(
                UUID.fromString(jwt.getSubject()), command.withId(null)),
                "Create current mentor achievement successfully");
    }

    @PutMapping
    public ApiResponse<MentorAchievementDetails> upsert(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody UpsertCurrentMentorAchievementCommand command) {
        return ApiResponse.success(upsertCurrentMentorAchievementUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                "Save current mentor achievement successfully");
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<MentorAchievementDetails> update(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable Long achievementId,
                                                        @Valid @RequestBody UpsertCurrentMentorAchievementCommand command) {
        return ApiResponse.success(upsertCurrentMentorAchievementUseCase.execute(
                UUID.fromString(jwt.getSubject()), command.withId(achievementId)),
                "Update current mentor achievement successfully");
    }

    @DeleteMapping("/{achievementId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long achievementId) {
        deleteCurrentMentorAchievementUseCase.execute(UUID.fromString(jwt.getSubject()), achievementId);
        return ApiResponse.success(null, "Delete current mentor achievement successfully");
    }
}
