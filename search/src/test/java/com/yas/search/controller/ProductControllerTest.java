package com.yas.search.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.search.ElasticsearchApplication;
import com.yas.search.model.ProductCriteriaDto;
import com.yas.search.service.ProductService;
import com.yas.search.viewmodel.ProductGetVm;
import com.yas.search.viewmodel.ProductListGetVm;
import com.yas.search.viewmodel.ProductNameGetVm;
import com.yas.search.viewmodel.ProductNameListVm;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ProductController.class)
@ContextConfiguration(classes = ElasticsearchApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @MockBean
    private ProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testFindProductAdvance_whenProductListIsExists_thenReturnProductListGetVm() throws Exception {

        ProductGetVm productGetVm = new ProductGetVm(
            1L,
            "Sample Product",
            "sample-product",
            123L,
            29.99,
            true,
            true,
            false,
            true,
            ZonedDateTime.now()
        );

        ProductListGetVm mockResponse = new ProductListGetVm(
            List.of(productGetVm), 0, 1, 1, 1, true, Map.of()
        );



        when(productService.findProductAdvance(any(ProductCriteriaDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/catalog-search")
                .param("keyword", "test")
                .param("page", "0")
                .param("size", "12")
                .param("sortType", "DEFAULT")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.products[0].id").value(productGetVm.id()))
            .andExpect(jsonPath("$.products[0].name").value(productGetVm.name()))
            .andExpect(jsonPath("$.products[0].slug").value(productGetVm.slug()));
    }

    @Test
    void testProductSearchAutoComplete_whenProductNameList_thenReturnProductNameListVm() throws Exception {

        ProductNameListVm mockResponse = new ProductNameListVm(
            List.of(new ProductNameGetVm("Product1"))
        );
        when(productService.autoCompleteProductName(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/search_suggest")
                .param("keyword", "test")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.productNames[0].name").value("Product1"));
    }

    @Test
    void testFindProductAdvance_whenWithBrandFilter_thenReturnProductListGetVm() throws Exception {

        ProductGetVm productGetVm = new ProductGetVm(
            1L,
            "Samsung Phone",
            "samsung-phone",
            123L,
            599.99,
            true,
            true,
            true,
            false,
            ZonedDateTime.now()
        );

        ProductListGetVm mockResponse = new ProductListGetVm(
            List.of(productGetVm), 0, 12, 1, 1, true, Map.of()
        );

        when(productService.findProductAdvance(any(ProductCriteriaDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/catalog-search")
                .param("keyword", "phone")
                .param("page", "0")
                .param("size", "12")
                .param("brand", "Samsung")
                .param("sortType", "DEFAULT")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.products[0].name").value("Samsung Phone"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testFindProductAdvance_whenWithPriceFilter_thenReturnProductListGetVm() throws Exception {

        ProductGetVm productGetVm = new ProductGetVm(
            2L,
            "Budget Phone",
            "budget-phone",
            456L,
            199.99,
            true,
            true,
            true,
            true,
            ZonedDateTime.now()
        );

        ProductListGetVm mockResponse = new ProductListGetVm(
            List.of(productGetVm), 0, 12, 1, 1, true, Map.of()
        );

        when(productService.findProductAdvance(any(ProductCriteriaDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/catalog-search")
                .param("keyword", "phone")
                .param("page", "0")
                .param("size", "12")
                .param("minPrice", "100")
                .param("maxPrice", "300")
                .param("sortType", "PRICE_ASC")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products[0].id").value(2L))
            .andExpect(jsonPath("$.pageNo").value(0));
    }

    @Test
    void testFindProductAdvance_whenSortByPriceDesc_thenReturnProductListGetVm() throws Exception {

        ProductGetVm productGetVm = new ProductGetVm(
            3L,
            "Premium Phone",
            "premium-phone",
            789L,
            999.99,
            true,
            true,
            true,
            false,
            ZonedDateTime.now()
        );

        ProductListGetVm mockResponse = new ProductListGetVm(
            List.of(productGetVm), 0, 12, 1, 1, true, Map.of()
        );

        when(productService.findProductAdvance(any(ProductCriteriaDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/catalog-search")
                .param("keyword", "premium")
                .param("page", "0")
                .param("size", "12")
                .param("sortType", "PRICE_DESC")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products[0].name").value("Premium Phone"));
    }

    @Test
    void testFindProductAdvance_whenNoProductsFound_thenReturnEmptyList() throws Exception {

        ProductListGetVm mockResponse = new ProductListGetVm(
            List.of(), 0, 12, 0, 0, true, Map.of()
        );

        when(productService.findProductAdvance(any(ProductCriteriaDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/catalog-search")
                .param("keyword", "nonexistent")
                .param("page", "0")
                .param("size", "12")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products").isArray())
            .andExpect(jsonPath("$.products.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testFindProductAdvance_whenPageTwo_thenReturnCorrectPage() throws Exception {

        ProductGetVm productGetVm = new ProductGetVm(
            4L,
            "Product Page 2",
            "product-page-2",
            321L,
            299.99,
            true,
            true,
            true,
            false,
            ZonedDateTime.now()
        );

        ProductListGetVm mockResponse = new ProductListGetVm(
            List.of(productGetVm), 1, 12, 25, 3, false, Map.of()
        );

        when(productService.findProductAdvance(any(ProductCriteriaDto.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/catalog-search")
                .param("keyword", "product")
                .param("page", "1")
                .param("size", "12")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNo").value(1))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.isLast").value(false));
    }

    @Test
    void testProductSearchAutoComplete_whenMultipleSuggestions_thenReturnProductNameList() throws Exception {

        ProductNameListVm mockResponse = new ProductNameListVm(
            List.of(
                new ProductNameGetVm("Product Alpha"),
                new ProductNameGetVm("Product Beta"),
                new ProductNameGetVm("Product Gamma")
            )
        );

        when(productService.autoCompleteProductName(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/search_suggest")
                .param("keyword", "product")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productNames.length()").value(3))
            .andExpect(jsonPath("$.productNames[0].name").value("Product Alpha"))
            .andExpect(jsonPath("$.productNames[1].name").value("Product Beta"))
            .andExpect(jsonPath("$.productNames[2].name").value("Product Gamma"));
    }

    @Test
    void testProductSearchAutoComplete_whenEmptySuggestions_thenReturnEmptyList() throws Exception {

        ProductNameListVm mockResponse = new ProductNameListVm(List.of());

        when(productService.autoCompleteProductName(anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/storefront/search_suggest")
                .param("keyword", "xyz")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productNames").isArray())
            .andExpect(jsonPath("$.productNames.length()").value(0));
    }

}