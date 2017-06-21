package com.ge.predix.acs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile({ "cloud-redis", "redis" })
public class CommonRedisConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonRedisConfig.class);

    private RedisConnectionFactory decisionRedisConnectionFactory;
    private RedisConnectionFactory resourceRedisConnectionFactory;
    private RedisConnectionFactory subjectRedisConnectionFactory;

    @Autowired
    public CommonRedisConfig(final RedisConnectionFactory decisionRedisConnectionFactory,
            final RedisConnectionFactory resourceRedisConnectionFactory,
            final RedisConnectionFactory subjectRedisConnectionFactory) {
        this.decisionRedisConnectionFactory = decisionRedisConnectionFactory;
        this.resourceRedisConnectionFactory = resourceRedisConnectionFactory;
        this.subjectRedisConnectionFactory = subjectRedisConnectionFactory;
    }

    @Bean(name = { "redisTemplate", "decisionCacheRedisTemplate" })
    public RedisTemplate<String, String> decisionCacheRedisTemplate() {
        return createCacheRedisTemplate(this.decisionRedisConnectionFactory, "Decision");

    }

    @Bean(name = { "resourceCacheRedisTemplate" })
    public RedisTemplate<String, String> resourceCacheRedisTemplate() {
        return createCacheRedisTemplate(this.resourceRedisConnectionFactory, "Resource");

    }

    @Bean(name = { "subjectCacheRedisTemplate" })
    public RedisTemplate<String, String> subjectCacheRedisTemplate() {
        return createCacheRedisTemplate(this.subjectRedisConnectionFactory, "Subject");
    }

    private RedisTemplate<String, String> createCacheRedisTemplate(final RedisConnectionFactory redisConnectionFactory,
            final String redisTemplateType) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        LOGGER.info("Successfully created {} Redis template.", redisTemplateType);
        return redisTemplate;
    }
}
