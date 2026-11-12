package com.mentormatching.mentor.service;

import com.mentormatching.mentor.dto.catalog.query.GetCitiesQuery;
import com.mentormatching.mentor.dto.catalog.query.GetDistrictsByCityQuery;
import com.mentormatching.mentor.dto.catalog.response.CitySummary;
import com.mentormatching.mentor.dto.catalog.response.DistrictSummary;
import com.mentormatching.mentor.entity.City;
import com.mentormatching.mentor.entity.District;
import com.mentormatching.mentor.repository.CityRepository;
import com.mentormatching.mentor.repository.DistrictRepository;
import com.mentormatching.mentor.service.catalog.GetCitiesUseCase;
import com.mentormatching.mentor.service.catalog.GetDistrictsByCityUseCase;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationSearchTest {

    @Mock
    CityRepository cityRepository;
    @Mock
    DistrictRepository districtRepository;

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    GetCitiesUseCase getCitiesUseCase;
    GetDistrictsByCityUseCase getDistrictsByCityUseCase;

    @BeforeEach
    void setUp() {
        getCitiesUseCase = new GetCitiesUseCase(cityRepository, validator);
        getDistrictsByCityUseCase = new GetDistrictsByCityUseCase(districtRepository, cityRepository, validator);
    }

    @Test
    void searchCitiesNormalizesVietnameseDiacritics() {
        City hcm = new City();
        ReflectionTestUtils.setField(hcm, "id", 1L);
        hcm.setName("Thành phố Hồ Chí Minh");
        hcm.setCode("HCM");

        City hn = new City();
        ReflectionTestUtils.setField(hn, "id", 2L);
        hn.setName("Thành phố Hà Nội");
        hn.setCode("HN");

        City dn = new City();
        ReflectionTestUtils.setField(dn, "id", 3L);
        dn.setName("Thành phố Đà Nẵng");
        dn.setCode("DN");

        when(cityRepository.findAll()).thenReturn(List.of(hcm, hn, dn));

        // Search with unaccented text "ho chi minh"
        List<CitySummary> results = getCitiesUseCase.execute(GetCitiesQuery.builder().search("ho chi minh").build());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Thành phố Hồ Chí Minh");

        // Search with accented text "Hà Nội"
        List<CitySummary> results2 = getCitiesUseCase.execute(GetCitiesQuery.builder().search("ha noi").build());
        assertThat(results2).hasSize(1);
        assertThat(results2.get(0).name()).isEqualTo("Thành phố Hà Nội");

        // Search with unaccented "da nang"
        List<CitySummary> results3 = getCitiesUseCase.execute(GetCitiesQuery.builder().search("da nang").build());
        assertThat(results3).hasSize(1);
        assertThat(results3.get(0).name()).isEqualTo("Thành phố Đà Nẵng");
    }

    @Test
    void searchDistrictsNormalizesVietnameseDiacritics() {
        when(cityRepository.existsById(1L)).thenReturn(true);

        District q1 = new District();
        ReflectionTestUtils.setField(q1, "id", 10L);
        q1.setCityId(1L);
        q1.setName("Quận Bình Thạnh");
        q1.setCode("QBT");

        District q2 = new District();
        ReflectionTestUtils.setField(q2, "id", 11L);
        q2.setCityId(1L);
        q2.setName("Quận Gò Vấp");
        q2.setCode("QGV");

        when(districtRepository.findByCityId(1L)).thenReturn(List.of(q1, q2));

        // Search unaccented "binh thanh"
        List<DistrictSummary> results = getDistrictsByCityUseCase.execute(
                GetDistrictsByCityQuery.builder().cityId(1L).search("binh thanh").build());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Quận Bình Thạnh");

        // Search unaccented "go vap"
        List<DistrictSummary> results2 = getDistrictsByCityUseCase.execute(
                GetDistrictsByCityQuery.builder().cityId(1L).search("go vap").build());
        assertThat(results2).hasSize(1);
        assertThat(results2.get(0).name()).isEqualTo("Quận Gò Vấp");
    }
}
