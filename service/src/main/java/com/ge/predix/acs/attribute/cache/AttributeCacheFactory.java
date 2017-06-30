package com.ge.predix.acs.attribute.cache;

import java.util.function.BiFunction;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

// CHECKSTYLE:OFF: FinalClass
@Component
public class AttributeCacheFactory {

    @Value("${ENABLE_ATTRIBUTE_CACHING:false}")
    private boolean enableAttributeCaching;
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
        return createAttributeCache(maxCachedIntervalMinutes, zoneName, resourceCacheRedisTemplate,
                AbstractAttributeCache::resourceKey);
    }

    public AttributeCache createSubjectAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final RedisTemplate<String, String> subjectCacheRedisTemplate) {
        return createAttributeCache(maxCachedIntervalMinutes, zoneName, subjectCacheRedisTemplate,
                AbstractAttributeCache::subjectKey);
    }

    private AttributeCache createAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final RedisTemplate<String, String> cacheRedisTemplate, final BiFunction<String, String, String> getKey) {
        String[] profiles = environment.getActiveProfiles();

        if (!this.enableAttributeCaching) {
            return new NonCachingAttributeCache();
        }
        if (ArrayUtils.contains(profiles, "redis") || ArrayUtils.contains(profiles, "cloud-redis")) {
            return new RedisAttributeCache(maxCachedIntervalMinutes, zoneName, getKey, cacheRedisTemplate);
        }
        return new InMemoryAttributeCache(maxCachedIntervalMinutes, zoneName, getKey);
    }
}
// CHECKSTYLE:ON: FinalClass
