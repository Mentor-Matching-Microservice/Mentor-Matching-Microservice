package com.mentormatching.mentor.service.catalog;

import com.mentormatching.mentor.dto.catalog.response.DistrictSummary;
import com.mentormatching.mentor.exception.ApiException;
import com.mentormatching.mentor.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GetDistrictSummaryUseCase {
    private final DistrictRepository repository;

    @Transactional(readOnly = true)
    public DistrictSummary execute(Long districtId) {
        if (districtId == null || districtId <= 0) {
            throw new ApiException(BAD_REQUEST, "INVALID_ID", "A positive ID is required");
        }
        return repository.findById(districtId).map(DistrictSummary::from)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "DISTRICT_NOT_FOUND", "District not found"));
    }
}
