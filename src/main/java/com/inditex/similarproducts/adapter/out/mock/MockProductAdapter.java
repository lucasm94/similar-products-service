package com.inditex.similarproducts.adapter.out.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inditex.similarproducts.application.port.out.ProductPort;
import com.inditex.similarproducts.domain.exception.ExternalServiceException;
import com.inditex.similarproducts.domain.exception.NotFoundException;
import com.inditex.similarproducts.domain.model.ProductDetail;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("local")
public class MockProductAdapter implements ProductPort {
    private final JsonNode root;

    public MockProductAdapter(ObjectMapper objectMapper) throws Exception {
        InputStream input = getClass().getClassLoader().getResourceAsStream("mock-api.json");
        this.root = objectMapper.readTree(input);
    }

    @Override
    public ProductDetail getProductDetail(String productId) {
        JsonNode node = root.path("product-details").path(productId);

        if (node.isMissingNode()) {
            throw new NotFoundException("Mock: product not found " + productId);
        }
        if (node.isTextual()) {
            switch (node.asText()) {
                case "NOT_FOUND" -> throw new NotFoundException("Mock: product not found " + productId);
                case "ERROR" -> throw new ExternalServiceException("Mock: error for " + productId, 500);
            }
        }
        return new ProductDetail(
                node.get("id").asText(),
                node.get("name").asText(),
                node.get("price").asDouble(),
                node.get("availability").asBoolean()
        );
    }

    @Override
    public List<String> getSimilarIds(String productId) {
        JsonNode node = root.path("similar-ids").path(productId);

        if (node.isMissingNode()) {
            return List.of();
        }
        if (node.isTextual()) {
            switch (node.asText()) {
                case "NOT_FOUND" -> throw new NotFoundException("Mock: not found similar ids for " + productId);
                case "ERROR" -> throw new ExternalServiceException("Mock: error similarIds for " + productId, 500);
            }
        }
        List<String> ids = new ArrayList<>();
        node.forEach(n -> ids.add(n.asText()));
        return ids;
    }
}
