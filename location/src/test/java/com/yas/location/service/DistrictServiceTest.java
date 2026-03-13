package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yas.location.LocationApplication;
import com.yas.location.model.Country;
import com.yas.location.model.District;
import com.yas.location.model.StateOrProvince;
import com.yas.location.repository.CountryRepository;
import com.yas.location.repository.DistrictRepository;
import com.yas.location.repository.StateOrProvinceRepository;
import com.yas.location.viewmodel.district.DistrictGetVm;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = LocationApplication.class)
public class DistrictServiceTest {

    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private StateOrProvinceRepository stateOrProvinceRepository;
    @Autowired
    private DistrictRepository districtRepository;
    @Autowired
    private DistrictService districtService;

    private District district1;
    private Country country;
    private StateOrProvince stateOrProvince;

    private void generateTestData() {
        country = countryRepository.save(Country.builder()
            .name("country-1")
            .build());
        stateOrProvince = stateOrProvinceRepository.save(StateOrProvince.builder()
            .name("state-or-province")
            .country(country)
            .build());
        district1 = districtRepository.save(District.builder()
            .name("district-1")
            .stateProvince(stateOrProvince)
            .build());
    }

    @AfterEach
    void tearDown() {
        districtRepository.deleteAll();
        stateOrProvinceRepository.deleteAll();
        countryRepository.deleteAll();
    }

    @Test
    void getDistrict_WithValidId_Success() {
        generateTestData();
        List<DistrictGetVm> districtGetVm = districtService.getList(district1.getId());
        assertNotNull(districtGetVm);
    }

    @Test
    void getDistrict_WithNoDistrictsForState_ReturnsEmptyList() {
        generateTestData();
        List<DistrictGetVm> districtGetVm = districtService.getList(9999L);
        assertNotNull(districtGetVm);
        org.junit.jupiter.api.Assertions.assertTrue(districtGetVm.isEmpty());
    }

    @Test
    void getDistrict_multipleDistrictsForState_returnsAll() {
        generateTestData();
        districtRepository.save(District.builder()
            .name("district-2")
            .stateProvince(stateOrProvince)
            .build());

        List<DistrictGetVm> districtGetVm = districtService.getList(stateOrProvince.getId());
        assertNotNull(districtGetVm);
        org.junit.jupiter.api.Assertions.assertTrue(districtGetVm.size() >= 2);
    }

    @Test
    void getDistrict_afterAllDistrictsDeleted_returnsEmptyList() {
        generateTestData();
        districtRepository.deleteAll();

        List<DistrictGetVm> districtGetVm = districtService.getList(stateOrProvince.getId());
        assertNotNull(districtGetVm);
        org.junit.jupiter.api.Assertions.assertTrue(districtGetVm.isEmpty());
    }

    @Test
    void getDistrict_WithNullId_returnsEmptyListOrDoesnThrow() {
        generateTestData();
        List<DistrictGetVm> districtGetVm = districtService.getList(null);
        assertNotNull(districtGetVm);
    }

    @Test
    void getDistrict_verifyOrderByNameAsc() {
        generateTestData();
        districtRepository.save(District.builder()
            .name("a-district")
            .stateProvince(stateOrProvince)
            .build());
        districtRepository.save(District.builder()
            .name("z-district")
            .stateProvince(stateOrProvince)
            .build());

        List<DistrictGetVm> result = districtService.getList(stateOrProvince.getId());
        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.size() >= 3);
        org.junit.jupiter.api.Assertions.assertTrue(
            result.get(0).name().compareTo(result.get(1).name()) <= 0);
    }

    @Test
    void getDistrict_singleDistrict_returnsOneElement() {
        generateTestData();
        List<DistrictGetVm> result = districtService.getList(stateOrProvince.getId());
        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals("district-1", result.getFirst().name());
    }

    @Test
    void getDistrict_verifyDistrictGetVmFields() {
        generateTestData();
        List<DistrictGetVm> result = districtService.getList(stateOrProvince.getId());
        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertFalse(result.isEmpty());
        DistrictGetVm first = result.getFirst();
        assertNotNull(first.id());
        assertNotNull(first.name());
    }

    @Test
    void getDistrict_afterAddingDistrict_returnsUpdatedList() {
        generateTestData();
        List<DistrictGetVm> before = districtService.getList(stateOrProvince.getId());
        int beforeSize = before.size();

        districtRepository.save(District.builder()
            .name("district-new")
            .stateProvince(stateOrProvince)
            .build());

        List<DistrictGetVm> after = districtService.getList(stateOrProvince.getId());
        org.junit.jupiter.api.Assertions.assertEquals(beforeSize + 1, after.size());
    }
}

