package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.LocationApplication;
import com.yas.location.model.Country;
import com.yas.location.repository.CountryRepository;

import com.yas.location.viewmodel.country.CountryListGetVm;
import com.yas.location.viewmodel.country.CountryPostVm;
import com.yas.location.viewmodel.country.CountryVm;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = LocationApplication.class)
public class CountryServiceTest {

    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CountryService countryService;
    private Country country1;

    private void generateTestData() {
        country1 = countryRepository.save(Country.builder()
            .code2("TS")
            .name("country-1")
            .build());
        countryRepository.save(Country.builder()
            .code2("TW")
            .name("country-2")
            .build());
    }

    @AfterEach
    void tearDown() {
        countryRepository.deleteAll();
    }

    @Test
    void getCountry_ExistInDatabase_Success() {
        generateTestData();
        CountryVm countryVm = countryService.findById(country1.getId());
        assertNotNull(countryVm);
        assertEquals("country-1", countryVm.name());
    }

    @Test
    void getCountry_NotInDatabase_ThrowsCountryNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> countryService.findById(1L));
        assertEquals(String.format("The country %s is not found", "1"), exception.getMessage());
    }

    @Test
    void getAllCountries_Success() {
        generateTestData();
        List<CountryVm> countryVms = countryService.findAllCountries();
        assertEquals(2, countryVms.size());
    }

    @Test
    void createCountry_ValidData_Success() {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("SE")
            .name("country")
            .build();
        Country country = countryService.create(countryPostVm);
        assertNotNull(country);
        assertEquals("country", country.getName());
    }

    @Test
    void createCountry_WithNameExisted_ThrowsNameAlreadyExistedException() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("SE")
            .name("country-1")
            .build();
        DuplicatedException exception =
            assertThrows(DuplicatedException.class, () -> countryService.create(countryPostVm));
        assertEquals(String.format("Request name %s is already existed", "country-1"), exception.getMessage());
    }

    @Test
    void createCountry_WithCodeExisted_ThrowsCodeAlreadyExistedException() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("TS")
            .name("country")
            .build();
        DuplicatedException exception =
            assertThrows(DuplicatedException.class, () -> countryService.create(countryPostVm));
        assertEquals(String.format("The code %s is already existed", "TS"), exception.getMessage());
    }

    @Test
    void updateCountry_ValidData_Success() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .name("country-1-update")
            .build();
        countryService.update(countryPostVm, country1.getId());
        // Get the country after update
        CountryVm countryVm = countryService.findById(country1.getId());
        assertNotNull(countryVm);
        assertEquals("country-1-update", countryVm.name());
    }

    @Test
    void updateCountry_WithIdNotValid_ThrowsCountryNotFoundException() {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .name("country-1-update")
            .build();
        NotFoundException exception =
            assertThrows(NotFoundException.class, () -> countryService.update(countryPostVm, 1L));
        assertEquals(String.format("The country %s is not found", "1"), exception.getMessage());
    }

    @Test
    void updateCountry_WithNameExisted_ThrowsNameAlreadyExistedException() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("SE")
            .name("country-2")
            .build();
        Long country1Id = country1.getId();
        DuplicatedException exception =
            assertThrows(DuplicatedException.class, () -> countryService.update(countryPostVm, country1Id));
        assertEquals(String.format("Request name %s is already existed", "country-2"), exception.getMessage());
    }

    @Test
    void updateCountry_WithCodeExisted_ThrowsCodeAlreadyExistedException() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("tW")
            .name("country-1")
            .build();
        Long country1Id = country1.getId();
        DuplicatedException exception =
            assertThrows(DuplicatedException.class, () -> countryService.update(countryPostVm, country1Id));
        assertEquals(String.format("The code %s is already existed", "tW"), exception.getMessage());
    }

    @Test
    void deleteCountry_WithValidId_Success() {
        generateTestData();
        countryService.delete(country1.getId());
        // Get the country with id after delete -> null
        Long country1Id = country1.getId();
        assertThrows(NotFoundException.class, () -> countryService.findById(country1Id));
    }

    @Test
    void deleteCountry_WithInValidId_ThrowsCountryNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> countryService.delete(1L));
        assertEquals(String.format("The country %s is not found", "1"), exception.getMessage());
    }

    @Test
    void getCountries_Pagination_Success() {
        generateTestData();
        int pageNo = 1;
        int pageSize = 2;
        CountryListGetVm countryListGetVm = countryService.getPageableCountries(pageNo, pageSize);
        assertNotNull(countryListGetVm);
        assertEquals(countryListGetVm.pageNo(), pageNo);
        assertEquals(countryListGetVm.pageSize(), pageSize);
        assertTrue(countryListGetVm.isLast());
        assertEquals(2, countryListGetVm.totalElements());
        assertEquals(1, countryListGetVm.totalPages());
    }

    @Test
    void getAllCountries_shouldReturnCountriesSortedByNameAsc() {
        countryRepository.save(Country.builder()
            .code2("C3")
            .name("country-c")
            .build());
        countryRepository.save(Country.builder()
            .code2("A1")
            .name("country-a")
            .build());
        List<CountryVm> countryVms = countryService.findAllCountries();
        assertNotNull(countryVms);
        assertTrue(countryVms.size() >= 2);
        assertEquals("country-a", countryVms.getFirst().name());
    }

    @Test
    void getPageableCountries_whenNoCountries_returnsEmptyPage() {
        int pageNo = 0;
        int pageSize = 5;

        CountryListGetVm result = countryService.getPageableCountries(pageNo, pageSize);

        assertNotNull(result);
        assertEquals(pageNo, result.pageNo());
        assertEquals(0, result.totalElements());
        assertTrue(result.countryContent().isEmpty());
    }

    @Test
    void findAllCountries_whenNoCountries_returnsEmptyList() {
        List<CountryVm> countryVms = countryService.findAllCountries();
        assertNotNull(countryVms);
        assertTrue(countryVms.isEmpty());
    }

    @Test
    void updateCountry_WithSameNameAndCode_Success() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2(country1.getCode2())
            .name(country1.getName())
            .build();
        countryService.update(countryPostVm, country1.getId());
        CountryVm countryVm = countryService.findById(country1.getId());
        assertNotNull(countryVm);
        assertEquals(country1.getName(), countryVm.name());
    }

    @Test
    void getPageableCountries_whenHasCountries_returnsCorrectPaginationMetrics() {
        generateTestData();
        CountryListGetVm result = countryService.getPageableCountries(0, 1);
        assertNotNull(result);
        assertEquals(0, result.pageNo());
        assertEquals(1, result.pageSize());
        assertEquals(2, result.totalElements());
        assertEquals(2, result.totalPages());
        org.junit.jupiter.api.Assertions.assertFalse(result.isLast());
    }

    @Test
    void createCountry_withCode3AndBooleanFlags_Success() {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("NW")
            .name("new-country")
            .code3("NWC")
            .isBillingEnabled(true)
            .isShippingEnabled(false)
            .isCityEnabled(true)
            .isZipCodeEnabled(false)
            .isDistrictEnabled(true)
            .build();
        Country country = countryService.create(countryPostVm);
        assertNotNull(country);
        assertEquals("NWC", country.getCode3());
        assertEquals(true, country.getIsBillingEnabled());
        assertEquals(false, country.getIsShippingEnabled());
        assertEquals(true, country.getIsCityEnabled());
        assertEquals(false, country.getIsZipCodeEnabled());
        assertEquals(true, country.getIsDistrictEnabled());
    }

    @Test
    void findById_shouldReturnAllCountryVmFields() {
        Country saved = countryRepository.save(Country.builder()
            .code2("FD")
            .name("field-country")
            .code3("FDC")
            .isBillingEnabled(true)
            .isShippingEnabled(true)
            .isCityEnabled(false)
            .isZipCodeEnabled(true)
            .isDistrictEnabled(false)
            .build());
        CountryVm vm = countryService.findById(saved.getId());
        assertNotNull(vm);
        assertEquals(saved.getId(), vm.id());
        assertEquals("FD", vm.code2());
        assertEquals("field-country", vm.name());
        assertEquals("FDC", vm.code3());
        assertEquals(true, vm.isBillingEnabled());
        assertEquals(true, vm.isShippingEnabled());
        assertEquals(false, vm.isCityEnabled());
        assertEquals(true, vm.isZipCodeEnabled());
        assertEquals(false, vm.isDistrictEnabled());
    }

    @Test
    void updateCountry_withCode2Change_Success() {
        generateTestData();
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("UP")
            .name("country-1")
            .code3("UPD")
            .isBillingEnabled(true)
            .isShippingEnabled(true)
            .build();
        countryService.update(countryPostVm, country1.getId());
        CountryVm countryVm = countryService.findById(country1.getId());
        assertNotNull(countryVm);
        assertEquals("UP", countryVm.code2());
        assertEquals("UPD", countryVm.code3());
        assertEquals(true, countryVm.isBillingEnabled());
    }

    @Test
    void getPageableCountries_returnsCountryContent() {
        generateTestData();
        CountryListGetVm result = countryService.getPageableCountries(0, 10);
        assertNotNull(result);
        assertEquals(2, result.countryContent().size());
        CountryVm first = result.countryContent().getFirst();
        assertNotNull(first.id());
        assertNotNull(first.name());
    }

    @Test
    void deleteCountry_afterDelete_countDecreasesByOne() {
        generateTestData();
        int beforeSize = countryService.findAllCountries().size();
        countryService.delete(country1.getId());
        int afterSize = countryService.findAllCountries().size();
        assertEquals(beforeSize - 1, afterSize);
    }

    @Test
    void getPageableCountries_lastPage_isLastTrue() {
        generateTestData();
        CountryListGetVm result = countryService.getPageableCountries(1, 1);
        assertNotNull(result);
        assertTrue(result.isLast());
        assertEquals(1, result.countryContent().size());
    }

    @Test
    void createCountry_withNullBooleanFlags_Success() {
        CountryPostVm countryPostVm = CountryPostVm.builder()
            .code2("NB")
            .name("null-booleans")
            .build();
        Country country = countryService.create(countryPostVm);
        assertNotNull(country);
        assertEquals("null-booleans", country.getName());
    }
}
