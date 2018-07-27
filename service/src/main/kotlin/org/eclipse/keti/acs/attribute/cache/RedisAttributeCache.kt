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

import org.apache.commons.lang3.StringUtils
import org.codehaus.jackson.map.ObjectMapper
import org.eclipse.keti.acs.attribute.readers.CachedAttributes
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import java.io.IOException
import java.util.concurrent.TimeUnit

private val LOGGER = LoggerFactory.getLogger(RedisAttributeCache::class.java)
private val OBJECT_MAPPER = ObjectMapper()

class RedisAttributeCache internal constructor(
    private val maxCachedIntervalMinutes: Long,
    private val zoneName: String,
    private val getKey: (String, String) -> String,
    private val resourceCacheRedisTemplate: RedisTemplate<String, String>?
) : AbstractAttributeCache {

    override fun set(
        key: String,
        value: CachedAttributes
    ) {
        val cachedValueString: String
        try {
            cachedValueString = OBJECT_MAPPER.writeValueAsString(value)
        } catch (e: IOException) {
            LOGGER.error("Unable to write attributes to cache.", e)
            return
        }

        this.resourceCacheRedisTemplate?.opsForValue()
            ?.set(
                this.getKey(this.zoneName, key), cachedValueString, this.maxCachedIntervalMinutes,
                TimeUnit.MINUTES
            )
        LOGGER.trace("Set key '{}', to value '{}' in attribute cache", key, cachedValueString)
    }

    override fun get(key: String): CachedAttributes? {
        val cachedValueString = this.resourceCacheRedisTemplate?.opsForValue()
            ?.get(this.getKey(this.zoneName, key))
        LOGGER.trace("Got value '{}' from attribute cache", cachedValueString)
        if (StringUtils.isEmpty(cachedValueString)) {
            return null
        }
        return try {
            OBJECT_MAPPER.readValue<CachedAttributes>(cachedValueString, CachedAttributes::class.java)
        } catch (e: IOException) {
            LOGGER.error("Unable to read attributes from cache.", e)
            null
        }
    }

    override fun flushAll() {
        this.resourceCacheRedisTemplate?.connectionFactory?.connection?.flushDb()
    }
}
