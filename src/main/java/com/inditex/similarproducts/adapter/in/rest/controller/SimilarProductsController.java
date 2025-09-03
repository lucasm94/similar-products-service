package com.inditex.similarproducts.adapter.in.rest.controller;

import com.inditex.similarproducts.application.port.in.SimilarProductsUseCase;
import com.inditex.similarproducts.domain.model.ProductDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product") // Suggestion: introduce API versioning `/v1/product`. Facilitate smoother API evolution and client integration.
@RequiredArgsConstructor
@Validated
public class SimilarProductsController {
    private final SimilarProductsUseCase service;

    @Operation(summary = "Retrieve similar products",
            description = "Given a productId, returns a list of similar products with their details.")
    @ApiResponse(responseCode = "200", description = "List of similar products found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductDetail.class)))
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{productId}/similar")
    public ResponseEntity<List<ProductDetail>> getSimilarProducts(
            @Parameter(description = "ID of the base product", required = true, example = "10")
            @PathVariable @NotBlank (message = "The 'productId' parameter is required") String productId) {
        List<ProductDetail> products = service.getSimilarProducts(productId);
        return ResponseEntity.ok(products);
    }
}
