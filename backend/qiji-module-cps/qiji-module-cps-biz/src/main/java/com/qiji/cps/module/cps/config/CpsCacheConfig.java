package com.qiji.cps.module.cps.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * CPS 缓存配置
 *
 * <p>基于 Redis 的 Spring Cache 配置，为 CPS 模块高频读取的数据提供缓存支持：
 * <ul>
 *   <li>{@link #CACHE_PLATFORM}：平台配置缓存，TTL=30分钟，按 platformCode 缓存</li>
 *   <li>{@link #CACHE_REBATE_CONFIG}：返利配置缓存，TTL=10分钟</li>
 *   <li>{@link #CACHE_RISK_RATE_RULE}：风控频率限制规则缓存，TTL=5分钟</li>
 * </ul>
 * </p>
 *
 * @author CPS System
 */
@Configuration
public class CpsCacheConfig {

    /** 平台配置缓存名称 */
    public static final String CACHE_PLATFORM = "cps:platform";
    /** API供应商配置缓存名称 */
    public static final String CACHE_API_VENDOR = "cps:apiVendor";
    /** 返利配置缓存名称 */
    public static final String CACHE_REBATE_CONFIG = "cps:rebateConfig";
    /** 风控频率限制规则缓存名称 */
    public static final String CACHE_RISK_RATE_RULE = "cps:riskRateRule";

    @Bean("cpsCacheManager")
    public CacheManager cpsCacheManager(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        // 使用 Jackson 序列化，支持泛型和多态
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisSerializationContext.SerializationPair<Object> serializationPair =
                RedisSerializationContext.SerializationPair.fromSerializer(serializer);

        // 默认缓存配置（10分钟）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(serializationPair);

        return RedisCacheManager.builder(factory)
                // 平台配置缓存 30 分钟（变更频率低）
                .withCacheConfiguration(CACHE_PLATFORM, defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // API供应商配置缓存 30 分钟（变更频率低）
                .withCacheConfiguration(CACHE_API_VENDOR, defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // 返利配置缓存 10 分钟
                .withCacheConfiguration(CACHE_REBATE_CONFIG, defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // 风控频率规则缓存 5 分钟（需要相对及时更新）
                .withCacheConfiguration(CACHE_RISK_RATE_RULE, defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }

}
