package com.mentormatching.mentor.dto.catalog.response;

import com.mentormatching.mentor.entity.District;
import lombok.Builder;

@Builder
public record DistrictSummary(Long id, Long cityId, String code, String name) {
    public static DistrictSummary from(District entity) {
        return DistrictSummary.builder()
                .id(entity.getId())
                .cityId(entity.getCityId())
                .code(entity.getCode())
                .name(entity.getName())
                .build();
    }
}
