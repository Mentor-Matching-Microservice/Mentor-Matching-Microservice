package com.mentormatching.mentor.dto.catalog.response;

import com.mentormatching.mentor.entity.Subject;
import lombok.Builder;

@Builder
public record SubjectDetails(Long id, Long categoryId, String name, String description) {
    public static SubjectDetails from(Subject entity) {
        return SubjectDetails.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}

