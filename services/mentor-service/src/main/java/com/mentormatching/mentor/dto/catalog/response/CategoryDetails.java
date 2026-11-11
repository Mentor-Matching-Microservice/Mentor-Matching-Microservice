package com.mentormatching.mentor.dto.catalog.response;

import com.mentormatching.mentor.entity.Category;
import lombok.Builder;

@Builder
public record CategoryDetails(Long id, String name, String description) {
    public static CategoryDetails from(Category entity) {
        return CategoryDetails.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}

