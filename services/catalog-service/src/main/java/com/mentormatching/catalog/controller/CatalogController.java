package com.mentormatching.catalog.controller;

import com.mentormatching.catalog.dto.command.*;
import com.mentormatching.catalog.dto.query.*;
import com.mentormatching.catalog.dto.response.*;
import com.mentormatching.catalog.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
