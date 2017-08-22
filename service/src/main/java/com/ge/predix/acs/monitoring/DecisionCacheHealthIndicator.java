package com.ge.predix.acs.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;

@Component
@Profile({ "cloud-redis", "redis" })
// This class doesn't extend RedisHealthIndicator on purpose because we don't want to output Redis-specific properties
public class DecisionCacheHealthIndicator extends AbstractCacheHealthIndicator {

    @Autowired
    public DecisionCacheHealthIndicator(final RedisConnectionFactory decisionRedisConnectionFactory,
            @Value("${ENABLE_DECISION_CACHING:false}") final boolean cacheEnabled) {
        super(decisionRedisConnectionFactory, PolicyEvaluationCache.DECISION, cacheEnabled);
    }
}
