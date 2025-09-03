package com.inditex.similarproducts.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.inditex.similarproducts.SimilarProductsApplication;
import com.inditex.similarproducts.domain.model.ProductDetail;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {SimilarProductsApplication.class, ApiIntegrationTest.TestCaches.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApiIntegrationTest {

    private static WireMockServer mockExternalApi;

    @LocalServerPort
    private int applicationPort;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @BeforeAll
    static void startMockServer() {
        mockExternalApi = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockExternalApi.start();
        WireMock.configureFor("localhost", mockExternalApi.port());
    }

    @AfterAll
    static void stopMockServer() {
        mockExternalApi.stop();
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("external.api.base-url", () -> "http://localhost:" + mockExternalApi.port());
        registry.add("spring.main.allow-bean-definition-overriding", () -> "true");
    }

    @TestConfiguration
    static class TestCaches {
        @Bean
        @Primary
        CacheManager testCacheManager() {
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            cacheManager.setCaches(List.of(
                    new ConcurrentMapCache("productDetail"),
                    new ConcurrentMapCache("similarIds")
            ));
            return cacheManager;
        }
    }

    @Test
    void shouldReturnSimilarProducts_whenExternalApiRespondsSuccessfully() {
        mockExternalApi.stubFor(get(urlEqualTo("/product/10/similarids"))
                .willReturn(okJson("[\"1\",\"2\"]")));
        mockExternalApi.stubFor(get(urlEqualTo("/product/1"))
                .willReturn(okJson("{\"id\":\"1\",\"name\":\"A\",\"price\":10.0,\"availability\":true}")));
        mockExternalApi.stubFor(get(urlEqualTo("/product/2"))
                .willReturn(okJson("{\"id\":\"2\",\"name\":\"B\",\"price\":20.0,\"availability\":false}")));

        ResponseEntity<List<ProductDetail>> response = testRestTemplate.exchange(
                "http://localhost:" + applicationPort + "/product/10/similar",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductDetail>>() {}
        );

        List<ProductDetail> products = response.getBody();

        assertNotNull(products);
        assertEquals(2, products.size());

        ProductDetail p1 = products.getFirst();
        assertEquals("1", p1.id());
        assertEquals("A", p1.name());
        assertEquals(10.0, p1.price());
        assertTrue(p1.availability());

        ProductDetail p2 = products.get(1);
        assertEquals("2", p2.id());
        assertEquals("B", p2.name());
        assertEquals(20.0, p2.price());
        assertFalse(p2.availability());
    }

    @Test
    void shouldReturnNotFound_whenBaseProductDoesNotExist() {
        mockExternalApi.stubFor(get(urlEqualTo("/product/99/similarids"))
                .willReturn(aResponse().withStatus(404)));

        ResponseEntity<String> response = testRestTemplate.getForEntity(
                "http://localhost:" + applicationPort + "/product/99/similar", String.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void shouldReturnPartialList_whenSomeSimilarProductsNotFound() {
        mockExternalApi.stubFor(get(urlEqualTo("/product/20/similarids"))
                .willReturn(okJson("[\"100\",\"200\"]")));
        mockExternalApi.stubFor(get(urlEqualTo("/product/100"))
                .willReturn(okJson("{\"id\":\"100\",\"name\":\"Valid\",\"price\":15.0,\"availability\":true}")));
        mockExternalApi.stubFor(get(urlEqualTo("/product/200"))
                .willReturn(aResponse().withStatus(404)));

        ResponseEntity<List<ProductDetail>> response = testRestTemplate.exchange(
                "http://localhost:" + applicationPort + "/product/20/similar",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        List<ProductDetail> products = response.getBody();

        assertNotNull(products);
        assertEquals(1, products.size());
        assertEquals("100", products.getFirst().id());
        assertEquals("Valid", products.getFirst().name());
        assertEquals(15.0, products.getFirst().price());
        assertTrue(products.getFirst().availability());
    }

    @Test
    void shouldReturnPartialList_whenSomeSimilarProductsFailWithError() {
        mockExternalApi.stubFor(get(urlEqualTo("/product/30/similarids"))
                .willReturn(okJson("[\"300\",\"400\"]")));
        mockExternalApi.stubFor(get(urlEqualTo("/product/300"))
                .willReturn(okJson("{\"id\":\"300\",\"name\":\"Valid\",\"price\":25.0,\"availability\":false}")));
        mockExternalApi.stubFor(get(urlEqualTo("/product/400"))
                .willReturn(aResponse().withStatus(500)));

        ResponseEntity<List<ProductDetail>> response = testRestTemplate.exchange(
                "http://localhost:" + applicationPort + "/product/30/similar",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        List<ProductDetail> products = response.getBody();

        assertNotNull(products);
        assertEquals(1, products.size());
        assertEquals("300", products.getFirst().id());
        assertEquals("Valid", products.getFirst().name());
        assertEquals(25.0, products.getFirst().price());
        assertFalse(products.getFirst().availability());
    }
}
