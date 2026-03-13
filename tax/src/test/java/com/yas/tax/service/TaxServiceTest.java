package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@SpringBootTest(classes = TaxRateService.class)
public class TaxServiceTest {
    @MockBean
    TaxRateRepository taxRateRepository;
    @MockBean
    LocationService locationService;
    @MockBean
    TaxClassRepository taxClassRepository;

    @Autowired
    TaxRateService taxRateService;

    TaxRate taxRate;
    TaxClass taxClass;

    @BeforeEach
    void setUp() {
        taxClass = Instancio.create(TaxClass.class);
        taxRate = Instancio.of(TaxRate.class)
            .set(field("taxClass"), taxClass)
            .create();
        lenient().when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));
    }

    @Test
    void  testFindAll_shouldReturnAllTaxRate() {
        // run
        List<TaxRateVm> result = taxRateService.findAll();
        // assert
        assertThat(result).hasSize(1).contains(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void createTaxRate_shouldSaveAndReturnEntity_whenTaxClassExists() {
        // given
        Long taxClassId = 1L;
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "12345", taxClassId, 2L, 3L);

        when(taxClassRepository.existsById(taxClassId)).thenReturn(true);
        when(taxClassRepository.getReferenceById(taxClassId)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        TaxRate result = taxRateService.createTaxRate(postVm);

        // then
        assertThat(result.getRate()).isEqualTo(postVm.rate());
        assertThat(result.getZipCode()).isEqualTo(postVm.zipCode());
        assertThat(result.getTaxClass()).isEqualTo(taxClass);
        assertThat(result.getStateOrProvinceId()).isEqualTo(postVm.stateOrProvinceId());
        assertThat(result.getCountryId()).isEqualTo(postVm.countryId());
        verify(taxRateRepository, times(1)).save(any(TaxRate.class));
    }

    @Test
    void createTaxRate_shouldThrowNotFound_whenTaxClassDoesNotExist() {
        Long taxClassId = 10L;
        TaxRatePostVm postVm = new TaxRatePostVm(5.0, "00000", taxClassId, 2L, 3L);
        when(taxClassRepository.existsById(taxClassId)).thenReturn(false);

        assertThatThrownBy(() -> taxRateService.createTaxRate(postVm))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateTaxRate_shouldUpdateFields_whenTaxRateAndTaxClassExist() {
        // given
        Long taxRateId = 5L;
        Long taxClassId = 7L;
        TaxRate existing = this.taxRate;
        TaxRatePostVm postVm = new TaxRatePostVm(8.0, "99999", taxClassId, 11L, 22L);

        when(taxRateRepository.findById(taxRateId)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsById(taxClassId)).thenReturn(true);
        when(taxClassRepository.getReferenceById(taxClassId)).thenReturn(taxClass);

        // when
        taxRateService.updateTaxRate(postVm, taxRateId);

        // then
        assertThat(existing.getRate()).isEqualTo(postVm.rate());
        assertThat(existing.getZipCode()).isEqualTo(postVm.zipCode());
        assertThat(existing.getTaxClass()).isEqualTo(taxClass);
        assertThat(existing.getStateOrProvinceId()).isEqualTo(postVm.stateOrProvinceId());
        assertThat(existing.getCountryId()).isEqualTo(postVm.countryId());
        verify(taxRateRepository, times(1)).save(existing);
    }

    @Test
    void updateTaxRate_shouldThrowNotFound_whenTaxRateDoesNotExist() {
        Long taxRateId = 99L;
        TaxRatePostVm postVm = new TaxRatePostVm(8.0, "99999", 1L, 11L, 22L);
        when(taxRateRepository.findById(taxRateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, taxRateId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateTaxRate_shouldThrowNotFound_whenTaxClassDoesNotExist() {
        Long taxRateId = 5L;
        Long taxClassId = 7L;
        TaxRate existing = this.taxRate;
        TaxRatePostVm postVm = new TaxRatePostVm(8.0, "99999", taxClassId, 11L, 22L);

        when(taxRateRepository.findById(taxRateId)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsById(taxClassId)).thenReturn(false);

        assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, taxRateId))
            .isInstanceOf(NotFoundException.class);
        verify(taxRateRepository, times(0)).save(any(TaxRate.class));
    }

    @Test
    void delete_shouldDelete_whenTaxRateExists() {
        Long id = 1L;
        when(taxRateRepository.existsById(id)).thenReturn(true);

        taxRateService.delete(id);

        verify(taxRateRepository, times(1)).deleteById(id);
    }

    @Test
    void delete_shouldThrowNotFound_whenTaxRateDoesNotExist() {
        Long id = 1L;
        when(taxRateRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> taxRateService.delete(id))
            .isInstanceOf(NotFoundException.class);
        verify(taxRateRepository, times(0)).deleteById(id);
    }

    @Test
    void findById_shouldReturnVm_whenTaxRateExists() {
        Long id = 3L;
        when(taxRateRepository.findById(id)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(id);

        assertThat(result).isEqualTo(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void findById_shouldThrowNotFound_whenTaxRateDoesNotExist() {
        Long id = 3L;
        when(taxRateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxRateService.findById(id))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPageableTaxRates_shouldReturnEmptyList_whenNoTaxRates() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxRate> page = new PageImpl<>(List.of(), pageable, 0);
        when(taxRateRepository.findAll(pageable)).thenReturn(page);

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void getPageableTaxRates_shouldMapDetailsAndCallLocationService_whenTaxRatesExist() {
        Pageable pageable = PageRequest.of(0, 10);
        TaxRate first = this.taxRate;
        first.setStateOrProvinceId(1L);
        first.setCountryId(10L);
        first.getTaxClass().setName("Standard");

        TaxRate second = Instancio.of(TaxRate.class)
            .set(field("taxClass"), taxClass)
            .create();
        second.setStateOrProvinceId(2L);
        second.setCountryId(20L);
        second.getTaxClass().setName("Reduced");

        Page<TaxRate> page = new PageImpl<>(List.of(first, second), pageable, 2);
        when(taxRateRepository.findAll(pageable)).thenReturn(page);

        List<StateOrProvinceAndCountryGetNameVm> locationVms = List.of(
            new StateOrProvinceAndCountryGetNameVm(1L, "State1", "Country1"),
            new StateOrProvinceAndCountryGetNameVm(2L, "State2", "Country2")
        );
        when(locationService.getStateOrProvinceAndCountryNames(List.of(1L, 2L))).thenReturn(locationVms);

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).hasSize(2);
        TaxRateGetDetailVm firstDetail = result.taxRateGetDetailContent().get(0);
        TaxRateGetDetailVm secondDetail = result.taxRateGetDetailContent().get(1);

        assertThat(firstDetail.stateOrProvinceName()).isEqualTo("State1");
        assertThat(firstDetail.countryName()).isEqualTo("Country1");
        assertThat(secondDetail.stateOrProvinceName()).isEqualTo("State2");
        assertThat(secondDetail.countryName()).isEqualTo("Country2");
    }

    @Test
    void getTaxPercent_shouldReturnValue_whenRepositoryReturnsNonNull() {
        Long taxClassId = 1L;
        Long countryId = 2L;
        Long stateId = 3L;
        String zipCode = "12345";
        when(taxRateRepository.getTaxPercent(countryId, stateId, zipCode, taxClassId)).thenReturn(5.5);

        double result = taxRateService.getTaxPercent(taxClassId, countryId, stateId, zipCode);

        assertThat(result).isEqualTo(5.5);
    }

    @Test
    void getTaxPercent_shouldReturnZero_whenRepositoryReturnsNull() {
        Long taxClassId = 1L;
        Long countryId = 2L;
        Long stateId = 3L;
        String zipCode = "12345";
        when(taxRateRepository.getTaxPercent(countryId, stateId, zipCode, taxClassId)).thenReturn(null);

        double result = taxRateService.getTaxPercent(taxClassId, countryId, stateId, zipCode);

        assertThat(result).isZero();
    }

    @Test
    void getBulkTaxRate_shouldReturnMappedVmsFromRepositoryResults() {
        Long countryId = 2L;
        Long stateId = 3L;
        String zipCode = "12345";
        List<Long> taxClassIds = List.of(1L, 2L);

        TaxRate first = this.taxRate;
        TaxRate second = Instancio.of(TaxRate.class)
            .set(field("taxClass"), taxClass)
            .create();

        when(taxRateRepository.getBatchTaxRates(countryId, stateId, zipCode, org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(first, second));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(taxClassIds, countryId, stateId, zipCode);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(
            TaxRateVm.fromModel(first),
            TaxRateVm.fromModel(second)
        );
    }
}