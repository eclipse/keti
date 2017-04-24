package com.ge.predix.acs.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Profile({ "cloud-redis", "redis" })
// This class doesn't extend RedisHealthIndicator on purpose because we don't want to output Redis-specific properties
public class DecisionCacheHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionCacheHealthIndicator.class);
    private static final String ERROR_MESSAGE_FORMAT = "Unexpected exception while checking decision cache status: {}";
    static final String DESCRIPTION = "Health check performed by attempting to invoke the Redis INFO command";

    @Value("${ENABLE_CACHING:false}")
    private boolean cachingEnabled;

    @Value("${ENABLED_REDIS_HEALTH_CHECK:false}")
    private boolean healthCheckEnabled;

    private final RedisConnectionFactory redisConnectionFactory;

    public DecisionCacheHealthIndicator(final RedisConnectionFactory connectionFactory) {
        Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
        this.redisConnectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        if (!this.cachingEnabled) {
            return AcsMonitoringUtilities.health(Status.UNKNOWN, AcsMonitoringUtilities.HealthCode.DISABLED,
                    DESCRIPTION);
        }
        if (!this.healthCheckEnabled) {
            return AcsMonitoringUtilities.health(Status.UNKNOWN,
                    AcsMonitoringUtilities.HealthCode.HEALTH_CHECK_DISABLED, DESCRIPTION);
        }
        return AcsMonitoringUtilities.health(this::check, DESCRIPTION);
    }

    RedisConnection getRedisConnection() {
        return RedisConnectionUtils.getConnection(this.redisConnectionFactory);
    }

    private AcsMonitoringUtilities.HealthCode check() {
        AcsMonitoringUtilities.HealthCode healthCode;

        RedisConnection connection = null;

        try {
            LOGGER.debug("Checking decision cache status");
            connection = getRedisConnection();
            connection.info();
            healthCode = AcsMonitoringUtilities.HealthCode.AVAILABLE;
        } catch (Exception e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.ERROR, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } finally {
            RedisConnectionUtils.releaseConnection(connection, this.redisConnectionFactory);
        }

        return healthCode;
    }
}
