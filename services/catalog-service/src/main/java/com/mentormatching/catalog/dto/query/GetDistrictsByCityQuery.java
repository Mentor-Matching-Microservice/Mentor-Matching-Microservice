package com.mentormatching.catalog.dto.query;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import com.mentormatching.catalog.util.SearchText;

@Builder
public record GetDistrictsByCityQuery(@NotNull @Positive Long cityId, String search) {
    public GetDistrictsByCityQuery {
        search = SearchText.normalize(search);
    }
}
