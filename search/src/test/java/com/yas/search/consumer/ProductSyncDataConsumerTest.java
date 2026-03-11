package com.yas.search.consumer;

import static com.yas.commonlibrary.kafka.cdc.message.Operation.CREATE;
import static com.yas.commonlibrary.kafka.cdc.message.Operation.DELETE;
import static com.yas.commonlibrary.kafka.cdc.message.Operation.UPDATE;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yas.commonlibrary.kafka.cdc.message.ProductCdcMessage;
import com.yas.commonlibrary.kafka.cdc.message.ProductMsgKey;
import com.yas.search.kafka.consumer.ProductSyncDataConsumer;
import com.yas.commonlibrary.kafka.cdc.message.Product;
import com.yas.search.service.ProductSyncDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProductSyncDataConsumerTest {

    @InjectMocks
    private ProductSyncDataConsumer productSyncDataConsumer;

    @Mock
    private ProductSyncDataService productSyncDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSync_whenCreateAction_createProduct() {
        // When
        long productId = 1L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(CREATE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).createProduct(productId);
    }

    @Test
    void testSync_whenUpdateAction_updateProduct() {
        // When
        long productId = 2L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(UPDATE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).updateProduct(productId);
    }

    @Test
    void testSync_whenDeleteAction_deleteProduct() {
        // When
        final long productId = 3L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(DELETE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).deleteProduct(productId);
    }

    @Test
    void testSync_whenHardDeleteEvent_deleteProduct() {
        // When - hard delete event with null message
        final long productId = 4L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            null
        );

        // Then
        verify(productSyncDataService, times(1)).deleteProduct(productId);
    }

    @Test
    void testSync_whenReadOperation_createProduct() {
        // When - READ operation should also create the product
        final long productId = 5L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(com.yas.commonlibrary.kafka.cdc.message.Operation.READ)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).createProduct(productId);
    }

    @Test
    void testSync_whenMultipleOperations_handlesAllCorrectly() {
        // Create product
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(1L).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(1L).build())
                .op(CREATE)
                .build()
        );

        verify(productSyncDataService, times(1)).createProduct(1L);

        // Update product
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(1L).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(1L).build())
                .op(UPDATE)
                .build()
        );

        verify(productSyncDataService, times(1)).updateProduct(1L);

        // Delete product
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(1L).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(1L).build())
                .op(DELETE)
                .build()
        );

        verify(productSyncDataService, times(1)).deleteProduct(1L);
    }

    @Test
    void testSync_whenDeleteWithNullMessage_callsDelete() {
        // When - null message indicates hard delete
        final long productId = 10L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            null
        );

        // Then
        verify(productSyncDataService, times(1)).deleteProduct(productId);
    }

    @Test
    void testSync_whenCreateWithLargeProductId_createsProduct() {
        // When
        long largeProductId = Long.MAX_VALUE;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(largeProductId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(largeProductId).build())
                .op(CREATE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).createProduct(largeProductId);
    }

    @Test
    void testSync_whenUpdateWithLargeProductId_updatesProduct() {
        // When
        long largeProductId = Long.MAX_VALUE - 1;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(largeProductId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(largeProductId).build())
                .op(UPDATE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).updateProduct(largeProductId);
    }

    @Test
    void testSync_whenDeleteWithLargeProductId_deletesProduct() {
        // When
        long largeProductId = Long.MAX_VALUE - 2;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(largeProductId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(largeProductId).build())
                .op(DELETE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).deleteProduct(largeProductId);
    }

    @Test
    void testSync_whenCreateMultipleProducts_createsAll() {
        // When - Create multiple products
        for (long i = 1; i <= 5; i++) {
            productSyncDataConsumer.sync(
                ProductMsgKey.builder().id(i).build(),
                ProductCdcMessage.builder()
                    .after(Product.builder().id(i).build())
                    .op(CREATE)
                    .build()
            );
        }

        // Then - Verify all were created
        for (long i = 1; i <= 5; i++) {
            verify(productSyncDataService, times(1)).createProduct(i);
        }
    }

    @Test
    void testSync_whenUpdateThenDelete_callsBothOperations() {
        // When - Update then delete same product
        final long productId = 100L;
        
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(UPDATE)
                .build()
        );

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(DELETE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).updateProduct(productId);
        verify(productSyncDataService, times(1)).deleteProduct(productId);
    }

    @Test
    void testSync_whenCreateThenUpdate_callsBothOperations() {
        // When - Create then update same product
        final long productId = 101L;
        
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(CREATE)
                .build()
        );

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(UPDATE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).createProduct(productId);
        verify(productSyncDataService, times(1)).updateProduct(productId);
    }

    @Test
    void testSync_whenProductIdIsOne_handlesCorrectly() {
        // When
        final long productId = 1L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(CREATE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).createProduct(1L);
    }

    @Test
    void testSync_whenProductIdIsZero_handlesCorrectly() {
        // When - Product ID 0 (edge case)
        final long productId = 0L;
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(DELETE)
                .build()
        );

        // Then
        verify(productSyncDataService, times(1)).deleteProduct(0L);
    }

    @Test
    void testSync_whenReadOperationMultipleTimes_createsProductEachTime() {
        // When - READ operation called multiple times
        final long productId = 50L;
        
        for (int i = 0; i < 3; i++) {
            productSyncDataConsumer.sync(
                ProductMsgKey.builder().id(productId).build(),
                ProductCdcMessage.builder()
                    .after(Product.builder().id(productId).build())
                    .op(com.yas.commonlibrary.kafka.cdc.message.Operation.READ)
                    .build()
            );
        }

        // Then - Should be called 3 times
        verify(productSyncDataService, times(3)).createProduct(productId);
    }

    @Test
    void testSync_whenHardDeleteMultipleTimes_deletesEachTime() {
        // When - Hard delete (null message) multiple times with same product
        final long productId = 60L;
        
        for (int i = 0; i < 2; i++) {
            productSyncDataConsumer.sync(
                ProductMsgKey.builder().id(productId).build(),
                null
            );
        }

        // Then
        verify(productSyncDataService, times(2)).deleteProduct(productId);
    }

    @Test
    void testSync_whenMixedOperationsSequence_handlesAllCorrectly() {
        // When - Complex sequence of operations
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(1L).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(1L).build())
                .op(CREATE)
                .build()
        );

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(1L).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(1L).build())
                .op(UPDATE)
                .build()
        );

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(1L).build(),
            null
        );

        // Then
        verify(productSyncDataService, times(1)).createProduct(1L);
        verify(productSyncDataService, times(1)).updateProduct(1L);
        verify(productSyncDataService, times(1)).deleteProduct(1L);
    }

    @Test
    void testSync_whenDeleteAfterHardDelete_handlesCorrectly() {
        // When - Hard delete then soft delete
        final long productId = 70L;
        
        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            null
        );

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder()
                .after(Product.builder().id(productId).build())
                .op(DELETE)
                .build()
        );

        // Then - Both should call delete
        verify(productSyncDataService, times(2)).deleteProduct(productId);
    }
}
