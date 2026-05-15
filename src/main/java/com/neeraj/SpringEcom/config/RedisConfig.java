package com.neeraj.SpringEcom.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neeraj.SpringEcom.model.Product;
import com.neeraj.SpringEcom.model.dto.WishlistResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    private final ObjectMapper objectMapper;

    public RedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        JavaType productListType = objectMapper
                .getTypeFactory()
                .constructCollectionType(List.class, Product.class);

        JavaType wishlistListType = objectMapper
                .getTypeFactory()
                .constructCollectionType(List.class, WishlistResponse.class);

        Jackson2JsonRedisSerializer<Product> productSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Product.class);

        Jackson2JsonRedisSerializer<List<Product>> productListSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, productListType);

        Jackson2JsonRedisSerializer<List<WishlistResponse>> wishlistListSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, wishlistListType);

        GenericJackson2JsonRedisSerializer defaultSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(
                "product",
                cacheConfig(Duration.ofMinutes(10), productSerializer)
        );

        cacheConfigurations.put(
                "products",
                cacheConfig(Duration.ofMinutes(10), productListSerializer)
        );

        cacheConfigurations.put(
                "searchProducts",
                cacheConfig(Duration.ofMinutes(2), productListSerializer)
        );

        cacheConfigurations.put(
                "wishlist",
                cacheConfig(Duration.ofMinutes(2), wishlistListSerializer)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig(Duration.ofMinutes(10), defaultSerializer))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private RedisCacheConfiguration cacheConfig(
            Duration ttl,
            RedisSerializer<?> valueSerializer
    ) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
                );
    }
}
