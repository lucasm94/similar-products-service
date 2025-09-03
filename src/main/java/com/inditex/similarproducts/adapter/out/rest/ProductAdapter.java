package com.inditex.similarproducts.adapter.out.rest;

import com.inditex.similarproducts.application.port.out.ProductPort;
import com.inditex.similarproducts.domain.exception.ExternalServiceException;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.domain.model.ProductDetail;
import com.inditex.similarproducts.infrastructure.config.ExternalApiProperties;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

import static com.inditex.similarproducts.adapter.in.rest.error.ErrorMessage.INTERNAL_ERROR;
import static com.inditex.similarproducts.adapter.in.rest.error.ErrorMessage.PRODUCT_API_ERROR;
import static com.inditex.similarproducts.infrastructure.monitoring.MetricsEndpoint.PRODUCT_DETAIL;
import static com.inditex.similarproducts.infrastructure.monitoring.MetricsEndpoint.SIMILAR_IDS;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * This class implements the {@link ProductPort} interface and acts as a gateway
 * between the application and the external API. It uses a {@link RestTemplate}
 * to perform HTTP requests and applies resilience patterns like
 * {@link io.github.resilience4j.retry.annotation.Retry} and
 * {@link io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class ProductAdapter implements ProductPort {
    private final RestTemplate restTemplate;
    private final ExternalApiProperties props;
    private final MetricsRecorder metrics;

    /**
     * Retrieves detailed information about a product from the external API.
     * Applies retry logic in case of transient failures.
     *
     * @param productId the product ID to look up.
     * @return a {@link ProductDetail} representing the product.
     * @throws NotFoundException if the product does not exist (HTTP 404).
     * @throws ExternalServiceException if the API responds with an error or an unexpected exception occurs.
     */
    @Override
    @Retry(name = "productDetail")
    @Cacheable(value = "productDetail", key = "#productId")
    public ProductDetail getProductDetail(String productId) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(props.getBaseUrl() + props.getProductDetailPath())
                    .buildAndExpand(Map.of("productId", productId))
                    .toUriString();

            log.info("Getting product detail for {}", productId);
            ProductDetail detail = restTemplate.getForObject(url, ProductDetail.class);
            metrics.recordRequest(PRODUCT_DETAIL, MetricsType.SUCCESS);
            return detail;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(NOT_FOUND)) {
                metrics.recordRequest(PRODUCT_DETAIL, MetricsType.NOT_FOUND);
                throw new NotFoundException(String.format("Product %s not found", productId));
            }
            metrics.recordRequest(PRODUCT_DETAIL, MetricsType.ERROR);
            log.error("Error getting product {}: status={}", productId, ex.getStatusCode(), ex);
            throw new ExternalServiceException(PRODUCT_API_ERROR.getMessage(), ex, ex.getStatusCode().value());
        } catch (Exception ex) {
            metrics.recordRequest(PRODUCT_DETAIL, MetricsType.ERROR);
            log.error("Unexpected error getting product {}", productId, ex);
            throw new ExternalServiceException(INTERNAL_ERROR.getMessage(), ex, INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Retrieves the list of IDs for products similar to the given product.
     * Uses retry and circuit breaker mechanisms to improve resilience.
     *
     * @param productId the product ID to search similar products for.
     * @return a list of similar product IDs (never {@code null}).
     * @throws NotFoundException        if no similar IDs are found (HTTP 404).
     * @throws ExternalServiceException if another error occurs when calling the API.
     */
    @Override
    @Retry(name = "similarIds")
    @CircuitBreaker(name = "similarIds", fallbackMethod = "similarIdsFallback")
    @Cacheable(value = "similarIds", key = "#productId")
    public List<String> getSimilarIds(String productId) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(props.getBaseUrl() + props.getSimilarIdsPath())
                    .buildAndExpand(Map.of("productId", productId))
                    .toUriString();
            log.info("Getting similar IDs for product {}", productId);
            String[] response = restTemplate.getForObject(url, String[].class);
            metrics.recordRequest(SIMILAR_IDS, MetricsType.SUCCESS);
            return response == null ? List.of() : Arrays.stream(response).toList();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(NOT_FOUND)) {
                log.info("No similar products found for {}", productId);
                metrics.recordRequest(SIMILAR_IDS, MetricsType.NOT_FOUND);
                throw new NotFoundException(String.format("Similar Ids not found for product %s", productId));
            }
            log.error("Error getting similar IDs for {}: status={}", productId, ex.getStatusCode(), ex);
            metrics.recordRequest(SIMILAR_IDS, MetricsType.ERROR);
            throw new ExternalServiceException(PRODUCT_API_ERROR.getMessage(), ex, ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Unexpected error fetching similar IDs for {}", productId, ex);
            metrics.recordRequest(SIMILAR_IDS, MetricsType.ERROR);
            throw new ExternalServiceException(INTERNAL_ERROR.getMessage(), ex, INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Fallback method triggered by the circuit breaker when {@link #getSimilarIds(String)} fails.
     *
     * @param productId the product ID used in the failed request.
     * @param t the throwable that caused the fallback.
     * @return an empty list of similar product IDs.
     */
    public List<String> similarIdsFallback(String productId, Throwable t) {
        log.error("Fallback similarIds for {}", productId, t);
        metrics.recordRequest(SIMILAR_IDS, MetricsType.FALLBACK);
        return List.of();
    }
}
