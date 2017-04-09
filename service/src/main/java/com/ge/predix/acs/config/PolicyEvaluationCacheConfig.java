package com.ge.predix.acs.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.ge.predix.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.NonCachingPolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.RedisPolicyEvaluationCache;

@Configuration
public class PolicyEvaluationCacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEvaluationCacheConfig.class);

    @Value("${ENABLE_CACHING:false}")
    private boolean cachingEnabled;

    @Autowired
    private Environment environment;

    @Bean
    public PolicyEvaluationCache cache() {
        if (!this.cachingEnabled) {
            LOGGER.info("Caching disabled for policy evaluation");
            return new NonCachingPolicyEvaluationCache();
        }
        List<String> activeProfiles = Arrays.asList(this.environment.getActiveProfiles());
        if (activeProfiles.contains("redis") || activeProfiles.contains("cloud-redis")) {
            LOGGER.info("Redis caching enabled for policy evaluation.");
            return new RedisPolicyEvaluationCache();
        }
        LOGGER.info("In-memory caching enabled for policy evaluation.");
        return new InMemoryPolicyEvaluationCache();
    }
}
