package com.mentormatching.mentor.dto.catalog.query;

import com.mentormatching.mentor.util.SearchText;
import lombok.Builder;

@Builder
public record GetCitiesQuery(String search) {
    public GetCitiesQuery {
        search = SearchText.normalize(search);
    }
}
