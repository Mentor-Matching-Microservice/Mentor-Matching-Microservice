package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.dto.ApiResponse;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.service.catalog.GetGradeDetailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/catalog")
public class InternalCatalogController {

    private final GetGradeDetailUseCase gradeDetailUseCase;

    @GetMapping("/grades/{gradeId}")
    public ApiResponse<GradeDetails> getGradeDetail(@PathVariable Long gradeId) {
        return ApiResponse.success(gradeDetailUseCase.execute(gradeId), "Get grade detail successfully");
    }
}
