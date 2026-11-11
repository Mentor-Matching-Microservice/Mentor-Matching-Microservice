package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.dto.ApiResponse;
import com.mentormatching.mentor.dto.catalog.command.CreateGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.CreateSubjectCommand;
import com.mentormatching.mentor.dto.catalog.command.DeleteGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.DeleteSubjectCommand;
import com.mentormatching.mentor.dto.catalog.command.GradeRequest;
import com.mentormatching.mentor.dto.catalog.command.SubjectRequest;
import com.mentormatching.mentor.dto.catalog.command.UpdateGradeCommand;
import com.mentormatching.mentor.dto.catalog.command.UpdateSubjectCommand;
import com.mentormatching.mentor.dto.catalog.response.GradeDetails;
import com.mentormatching.mentor.dto.catalog.response.SubjectDetails;
import com.mentormatching.mentor.service.catalog.CreateGradeUseCase;
import com.mentormatching.mentor.service.catalog.CreateSubjectUseCase;
import com.mentormatching.mentor.service.catalog.DeleteGradeUseCase;
import com.mentormatching.mentor.service.catalog.DeleteSubjectUseCase;
import com.mentormatching.mentor.service.catalog.UpdateGradeUseCase;
import com.mentormatching.mentor.service.catalog.UpdateSubjectUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasRole('ADMIN')")
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
