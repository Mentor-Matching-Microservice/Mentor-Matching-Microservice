package com.mentormatching.catalog.dto.response;

import com.mentormatching.catalog.entity.Grade;
import com.mentormatching.catalog.entity.GradeLevelGroup;
import lombok.Builder;

@Builder
public record GradeDetails(Long id, String name, GradeLevelGroup levelGroup) {
    public static GradeDetails from(Grade entity) {
        return GradeDetails.builder()
                .id(entity.getId())
                .name(entity.getName())
                .levelGroup(entity.getLevelGroup())
                .build();
    }
}
