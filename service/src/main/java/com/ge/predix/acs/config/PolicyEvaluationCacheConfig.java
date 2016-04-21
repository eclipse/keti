package com.ge.predix.acs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ge.predix.acs.policy.evaluation.cache.HystrixPolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.BasicPolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCacheCircuitBreaker;

@Configuration
public class PolicyEvaluationCacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEvaluationCacheConfig.class);

    @Value("${ENABLE_CACHING:false}")
    private boolean cachingEnabled;

    @Value("${ENABLE_CIRCUIT_BREAKER:false}")
    private boolean hystrixCircuitBreakerEnabled;

    @Bean
    public PolicyEvaluationCacheCircuitBreaker cache() {
        if (!this.cachingEnabled) {
            LOGGER.info("Caching is disabled.");
            return new BasicPolicyEvaluationCacheCircuitBreaker();
        }
        if (this.hystrixCircuitBreakerEnabled) {
            LOGGER.info(
                    "Caching is enabled with HystrixPolicyEvaluationCacheCircuitBreaker circuit breaker"
                    + " implementation.");
            return new HystrixPolicyEvaluationCacheCircuitBreaker();
        }
        LOGGER.info(
                "Caching is enabled with BasicPolicyEvaluationCacheCircuitBreaker circuit breaker"
                + " implementation.");
        return new BasicPolicyEvaluationCacheCircuitBreaker();
    }
}
