package com.mentormatching.mentor.dto.catalog.query;

import com.mentormatching.mentor.util.SearchText;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record GetDistrictsByCityQuery(@NotNull @Positive Long cityId, String search) {
    public GetDistrictsByCityQuery {
        search = SearchText.normalize(search);
    }
}
