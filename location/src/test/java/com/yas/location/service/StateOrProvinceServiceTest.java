package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.LocationApplication;
import com.yas.location.model.Country;
import com.yas.location.model.StateOrProvince;
import com.yas.location.repository.CountryRepository;
import com.yas.location.repository.StateOrProvinceRepository;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceAndCountryGetNameVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceListGetVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvincePostVm;
import com.yas.location.viewmodel.stateorprovince.StateOrProvinceVm;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = LocationApplication.class)
public class StateOrProvinceServiceTest {

    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private StateOrProvinceRepository stateOrProvinceRepository;
    @Autowired
    private StateOrProvinceService stateOrProvinceService;

    private Country country;
    private StateOrProvince stateOrProvince1;
    private StateOrProvince stateOrProvince2;

    private void generateTestData() {
        country = countryRepository.save(Country.builder()
            .name("country-1")
            .build());
        stateOrProvince1 = stateOrProvinceRepository.save(StateOrProvince.builder()
            .name("state-or-province-1")
            .country(country)
            .build());
        stateOrProvince2 = stateOrProvinceRepository.save(StateOrProvince.builder()
            .name("state-or-province-2")
            .country(country)
            .build());
    }

    @AfterEach
    void tearDown() {
        stateOrProvinceRepository.deleteAll();
        countryRepository.deleteAll();
    }
    
    @Test
    void getStateOrProvince_WithValidId_Success() {
        generateTestData();
        StateOrProvinceVm stateOrProvinceVm = stateOrProvinceService.findById(stateOrProvince1.getId());
        assertNotNull(stateOrProvinceVm);
        assertEquals("state-or-province-1", stateOrProvinceVm.name());
    }
    
    @Test
    void getStateOrProvince_WithInValidId_ThrowsStateOrProvinceNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> stateOrProvinceService.findById(1L));
        assertEquals(String.format("The state or province %s is not found", "1"), exception.getMessage());
    }
    
    @Test
    void createStateOrProvince_ValidData_Success() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-or-province")
            .code("STATE")
            .build();
        StateOrProvince stateOrProvince = stateOrProvinceService.createStateOrProvince(stateOrProvincePostVm);
        assertEquals("STATE", stateOrProvince.getCode());
    }
    
    @Test
    void createStateOrProvince_WithNameExisted_ThrowsNameAlreadyExistException() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-or-province-1")
            .code("STATE")
            .build();
        DuplicatedException exception = assertThrows(DuplicatedException.class, () -> stateOrProvinceService.createStateOrProvince(stateOrProvincePostVm));
        assertEquals(String.format("Request name %s is already existed", "state-or-province-1"), exception.getMessage());
    }
    
    @Test
    void createStateOrProvince_WithCountryNotExist_ThrowsCountryNotFoundException() {
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(1L)
            .name("state-or-province-1")
            .code("STATE")
            .build();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> stateOrProvinceService.createStateOrProvince(stateOrProvincePostVm));
        assertEquals(String.format("The country %s is not found", "1"), exception.getMessage());
    }
    
    @Test
    void updateStateOrProvince_WithValidData_Success() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-or-province-update")
            .code("STATE")
            .build();
        stateOrProvinceService.updateStateOrProvince(stateOrProvincePostVm, stateOrProvince1.getId());
        // Get the updated state-or-province to check
        StateOrProvinceVm stateOrProvinceVm = stateOrProvinceService.findById(stateOrProvince1.getId());
        assertNotNull(stateOrProvinceVm);
        assertEquals("state-or-province-update", stateOrProvinceVm.name());
    }

    @Test
    void updateStateOrProvince_WithInValidId_ThrowsStateOrProvinceNotFound() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-or-province-update")
            .code("STATE")
            .build();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> stateOrProvinceService.updateStateOrProvince(stateOrProvincePostVm, 1000L));
        assertEquals(String.format("The state or province %s is not found", "1000"), exception.getMessage());
    }

    @Test
    void updateStateOrProvince_WithNameExisted_ThrowsNameAlreadyExistedException() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-or-province-2")
            .code("STATE")
            .build();
        Long stateOrProvinceId = stateOrProvince1.getId();
        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> stateOrProvinceService.updateStateOrProvince(stateOrProvincePostVm, stateOrProvinceId));
        assertEquals(String.format("Request name %s is already existed", "state-or-province-2"), exception.getMessage());
    }

    @Test
    void deleteStateOrProvince_WithExisted_Success() {
        generateTestData();
        stateOrProvinceService.delete(stateOrProvince1.getId());
        // Get the state-or-province which deleted -> got exception
        Long stateOrProvinceId = stateOrProvince1.getId();
        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> stateOrProvinceService.findById(stateOrProvinceId));
        assertEquals(String.format("The state or province %s is not found", stateOrProvince1.getId()), exception.getMessage());
    }

    @Test
    void deleteStateOrProvince_WithInvalidId_ThrowsStateOrProvinceNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> stateOrProvinceService.delete(1L));
        assertEquals(String.format("The state or province %s is not found", "1"), exception.getMessage());
    }

    @Test
    void getStateOrProvinceAndCountryName_Success() {
        generateTestData();
        List<StateOrProvinceAndCountryGetNameVm> stateOrProvinceAndCountryGetNameVms =
            stateOrProvinceService.getStateOrProvinceAndCountryNames(List.of(stateOrProvince1.getId(), stateOrProvince2.getId()));
        assertNotNull(stateOrProvinceAndCountryGetNameVms);
        assertEquals("country-1", stateOrProvinceAndCountryGetNameVms.getFirst().countryName());
    }

    @Test
    void getAllStateOrProvinces_Success() {
        generateTestData();
        List<StateOrProvinceVm> stateOrProvinceVms = stateOrProvinceService.findAll();
        assertNotNull(stateOrProvinceVms);
        assertEquals(2, stateOrProvinceVms.size());
    }

    @Test
    void getAllStateOrProvinceByCountryId_Success() {
        generateTestData();
        List<StateOrProvinceVm> stateOrProvinceVms = stateOrProvinceService.getAllByCountryId(country.getId());
        assertNotNull(stateOrProvinceVms);
        assertEquals(2, stateOrProvinceVms.size());
    }

    @Test
    void getStateOrProvincePagination_Success() {
        generateTestData();
        int pageNo = 0;
        int pageSize = 2;
        StateOrProvinceListGetVm stateOrProvinceListGetVm = stateOrProvinceService.getPageableStateOrProvinces(pageNo, pageSize, country.getId());
        assertNotNull(stateOrProvinceListGetVm);
        assertEquals(stateOrProvinceListGetVm.pageNo(), pageNo);
        assertEquals(stateOrProvinceListGetVm.pageSize(), pageSize);
        assertEquals(2, stateOrProvinceListGetVm.stateOrProvinceContent().size());
    }

    @Test
    void getStateOrProvinceAndCountryName_givenEmptyList_returnsEmptyList() {
        List<StateOrProvinceAndCountryGetNameVm> result =
            stateOrProvinceService.getStateOrProvinceAndCountryNames(java.util.List.of());
        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getAllStateOrProvinces_whenNoData_returnsEmptyList() {
        List<StateOrProvinceVm> stateOrProvinceVms = stateOrProvinceService.findAll();
        assertNotNull(stateOrProvinceVms);
        org.junit.jupiter.api.Assertions.assertTrue(stateOrProvinceVms.isEmpty());
    }

    @Test
    void getAllByCountryId_whenNoStateOrProvinceForCountry_returnsEmptyList() {
        Country otherCountry = countryRepository.save(Country.builder()
            .name("country-2")
            .build());

        List<StateOrProvinceVm> stateOrProvinceVms =
            stateOrProvinceService.getAllByCountryId(otherCountry.getId());

        assertNotNull(stateOrProvinceVms);
        org.junit.jupiter.api.Assertions.assertTrue(stateOrProvinceVms.isEmpty());
    }

    @Test
    void createStateOrProvince_withSameNameButDifferentCountry_succeeds() {
        generateTestData();
        Country otherCountry = countryRepository.save(Country.builder()
            .name("country-2")
            .build());

        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(otherCountry.getId())
            .name("state-or-province-1")
            .code("STATE2")
            .build();

        StateOrProvince created = stateOrProvinceService.createStateOrProvince(stateOrProvincePostVm);
        assertNotNull(created);
        assertEquals("STATE2", created.getCode());
        assertEquals(otherCountry.getId(), created.getCountry().getId());
    }

    @Test
    void updateStateOrProvince_withTypeField_Success() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-typed")
            .code("TYPED")
            .type("province")
            .build();
        stateOrProvinceService.updateStateOrProvince(stateOrProvincePostVm, stateOrProvince1.getId());
        StateOrProvinceVm vm = stateOrProvinceService.findById(stateOrProvince1.getId());
        assertNotNull(vm);
        assertEquals("state-typed", vm.name());
        assertEquals("TYPED", vm.code());
        assertEquals("province", vm.type());
    }

    @Test
    void updateStateOrProvince_withSameName_Success() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-or-province-1")
            .code("SAME")
            .build();
        stateOrProvinceService.updateStateOrProvince(stateOrProvincePostVm, stateOrProvince1.getId());
        StateOrProvinceVm vm = stateOrProvinceService.findById(stateOrProvince1.getId());
        assertNotNull(vm);
        assertEquals("state-or-province-1", vm.name());
        assertEquals("SAME", vm.code());
    }

    @Test
    void getPageableStateOrProvinces_verifyAllPagingFields() {
        generateTestData();
        StateOrProvinceListGetVm result = stateOrProvinceService.getPageableStateOrProvinces(0, 1, country.getId());
        assertNotNull(result);
        assertEquals(0, result.pageNo());
        assertEquals(1, result.pageSize());
        assertEquals(2, result.totalElements());
        assertEquals(2, result.totalPages());
        org.junit.jupiter.api.Assertions.assertFalse(result.isLast());
    }

    @Test
    void getPageableStateOrProvinces_lastPage_isLastTrue() {
        generateTestData();
        StateOrProvinceListGetVm result = stateOrProvinceService.getPageableStateOrProvinces(1, 1, country.getId());
        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.isLast());
        assertEquals(1, result.stateOrProvinceContent().size());
    }

    @Test
    void getPageableStateOrProvinces_noData_returnsEmptyPage() {
        Country emptyCountry = countryRepository.save(Country.builder()
            .name("empty-country")
            .build());
        StateOrProvinceListGetVm result = stateOrProvinceService.getPageableStateOrProvinces(0, 10, emptyCountry.getId());
        assertNotNull(result);
        assertEquals(0, result.totalElements());
        org.junit.jupiter.api.Assertions.assertTrue(result.stateOrProvinceContent().isEmpty());
    }

    @Test
    void getStateOrProvinceAndCountryNames_multipleItems_returnsAll() {
        generateTestData();
        List<StateOrProvinceAndCountryGetNameVm> result =
            stateOrProvinceService.getStateOrProvinceAndCountryNames(
                List.of(stateOrProvince1.getId(), stateOrProvince2.getId()));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("state-or-province-1", result.get(0).stateOrProvinceName());
        assertEquals("country-1", result.get(0).countryName());
        assertEquals("state-or-province-2", result.get(1).stateOrProvinceName());
    }

    @Test
    void getAllByCountryId_returnsCorrectFieldMapping() {
        generateTestData();
        List<StateOrProvinceVm> vms = stateOrProvinceService.getAllByCountryId(country.getId());
        assertNotNull(vms);
        assertEquals(2, vms.size());
        StateOrProvinceVm first = vms.getFirst();
        assertNotNull(first.id());
        assertNotNull(first.name());
        assertEquals(country.getId(), first.countryId());
    }

    @Test
    void createStateOrProvince_withTypeField_Success() {
        generateTestData();
        StateOrProvincePostVm stateOrProvincePostVm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("state-with-type")
            .code("SWT")
            .type("state")
            .build();
        StateOrProvince created = stateOrProvinceService.createStateOrProvince(stateOrProvincePostVm);
        assertNotNull(created);
        assertEquals("state-with-type", created.getName());
        assertEquals("SWT", created.getCode());
        assertEquals("state", created.getType());
    }

    @Test
    void findAll_returnsVmWithCountryId() {
        generateTestData();
        List<StateOrProvinceVm> vms = stateOrProvinceService.findAll();
        assertNotNull(vms);
        assertEquals(2, vms.size());
        for (StateOrProvinceVm vm : vms) {
            assertEquals(country.getId(), vm.countryId());
        }
    }

    @Test
    void deleteStateOrProvince_afterDelete_countDecreases() {
        generateTestData();
        int before = stateOrProvinceService.findAll().size();
        stateOrProvinceService.delete(stateOrProvince1.getId());
        int after = stateOrProvinceService.findAll().size();
        assertEquals(before - 1, after);
    }

    @Test
    void updateStateOrProvince_withCountryNotExist_ThrowsCountryNotFound() {
        generateTestData();

        StateOrProvincePostVm vm = StateOrProvincePostVm.builder()
            .countryId(9999L)
            .name("new-name")
            .code("NEW")
            .build();

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> stateOrProvinceService.updateStateOrProvince(vm, stateOrProvince1.getId())
        );

        assertEquals(String.format("The country %s is not found", "9999"), exception.getMessage());
    }

    @Test
    void getStateOrProvinceAndCountryNames_withInvalidIds_returnsEmptyList() {
        generateTestData();

        List<StateOrProvinceAndCountryGetNameVm> result =
            stateOrProvinceService.getStateOrProvinceAndCountryNames(List.of(999L, 888L));

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getPageableStateOrProvinces_pageOutOfRange_returnsEmptyContent() {
        generateTestData();

        StateOrProvinceListGetVm result =
            stateOrProvinceService.getPageableStateOrProvinces(10, 5, country.getId());

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.stateOrProvinceContent().isEmpty());
    }

    @Test
    void getAllByCountryId_withInvalidCountry_returnsEmptyList() {
        generateTestData();

        List<StateOrProvinceVm> result =
            stateOrProvinceService.getAllByCountryId(9999L);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void createStateOrProvince_verifyPersistedInDatabase() {
        generateTestData();

        StateOrProvincePostVm vm = StateOrProvincePostVm.builder()
            .countryId(country.getId())
            .name("persist-test")
            .code("PST")
            .build();

        StateOrProvince created = stateOrProvinceService.createStateOrProvince(vm);

        StateOrProvinceVm fetched = stateOrProvinceService.findById(created.getId());

        assertNotNull(fetched);
        assertEquals("persist-test", fetched.name());
    }
}
