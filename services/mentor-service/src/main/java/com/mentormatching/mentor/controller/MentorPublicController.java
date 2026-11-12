package com.mentormatching.mentor.controller;

import java.util.List;

import com.mentormatching.mentor.dto.query.SearchMentorsQuery;
import com.mentormatching.mentor.dto.response.*;
import com.mentormatching.mentor.service.GetHighlightOptionsUseCase;
import com.mentormatching.mentor.service.GetMentorAchievementsUseCase;
import com.mentormatching.mentor.service.GetMentorSubjectsUseCase;
import com.mentormatching.mentor.service.GetMentorTraitsUseCase;
import com.mentormatching.mentor.service.GetPersonalityOptionsUseCase;
import com.mentormatching.mentor.usecase.GetMentorDetailUseCase;
import com.mentormatching.mentor.usecase.SearchMentorsUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API tra cứu mentor công khai. */
@RestController
@RequestMapping("/api/v1/mentors")
public class MentorPublicController {

    private final SearchMentorsUseCase searchMentorsUseCase;
    private final GetMentorDetailUseCase getMentorDetailUseCase;
    private final GetMentorSubjectsUseCase getMentorSubjectsUseCase;
    private final GetMentorTraitsUseCase getMentorTraitsUseCase;
    private final GetMentorAchievementsUseCase getMentorAchievementsUseCase;
    private final GetPersonalityOptionsUseCase getPersonalityOptionsUseCase;
    private final GetHighlightOptionsUseCase getHighlightOptionsUseCase;

    public MentorPublicController(SearchMentorsUseCase searchMentorsUseCase,
                                  GetMentorDetailUseCase getMentorDetailUseCase,
                                  GetMentorSubjectsUseCase getMentorSubjectsUseCase,
                                  GetMentorTraitsUseCase getMentorTraitsUseCase,
                                  GetMentorAchievementsUseCase getMentorAchievementsUseCase,
                                  GetPersonalityOptionsUseCase getPersonalityOptionsUseCase,
                                  GetHighlightOptionsUseCase getHighlightOptionsUseCase) {
        this.searchMentorsUseCase = searchMentorsUseCase;
        this.getMentorDetailUseCase = getMentorDetailUseCase;
        this.getMentorSubjectsUseCase = getMentorSubjectsUseCase;
        this.getMentorTraitsUseCase = getMentorTraitsUseCase;
        this.getMentorAchievementsUseCase = getMentorAchievementsUseCase;
        this.getPersonalityOptionsUseCase = getPersonalityOptionsUseCase;
        this.getHighlightOptionsUseCase = getHighlightOptionsUseCase;
    }

    @GetMapping
    public ApiResponse<PageResponse<MentorSearchItemResponse>> search(@Valid @ModelAttribute SearchMentorsQuery query) {
        return ApiResponse.success(searchMentorsUseCase.execute(query), "Get mentors successfully");
    }

    @GetMapping("/{mentorId:[0-9]+}")
    public ApiResponse<MentorDetailResponse> detail(@PathVariable Long mentorId) {
        return ApiResponse.success(getMentorDetailUseCase.execute(mentorId), "Get mentor detail successfully");
    }

    @GetMapping("/{mentorId:[0-9]+}/subjects")
    public ApiResponse<List<MentorSubjectDetails>> subjects(@PathVariable Long mentorId) {
        return ApiResponse.success(getMentorSubjectsUseCase.execute(mentorId), "Get mentor subjects successfully");
    }

    @GetMapping("/{mentorId:[0-9]+}/traits")
    public ApiResponse<MentorTraitsDetailResponse> traits(@PathVariable Long mentorId) {
        return ApiResponse.success(getMentorTraitsUseCase.execute(mentorId), "Get mentor traits successfully");
    }

    @GetMapping("/{mentorId:[0-9]+}/achievements")
    public ApiResponse<List<MentorAchievementDetails>> achievements(@PathVariable Long mentorId) {
        return ApiResponse.success(getMentorAchievementsUseCase.execute(mentorId), "Get mentor achievements successfully");
    }

    @GetMapping("/personality-options")
    public ApiResponse<List<MentorOptionResponse>> personalityOptions() {
        return ApiResponse.success(getPersonalityOptionsUseCase.execute(), "Get personality options successfully");
    }

    @GetMapping("/highlight-options")
    public ApiResponse<List<MentorOptionResponse>> highlightOptions() {
        return ApiResponse.success(getHighlightOptionsUseCase.execute(), "Get highlight options successfully");
    }
}
