package com.inditex.similarproducts.application.port.out;

import com.inditex.similarproducts.domain.model.ProductDetail;

import java.util.List;

public interface ProductPort {
    ProductDetail getProductDetail(String productId);
    List<String> getSimilarIds(String productId);
}
