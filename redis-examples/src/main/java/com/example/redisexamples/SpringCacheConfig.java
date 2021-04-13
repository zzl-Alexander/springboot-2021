package com.example.redisexamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class SpringCacheConfig {
    // 按默认jdk序列化对象。未声明全局配置，缓存数据永不过期
    /*@Bean
    public CacheManager cacheManager(RedisConnectionFactory cf) {
        // 全局配置
        RedisCacheConfiguration configG = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(120L));
        // 独立配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60L));
        RedisCacheManager manager = RedisCacheManager.builder(cf)
                .cacheDefaults(configG)
                .withCacheConfiguration("user", config)
                .build();
        return manager;
    }*/
    // -----------------------------------
    // 持基于jackson的序列化，以及自定义缓存策略
    @Bean
    public CacheManager cacheManager(Jackson2JsonRedisSerializer<Object> serializer,
                                     RedisConnectionFactory cf) {
        // 全局配置
        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(4))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(serializer));
        // 独立的缓存配置
        RedisCacheConfiguration userR = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(serializer));
        // 基于全局配置/独立配置，创建redis缓存管理器
        RedisCacheManager manager = RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withCacheConfiguration("user", userR)
                .build();
        return manager;
    }

    // 全局jackson-redis序列化配置。可直接注入到容器覆盖默认配置
    @Bean
    public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        // 创建jackson配置，对象
        ObjectMapper mapper = new ObjectMapper();
        // 序列化时，忽略空值属性。
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 反序列化时，忽略不存在属性。即，缓存中有而类中没有的属性，不会异常
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 序列化时，同时记录类型的全限定性类名。否则按Map反序列化
        PolymorphicTypeValidator ptv =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        return serializer;
    }
    // ------------------------------------
    @Bean("cum")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        serializer.setObjectMapper(objectMapper);
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(serializer);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }

    // ---------------------------------------------

    // 也可自定义RedisTemplate注入
    /*@Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        PolymorphicTypeValidator ptv =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(objectMapper);
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(serializer);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }*/

    /*@Bean
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate,
                                     RedisConnectionFactory cf) {
        // 全局配置
        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(4))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(redisTemplate.getValueSerializer()));
        // 独立的缓存配置
        RedisCacheConfiguration userR = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(redisTemplate.getValueSerializer()));
        // 基于全局配置/独立配置，创建redis缓存管理器
        RedisCacheManager manager = RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withCacheConfiguration("user", userR)
                .build();
        return manager;
    }*/
    // -------------------------------

}
