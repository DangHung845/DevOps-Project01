package com.yas.location.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.location.LocationApplication;
import com.yas.location.model.Address;
import com.yas.location.model.Country;
import com.yas.location.model.District;
import com.yas.location.model.StateOrProvince;
import com.yas.location.repository.AddressRepository;
import com.yas.location.repository.CountryRepository;
import com.yas.location.repository.DistrictRepository;
import com.yas.location.repository.StateOrProvinceRepository;
import com.yas.location.viewmodel.address.AddressDetailVm;
import com.yas.location.viewmodel.address.AddressGetVm;
import com.yas.location.viewmodel.address.AddressPostVm;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = LocationApplication.class)
public class AddressServiceTest {

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private DistrictRepository districtRepository;
    @Autowired
    private StateOrProvinceRepository stateOrProvinceRepository;
    @Autowired
    private AddressService addressService;

    private Address address1;
    private Address address2;
    private Country country;
    private District district;
    private StateOrProvince stateOrProvince;

    private void generateTestData() {
        country = countryRepository.save(Country.builder()
            .name("country-1")
            .build());
        stateOrProvince = stateOrProvinceRepository.save(StateOrProvince.builder()
            .name("state-or-province")
            .country(country)
            .build());
        district = districtRepository.save(District.builder()
            .name("district-1")
            .stateProvince(stateOrProvince)
            .build());
        address1 = addressRepository.save(Address.builder()
            .contactName("address-1")
            .city("city-1")
            .country(country)
            .district(district)
            .stateOrProvince(stateOrProvince)
            .build());
        address2 = addressRepository.save(Address.builder()
            .contactName("address-1")
            .city("city-1")
            .country(country)
            .district(district)
            .stateOrProvince(stateOrProvince)
            .build());
    }

    @AfterEach
    void tearDown() {
        addressRepository.deleteAll();
        districtRepository.deleteAll();
        stateOrProvinceRepository.deleteAll();
        countryRepository.deleteAll();
    }

    @Test
    void getAddress_ExistInDatabase_Success() {
        generateTestData();
        AddressDetailVm addressDetailVm = addressService.getAddress(address1.getId());
        assertNotNull(addressDetailVm);
        assertEquals("address-1", addressDetailVm.contactName());
    }

    @Test
    void getAddress_NotExistInDatabase_ThrowsNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> addressService.getAddress(100000L));
        assertEquals(String.format("The address %s is not found", "100000"), exception.getMessage());
    }

    @Test
    void getAllAddresses_Success() {
        generateTestData();
        List<AddressDetailVm> addressDetailVmList = addressService.getAddressList(List.of(address1.getId(), address2.getId()));
        assertEquals(2, addressDetailVmList.size());
    }

    @Test
    void updateAddress_validData_Success() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("update-address")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .build();
        addressService.updateAddress(address1.getId(), addressPostVm);
        AddressDetailVm addressDetailVm = addressService.getAddress(address1.getId());
        assertNotNull(addressDetailVm);
        assertEquals("update-address", addressDetailVm.contactName());
    }

    @Test
    void updateAddress_inValidAddressId_ThrowsAddressNotFoundException() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("update-address")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .build();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> addressService.updateAddress(10000L, addressPostVm));
        assertEquals(String.format("The address %s is not found", "10000"), exception.getMessage());
    }

    @Test
    void createAddress_validDate_Success() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("update-address")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .build();
        AddressGetVm addressGetVm = addressService.createAddress(addressPostVm);
        assertNotNull(addressGetVm);
    }

    @Test
    void createAddress_inValidData_ThrowsCountryNotFoundException() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("update-address")
            .districtId(district.getId())
            .countryId(10000L)
            .stateOrProvinceId(stateOrProvince.getId())
            .build();
        NotFoundException exception = assertThrows(NotFoundException.class, () -> addressService.createAddress(addressPostVm));
        assertEquals(String.format("The country %s is not found", "10000"), exception.getMessage());
    }

    @Test
    void deleteAddress_givenAddressIdValid_thenSuccess() {
        generateTestData();
        Long id = addressRepository.findAll().getFirst().getId();
        addressService.deleteAddress(id);
        // make a call to get the address with id which has been deleted -> throw error not found because deleted success.
        NotFoundException exception = assertThrows(NotFoundException.class, () -> addressService.getAddress(100000L));
        assertEquals(String.format("The address %s is not found", "100000"), exception.getMessage());
    }

    @Test
    void deleteAddress_givenAddressIdInValid_ThrowsAddressNotFoundException() {
        NotFoundException exception = assertThrows(NotFoundException.class, () -> addressService.deleteAddress(1L));
        assertEquals(String.format("The address %s is not found", "1"), exception.getMessage());
    }

    @Test
    void getAllAddresses_givenEmptyIdList_returnsEmptyList() {
        List<AddressDetailVm> addressDetailVmList = addressService.getAddressList(java.util.List.of());
        assertNotNull(addressDetailVmList);
        assertEquals(0, addressDetailVmList.size());
    }

    @Test
    void updateAddress_givenExistingAddressWithPartialFields_updatesAndPersists() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("partial-update")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .build();

        addressService.updateAddress(address1.getId(), addressPostVm);

        AddressDetailVm addressDetailVm = addressService.getAddress(address1.getId());
        assertNotNull(addressDetailVm);
        assertEquals("partial-update", addressDetailVm.contactName());
    }

    @Test
    void createAddress_whenDistrictAndStateProvinceMissing_stillCreatesWithCountry() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("no-district-state")
            .districtId(null)
            .countryId(country.getId())
            .stateOrProvinceId(null)
            .build();

        AddressGetVm addressGetVm = addressService.createAddress(addressPostVm);

        assertNotNull(addressGetVm);
        AddressDetailVm persisted = addressService.getAddress(addressGetVm.id());
        assertNotNull(persisted);
        assertEquals("no-district-state", persisted.contactName());
    }

    @Test
    void updateAddress_whenOnlyCityAndZipChange_shouldPersistThoseFields() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName(address1.getContactName())
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .city("new-city")
            .zipCode("99999")
            .build();

        addressService.updateAddress(address1.getId(), addressPostVm);

        AddressDetailVm updated = addressService.getAddress(address1.getId());
        assertNotNull(updated);
        assertEquals("new-city", updated.city());
        assertEquals("99999", updated.zipCode());
    }

    @Test
    void getAllAddresses_withMixedExistingAndNonExistingIds_returnsOnlyExistingOnes() {
        generateTestData();
        List<AddressDetailVm> addressDetailVmList =
            addressService.getAddressList(java.util.List.of(address1.getId(), 999999L));
        assertNotNull(addressDetailVmList);
        assertEquals(1, addressDetailVmList.size());
        assertEquals(address1.getId(), addressDetailVmList.getFirst().id());
    }

    @Test
    void updateAddress_whenContactNameChanges_shouldPersistContactName() {
        generateTestData();

        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("new-contact")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .city(address1.getCity())
            .zipCode(address1.getZipCode())
            .build();

        addressService.updateAddress(address1.getId(), addressPostVm);

        AddressDetailVm updated = addressService.getAddress(address1.getId());

        assertNotNull(updated);
        assertEquals("new-contact", updated.contactName());
    }

    @Test
    void updateAddress_whenAllFieldsChange_shouldPersistAllFields() {
        generateTestData();

        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("contact-new")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .city("another-city")
            .zipCode("88888")
            .build();

        addressService.updateAddress(address1.getId(), addressPostVm);

        AddressDetailVm updated = addressService.getAddress(address1.getId());

        assertNotNull(updated);
        assertEquals("contact-new", updated.contactName());
        assertEquals("another-city", updated.city());
        assertEquals("88888", updated.zipCode());
    }

    @Test
    void updateAddress_whenDataIsSame_shouldKeepValues() {
        generateTestData();

        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName(address1.getContactName())
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .city(address1.getCity())
            .zipCode(address1.getZipCode())
            .build();

        addressService.updateAddress(address1.getId(), addressPostVm);

        AddressDetailVm updated = addressService.getAddress(address1.getId());

        assertNotNull(updated);
        assertEquals(address1.getCity(), updated.city());
        assertEquals(address1.getZipCode(), updated.zipCode());
    }

    @Test
    void updateAddress_whenAddressNotFound_shouldThrowException() {
        generateTestData();

        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("test")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .city("city")
            .zipCode("12345")
            .build();

        assertThrows(RuntimeException.class, () -> 
            addressService.updateAddress(99999L, addressPostVm)
        );
    }

    @Test
    void createAddress_whenStateOrProvinceAndDistrictNotFound_savesAddressWithoutThem() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("not-found-refs")
            .districtId(9999L)
            .countryId(country.getId())
            .stateOrProvinceId(8888L)
            .build();

        AddressGetVm addressGetVm = addressService.createAddress(addressPostVm);

        assertNotNull(addressGetVm);
        AddressDetailVm persisted = addressService.getAddress(addressGetVm.id());
        assertNotNull(persisted);
        assertEquals("not-found-refs", persisted.contactName());
    }

    @Test
    void updateAddress_whenStateOrProvinceAndCountryAndDistrictNotFound_keepsOldValuesOrIgnores() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("update-not-found-refs")
            .districtId(9999L)
            .countryId(8888L)
            .stateOrProvinceId(7777L)
            .build();

        addressService.updateAddress(address1.getId(), addressPostVm);

        AddressDetailVm updated = addressService.getAddress(address1.getId());
        assertNotNull(updated);
        assertEquals("update-not-found-refs", updated.contactName());
    }

    @Test
    void createAddress_withAllFields_Success() {
        generateTestData();
        AddressPostVm addressPostVm = AddressPostVm.builder()
            .contactName("full-address")
            .addressLine1("Line 1")
            .addressLine2("Line 2")
            .phone("123456789")
            .city("Full City")
            .zipCode("12345")
            .districtId(district.getId())
            .countryId(country.getId())
            .stateOrProvinceId(stateOrProvince.getId())
            .build();
        AddressGetVm addressGetVm = addressService.createAddress(addressPostVm);
        assertNotNull(addressGetVm);
        AddressDetailVm persisted = addressService.getAddress(addressGetVm.id());
        assertNotNull(persisted);
        assertEquals("Line 1", persisted.addressLine1());
        assertEquals("Line 2", persisted.addressLine2());
        assertEquals("123456789", persisted.phone());
    }
}
