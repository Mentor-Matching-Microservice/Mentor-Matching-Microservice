package com.mentormatching.catalog.service;

import com.mentormatching.catalog.exception.ApiException;
import com.mentormatching.catalog.dto.response.CitySummary;
import com.mentormatching.catalog.entity.City;
import com.mentormatching.catalog.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GetCitySummaryUseCase {
    private final CityRepository repository;

    @Transactional(readOnly = true)
    public CitySummary execute(Long cityId) {
        if (cityId == null || cityId <= 0) {
            throw new ApiException(BAD_REQUEST, "INVALID_ID", "A positive ID is required");
        }
        return repository.findById(cityId).map(CitySummary::from)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "CITY_NOT_FOUND", "City not found"));
    }
}
