package com.ge.predix.acs.attribute.cache;

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
        if (!this.enableAttributeCaching) {
            return new NonCachingAttributeCache();
        }
        if (ArrayUtils.contains(environment.getActiveProfiles(), "redis") || ArrayUtils
                .contains(environment.getActiveProfiles(), "cloud-redis")) {
            return new RedisAttributeCache(maxCachedIntervalMinutes, zoneName, AbstractAttributeCache::resourceKey,
                    resourceCacheRedisTemplate);
        }
        return new InMemoryAttributeCache(maxCachedIntervalMinutes, zoneName, AbstractAttributeCache::resourceKey);
    }

    public AttributeCache createSubjectAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final RedisTemplate<String, String> subjectCacheRedisTemplate) {
        if (!this.enableAttributeCaching) {
            return new NonCachingAttributeCache();
        }
        if (ArrayUtils.contains(environment.getActiveProfiles(), "redis") || ArrayUtils
                .contains(environment.getActiveProfiles(), "cloud-redis")) {
            return new RedisAttributeCache(maxCachedIntervalMinutes, zoneName, AbstractAttributeCache::subjectKey,
                    subjectCacheRedisTemplate);
        }
        return new InMemoryAttributeCache(maxCachedIntervalMinutes, zoneName, AbstractAttributeCache::subjectKey);
    }
}
// CHECKSTYLE:ON: FinalClass
