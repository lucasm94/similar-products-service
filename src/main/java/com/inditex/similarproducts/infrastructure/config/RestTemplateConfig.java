package com.inditex.similarproducts.infrastructure.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT))
                .readTimeout(Duration.ofSeconds(READ_TIMEOUT))
                .build();
    }
}
