package com.mentormatching.mentor.dto.catalog.response;

import com.mentormatching.mentor.entity.City;
import lombok.Builder;

@Builder
public record CitySummary(Long id, String code, String name) {
    public static CitySummary from(City entity) {
        return CitySummary.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .build();
    }
}
