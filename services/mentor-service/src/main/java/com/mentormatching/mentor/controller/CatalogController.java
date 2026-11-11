package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.dto.ApiResponse;
import com.mentormatching.mentor.dto.catalog.query.GetSubjectGradeIdsQuery;
import com.mentormatching.mentor.dto.catalog.response.CatalogOptions;
import com.mentormatching.mentor.dto.catalog.response.SubjectGradeSummary;
import com.mentormatching.mentor.service.catalog.GetCatalogOptionsUseCase;
import com.mentormatching.mentor.service.catalog.GetSubjectGradeIdsUseCase;
import com.mentormatching.mentor.service.catalog.GetSubjectGradeSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final GetCatalogOptionsUseCase options;
    private final GetSubjectGradeIdsUseCase ids;
    private final GetSubjectGradeSummaryUseCase summary;

    @GetMapping("/options")
    public ApiResponse<CatalogOptions> getOptions() {
        return ApiResponse.success(options.execute(), "Get catalog options successfully");
    }

    @GetMapping("/subject-grades/ids")
    public ApiResponse<List<Long>> getIds(@RequestParam(required = false) Long subjectId,
                                          @RequestParam(required = false) Long gradeId) {
        return ApiResponse.success(ids.execute(GetSubjectGradeIdsQuery.builder()
                .subjectId(subjectId).gradeId(gradeId).build()), "Get subject grade IDs successfully");
    }

    @GetMapping("/subject-grades/{id}")
    public ApiResponse<SubjectGradeSummary> getSummary(@PathVariable Long id) {
        return ApiResponse.success(summary.execute(id), "Get subject grade successfully");
    }
}
