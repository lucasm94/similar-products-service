package com.inditex.similarproducts.infrastructure.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inditex.similarproducts.domain.model.ProductDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.util.List;
import java.util.Map;

@EnableCaching
@Configuration
@RequiredArgsConstructor
@Profile("!local")
public class CacheConfig {
    private final CacheProperties cacheProperties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisCacheManager cacheManager() {
        Jackson2JsonRedisSerializer<ProductDetail> productDetailSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ProductDetail.class);

        RedisCacheConfiguration productDetailConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getProductDetailTtl())
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productDetailSerializer)
                );

        JavaType listOfStringType = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
        Jackson2JsonRedisSerializer<List<String>> similarIdsSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, listOfStringType);

        RedisCacheConfiguration similarIdsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getSimilarIdsTtl())
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(similarIdsSerializer)
                );

        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(Map.of(
                        "productDetail", productDetailConfig,
                        "similarIds", similarIdsConfig
                ))
                .build();
    }
}
