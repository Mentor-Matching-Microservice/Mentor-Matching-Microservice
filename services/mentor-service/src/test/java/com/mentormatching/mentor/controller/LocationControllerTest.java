package com.mentormatching.mentor.controller;

import com.mentormatching.mentor.config.SecurityConfiguration;
import com.mentormatching.mentor.dto.catalog.query.GetCitiesQuery;
import com.mentormatching.mentor.dto.catalog.query.GetDistrictsByCityQuery;
import com.mentormatching.mentor.dto.catalog.response.CitySummary;
import com.mentormatching.mentor.dto.catalog.response.DistrictSummary;
import com.mentormatching.mentor.exception.GlobalExceptionHandler;
import com.mentormatching.mentor.exception.ProblemResponses;
import com.mentormatching.mentor.exception.RequestIdFilter;
import com.mentormatching.mentor.security.ProblemAccessDeniedHandler;
import com.mentormatching.mentor.security.ProblemAuthenticationEntryPoint;
import com.mentormatching.mentor.service.catalog.GetCitiesUseCase;
import com.mentormatching.mentor.service.catalog.GetCitySummaryUseCase;
import com.mentormatching.mentor.service.catalog.GetDistrictSummaryUseCase;
import com.mentormatching.mentor.service.catalog.GetDistrictsByCityUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LocationController.class)
@Import({GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class LocationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    GetCitiesUseCase citiesUseCase;
    @MockitoBean
    GetDistrictsByCityUseCase districtsUseCase;
    @MockitoBean
    GetCitySummaryUseCase cityUseCase;
    @MockitoBean
    GetDistrictSummaryUseCase districtUseCase;

    @Test
    void getCitiesIsPubliclyAccessible() throws Exception {
        CitySummary city = new CitySummary(1L, "HCM", "Thành phố Hồ Chí Minh");
        when(citiesUseCase.execute(any(GetCitiesQuery.class))).thenReturn(List.of(city));

        mvc.perform(get("/api/v1/locations/cities").param("search", "ho chi minh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Thành phố Hồ Chí Minh")));
    }

    @Test
    void getDistrictsIsPubliclyAccessible() throws Exception {
        DistrictSummary district = new DistrictSummary(10L, 1L, "Q1", "Quận 1");
        when(districtsUseCase.execute(any(GetDistrictsByCityQuery.class))).thenReturn(List.of(district));

        mvc.perform(get("/api/v1/locations/cities/{id}/districts", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Quận 1")));
    }

    @Test
    void getCityDetailIsPubliclyAccessible() throws Exception {
        CitySummary city = new CitySummary(1L, "HCM", "Thành phố Hồ Chí Minh");
        when(cityUseCase.execute(1L)).thenReturn(city);

        mvc.perform(get("/api/v1/locations/cities/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("HCM")));
    }

    @Test
    void getDistrictDetailIsPubliclyAccessible() throws Exception {
        DistrictSummary district = new DistrictSummary(10L, 1L, "Q1", "Quận 1");
        when(districtUseCase.execute(10L)).thenReturn(district);

        mvc.perform(get("/api/v1/locations/districts/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("Q1")));
    }
}

