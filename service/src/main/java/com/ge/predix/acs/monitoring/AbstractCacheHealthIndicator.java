package com.ge.predix.acs.monitoring;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.util.Assert;

public abstract class AbstractCacheHealthIndicator implements CacheHealthIndicator {

    static final String DESCRIPTION = "Health check performed by attempting to invoke the Redis INFO command";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheHealthIndicator.class);

    private RedisConnectionFactory redisConnectionFactory;

    @Value("${ENABLE_CACHING:false}")
    private boolean cachingEnabled;

    @Value("${ENABLED_REDIS_HEALTH_CHECK:false}")
    private boolean healthCheckEnabled;

    private String cacheType;

    public AbstractCacheHealthIndicator(final RedisConnectionFactory redisConnectionFactory, final String cacheType) {
        Assert.notNull(redisConnectionFactory, "ConnectionFactory must not be null");
        this.redisConnectionFactory = redisConnectionFactory;
        this.cacheType = cacheType;
    }

    @Override
    public Health health() {
        if (!this.cachingEnabled) {
            return AcsMonitoringUtilities
                    .health(Status.UNKNOWN, AcsMonitoringUtilities.HealthCode.DISABLED, DESCRIPTION);
        }
        if (!this.healthCheckEnabled) {
            return AcsMonitoringUtilities
                    .health(Status.UNKNOWN, AcsMonitoringUtilities.HealthCode.HEALTH_CHECK_DISABLED, DESCRIPTION);
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

    public RedisConnection getRedisConnection() {
        return RedisConnectionUtils.getConnection(this.redisConnectionFactory);
    }
}
