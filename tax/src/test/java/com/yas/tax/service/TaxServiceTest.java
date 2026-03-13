package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.mockito.Mockito.lenient;

import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
    @BeforeEach
    void setUp() {
        TaxClass taxClass = Instancio.create(TaxClass.class);
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
    void testFindById_shouldReturnTaxRateVm() {
        lenient().when(taxRateRepository.findById(1L)).thenReturn(java.util.Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(1L);

        assertThat(result).isEqualTo(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void testFindById_whenNotFound_shouldThrowNotFoundException() {
        lenient().when(taxRateRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxRateService.findById(1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testDelete_whenExisting_shouldDelete() {
        lenient().when(taxRateRepository.existsById(1L)).thenReturn(true);

        taxRateService.delete(1L);

        org.mockito.Mockito.verify(taxRateRepository).deleteById(1L);
    }

    @Test
    void testDelete_whenNotExisting_shouldThrowNotFoundException() {
        lenient().when(taxRateRepository.existsById(1L)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxRateService.delete(1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testGetTaxPercent_whenFound_shouldReturnValue() {
        lenient().when(taxRateRepository.getTaxPercent(3L, 2L, "12345", 1L)).thenReturn(10.0);

        double result = taxRateService.getTaxPercent(1L, 3L, 2L, "12345");

        assertThat(result).isEqualTo(10.0);
    }

    @Test
    void testGetTaxPercent_whenNotFound_shouldReturnZero() {
        lenient().when(taxRateRepository.getTaxPercent(3L, 2L, "12345", 1L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(1L, 3L, 2L, "12345");

        assertThat(result).isZero();
    }

    @Test
    void testGetBulkTaxRate_shouldReturnMappedTaxRates() {
        taxRate.setStateOrProvinceId(2L);
        taxRate.setCountryId(3L);

        lenient().when(taxRateRepository.getBatchTaxRates(3L, 2L, "12345",
                new java.util.HashSet<>(java.util.List.of(1L))))
            .thenReturn(java.util.List.of(taxRate));

        java.util.List<TaxRateVm> result = taxRateService.getBulkTaxRate(
            java.util.List.of(1L), 3L, 2L, "12345");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void testCreateTaxRate_whenTaxClassExists_shouldSaveAndReturnTaxRate() {
        com.yas.tax.viewmodel.taxrate.TaxRatePostVm postVm =
            new com.yas.tax.viewmodel.taxrate.TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);
        TaxClass taxClass = Instancio.create(TaxClass.class);

        lenient().when(taxClassRepository.existsById(1L)).thenReturn(true);
        lenient().when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        lenient().when(taxRateRepository.save(org.mockito.ArgumentMatchers.any(TaxRate.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TaxRate result = taxRateService.createTaxRate(postVm);

        assertThat(result.getRate()).isEqualTo(10.0);
        assertThat(result.getZipCode()).isEqualTo("12345");
        assertThat(result.getTaxClass()).isEqualTo(taxClass);
        assertThat(result.getStateOrProvinceId()).isEqualTo(2L);
        assertThat(result.getCountryId()).isEqualTo(3L);
    }

    @Test
    void testCreateTaxRate_whenTaxClassNotExists_shouldThrowNotFoundException() {
        com.yas.tax.viewmodel.taxrate.TaxRatePostVm postVm =
            new com.yas.tax.viewmodel.taxrate.TaxRatePostVm(10.0, "12345", 1L, 2L, 3L);

        lenient().when(taxClassRepository.existsById(1L)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxRateService.createTaxRate(postVm))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testUpdateTaxRate_whenExistingAndTaxClassExists_shouldUpdateAndSave() {
        TaxClass taxClass = Instancio.create(TaxClass.class);
        taxRate.setTaxClass(taxClass);
        taxRate.setId(1L);

        com.yas.tax.viewmodel.taxrate.TaxRatePostVm postVm =
            new com.yas.tax.viewmodel.taxrate.TaxRatePostVm(15.0, "54321", 2L, 4L, 5L);
        TaxClass newTaxClass = Instancio.create(TaxClass.class);

        lenient().when(taxRateRepository.findById(1L)).thenReturn(java.util.Optional.of(taxRate));
        lenient().when(taxClassRepository.existsById(2L)).thenReturn(true);
        lenient().when(taxClassRepository.getReferenceById(2L)).thenReturn(newTaxClass);

        taxRateService.updateTaxRate(postVm, 1L);

        org.mockito.Mockito.verify(taxRateRepository).save(taxRate);
        assertThat(taxRate.getRate()).isEqualTo(15.0);
        assertThat(taxRate.getZipCode()).isEqualTo("54321");
        assertThat(taxRate.getTaxClass()).isEqualTo(newTaxClass);
        assertThat(taxRate.getStateOrProvinceId()).isEqualTo(4L);
        assertThat(taxRate.getCountryId()).isEqualTo(5L);
    }

    @Test
    void testUpdateTaxRate_whenNotExisting_shouldThrowNotFoundException() {
        com.yas.tax.viewmodel.taxrate.TaxRatePostVm postVm =
            new com.yas.tax.viewmodel.taxrate.TaxRatePostVm(15.0, "54321", 2L, 4L, 5L);

        lenient().when(taxRateRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testUpdateTaxRate_whenTaxClassNotExists_shouldThrowNotFoundException() {
        TaxClass taxClass = Instancio.create(TaxClass.class);
        taxRate.setTaxClass(taxClass);
        taxRate.setId(1L);

        com.yas.tax.viewmodel.taxrate.TaxRatePostVm postVm =
            new com.yas.tax.viewmodel.taxrate.TaxRatePostVm(15.0, "54321", 2L, 4L, 5L);

        lenient().when(taxRateRepository.findById(1L)).thenReturn(java.util.Optional.of(taxRate));
        lenient().when(taxClassRepository.existsById(2L)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testGetPageableTaxRates_shouldReturnPageWithDetails() {
        taxRate.setId(1L);
        taxRate.setStateOrProvinceId(2L);
        taxRate.setCountryId(3L);

        org.springframework.data.domain.Page<TaxRate> page =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of(taxRate));
        lenient().when(taxRateRepository.findAll(
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm locationVm =
            new com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm(2L, "StateName", "CountryName");
        lenient().when(locationService.getStateOrProvinceAndCountryNames(java.util.List.of(2L)))
            .thenReturn(java.util.List.of(locationVm));

        com.yas.tax.viewmodel.taxrate.TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.pageNo()).isEqualTo(0);
        assertThat(result.taxRateGetDetailContent()).hasSize(1);
        com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm detailVm = result.taxRateGetDetailContent().get(0);
        assertThat(detailVm.id()).isEqualTo(1L);
        assertThat(detailVm.rate()).isEqualTo(taxRate.getRate());
        assertThat(detailVm.zipCode()).isEqualTo(taxRate.getZipCode());
        assertThat(detailVm.taxClassName()).isEqualTo(taxRate.getTaxClass().getName());
        assertThat(detailVm.stateOrProvinceName()).isEqualTo("StateName");
        assertThat(detailVm.countryName()).isEqualTo("CountryName");
    }

    @Test
    void testGetPageableTaxRates_whenNoTaxRates_shouldReturnEmptyContent() {
        org.springframework.data.domain.Page<TaxRate> emptyPage =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        lenient().when(taxRateRepository.findAll(
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(emptyPage);

        com.yas.tax.viewmodel.taxrate.TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);

        assertThat(result.taxRateGetDetailContent()).isEmpty();
    }

    @Test
    void testFindAllTaxClasses_shouldReturnSortedVmList() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        TaxClass taxClassA = new TaxClass();
        taxClassA.setId(1L);
        taxClassA.setName("A");

        TaxClass taxClassB = new TaxClass();
        taxClassB.setId(2L);
        taxClassB.setName("B");

        lenient().when(taxClassRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name")))
            .thenReturn(java.util.List.of(taxClassA, taxClassB));

        java.util.List<com.yas.tax.viewmodel.taxclass.TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("A");
        assertThat(result.get(1).name()).isEqualTo("B");
    }

    @Test
    void testFindTaxClassById_shouldReturnTaxClassVm() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        TaxClass taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");

        lenient().when(taxClassRepository.findById(1L)).thenReturn(java.util.Optional.of(taxClass));

        com.yas.tax.viewmodel.taxclass.TaxClassVm result = taxClassService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Standard");
    }

    @Test
    void testFindTaxClassById_whenNotFound_shouldThrowNotFoundException() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        lenient().when(taxClassRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxClassService.findById(1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testCreateTaxClass_whenNameNotExists_shouldSaveAndReturnTaxClass() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        com.yas.tax.viewmodel.taxclass.TaxClassPostVm postVm =
            new com.yas.tax.viewmodel.taxclass.TaxClassPostVm("id-1", "Standard");

        TaxClass saved = new TaxClass();
        saved.setId(1L);
        saved.setName("Standard");

        lenient().when(taxClassRepository.existsByName("Standard")).thenReturn(false);
        lenient().when(taxClassRepository.save(org.mockito.ArgumentMatchers.any(TaxClass.class))).thenReturn(saved);

        TaxClass result = taxClassService.create(postVm);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Standard");
    }

    @Test
    void testCreateTaxClass_whenNameExists_shouldThrowDuplicatedException() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        com.yas.tax.viewmodel.taxclass.TaxClassPostVm postVm =
            new com.yas.tax.viewmodel.taxclass.TaxClassPostVm("id-1", "Standard");

        lenient().when(taxClassRepository.existsByName("Standard")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxClassService.create(postVm))
            .isInstanceOf(com.yas.commonlibrary.exception.DuplicatedException.class);
    }

    @Test
    void testUpdateTaxClass_whenExistingAndNameNotDuplicated_shouldUpdateAndSave() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        TaxClass existing = new TaxClass();
        existing.setId(1L);
        existing.setName("Old");

        com.yas.tax.viewmodel.taxclass.TaxClassPostVm postVm =
            new com.yas.tax.viewmodel.taxclass.TaxClassPostVm("id-1", "New");

        lenient().when(taxClassRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        lenient().when(taxClassRepository.existsByNameNotUpdatingTaxClass("New", 1L)).thenReturn(false);

        taxClassService.update(postVm, 1L);

        org.mockito.Mockito.verify(taxClassRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("New");
    }

    @Test
    void testUpdateTaxClass_whenNotExisting_shouldThrowNotFoundException() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        com.yas.tax.viewmodel.taxclass.TaxClassPostVm postVm =
            new com.yas.tax.viewmodel.taxclass.TaxClassPostVm("id-1", "New");

        lenient().when(taxClassRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxClassService.update(postVm, 1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testUpdateTaxClass_whenNameDuplicated_shouldThrowDuplicatedException() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        TaxClass existing = new TaxClass();
        existing.setId(1L);
        existing.setName("Old");

        com.yas.tax.viewmodel.taxclass.TaxClassPostVm postVm =
            new com.yas.tax.viewmodel.taxclass.TaxClassPostVm("id-1", "New");

        lenient().when(taxClassRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        lenient().when(taxClassRepository.existsByNameNotUpdatingTaxClass("New", 1L)).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxClassService.update(postVm, 1L))
            .isInstanceOf(com.yas.commonlibrary.exception.DuplicatedException.class);
    }

    @Test
    void testDeleteTaxClass_whenExisting_shouldDelete() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        lenient().when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        org.mockito.Mockito.verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void testDeleteTaxClass_whenNotExisting_shouldThrowNotFoundException() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        lenient().when(taxClassRepository.existsById(1L)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taxClassService.delete(1L))
            .isInstanceOf(com.yas.commonlibrary.exception.NotFoundException.class);
    }

    @Test
    void testGetPageableTaxClasses_shouldReturnPageWithContent() {
        com.yas.tax.service.TaxClassService taxClassService = new com.yas.tax.service.TaxClassService(
            taxClassRepository);

        TaxClass taxClass = new TaxClass();
        taxClass.setId(1L);
        taxClass.setName("Standard");

        org.springframework.data.domain.Page<TaxClass> page =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of(taxClass));

        lenient().when(taxClassRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 10)))
            .thenReturn(page);

        com.yas.tax.viewmodel.taxclass.TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result.pageNo()).isEqualTo(0);
        assertThat(result.taxClassContent()).hasSize(1);
        assertThat(result.taxClassContent().get(0).name()).isEqualTo("Standard");
    }
}