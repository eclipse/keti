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

package org.eclipse.keti.acs.monitoring

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisConnectionUtils
import org.springframework.util.Assert

const val CACHE_DESCRIPTION = "Health check performed by attempting to invoke the Redis INFO command"
private val LOGGER = LoggerFactory.getLogger(AbstractCacheHealthIndicator::class.java)

internal fun cacheHealth(
    connectionFactory: RedisConnectionFactory,
    cacheType: String,
    description: String,
    connectionSupplier: () -> RedisConnection
): Health {
    var healthCode: HealthCode
    var connection: RedisConnection? = null

    try {
        LOGGER.debug("Checking {} cache status", cacheType)
        connection = connectionSupplier()
        connection.info()
        healthCode = HealthCode.AVAILABLE
    } catch (e: Exception) {
        healthCode = logError(
            HealthCode.ERROR, LOGGER,
            ERROR_MESSAGE_FORMAT, e
        )
    } finally {
        RedisConnectionUtils.releaseConnection(connection, connectionFactory)
    }

    return try {
        if (healthCode == HealthCode.AVAILABLE) {
            health(Status.UP, healthCode, description)
        } else health(Status.DOWN, healthCode, description)
    } catch (e: Exception) {
        health(
            Status.DOWN,
            logError(
                HealthCode.ERROR, LOGGER,
                ERROR_MESSAGE_FORMAT, e
            ),
            description
        )
    }
}

@Profile("cloud-redis", "redis")
abstract class AbstractCacheHealthIndicator(
    private val redisConnectionFactory: RedisConnectionFactory, private val cacheType: String,
    private val cachingEnabled: Boolean
) : CacheHealthIndicator {

    @Value("\${ENABLED_REDIS_HEALTH_CHECK:false}")
    private var healthCheckEnabled: Boolean = false

    override val redisConnection: RedisConnection
        get() = RedisConnectionUtils.getConnection(this.redisConnectionFactory)

    init {
        Assert.notNull(redisConnectionFactory, "ConnectionFactory must not be null")
    }

    override fun health(): Health {
        if (!this.healthCheckEnabled) {
            return health(Status.UNKNOWN, HealthCode.HEALTH_CHECK_DISABLED, CACHE_DESCRIPTION)
        }
        return if (!this.cachingEnabled) {
            health(Status.UNKNOWN, HealthCode.DISABLED, CACHE_DESCRIPTION)
        } else cacheHealth(
            connectionFactory = redisConnectionFactory,
            cacheType = this.cacheType,
            description = CACHE_DESCRIPTION,
            connectionSupplier = { this.redisConnection }
        )
    }
}
