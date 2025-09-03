package com.inditex.similarproducts.adapter.in.rest.error;

import lombok.Getter;

@Getter
public enum ErrorMessage {
    PRODUCT_NOT_FOUND("Product not found"),
    VALIDATION_ERROR("Validation error"),
    PRODUCT_API_ERROR("Product API error"),
    INTERNAL_ERROR("Internal error");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

}
