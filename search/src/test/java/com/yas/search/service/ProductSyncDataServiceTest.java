package com.yas.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.search.config.ServiceUrlConfig;
import com.yas.search.model.Product;
import com.yas.search.repository.ProductRepository;
import com.yas.search.viewmodel.ProductEsDetailVm;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class ProductSyncDataServiceTest {
    private static final String PRODUCT_URL = "http://api.yas.local/product";

    private ProductRepository productRepository;

    private RestClient restClient;

    private ServiceUrlConfig serviceUrlConfig;

    RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    RestClient.ResponseSpec responseSpec;

    private ProductSyncDataService productSyncDataService;

    private static final Long ID = 1L;

    @BeforeEach
    void setUp() {

        productRepository = mock(ProductRepository.class);
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        productSyncDataService = new ProductSyncDataService(restClient, serviceUrlConfig, productRepository);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

    }

    private void mockProductThumbnailVmsByUri() {

        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(ID).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(getProductThumbnailVms());
    }

    private ProductEsDetailVm getProductThumbnailVms() {
        return new ProductEsDetailVm(
            ID,
            "Smartphone XYZ",
            "smartphone-xyz",
            299.99,
            true,
            true,
            true,
            false,
            456L,
            "BrandName",
            List.of("Electronics", "Mobile Phones"),
            List.of("Color: Black", "Storage: 128GB", "RAM: 6GB")
        );
    }

    @Test
    void testGetProductEsDetailById_whenNormalCase_returnProductEsDetailVm() {

        mockProductThumbnailVmsByUri();
        ProductEsDetailVm productEsDetailVm = productSyncDataService.getProductEsDetailById(ID);
        assertThat(productEsDetailVm.id()).isEqualTo(1);
        assertThat(productEsDetailVm.name()).isEqualTo("Smartphone XYZ");
        assertThat(productEsDetailVm.slug()).isEqualTo("smartphone-xyz");
        assertThat(productEsDetailVm.price()).isEqualTo(299.99);
        assertThat(productEsDetailVm.isPublished()).isTrue();
        assertThat(productEsDetailVm.isVisibleIndividually()).isTrue();
        assertThat(productEsDetailVm.isAllowedToOrder()).isTrue();
        assertThat(productEsDetailVm.isFeatured()).isFalse();
        assertThat(productEsDetailVm.thumbnailMediaId()).isEqualTo(456L);
        assertThat(productEsDetailVm.brand()).isEqualTo("BrandName");
        assertThat(productEsDetailVm.categories().getFirst()).isEqualTo("Electronics");
        assertThat(productEsDetailVm.categories().getLast()).isEqualTo("Mobile Phones");
        assertThat(productEsDetailVm.attributes().getFirst()).isEqualTo("Color: Black");
        assertThat(productEsDetailVm.attributes().get(1)).isEqualTo("Storage: 128GB");
        assertThat(productEsDetailVm.attributes().getLast()).isEqualTo("RAM: 6GB");
    }

    @Test
    void updateProduct_whenProductExists_updatesProductAndSaves() {

        mockProductThumbnailVmsByUri();
        Product existingProduct = new Product();
        existingProduct.setId(ID);

        ProductEsDetailVm productEsDetailVm = getProductThumbnailVms();

        when(productRepository.findById(ID)).thenReturn(Optional.of(existingProduct));

        productSyncDataService.updateProduct(ID);

        assertThat(existingProduct.getName()).isEqualTo(productEsDetailVm.name());
        assertThat(existingProduct.getSlug()).isEqualTo(productEsDetailVm.slug());
        assertThat(existingProduct.getPrice()).isEqualTo(productEsDetailVm.price());
        assertThat(existingProduct.getIsPublished()).isEqualTo(productEsDetailVm.isPublished());
        assertThat(existingProduct.getIsVisibleIndividually()).isEqualTo(productEsDetailVm.isVisibleIndividually());
        assertThat(existingProduct.getIsAllowedToOrder()).isEqualTo(productEsDetailVm.isAllowedToOrder());
        assertThat(existingProduct.getIsFeatured()).isEqualTo(productEsDetailVm.isFeatured());
        assertThat(existingProduct.getThumbnailMediaId()).isEqualTo(productEsDetailVm.thumbnailMediaId());
        assertThat(existingProduct.getBrand()).isEqualTo(productEsDetailVm.brand());
        assertThat(existingProduct.getCategories()).isEqualTo(productEsDetailVm.categories());
        assertThat(existingProduct.getAttributes()).isEqualTo(productEsDetailVm.attributes());

        verify(productRepository).save(existingProduct);
    }

    @Test
    void updateProductNotPublished_whenProductExists_deleteProduct() {
        Product existingProduct = new Product();
        existingProduct.setId(ID);

        when(productRepository.findById(ID)).thenReturn(Optional.of(existingProduct));

        ProductEsDetailVm productEsDetailVm = new ProductEsDetailVm(
            ID,
            "Smartphone XYZ",
            "smartphone-xyz",
            299.99,
            false,
            true,
            true,
            false,
            456L,
            "BrandName",
            List.of("Electronics", "Mobile Phones"),
            List.of("Color: Black", "Storage: 128GB", "RAM: 6GB")
        );

        URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(ID).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(productEsDetailVm);

        productSyncDataService.updateProduct(ID);

        verify(productRepository).deleteById(ID);
    }

    @Test
    void testUpdateProduct_whenProductDoesNotExist_throwsNotFoundException() {

        mockProductThumbnailVmsByUri();
        when(productRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productSyncDataService.updateProduct(ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("The product 1 is not found");
    }


    @Test
    void testCreateProduct_whenNormalCase_createsAndSavesProduct() {

        mockProductThumbnailVmsByUri();
        ProductEsDetailVm productEsDetailVm = getProductThumbnailVms();

        when(productSyncDataService.getProductEsDetailById(ID)).thenReturn(productEsDetailVm);

        productSyncDataService.createProduct(ID);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();
        assertThat(actual.getId()).isEqualTo(productEsDetailVm.id());
        assertThat(actual.getName()).isEqualTo(productEsDetailVm.name());
        assertThat(actual.getSlug()).isEqualTo(productEsDetailVm.slug());
        assertThat(actual.getPrice()).isEqualTo(productEsDetailVm.price());
        assertThat(actual.getIsPublished()).isEqualTo(productEsDetailVm.isPublished());
        assertThat(actual.getIsVisibleIndividually()).isEqualTo(productEsDetailVm.isVisibleIndividually());
        assertThat(actual.getIsAllowedToOrder()).isEqualTo(productEsDetailVm.isAllowedToOrder());
        assertThat(actual.getIsFeatured()).isEqualTo(productEsDetailVm.isFeatured());
        assertThat(actual.getThumbnailMediaId()).isEqualTo(productEsDetailVm.thumbnailMediaId());
        assertThat(actual.getBrand()).isEqualTo(productEsDetailVm.brand());
        assertThat(actual.getCategories()).isEqualTo(productEsDetailVm.categories());
        assertThat(actual.getAttributes()).isEqualTo(productEsDetailVm.attributes());
    }


    @Test
    void testDeleteProduct_whenProductExists_deletesProduct() {
        Long id = 1L;

        when(productRepository.existsById(id)).thenReturn(true);

        productSyncDataService.deleteProduct(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void testDeleteProduct_whenProductDoesNotExist_doNotThrowException() {
        Long id = 1L;

        when(productRepository.existsById(id)).thenReturn(false);

        // Should not throw any exception
        productSyncDataService.deleteProduct(id);

        // Verify that deleteById is never called
        verify(productRepository, never()).deleteById(id);
    }

    @Test
    void testCreateProduct_whenProductWithAllAttributes_createsAndSavesProduct() {

        mockProductThumbnailVmsByUri();
        ProductEsDetailVm productEsDetailVm = getProductThumbnailVms();

        productSyncDataService.createProduct(ID);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getId()).isEqualTo(ID);
        assertThat(actual.getName()).isEqualTo(productEsDetailVm.name());
        assertThat(actual.getSlug()).isEqualTo(productEsDetailVm.slug());
        assertThat(actual.getPrice()).isEqualTo(productEsDetailVm.price());
        assertThat(actual.getIsPublished()).isTrue();
        assertThat(actual.getIsVisibleIndividually()).isTrue();
        assertThat(actual.getIsAllowedToOrder()).isTrue();
        assertThat(actual.getIsFeatured()).isFalse();
        assertThat(actual.getThumbnailMediaId()).isEqualTo(456L);
        assertThat(actual.getBrand()).isEqualTo("BrandName");
        assertThat(actual.getCategories()).containsExactly("Electronics", "Mobile Phones");
        assertThat(actual.getAttributes()).containsExactly("Color: Black", "Storage: 128GB", "RAM: 6GB");
    }

    @Test
    void testUpdateProduct_whenProductIsPublishedWithNewData_updatesAllFields() {

        mockProductThumbnailVmsByUri();
        Product existingProduct = new Product();
        existingProduct.setId(ID);
        existingProduct.setName("Old Name");
        existingProduct.setPrice(99.99);

        when(productRepository.findById(ID)).thenReturn(Optional.of(existingProduct));

        productSyncDataService.updateProduct(ID);

        assertThat(existingProduct.getName()).isEqualTo("Smartphone XYZ");
        assertThat(existingProduct.getPrice()).isEqualTo(299.99);
        assertThat(existingProduct.getIsPublished()).isTrue();
        assertThat(existingProduct.getBrand()).isEqualTo("BrandName");

        verify(productRepository).save(existingProduct);
    }

    @Test
    void testCreateProduct_withMultipleCategories_createsProductCorrectly() {

        final Long productId = 2L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Laptop Pro",
                "laptop-pro",
                1299.99,
                true,
                true,
                true,
                true,
                789L,
                "Dell",
                List.of("Electronics", "Computers", "Laptops"),
                List.of("CPU: Intel i7", "RAM: 16GB", "Storage: 512GB SSD")
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getId()).isEqualTo(productId);
        assertThat(actual.getName()).isEqualTo("Laptop Pro");
        assertThat(actual.getCategories()).hasSize(3);
        assertThat(actual.getBrand()).isEqualTo("Dell");
    }

    @Test
    void testDeleteProduct_whenCalledMultipleTimes_deletesEachProduct() {
        Long id1 = 1L;
        Long id2 = 2L;

        when(productRepository.existsById(id1)).thenReturn(true);
        when(productRepository.existsById(id2)).thenReturn(true);

        productSyncDataService.deleteProduct(id1);
        productSyncDataService.deleteProduct(id2);

        verify(productRepository).deleteById(id1);
        verify(productRepository).deleteById(id2);
    }

    @Test
    void testGetProductEsDetailById_returnsCompleteProductDetails() {

        final Long productId = 3L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        ProductEsDetailVm expectedProduct = new ProductEsDetailVm(
            productId,
            "Camera DSLR",
            "camera-dslr",
            899.99,
            true,
            true,
            false,
            false,
            321L,
            "Canon",
            List.of("Photography", "Cameras"),
            List.of("Resolution: 24MP", "Type: DSLR")
        );

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class)).thenReturn(expectedProduct);

        ProductEsDetailVm result = productSyncDataService.getProductEsDetailById(productId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Camera DSLR");
        assertThat(result.brand()).isEqualTo("Canon");
        assertThat(result.isAllowedToOrder()).isFalse();
    }

    @Test
    void testUpdateProduct_whenProductChangesFromPublishedToUnpublished_deletesProduct() {

        mockProductThumbnailVmsByUri();
        Product existingProduct = new Product();
        existingProduct.setId(ID);
        existingProduct.setName("Published Product");
        existingProduct.setIsPublished(true);

        when(productRepository.findById(ID)).thenReturn(Optional.of(existingProduct));

        // Mock the response with an unpublished product
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(ID).toUri();

        ProductEsDetailVm unpublishedProduct = new ProductEsDetailVm(
            ID,
            "Unpublished Product",
            "unpublished",
            0.0,
            false,
            true,
            true,
            false,
            0L,
            "Brand",
            List.of(),
            List.of()
        );

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class)).thenReturn(unpublishedProduct);

        productSyncDataService.updateProduct(ID);

        verify(productRepository).deleteById(ID);
    }

    @Test
    void testCreateProduct_withEmptyCategoriesAndAttributes_createsProductCorrectly() {

        final Long productId = 4L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Simple Product",
                "simple-product",
                50.0,
                true,
                true,
                true,
                false,
                0L,
                "NoName",
                List.of(),
                List.of()
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getCategories()).isEmpty();
        assertThat(actual.getAttributes()).isEmpty();
        assertThat(actual.getName()).isEqualTo("Simple Product");
    }

    @Test
    void testCreateProduct_withFeaturedProduct_createsWithFeaturedFlag() {

        final Long productId = 5L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Featured Product",
                "featured-product",
                199.99,
                true,
                true,
                true,
                true,
                500L,
                "FeaturedBrand",
                List.of("Featured"),
                List.of("Special")
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getIsFeatured()).isTrue();
    }

    @Test
    void testCreateProduct_withNotVisibleIndividually_createsProduct() {

        final Long productId = 6L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Bundle Product",
                "bundle-product",
                499.99,
                true,
                false,
                true,
                false,
                600L,
                "BundleBrand",
                List.of("Bundle"),
                List.of()
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getIsVisibleIndividually()).isFalse();
    }

    @Test
    void testCreateProduct_withNotAllowedToOrder_createsProduct() {

        final Long productId = 7L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Out of Stock Product",
                "out-of-stock",
                0.0,
                true,
                true,
                false,
                false,
                0L,
                "OutOfStockBrand",
                List.of("OutOfStock"),
                List.of()
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getIsAllowedToOrder()).isFalse();
    }

    @Test
    void testUpdateProduct_withVeryHighPrice_updatesCorrectly() {

        final Long productId = 8L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        Product existingProduct = new Product();
        existingProduct.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Luxury Product",
                "luxury-product",
                99999.99,
                true,
                true,
                true,
                true,
                999L,
                "LuxuryBrand",
                List.of("Luxury", "Premium"),
                List.of("Materials: Premium")
            ));

        productSyncDataService.updateProduct(productId);

        assertThat(existingProduct.getPrice()).isEqualTo(99999.99);
        assertThat(existingProduct.getIsFeatured()).isTrue();
        verify(productRepository).save(existingProduct);
    }

    @Test
    void testDeleteProduct_withLargeProductId_deletesSuccessfully() {

        Long largeId = Long.MAX_VALUE;
        when(productRepository.existsById(largeId)).thenReturn(true);

        productSyncDataService.deleteProduct(largeId);

        verify(productRepository).deleteById(largeId);
    }

    @Test
    void testCreateProduct_withLongProductName_createsProductCorrectly() {

        final Long productId = 9L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        String longName = "A".repeat(500); // Very long product name

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                longName,
                "long-name-product",
                99.99,
                true,
                true,
                true,
                false,
                100L,
                "Brand",
                List.of("Category"),
                List.of()
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getName()).hasSize(500);
    }

    @Test
    void testCreateProduct_withSpecialCharactersInName_createsProductCorrectly() {

        final Long productId = 10L;
        final URI url = UriComponentsBuilder.fromHttpUrl(PRODUCT_URL)
            .path("/storefront/products-es/{id}").buildAndExpand(productId).toUri();

        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductEsDetailVm.class))
            .thenReturn(new ProductEsDetailVm(
                productId,
                "Product @#$% & Special™",
                "product-special-chars",
                150.0,
                true,
                true,
                true,
                false,
                200L,
                "Brand®",
                List.of("Category™"),
                List.of("Property: Value©")
            ));

        productSyncDataService.createProduct(productId);

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(argumentCaptor.capture());
        Product actual = argumentCaptor.getValue();

        assertThat(actual.getName()).contains("@#$%");
    }

}