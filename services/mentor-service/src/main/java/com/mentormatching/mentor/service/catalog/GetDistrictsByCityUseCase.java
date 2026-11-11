package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.query.GetDistrictsByCityQuery;
import com.mentormatching.mentor.dto.catalog.response.DistrictSummary;
import com.mentormatching.mentor.entity.District;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.CityRepository;
import com.mentormatching.mentor.repository.DistrictRepository;
import com.mentormatching.mentor.util.SearchText;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GetDistrictsByCityUseCase {
    private final DistrictRepository repository;
    private final CityRepository cities;
    private final Validator validator;

    @Transactional(readOnly = true)
    public List<DistrictSummary> execute(GetDistrictsByCityQuery query) {
        if (query == null || !validator.validate(query).isEmpty()) {
            throw new ApiException(BAD_REQUEST, "INVALID_LOCATION_QUERY", "Invalid input data");
        }
        if (!cities.existsById(query.cityId())) {
            throw new ApiException(NOT_FOUND, "CITY_NOT_FOUND", "City not found");
        }
        return repository.findByCityId(query.cityId()).stream()
                .filter(district -> query.search() == null || query.search().isBlank()
                        || SearchText.normalize(district.getName()).contains(query.search()))
                .sorted(Comparator.comparing(District::getName))
                .map(DistrictSummary::from)
                .toList();
    }
}
