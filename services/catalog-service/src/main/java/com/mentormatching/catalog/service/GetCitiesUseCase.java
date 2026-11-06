package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.dto.response.CitySummary;
import com.mentormatching.catalog.dto.query.GetCitiesQuery;
import com.mentormatching.catalog.util.SearchText;
import com.mentormatching.catalog.entity.City;
import com.mentormatching.catalog.repository.CityRepository;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class GetCitiesUseCase {
    private final CityRepository repository;
    private final Validator validator;

    @Transactional(readOnly = true)
    public List<CitySummary> execute(GetCitiesQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_LOCATION_QUERY", "Invalid input data");
        }
        return repository.findAll().stream()
                .filter(value -> query.search() == null || query.search().isBlank()
                        || SearchText.normalize(value.getName()).contains(query.search()))
                .sorted(java.util.Comparator.comparing(City::getName))
                .map(CitySummary::from).toList();
    }
}
