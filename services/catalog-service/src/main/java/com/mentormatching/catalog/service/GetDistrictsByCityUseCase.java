package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.dto.response.DistrictSummary;
import com.mentormatching.catalog.dto.query.GetDistrictsByCityQuery;
import com.mentormatching.catalog.util.SearchText;
import com.mentormatching.catalog.entity.City;
import com.mentormatching.catalog.entity.District;
import com.mentormatching.catalog.repository.CityRepository;
import com.mentormatching.catalog.repository.DistrictRepository;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GetDistrictsByCityUseCase {
    private final DistrictRepository repository;
    private final Validator validator;
    private final CityRepository cities;

    @Transactional(readOnly = true)
    public List<DistrictSummary> execute(GetDistrictsByCityQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_LOCATION_QUERY", "Invalid input data");
        }
        if (!cities.existsById(query.cityId())) {
            throw new ApiException(NOT_FOUND, "CITY_NOT_FOUND", "City not found");
        }
        return repository.findByCityId(query.cityId()).stream()
                .filter(value -> query.search() == null || query.search().isBlank()
                        || SearchText.normalize(value.getName()).contains(query.search()))
                .sorted(java.util.Comparator.comparing(District::getName))
                .map(DistrictSummary::from).toList();
    }
}
