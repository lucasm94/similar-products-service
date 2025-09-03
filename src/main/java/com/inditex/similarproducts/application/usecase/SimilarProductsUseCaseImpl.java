package com.inditex.similarproducts.application.usecase;

import com.inditex.similarproducts.adapter.out.rest.ProductAdapter;
import com.inditex.similarproducts.application.port.in.SimilarProductsUseCase;
import com.inditex.similarproducts.application.port.out.ProductPort;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.domain.model.ProductDetail;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsEndpoint;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsRecorder;
import com.inditex.similarproducts.infrastructure.monitoring.MetricsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Use case implementation for retrieving products similar to a given product.
 * This class coordinates the interaction between the {@link ProductAdapter} (for getting product details and similar IDs)
 * and {@link MetricsRecorder} (for tracking metrics of successful and failed).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarProductsUseCaseImpl implements SimilarProductsUseCase {
    private final ProductPort productAdapter;
    private final MetricsRecorder metrics;

    /**
     * Retrieves product details for all products similar to the given product ID.
     *
     * @param productId the ID of the product to search similar products for.
     * @return a list of {@link ProductDetail} for all successfully retrieved similar products.
     * @throws NotFoundException if the provided productId has no similar IDs or the external service returns 404.
     */
    @Override
    public List<ProductDetail> getSimilarProducts(String productId) {
        log.info("Getting similar products for product {}", productId);

        List<String> similarIds = productAdapter.getSimilarIds(productId);
        log.info("Found {} similar ids for {}", similarIds.size(), productId);

        List<ProductDetail> similarProducts = getProductsDetailFromSimilarProducts(similarIds);

        log.info("Returning {} similar products for {}", similarProducts.size(), productId);
        metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS,
                calculateMetricsCategory(similarProducts.size(), similarIds.size()));
        return similarProducts;
    }

    /**
     * Retrieves the details of a list of similar products.
     * Iterates over the provided product IDs and attempts to fetch each product's details.
     * Products that are not found or produce an error are skipped, with appropriate metrics recorded.
     * The returned list only contains successfully retrieved products.
     * @param similarIds a list of product IDs to fetch details for
     * @return a list of {@link ProductDetail} objects successfully retrieved
     */
    private List<ProductDetail> getProductsDetailFromSimilarProducts(List<String> similarIds) {
        return similarIds.stream()
                .map(this::fetchProductDetail)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Fetches the details of a single product.
     * This method wraps {@link ProductAdapter#getProductDetail(String)} and ensures that exceptions do not propagate.
     *
     * @param productId the ID of the product
     * @return an {@link Optional} containing the {@link ProductDetail} if successfully retrieved,
     * or empty if the product was not found or an error occurred.
     */
    private Optional<ProductDetail> fetchProductDetail(String productId) {
        try {
            var detail = productAdapter.getProductDetail(productId);
            return Optional.ofNullable(detail);
        } catch (NotFoundException e) {
            metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.SKIPPED_NOT_FOUND);
            log.info("Product {} not found, skipping.", productId);
        } catch (Exception ex) {
            metrics.recordRequest(MetricsEndpoint.SIMILAR_PRODUCTS, MetricsType.SKIPPED_ERROR);
            log.warn("Skipping product {} due to unexpected error: {}", productId, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /**
     * Determines the metrics category based on the number of successfully retrieved products.
     *
     * @param productsFounded the number of products successfully retrieved
     * @param similarIds the total number of requested product IDs
     * @return the appropriate {@link MetricsType} representing the result
     */
    private MetricsType calculateMetricsCategory(int productsFounded, int similarIds) {
        if (productsFounded == 0) return MetricsType.EMPTY;
        if (productsFounded == similarIds) return MetricsType.SUCCESS;
        return MetricsType.PARTIAL_SUCCESS;
    }
}
