package com.mentormatching.catalog.dto.query;

import lombok.Builder;
import com.mentormatching.catalog.util.SearchText;

@Builder
public record GetCitiesQuery(String search) {
    public GetCitiesQuery {
        search = SearchText.normalize(search);
    }
}
