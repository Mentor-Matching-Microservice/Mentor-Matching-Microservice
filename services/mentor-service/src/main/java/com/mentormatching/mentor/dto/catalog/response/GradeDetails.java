package com.mentormatching.mentor.dto.catalog.response;

import com.mentormatching.mentor.entity.Grade;
import com.mentormatching.mentor.entity.GradeLevelGroup;
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

