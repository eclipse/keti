package com.ge.predix.acs.attribute.cache;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.ge.predix.acs.attribute.readers.CachedAttributes;

public class RedisAttributeCache extends AbstractAttributeCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisAttributeCache.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String zoneName;
    private BiFunction<String, String, String> getKey;
    private RedisTemplate<String, String> resourceCacheRedisTemplate;
    private long maxCachedIntervalMinutes;

    RedisAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final BiFunction<String, String, String> getKey,
            final RedisTemplate<String, String> resourceCacheRedisTemplate) {
        this.maxCachedIntervalMinutes = maxCachedIntervalMinutes;
        this.zoneName = zoneName;
        this.getKey = getKey;
        this.resourceCacheRedisTemplate = resourceCacheRedisTemplate;
    }

    @Override
    public void set(final String key, final CachedAttributes value) {
        String cachedValueString;
        try {
            cachedValueString = OBJECT_MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            LOGGER.error("Unable to write attributes to cache.", e);
            return;
        }
        this.resourceCacheRedisTemplate.opsForValue()
                .set(this.getKey.apply(this.zoneName, key), cachedValueString, this.maxCachedIntervalMinutes,
                        TimeUnit.MINUTES);
        LOGGER.trace("Set key '{}', to value '{}' in attribute cache", key, cachedValueString);
    }

    @Override
    public CachedAttributes get(final String key) {
        String cachedValueString = this.resourceCacheRedisTemplate.opsForValue()
                .get(this.getKey.apply(this.zoneName, key));
        LOGGER.trace("Got value '{}' from attribute cache", cachedValueString);
        if (StringUtils.isEmpty(cachedValueString)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(cachedValueString, CachedAttributes.class);
        } catch (IOException e) {
            LOGGER.error("Unable to read attributes from cache.", e);
            return null;
        }
    }

    @Override
    public void flushAll() {
        this.resourceCacheRedisTemplate.getConnectionFactory().getConnection().flushDb();
    }

}
