package com.yas.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.search.constant.enums.SortType;
import com.yas.search.model.Product;
import com.yas.search.model.ProductCriteriaDto;
import com.yas.search.viewmodel.ProductListGetVm;
import com.yas.search.viewmodel.ProductNameGetVm;
import com.yas.search.viewmodel.ProductNameListVm;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.AggregationsContainer;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.SearchShardStatistics;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;

class ProductServiceTest {

    private ElasticsearchOperations elasticsearchOperations;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        elasticsearchOperations = mock(ElasticsearchOperations.class);
        productService = new ProductService(elasticsearchOperations);
    }

    @Test
    void testFindProductAdvance_whenSortTypeIsPriceAsc_ReturnProductListGetVm() {

        Integer page = 0;
        Integer size = 10;

        SearchHits<Product> searchHits =
            getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(page);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(size);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, "testBrand", "testCategory",
            "testAttribute", 10.0, 100.0, SortType.PRICE_ASC);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        verify(elasticsearchOperations, times(1))
            .search(captor.capture(), eq(Product.class));
        assertEquals("price: ASC", Objects.requireNonNull(captor.getValue().getSort()).toString());

        assertNotNull(result);
        assertEquals(1, result.products().size());
        assertEquals(0, result.pageNo());
        assertEquals(10, result.pageSize());
        assertEquals(1, result.totalElements());
        assertTrue(result.isLast());
    }

    @Test
    void testFindProductAdvance_whenSortTypeIsPriceDesc_ReturnProductListGetVm() {

        Integer page = 0;
        Integer size = 10;

        SearchHits<Product> searchHits =
            getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(page);
        when(productPage.getSize()).thenReturn(size);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto("test", 0, 10, "testBrand", "testCategory",
            "testAttribute", 10.0, 100.0, SortType.PRICE_DESC);
        productService.findProductAdvance(criteriaDto);

        verify(elasticsearchOperations, times(1))
            .search(captor.capture(), eq(Product.class));

        assertEquals("price: DESC", Objects.requireNonNull(captor.getValue().getSort()).toString());
    }

    @Test
    void testFindProductAdvance_whenSortTypeIsDefault_ReturnProductListGetVm() {

        SearchHits<Product> searchHits =
            getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, "testBrand", "testCategory",
            "testAttribute", 10.0, 100.0, SortType.DEFAULT);
        productService.findProductAdvance(criteriaDto);

        verify(elasticsearchOperations, times(1))
            .search(captor.capture(), eq(Product.class));

        assertEquals("createdOn: DESC", Objects.requireNonNull(captor.getValue().getSort()).toString());
    }

    @Test
    void testAutoCompleteProductName_whenExistsProducts_returnProductNameListVm() {

        SearchHits<Product> searchHits =
            getSearchHits();

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class)))
            .thenReturn(searchHits);

        ProductNameListVm result = productService.autoCompleteProductName("Product");

        assertNotNull(result);
        assertEquals(1, result.productNames().size());
        ProductNameGetVm productNameGetVm = result.productNames().getFirst();
        assertEquals("Test Product", productNameGetVm.name());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testAutoCompleteProductName_whenNoProductsFound_returnEmptyProductNameListVm() {

        SearchHits<Product> emptySearchHits = getEmptySearchHits();

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class)))
            .thenReturn(emptySearchHits);

        ProductNameListVm result = productService.autoCompleteProductName("NonexistentProduct");

        assertNotNull(result);
        assertEquals(0, result.productNames().size());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testFindProductAdvance_whenNoProductsFound_returnEmptyProductListGetVm() {

        SearchHits<Product> emptySearchHits = getEmptySearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalElements()).thenReturn(0L);
        when(productPage.getTotalPages()).thenReturn(0);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(emptySearchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "nonexistent", 0, 10, null, null, null, null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(0, result.products().size());
        assertEquals(0, result.totalElements());
    }

    @Test
    void testFindProductAdvance_whenWithNullFilters_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, null, null, null, null, null, SortType.PRICE_ASC);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        verify(elasticsearchOperations, times(1))
            .search(captor.capture(), eq(Product.class));

        assertNotNull(result);
        assertEquals(1, result.products().size());
    }

    @Test
    void testFindProductAdvance_whenWithPriceRange_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, null, null, null, 15.0, 50.0, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());

        verify(elasticsearchOperations, times(1))
            .search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testFindProductAdvance_whenEmptyKeyword_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(12);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "", 0, 12, null, null, null, null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());
        assertEquals(12, result.pageSize());
    }

    @Test
    void testFindProductAdvance_whenMultipleProducts_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getMultipleProductSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(3L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "product", 0, 10, null, null, null, null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(3, result.products().size());
        assertEquals(0, result.pageNo());
        assertEquals(3, result.totalElements());
    }

    @Test
    void testFindProductAdvance_whenSecondPage_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(1);
        when(productPage.getTotalElements()).thenReturn(25L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(3);
        when(productPage.isLast()).thenReturn(false);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 1, 10, null, null, null, null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.pageNo());
        assertEquals(25, result.totalElements());
        assertEquals(3, result.totalPages());
        assertFalse(result.isLast());
    }

    @Test
    void testFindProductAdvance_whenOnlyMinPrice_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, null, null, null, 50.0, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testFindProductAdvance_whenOnlyMaxPrice_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, null, null, null, null, 150.0, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testFindProductAdvance_whenBrandFilter_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, "Samsung,LG", null, null, null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testFindProductAdvance_whenCategoryFilter_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, null, "Electronics,Phones", null, null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    @Test
    void testFindProductAdvance_whenAttributeFilter_ReturnProductListGetVm() {

        SearchHits<Product> searchHits = getSearchHits();

        SearchPage<Product> productPage = mock(SearchPage.class);
        when(productPage.getNumber()).thenReturn(0);
        when(productPage.getTotalElements()).thenReturn(1L);
        when(productPage.getSize()).thenReturn(10);
        when(productPage.getTotalPages()).thenReturn(1);
        when(productPage.isLast()).thenReturn(true);

        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Product.class))).thenReturn(searchHits);

        ProductCriteriaDto criteriaDto = new ProductCriteriaDto(
            "test", 0, 10, null, null, "Color:Black,Size:Large", null, null, SortType.DEFAULT);
        ProductListGetVm result = productService.findProductAdvance(criteriaDto);

        assertNotNull(result);
        assertEquals(1, result.products().size());

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Product.class));
    }

    private static SearchHits<Product> getMultipleProductSearchHits() {
        List<SearchHit<Product>> hits = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            Product product = Product.builder()
                .id((long) i)
                .name("Test Product " + i)
                .slug("test-product-" + i)
                .price(20.0 * i)
                .isPublished(true)
                .isVisibleIndividually(true)
                .isAllowedToOrder(true)
                .isFeatured(i % 2 == 0)
                .thumbnailMediaId(100L + i)
                .categories(List.of("Category" + i))
                .attributes(List.of("Attribute" + i))
                .createdOn(ZonedDateTime.now())
                .build();

            SearchHit<Product> searchHit = new SearchHit<>(
                "products",
                String.valueOf(i),
                null,
                1.0f,
                null,
                new HashMap<>(),
                new HashMap<>(),
                null,
                null,
                new ArrayList<>(),
                product
            );
            hits.add(searchHit);
        }

        return new SearchHits<>() {
            @Override
            public @NotNull SearchHit<Product> getSearchHit(int index) {
                return hits.get(index);
            }

            @Override
            public AggregationsContainer<?> getAggregations() {
                return null;
            }

            @Override
            public float getMaxScore() {
                return 1;
            }

            @Override
            public @NotNull List<SearchHit<Product>> getSearchHits() {
                return hits;
            }

            @Override
            public long getTotalHits() {
                return 3;
            }

            @Override
            public @NotNull TotalHitsRelation getTotalHitsRelation() {
                return TotalHitsRelation.EQUAL_TO;
            }

            @Override
            public Suggest getSuggest() {
                return null;
            }

            @Override
            public String getPointInTimeId() {
                return "";
            }

            @Override
            public SearchShardStatistics getSearchShardStatistics() {
                return null;
            }
        };
    }

    private static SearchHits<Product> getSearchHits() {

        Product product = Product.builder()
            .id(1L)
            .name("Test Product")
            .slug("test-product")
            .price(20.0)
            .isPublished(true)
            .isVisibleIndividually(true)
            .isAllowedToOrder(true)
            .isFeatured(true)
            .thumbnailMediaId(123L)
            .categories(List.of("testCategory"))
            .attributes(List.of("testAttribute"))
            .createdOn(ZonedDateTime.now())
            .build();

        SearchHit<Product> searchHit = new SearchHit<>(
            "products",
            "1",
            null,
            1.0f,
            null,
            new HashMap<>(),
            new HashMap<>(),
            null,
            null,
            new ArrayList<>(),
            product
        );

        return new SearchHits<>(
        ) {

            @Override
            public @NotNull SearchHit<Product> getSearchHit(int index) {
                return searchHit;
            }

            @Override
            public AggregationsContainer<?> getAggregations() {
                return null;
            }

            @Override
            public float getMaxScore() {
                return 1;
            }

            @Override
            public @NotNull List<SearchHit<Product>> getSearchHits() {
                return List.of(searchHit);
            }

            @Override
            public long getTotalHits() {
                return 1;
            }

            @Override
            public @NotNull TotalHitsRelation getTotalHitsRelation() {
                return TotalHitsRelation.EQUAL_TO;
            }

            @Override
            public Suggest getSuggest() {
                return null;
            }

            @Override
            public String getPointInTimeId() {
                return "";
            }

            @Override
            public SearchShardStatistics getSearchShardStatistics() {
                return null;
            }
        };
    }

    private static SearchHits<Product> getEmptySearchHits() {
        return new SearchHits<>() {
            @Override
            public @NotNull SearchHit<Product> getSearchHit(int index) {
                return null;
            }

            @Override
            public AggregationsContainer<?> getAggregations() {
                return null;
            }

            @Override
            public float getMaxScore() {
                return 0;
            }

            @Override
            public @NotNull List<SearchHit<Product>> getSearchHits() {
                return List.of();
            }

            @Override
            public long getTotalHits() {
                return 0;
            }

            @Override
            public @NotNull TotalHitsRelation getTotalHitsRelation() {
                return TotalHitsRelation.EQUAL_TO;
            }

            @Override
            public Suggest getSuggest() {
                return null;
            }

            @Override
            public String getPointInTimeId() {
                return "";
            }

            @Override
            public SearchShardStatistics getSearchShardStatistics() {
                return null;
            }
        };
    }

}
