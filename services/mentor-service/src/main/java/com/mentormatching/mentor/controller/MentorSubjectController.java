package com.mentormatching.mentor.controller;

import java.util.List;
import java.util.UUID;

import com.mentormatching.mentor.dto.command.UpsertCurrentMentorSubjectCommand;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.service.DeleteCurrentMentorSubjectUseCase;
import com.mentormatching.mentor.usecase.GetCurrentMentorSubjectsUseCase;
import com.mentormatching.mentor.usecase.UpsertCurrentMentorSubjectUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Các môn dạy của mentor đang đăng nhập. */
@RestController
@RequestMapping("/api/v1/mentors/me/subjects")
public class MentorSubjectController {

    private final GetCurrentMentorSubjectsUseCase getCurrentMentorSubjectsUseCase;
    private final UpsertCurrentMentorSubjectUseCase upsertCurrentMentorSubjectUseCase;
    private final DeleteCurrentMentorSubjectUseCase deleteCurrentMentorSubjectUseCase;

    public MentorSubjectController(GetCurrentMentorSubjectsUseCase getCurrentMentorSubjectsUseCase,
                                   UpsertCurrentMentorSubjectUseCase upsertCurrentMentorSubjectUseCase,
                                   DeleteCurrentMentorSubjectUseCase deleteCurrentMentorSubjectUseCase) {
        this.getCurrentMentorSubjectsUseCase = getCurrentMentorSubjectsUseCase;
        this.upsertCurrentMentorSubjectUseCase = upsertCurrentMentorSubjectUseCase;
        this.deleteCurrentMentorSubjectUseCase = deleteCurrentMentorSubjectUseCase;
    }

    @GetMapping
    public ApiResponse<List<MentorSubjectDetails>> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(getCurrentMentorSubjectsUseCase.execute(UUID.fromString(jwt.getSubject())),
                "Get current mentor subjects successfully");
    }

    @PutMapping
    public ApiResponse<MentorSubjectDetails> upsert(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody UpsertCurrentMentorSubjectCommand command) {
        return ApiResponse.success(upsertCurrentMentorSubjectUseCase.execute(UUID.fromString(jwt.getSubject()), command),
                "Save current mentor subject successfully");
    }

    @DeleteMapping("/{subjectId}")
    public ApiResponse<Void> deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable Long subjectId) {
        deleteCurrentMentorSubjectUseCase.execute(UUID.fromString(jwt.getSubject()), subjectId);
        return ApiResponse.success(null, "Deactivate current mentor subject successfully");
    }
}
