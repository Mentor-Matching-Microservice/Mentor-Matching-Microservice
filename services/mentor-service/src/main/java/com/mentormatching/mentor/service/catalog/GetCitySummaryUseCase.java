package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.response.CitySummary;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.CityRepository;
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
