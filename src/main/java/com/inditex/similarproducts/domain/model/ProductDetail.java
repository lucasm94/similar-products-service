package com.inditex.similarproducts.domain.model;

public record ProductDetail(
        String id,
        String name,
        double price,
        boolean availability
) {}