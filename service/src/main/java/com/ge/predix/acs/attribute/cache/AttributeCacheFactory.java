package com.ge.predix.acs.attribute.cache;

import java.util.function.BiFunction;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AttributeCacheFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeCacheFactory.class);

    @Value("${ENABLE_RESOURCE_CACHING:false}")
    private boolean resourceCachingEnabled;

    @Value("${ENABLE_SUBJECT_CACHING:false}")
    private boolean subjectCachingEnabled;

    private Environment environment;

    @Autowired
    public AttributeCacheFactory(final Environment environment) {
        this.environment = environment;
    }

    private AttributeCacheFactory() {
        throw new AssertionError();
    }

    public AttributeCache createResourceAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final RedisTemplate<String, String> resourceCacheRedisTemplate) {
        return createAttributeCache(AttributeCache.RESOURCE, maxCachedIntervalMinutes, zoneName,
                resourceCacheRedisTemplate, AbstractAttributeCache::resourceKey, resourceCachingEnabled);
    }

    public AttributeCache createSubjectAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final RedisTemplate<String, String> subjectCacheRedisTemplate) {
        return createAttributeCache(AttributeCache.SUBJECT, maxCachedIntervalMinutes, zoneName,
                subjectCacheRedisTemplate, AbstractAttributeCache::subjectKey, subjectCachingEnabled);
    }

    private AttributeCache createAttributeCache(final String cacheType, final long maxCachedIntervalMinutes,
            final String zoneName, final RedisTemplate<String, String> cacheRedisTemplate,
            final BiFunction<String, String, String> getKey, final boolean enableAttributeCaching) {
        String[] profiles = environment.getActiveProfiles();

        if (!enableAttributeCaching) {
            LOGGER.info("Caching disabled for {} attributes.", cacheType);
            return new NonCachingAttributeCache();
        }
        if (ArrayUtils.contains(profiles, "redis") || ArrayUtils.contains(profiles, "cloud-redis")) {
            LOGGER.info("Redis caching enabled for {} attributes.", cacheType);
            return new RedisAttributeCache(maxCachedIntervalMinutes, zoneName, getKey, cacheRedisTemplate);
        }
        LOGGER.info("In-memory caching enabled for {} attributes.", cacheType);
        return new InMemoryAttributeCache(maxCachedIntervalMinutes, zoneName, getKey);
    }
}