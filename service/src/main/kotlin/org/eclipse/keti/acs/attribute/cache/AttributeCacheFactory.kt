/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.attribute.cache

import org.apache.commons.lang3.ArrayUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(AttributeCacheFactory::class.java)

@Component
class AttributeCacheFactory {

    @Value("\${ENABLE_RESOURCE_CACHING:true}")
    private var resourceCachingEnabled: Boolean = true

    @Value("\${ENABLE_SUBJECT_CACHING:true}")
    private var subjectCachingEnabled: Boolean = true

    private lateinit var environment: Environment

    @Autowired
    constructor(environment: Environment) {
        this.environment = environment
    }

    private constructor() {
        throw AssertionError()
    }

    fun createResourceAttributeCache(
        maxCachedIntervalMinutes: Long, zoneName: String,
        resourceCacheRedisTemplate: RedisTemplate<String, String>?
    ): AttributeCache {
        return createAttributeCache(RESOURCE,
            maxCachedIntervalMinutes,
            zoneName,
            resourceCacheRedisTemplate,
            { zoneId, identifier -> resourceKey(zoneId, identifier) },
            resourceCachingEnabled
        )
    }

    fun createSubjectAttributeCache(
        maxCachedIntervalMinutes: Long, zoneName: String,
        subjectCacheRedisTemplate: RedisTemplate<String, String>?
    ): AttributeCache {
        return createAttributeCache(SUBJECT,
            maxCachedIntervalMinutes,
            zoneName,
            subjectCacheRedisTemplate,
            { zoneId, identifier -> subjectKey(zoneId, identifier) },
            subjectCachingEnabled
        )
    }

    private fun createAttributeCache(
        cacheType: String, maxCachedIntervalMinutes: Long,
        zoneName: String, cacheRedisTemplate: RedisTemplate<String, String>?,
        getKey: (String, String) -> String, enableAttributeCaching: Boolean
    ): AttributeCache {
        val profiles = environment.activeProfiles

        if (!enableAttributeCaching) {
            LOGGER.info("Caching disabled for {} attributes.", cacheType)
            return NonCachingAttributeCache()
        }
        if (ArrayUtils.contains(profiles, "redis") || ArrayUtils.contains(profiles, "cloud-redis")) {
            LOGGER.info("Redis caching enabled for {} attributes.", cacheType)
            return RedisAttributeCache(maxCachedIntervalMinutes, zoneName, getKey, cacheRedisTemplate)
        }
        LOGGER.info("In-memory caching enabled for {} attributes.", cacheType)
        return InMemoryAttributeCache(maxCachedIntervalMinutes, zoneName, getKey)
    }
}
