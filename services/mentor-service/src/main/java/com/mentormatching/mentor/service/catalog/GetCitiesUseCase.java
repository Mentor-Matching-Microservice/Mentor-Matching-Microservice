package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.query.GetCitiesQuery;
import com.mentormatching.mentor.dto.catalog.response.CitySummary;
import com.mentormatching.mentor.entity.City;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.CityRepository;
import com.mentormatching.mentor.util.SearchText;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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
                .filter(city -> query.search() == null || query.search().isBlank()
                        || SearchText.normalize(city.getName()).contains(query.search()))
                .sorted(Comparator.comparing(City::getName))
                .map(CitySummary::from)
                .toList();
    }
}

