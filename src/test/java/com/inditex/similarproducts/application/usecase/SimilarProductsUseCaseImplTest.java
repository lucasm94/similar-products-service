package com.inditex.similarproducts.application.usecase;

import com.inditex.similarproducts.adapter.out.rest.ProductAdapter;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.domain.model.ProductDetail;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsEndpoint;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimilarProductsUseCaseImplTest {

    @Mock
    private ProductAdapter productAdapter;

    @Mock
    private MetricsRecorder metrics;

    private SimilarProductsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new SimilarProductsUseCaseImpl(productAdapter, metrics);
    }

    @Test
    void getSimilarProducts_shouldReturnList_whenAllSuccess() {
        ProductDetail mainProduct = new ProductDetail("1", "A", 10.0, true);
        when(productAdapter.getProductDetail("1")).thenReturn(mainProduct);
        when(productAdapter.getSimilarIds("1")).thenReturn(List.of("2", "3"));
        ProductDetail productB = new ProductDetail("2", "B", 20.0, true);
        ProductDetail productC = new ProductDetail("3", "C", 30.0, true);
        when(productAdapter.getProductDetail("2")).thenReturn(productB);
        when(productAdapter.getProductDetail("3")).thenReturn(productC);

        List<ProductDetail> result = useCase.getSimilarProducts("1");

        assertEquals(List.of(productB, productC), result);
        verify(metrics).recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.SUCCESS);
    }

    @Test
    void getSimilarProducts_shouldReturnEmpty_whenNoSimilarIds() {
        ProductDetail mainProduct = new ProductDetail("1", "A", 10.0, true);
        when(productAdapter.getProductDetail("1")).thenReturn(mainProduct);
        when(productAdapter.getSimilarIds("1")).thenReturn(List.of());

        List<ProductDetail> result = useCase.getSimilarProducts("1");

        assertTrue(result.isEmpty());
        verify(metrics).recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.EMPTY);
    }

    @Test
    void getSimilarProducts_shouldSkipNotFoundProduct() {
        ProductDetail mainProduct = new ProductDetail("1", "C", 10.0, true);
        when(productAdapter.getProductDetail("1")).thenReturn(mainProduct);
        when(productAdapter.getSimilarIds("1")).thenReturn(List.of("2", "3"));

        ProductDetail productB = new ProductDetail("2", "B", 20.0, true);
        when(productAdapter.getProductDetail("2")).thenReturn(productB);
        when(productAdapter.getProductDetail("3")).thenThrow(new NotFoundException("not found"));

        List<ProductDetail> result = useCase.getSimilarProducts("1");

        assertEquals(List.of(productB), result);
        verify(metrics).recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.PARTIAL_SUCCESS);
        verify(metrics).recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.SKIPPED_NOT_FOUND);
    }

    @Test
    void getSimilarProducts_shouldSkipErrorProduct() {
        ProductDetail mainProduct = new ProductDetail("1", "A", 10.0, true);
        when(productAdapter.getProductDetail("1")).thenReturn(mainProduct);
        when(productAdapter.getSimilarIds("1")).thenReturn(List.of("2", "3"));

        ProductDetail p2 = new ProductDetail("2", "B", 20.0, true);
        when(productAdapter.getProductDetail("2")).thenReturn(p2);
        when(productAdapter.getProductDetail("3")).thenThrow(new RuntimeException("error"));

        List<ProductDetail> result = useCase.getSimilarProducts("1");

        assertEquals(List.of(p2), result);
        verify(metrics).recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.PARTIAL_SUCCESS);
        verify(metrics).recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.SKIPPED_ERROR);
    }
}
