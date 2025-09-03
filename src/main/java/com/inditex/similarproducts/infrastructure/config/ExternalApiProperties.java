package com.inditex.similarproducts.infrastructure.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "external.api")
public class ExternalApiProperties {
    private String baseUrl;
    private String similarIdsPath;
    private String productDetailPath;
}
