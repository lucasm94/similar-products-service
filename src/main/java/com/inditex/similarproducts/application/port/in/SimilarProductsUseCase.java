package com.inditex.similarproducts.application.port.in;

import com.inditex.similarproducts.domain.model.ProductDetail;

import java.util.List;

public interface SimilarProductsUseCase {
    List<ProductDetail> getSimilarProducts(String productId);
}
