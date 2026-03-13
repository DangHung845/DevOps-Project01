package com.yas.product.service;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.repository.BrandRepository;
import com.yas.product.viewmodel.brand.BrandListGetVm;
import com.yas.product.viewmodel.brand.BrandPostVm;
import com.yas.product.viewmodel.brand.BrandVm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    public void test_retrieve_paginated_brands_successfully() {

        List<Brand> brands = List.of(new Brand(), new Brand());
        Page<Brand> brandPage = new PageImpl<>(brands);

        when(brandRepository.findAll(any(Pageable.class)))
                .thenReturn(brandPage);

        BrandListGetVm result = brandService.getBrands(0, 2);

        assertEquals(2, result.brandContent().size());
        assertEquals(0, result.pageNo());
        assertEquals(2, result.pageSize());
    }

    @Test
    public void test_create_brand_successfully() {

        BrandPostVm vm = new BrandPostVm("BrandName", "brand-slug", true);
        Brand brand = vm.toModel();

        when(brandRepository.save(any(Brand.class)))
                .thenReturn(brand);

        Brand result = brandService.create(vm);

        assertEquals("BrandName", result.getName());
        assertEquals("brand-slug", result.getSlug());
    }

    @Test
    public void test_create_brand_with_existing_name() {

        BrandPostVm vm = new BrandPostVm("ExistingName", "slug", true);

        when(brandRepository.findExistedName("ExistingName", null))
                .thenReturn(new Brand());

        assertThrows(DuplicatedException.class,
                () -> brandService.create(vm));
    }

    @Test
    public void test_update_brand_successfully() {

        BrandPostVm vm = new BrandPostVm("UpdatedName", "updated-slug", true);

        Brand brand = new Brand();
        brand.setId(1L);

        when(brandRepository.findExistedName("UpdatedName", 1L))
                .thenReturn(null);

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(brand));

        when(brandRepository.save(any(Brand.class)))
                .thenReturn(brand);

        Brand result = brandService.update(vm, 1L);

        assertEquals("UpdatedName", result.getName());
        assertEquals("updated-slug", result.getSlug());
    }

    @Test
    public void test_update_brand_with_existing_name() {

        BrandPostVm vm = new BrandPostVm("ExistingName", "slug", true);

        when(brandRepository.findExistedName("ExistingName", 1L))
                .thenReturn(new Brand());

        assertThrows(DuplicatedException.class,
                () -> brandService.update(vm, 1L));
    }

    @Test
    public void test_update_nonexistent_brand() {

        BrandPostVm vm = new BrandPostVm("Test", "slug", true);

        when(brandRepository.findExistedName("Test", 1L))
                .thenReturn(null);

        when(brandRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> brandService.update(vm, 1L));
    }

    @Test
    public void test_get_brands_by_ids() {

        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Nike");

        when(brandRepository.findAllById(any()))
                .thenReturn(List.of(brand));

        List<BrandVm> result =
                brandService.getBrandsByIds(List.of(1L));

        assertEquals(1, result.size());
    }

    @Test
    public void test_delete_brand_success() {

        Brand brand = new Brand();
        brand.setId(1L);
        brand.setProducts(new java.util.ArrayList<>());

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(brand));

        brandService.delete(1L);
    }

    @Test
    public void test_delete_brand_not_found() {

        when(brandRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> brandService.delete(1L));
    }

    @Test
    public void test_delete_brand_with_products() {

        Brand brand = new Brand();
        brand.setProducts(new java.util.ArrayList<>());
        brand.getProducts().add(new com.yas.product.model.Product());

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(brand));

        assertThrows(BadRequestException.class,
                () -> brandService.delete(1L));
    }
}