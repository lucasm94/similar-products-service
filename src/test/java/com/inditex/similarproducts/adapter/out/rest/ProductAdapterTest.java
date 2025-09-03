package com.inditex.similarproducts.adapter.out.rest;

import com.inditex.similarproducts.domain.exception.ExternalServiceException;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.domain.model.ProductDetail;
import com.inditex.similarproducts.infrastructure.config.ExternalApiProperties;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

class ProductAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MetricsRecorder metrics;

    private ProductAdapter adapter;

    private ExternalApiProperties props;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        props = new ExternalApiProperties();
        props.setBaseUrl("http://localhost:8080");
        props.setProductDetailPath("/product/{productId}");
        props.setSimilarIdsPath("/product/{productId}/similarids");
        adapter = new ProductAdapter(restTemplate, props, metrics);
    }

    // --- Tests getProductDetail ---

    @Test
    void getProductDetail_shouldReturnProductDetail_whenApiReturnsOk() {
        ProductDetail mockDetail = new ProductDetail("123", "Test", 10.0, true);
        when(restTemplate.getForObject(anyString(), eq(ProductDetail.class))).thenReturn(mockDetail);

        ProductDetail result = adapter.getProductDetail("123");

        assertNotNull(result);
        assertEquals("123", result.id());
        verify(metrics).recordRequest(any(), eq(MetricsType.SUCCESS));
    }

    @Test
    void getProductDetail_shouldThrowNotFoundException_whenApiReturns404() {
        when(restTemplate.getForObject(anyString(), eq(ProductDetail.class)))
                .thenThrow(new HttpClientErrorException(NOT_FOUND));

        assertThrows(NotFoundException.class, () -> adapter.getProductDetail("123"));

        verify(metrics).recordRequest(any(), eq(MetricsType.NOT_FOUND));
    }

    @Test
    void getProductDetail_shouldThrowExternalServiceException_whenApiReturns500() {
        when(restTemplate.getForObject(anyString(), eq(ProductDetail.class)))
                .thenThrow(new HttpClientErrorException(INTERNAL_SERVER_ERROR));

        assertThrows(ExternalServiceException.class, () -> adapter.getProductDetail("123"));

        verify(metrics).recordRequest(any(), eq(MetricsType.ERROR));
    }

    @Test
    void getProductDetail_shouldThrowExternalServiceException_whenUnexpectedError() {
        when(restTemplate.getForObject(anyString(), eq(ProductDetail.class))).thenThrow(new RuntimeException("error"));

        assertThrows(ExternalServiceException.class, () -> adapter.getProductDetail("123"));

        verify(metrics).recordRequest(any(), eq(MetricsType.ERROR));
    }

    // --- Tests getSimilarIds ---

    @Test
    void getSimilarIds_shouldReturnIds_whenApiReturnsOk() {
        String[] mockResponse = {"10", "20", "30"};
        when(restTemplate.getForObject(anyString(), eq(String[].class))).thenReturn(mockResponse);

        List<String> result = adapter.getSimilarIds("123");

        assertEquals(3, result.size());
        assertEquals(List.of("10", "20", "30"), result);
        verify(metrics).recordRequest(any(), eq(MetricsType.SUCCESS));
    }

    @Test
    void getSimilarIds_shouldReturnEmptyList_whenApiReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(String[].class))).thenReturn(null);

        List<String> result = adapter.getSimilarIds("123");

        assertTrue(result.isEmpty());
        verify(metrics).recordRequest(any(), eq(MetricsType.SUCCESS));
    }

    @Test
    void getSimilarIds_shouldThrowNotFoundException_whenApiReturns404() {
        when(restTemplate.getForObject(anyString(), eq(String[].class)))
                .thenThrow(new HttpClientErrorException(NOT_FOUND));

        assertThrows(NotFoundException.class, () -> adapter.getSimilarIds("123"));

        verify(metrics).recordRequest(any(), eq(MetricsType.NOT_FOUND));
    }

    @Test
    void getSimilarIds_shouldThrowExternalServiceException_whenApiReturns500() {
        when(restTemplate.getForObject(anyString(), eq(String[].class)))
                .thenThrow(new HttpClientErrorException(INTERNAL_SERVER_ERROR));

        assertThrows(ExternalServiceException.class, () -> adapter.getSimilarIds("123"));

        verify(metrics).recordRequest(any(), eq(MetricsType.ERROR));
    }

    @Test
    void getSimilarIds_shouldThrowExternalServiceException_whenUnexpectedExceptionOccurs() {
        when(restTemplate.getForObject(anyString(), eq(String[].class))).thenThrow(new RuntimeException("error"));

        assertThrows(ExternalServiceException.class, () -> adapter.getSimilarIds("123"));

        verify(metrics).recordRequest(any(), eq(MetricsType.ERROR));
    }

    // --- Test fallback ---

    @Test
    void similarIdsFallback_shouldReturnEmptyList() {
        List<String> result = adapter.similarIdsFallback("123", new RuntimeException("error"));

        assertTrue(result.isEmpty());
        verify(metrics).recordRequest(any(), eq(MetricsType.FALLBACK));
    }
}
