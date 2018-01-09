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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.monitoring;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.util.Assert;

@Profile({ "cloud-redis", "redis" })
public abstract class AbstractCacheHealthIndicator implements CacheHealthIndicator {

    static final String DESCRIPTION = "Health check performed by attempting to invoke the Redis INFO command";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheHealthIndicator.class);

    private RedisConnectionFactory redisConnectionFactory;

    private boolean cachingEnabled;

    @Value("${ENABLED_REDIS_HEALTH_CHECK:false}")
    private boolean healthCheckEnabled;

    private String cacheType;

    public AbstractCacheHealthIndicator(final RedisConnectionFactory redisConnectionFactory, final String cacheType,
            final boolean cachingEnabled) {
        Assert.notNull(redisConnectionFactory, "ConnectionFactory must not be null");
        this.redisConnectionFactory = redisConnectionFactory;
        this.cacheType = cacheType;
        this.cachingEnabled = cachingEnabled;
    }

    @Override
    public Health health() {
        if (!this.healthCheckEnabled) {
            return AcsMonitoringUtilities
                    .health(Status.UNKNOWN, AcsMonitoringUtilities.HealthCode.HEALTH_CHECK_DISABLED, DESCRIPTION);
        }
        if (!this.cachingEnabled) {
            return AcsMonitoringUtilities
                    .health(Status.UNKNOWN, AcsMonitoringUtilities.HealthCode.DISABLED, DESCRIPTION);
        }
        return cacheHealth(redisConnectionFactory, this.cacheType, DESCRIPTION, this::getRedisConnection);
    }

    static Health cacheHealth(final RedisConnectionFactory connectionFactory, final String cacheType,
            final String description, final Supplier<RedisConnection> connectionSupplier) {
        AcsMonitoringUtilities.HealthCode healthCode;
        RedisConnection connection = null;

        try {
            LOGGER.debug("Checking {} cache status", cacheType);
            connection = connectionSupplier.get();
            connection.info();
            healthCode = AcsMonitoringUtilities.HealthCode.AVAILABLE;
        } catch (Exception e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.ERROR, LOGGER,
                    AcsMonitoringUtilities.ERROR_MESSAGE_FORMAT, e);
        } finally {
            RedisConnectionUtils.releaseConnection(connection, connectionFactory);
        }

        try {
            if (healthCode == AcsMonitoringUtilities.HealthCode.AVAILABLE) {
                return AcsMonitoringUtilities.health(Status.UP, healthCode, description);
            }
            return AcsMonitoringUtilities.health(Status.DOWN, healthCode, description);
        } catch (Exception e) {
            return AcsMonitoringUtilities.health(Status.DOWN, AcsMonitoringUtilities
                    .logError(AcsMonitoringUtilities.HealthCode.ERROR, LOGGER,
                            AcsMonitoringUtilities.ERROR_MESSAGE_FORMAT, e), description);
        }
    }

    @Override
    public RedisConnection getRedisConnection() {
        return RedisConnectionUtils.getConnection(this.redisConnectionFactory);
    }
}
