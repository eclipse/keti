/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
