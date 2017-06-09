package com.ge.predix.acs.monitoring;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;

public interface CacheHealthIndicator extends HealthIndicator {

    RedisConnection getRedisConnection();
}
