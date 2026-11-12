package com.mentormatching.catalog.controller;

import com.mentormatching.catalog.dto.response.ApiResponse;
import com.mentormatching.catalog.dto.response.GradeDetails;
import com.mentormatching.catalog.service.GetGradeDetailUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/catalog")
public class InternalCatalogController {

    private final GetGradeDetailUseCase getGradeDetailUseCase;

    public InternalCatalogController(GetGradeDetailUseCase getGradeDetailUseCase) {
        this.getGradeDetailUseCase = getGradeDetailUseCase;
    }

    @GetMapping("/grades/{gradeId}")
    public ApiResponse<GradeDetails> getGradeDetail(@PathVariable Long gradeId) {
        return ApiResponse.success(getGradeDetailUseCase.execute(gradeId), "Get grade detail successfully");
    }
}
