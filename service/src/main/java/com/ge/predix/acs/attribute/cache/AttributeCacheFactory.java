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

    @Value("${ENABLE_RESOURCE_CACHING:true}")
    private boolean resourceCachingEnabled;

    @Value("${ENABLE_SUBJECT_CACHING:true}")
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