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
@RequestMapping("/api/v1/admin/catalog")
public class AdminCatalogController {

    private final CreateSubjectUseCase createSubject;
    private final UpdateSubjectUseCase updateSubject;
    private final DeleteSubjectUseCase deleteSubject;
    private final CreateGradeUseCase createGrade;
    private final UpdateGradeUseCase updateGrade;
    private final DeleteGradeUseCase deleteGrade;

    @PostMapping("/subjects")
    public ApiResponse<SubjectDetails> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success(createSubject.execute(CreateSubjectCommand.builder()
                .categoryId(request.categoryId()).name(request.name()).description(request.description()).build()), "Subject created successfully");
    }

    @PutMapping("/subjects/{id}")
    public ApiResponse<SubjectDetails> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.success(updateSubject.execute(UpdateSubjectCommand.builder().id(id)
                .categoryId(request.categoryId()).name(request.name()).description(request.description()).build()), "Subject updated successfully");
    }

    @DeleteMapping("/subjects/{id}")
    public ApiResponse<Void> deleteSubject(@PathVariable Long id) {
        deleteSubject.execute(DeleteSubjectCommand.builder().id(id).build());
        return ApiResponse.success(null, "Subject deleted successfully");
    }

    @PostMapping("/grades")
    public ApiResponse<GradeDetails> createGrade(@Valid @RequestBody GradeRequest request) {
        return ApiResponse.success(createGrade.execute(CreateGradeCommand.builder()
                .name(request.name()).levelGroup(request.levelGroup()).build()), "Grade created successfully");
    }

    @PutMapping("/grades/{id}")
    public ApiResponse<GradeDetails> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return ApiResponse.success(updateGrade.execute(UpdateGradeCommand.builder().id(id)
                .name(request.name()).levelGroup(request.levelGroup()).build()), "Grade updated successfully");
    }

    @DeleteMapping("/grades/{id}")
    public ApiResponse<Void> deleteGrade(@PathVariable Long id) {
        deleteGrade.execute(DeleteGradeCommand.builder().id(id).build());
        return ApiResponse.success(null, "Grade deleted successfully");
    }
}
