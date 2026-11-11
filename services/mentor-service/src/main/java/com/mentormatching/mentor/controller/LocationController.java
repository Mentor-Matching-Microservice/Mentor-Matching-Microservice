package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.dto.response.ApiResponse;
import com.mentormatching.mentor.dto.catalog.query.GetCitiesQuery;
import com.mentormatching.mentor.dto.catalog.query.GetDistrictsByCityQuery;
import com.mentormatching.mentor.dto.catalog.response.CitySummary;
import com.mentormatching.mentor.dto.catalog.response.DistrictSummary;
import com.mentormatching.mentor.service.catalog.GetCitiesUseCase;
import com.mentormatching.mentor.service.catalog.GetCitySummaryUseCase;
import com.mentormatching.mentor.service.catalog.GetDistrictSummaryUseCase;
import com.mentormatching.mentor.service.catalog.GetDistrictsByCityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final GetCitiesUseCase cities;
    private final GetDistrictsByCityUseCase districts;
    private final GetCitySummaryUseCase city;
    private final GetDistrictSummaryUseCase district;

    @GetMapping("/cities")
    public ApiResponse<List<CitySummary>> getCities(@RequestParam(required = false) String search) {
        return ApiResponse.success(cities.execute(GetCitiesQuery.builder().search(search).build()), "Get cities successfully");
    }

    @GetMapping("/cities/{id}/districts")
    public ApiResponse<List<DistrictSummary>> getDistricts(@PathVariable Long id,
                                                           @RequestParam(required = false) String search) {
        return ApiResponse.success(districts.execute(GetDistrictsByCityQuery.builder().cityId(id).search(search).build()),
                "Get districts successfully");
    }

    @GetMapping("/cities/{id}")
    public ApiResponse<CitySummary> getCity(@PathVariable Long id) {
        return ApiResponse.success(city.execute(id), "Get city successfully");
    }

    @GetMapping("/districts/{id}")
    public ApiResponse<DistrictSummary> getDistrict(@PathVariable Long id) {
        return ApiResponse.success(district.execute(id), "Get district successfully");
    }
}

