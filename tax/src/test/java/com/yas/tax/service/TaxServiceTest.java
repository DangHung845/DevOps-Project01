package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.constants.MessageCode;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
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
import org.springframework.data.domain.Sort;

@SpringBootTest(classes = {TaxRateService.class, TaxClassService.class})
public class TaxServiceTest {
    
    @MockBean
    TaxRateRepository taxRateRepository;
    @MockBean
    TaxClassRepository taxClassRepository;
    @MockBean
    LocationService locationService;

    @Autowired
    TaxRateService taxRateService;
    
    @Autowired
    TaxClassService taxClassService;

    TaxRate taxRate;
    TaxClass taxClass;
    
    @BeforeEach
    void setUp() {
        taxClass = Instancio.create(TaxClass.class);
        taxClass.setId(1L);
        taxClass.setName("Standard Tax");
        
        taxRate = Instancio.of(TaxRate.class)
            .set(field("taxClass"), taxClass)
            .set(field("id"), 1L)
            .set(field("rate"), 10.0)
            .create();
        
        lenient().when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));
        lenient().when(taxClassRepository.findAll(any(Sort.class))).thenReturn(List.of(taxClass));
    }

    // ============ TaxRateService Tests ============
    
    @Test
    void testFindAll_shouldReturnAllTaxRates() {
        List<TaxRateVm> result = taxRateService.findAll();
        assertThat(result).hasSize(1).contains(TaxRateVm.fromModel(taxRate));
    }

    @Test
    void testFindById_shouldReturnTaxRateWhenExists() {
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        
        TaxRateVm result = taxRateService.findById(1L);
        
        assertThat(result).isEqualTo(TaxRateVm.fromModel(taxRate));
        verify(taxRateRepository, times(1)).findById(1L);
    }

    @Test
    void testFindById_shoul dThrowNotFoundExceptionWhenDoesNotExist() {
        when(taxRateRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> taxRateService.findById(999L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCreateTaxRate_shouldCreateSuccessfully() {
        TaxRatePostVm postVm = Instancio.of(TaxRatePostVm.class)
            .set(field("taxClassId"), 1L)
            .set(field("rate"), 15.0)
            .create();
        
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);
        
        TaxRate result = taxRateService.createTaxRate(postVm);
        
        assertThat(result).isNotNull();
        verify(taxClassRepository, times(1)).existsById(1L);
        verify(taxRateRepository, times(1)).save(any(TaxRate.class));
    }

    @Test
    void testCreateTaxRate_shouldThrowNotFoundWhenTaxClassDoesNotExist() {
        TaxRatePostVm postVm = Instancio.of(TaxRatePostVm.class)
            .set(field("taxClassId"), 999L)
            .create();
        
        when(taxClassRepository.existsById(999L)).thenReturn(false);
        
        assertThatThrownBy(() -> taxRateService.createTaxRate(postVm))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void testUpdateTaxRate_shouldUpdateSuccessfully() {
        TaxRatePostVm postVm = Instancio.of(TaxRatePostVm.class)
            .set(field("taxClassId"), 1L)
            .set(field("rate"), 20.0)
            .create();
        
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenReturn(taxRate);
        
        taxRateService.updateTaxRate(postVm, 1L);
        
        verify(taxRateRepository, times(1)).findById(1L);
        verify(taxRateRepository, times(1)).save(any(TaxRate.class));
    }

    @Test
    void testUpdateTaxRate_shouldThrowNotFoundWhenTaxRateDoesNotExist() {
        TaxRatePostVm postVm = Instancio.create(TaxRatePostVm.class);
        
        when(taxRateRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_RATE_NOT_FOUND);
    }

    @Test
    void testUpdateTaxRate_shouldThrowNotFoundWhenTaxClassDoesNotExist() {
        TaxRatePostVm postVm = Instancio.of(TaxRatePostVm.class)
            .set(field("taxClassId"), 999L)
            .create();
        
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(999L)).thenReturn(false);
        
        assertThatThrownBy(() -> taxRateService.updateTaxRate(postVm, 1L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void testDeleteTaxRate_shouldDeleteSuccessfully() {
        when(taxRateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taxRateRepository).deleteById(1L);
        
        taxRateService.delete(1L);
        
        verify(taxRateRepository, times(1)).existsById(1L);
        verify(taxRateRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteTaxRate_shouldThrowNotFoundWhenDoesNotExist() {
        when(taxRateRepository.existsById(999L)).thenReturn(false);
        
        assertThatThrownBy(() -> taxRateService.delete(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_RATE_NOT_FOUND);
    }

    @Test
    void testGetPageableTaxRates_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxRate> page = new PageImpl<>(List.of(taxRate), pageable, 1);
        
        StateOrProvinceAndCountryGetNameVm locationVm = new StateOrProvinceAndCountryGetNameVm(
            taxRate.getStateOrProvinceId(), "State", "Country"
        );
        
        when(taxRateRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(locationService.getStateOrProvinceAndCountryNames(any(List.class)))
            .thenReturn(List.of(locationVm));
        
        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);
        
        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        verify(taxRateRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetPageableTaxRates_shouldHandleEmptyResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxRate> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(taxRateRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
        when(locationService.getStateOrProvinceAndCountryNames(any(List.class)))
            .thenReturn(List.of());
        
        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 10);
        
        assertThat(result).isNotNull();
        assertThat(result.getCountItem()).isEqualTo(0);
    }

    @Test
    void testGetTaxPercent_shouldReturnValidTaxPercent() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "12345", 1L))
            .thenReturn(10.5);
        
        double result = taxRateService.getTaxPercent(1L, 1L, 2L, "12345");
        
        assertThat(result).isEqualTo(10.5);
    }

    @Test
    void testGetTaxPercent_shouldReturnZeroWhenNotFound() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "12345", 1L))
            .thenReturn(null);
        
        double result = taxRateService.getTaxPercent(1L, 1L, 2L, "12345");
        
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void testGetBulkTaxRate_shouldReturnMultipleTaxRates() {
        TaxRate taxRate2 = Instancio.of(TaxRate.class)
            .set(field("taxClass"), taxClass)
            .set(field("id"), 2L)
            .create();
        
        when(taxRateRepository.getBatchTaxRates(1L, 2L, "12345", any()))
            .thenReturn(List.of(taxRate, taxRate2));
        
        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 1L, 2L, "12345");
        
        assertThat(result).hasSize(2);
    }

    @Test
    void testGetBulkTaxRate_shouldReturnEmptyWhenNotFound() {
        when(taxRateRepository.getBatchTaxRates(1L, 2L, "12345", any()))
            .thenReturn(List.of());
        
        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L), 1L, 2L, "12345");
        
        assertThat(result).isEmpty();
    }

    // ============ TaxClassService Tests ============

    @Test
    void testFindAllTaxClasses_shouldReturnAllClasses() {
        List<TaxClassVm> result = taxClassService.findAllTaxClasses();
        
        assertThat(result).hasSize(1).contains(TaxClassVm.fromModel(taxClass));
        verify(taxClassRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    void testFindTaxClassById_shouldReturnClassWhenExists() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        
        TaxClassVm result = taxClassService.findById(1L);
        
        assertThat(result).isEqualTo(TaxClassVm.fromModel(taxClass));
        verify(taxClassRepository, times(1)).findById(1L);
    }

    @Test
    void testFindTaxClassById_shouldThrowNotFoundWhenDoesNotExist() {
        when(taxClassRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> taxClassService.findById(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void testCreateTaxClass_shouldCreateSuccessfully() {
        TaxClassPostVm postVm = Instancio.of(TaxClassPostVm.class)
            .set(field("name"), "New Tax Class")
            .create();
        
        when(taxClassRepository.existsByName("New Tax Class")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);
        
        TaxClass result = taxClassService.create(postVm);
        
        assertThat(result).isNotNull();
        verify(taxClassRepository, times(1)).existsByName("New Tax Class");
        verify(taxClassRepository, times(1)).save(any(TaxClass.class));
    }

    @Test
    void testCreateTaxClass_shouldThrowDuplicatedExceptionWhenNameExists() {
        TaxClassPostVm postVm = Instancio.of(TaxClassPostVm.class)
            .set(field("name"), "Existing Tax Class")
            .create();
        
        when(taxClassRepository.existsByName("Existing Tax Class")).thenReturn(true);
        
        assertThatThrownBy(() -> taxClassService.create(postVm))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining(MessageCode.NAME_ALREADY_EXITED);
    }

    @Test
    void testUpdateTaxClass_shouldUpdateSuccessfully() {
        TaxClassPostVm postVm = Instancio.of(TaxClassPostVm.class)
            .set(field("name"), "Updated Name")
            .create();
        
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("Updated Name", 1L)).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(taxClass);
        
        taxClassService.update(postVm, 1L);
        
        verify(taxClassRepository, times(1)).findById(1L);
        verify(taxClassRepository, times(1)).save(any(TaxClass.class));
    }

    @Test
    void testUpdateTaxClass_shouldThrowNotFoundWhenDoesNotExist() {
        TaxClassPostVm postVm = Instancio.create(TaxClassPostVm.class);
        
        when(taxClassRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> taxClassService.update(postVm, 999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void testUpdateTaxClass_shouldThrowDuplicatedExceptionWhenNameExists() {
        TaxClassPostVm postVm = Instancio.of(TaxClassPostVm.class)
            .set(field("name"), "Duplicate Name")
            .create();
        
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("Duplicate Name", 1L)).thenReturn(true);
        
        assertThatThrownBy(() -> taxClassService.update(postVm, 1L))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining(MessageCode.NAME_ALREADY_EXITED);
    }

    @Test
    void testDeleteTaxClass_shouldDeleteSuccessfully() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        doNothing().when(taxClassRepository).deleteById(1L);
        
        taxClassService.delete(1L);
        
        verify(taxClassRepository, times(1)).existsById(1L);
        verify(taxClassRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteTaxClass_shouldThrowNotFoundWhenDoesNotExist() {
        when(taxClassRepository.existsById(999L)).thenReturn(false);
        
        assertThatThrownBy(() -> taxClassService.delete(999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(MessageCode.TAX_CLASS_NOT_FOUND);
    }

    @Test
    void testGetPageableTaxClasses_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxClass> page = new PageImpl<>(List.of(taxClass), pageable, 1);
        
        when(taxClassRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);
        
        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getCountItem()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        verify(taxClassRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetPageableTaxClasses_shouldHandleEmptyResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxClass> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(taxClassRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
        
        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);
        
        assertThat(result).isNotNull();
        assertThat(result.getCountItem()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    void testGetPageableTaxClasses_shouldHandleMultiplePages() {
        TaxClass taxClass2 = Instancio.of(TaxClass.class)
            .set(field("id"), 2L)
            .set(field("name"), "Another Class")
            .create();
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaxClass> page = new PageImpl<>(List.of(taxClass, taxClass2), pageable, 20);
        
        when(taxClassRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);
        
        assertThat(result).isNotNull();
        assertThat(result.getCountItem()).isEqualTo(20);
        assertThat(result.getTaxClasses()).hasSize(2);
    }
}
