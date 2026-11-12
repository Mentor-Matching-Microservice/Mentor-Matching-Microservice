package com.mentormatching.mentor.controller;

import java.util.List;

import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.response.MentorAchievementDetails;
import com.mentormatching.mentor.dto.response.MentorDetailResponse;
import com.mentormatching.mentor.dto.response.MentorOptionResponse;
import com.mentormatching.mentor.dto.response.MentorSearchItemResponse;
import com.mentormatching.mentor.dto.response.MentorSubjectDetails;
import com.mentormatching.mentor.dto.response.MentorTraitsDetailResponse;
import com.mentormatching.mentor.dto.response.PageResponse;
import com.mentormatching.mentor.service.GetHighlightOptionsUseCase;
import com.mentormatching.mentor.service.GetPersonalityOptionsUseCase;
import com.mentormatching.mentor.usecase.GetMentorDetailUseCase;
import com.mentormatching.mentor.usecase.GetPublicMentorAchievementsUseCase;
import com.mentormatching.mentor.usecase.GetPublicMentorSubjectsUseCase;
import com.mentormatching.mentor.usecase.GetPublicMentorTraitsUseCase;
import com.mentormatching.mentor.usecase.SearchMentorsUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Các endpoint công khai của mentor. */
@RestController
@RequestMapping("/api/v1/mentors")
public class MentorController {

    private final SearchMentorsUseCase searchMentorsUseCase;
    private final GetMentorDetailUseCase getMentorDetailUseCase;
    private final GetPersonalityOptionsUseCase getPersonalityOptionsUseCase;
    private final GetHighlightOptionsUseCase getHighlightOptionsUseCase;
    private final GetPublicMentorSubjectsUseCase getPublicMentorSubjectsUseCase;
    private final GetPublicMentorTraitsUseCase getPublicMentorTraitsUseCase;
    private final GetPublicMentorAchievementsUseCase getPublicMentorAchievementsUseCase;

    public MentorController(SearchMentorsUseCase searchMentorsUseCase,
                            GetMentorDetailUseCase getMentorDetailUseCase,
                            GetPersonalityOptionsUseCase getPersonalityOptionsUseCase,
                            GetHighlightOptionsUseCase getHighlightOptionsUseCase,
                            GetPublicMentorSubjectsUseCase getPublicMentorSubjectsUseCase,
                            GetPublicMentorTraitsUseCase getPublicMentorTraitsUseCase,
                            GetPublicMentorAchievementsUseCase getPublicMentorAchievementsUseCase) {
        this.searchMentorsUseCase = searchMentorsUseCase;
        this.getMentorDetailUseCase = getMentorDetailUseCase;
        this.getPersonalityOptionsUseCase = getPersonalityOptionsUseCase;
        this.getHighlightOptionsUseCase = getHighlightOptionsUseCase;
        this.getPublicMentorSubjectsUseCase = getPublicMentorSubjectsUseCase;
        this.getPublicMentorTraitsUseCase = getPublicMentorTraitsUseCase;
        this.getPublicMentorAchievementsUseCase = getPublicMentorAchievementsUseCase;
    }

    @GetMapping
    public ApiResponse<PageResponse<MentorSearchItemResponse>> search(
            @Valid @ModelAttribute SearchMentorsQuery query) {
        return ApiResponse.success(searchMentorsUseCase.execute(query), "Search mentors successfully");
    }

    @GetMapping("/personality-options")
    public ApiResponse<List<MentorOptionResponse>> getPersonalityOptions() {
        return ApiResponse.success(getPersonalityOptionsUseCase.execute(), "Get personality options successfully");
    }

    @GetMapping("/highlight-options")
    public ApiResponse<List<MentorOptionResponse>> getHighlightOptions() {
        return ApiResponse.success(getHighlightOptionsUseCase.execute(), "Get highlight options successfully");
    }

    @GetMapping("/{mentorId}")
    public ApiResponse<MentorDetailResponse> getDetail(@PathVariable Long mentorId) {
        return ApiResponse.success(getMentorDetailUseCase.execute(mentorId), "Get mentor detail successfully");
    }

    @GetMapping("/{mentorId}/subjects")
    public ApiResponse<List<MentorSubjectDetails>> getSubjects(@PathVariable Long mentorId) {
        return ApiResponse.success(getPublicMentorSubjectsUseCase.execute(mentorId), "Get mentor subjects successfully");
    }

    @GetMapping("/{mentorId}/traits")
    public ApiResponse<MentorTraitsDetailResponse> getTraits(@PathVariable Long mentorId) {
        return ApiResponse.success(getPublicMentorTraitsUseCase.execute(mentorId), "Get mentor traits successfully");
    }

    @GetMapping("/{mentorId}/achievements")
    public ApiResponse<List<MentorAchievementDetails>> getAchievements(@PathVariable Long mentorId) {
        return ApiResponse.success(getPublicMentorAchievementsUseCase.execute(mentorId), "Get mentor achievements successfully");
    }
}

