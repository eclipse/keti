package com.ge.predix.acs.monitoring;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@Profile({ "cloud-redis", "redis" })
// This class doesn't extend RedisHealthIndicator on purpose because we don't want to output Redis-specific properties
public class ResourceCacheHealthIndicator extends AbstractCacheHealthIndicator {

    public ResourceCacheHealthIndicator(final RedisConnectionFactory resourceRedisConnectionFactory) {
        super(resourceRedisConnectionFactory, "resource");
    }
}
