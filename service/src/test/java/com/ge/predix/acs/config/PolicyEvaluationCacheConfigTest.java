package com.ge.predix.acs.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.policy.evaluation.cache.NonCachingPolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.RedisPolicyEvaluationCache;

public class PolicyEvaluationCacheConfigTest {

    @Mock
    private Environment mockEnvironment;

    @InjectMocks
    private PolicyEvaluationCacheConfig policyEvaluationCacheConfig;

    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPolicyEvaluationCacheConfigDisabled() {
        setupEnvironment(null);
        assertThat(policyEvaluationCacheConfig.cache(false), instanceOf(NonCachingPolicyEvaluationCache.class));
    }

    @Test
    public void testPolicyEvaluationCacheConfigRedis() {
        setupEnvironment("redis");
        assertThat(policyEvaluationCacheConfig.cache(true), instanceOf(RedisPolicyEvaluationCache.class));
    }

    @Test
    public void testPolicyEvaluationCacheConfigCloudRedis() {
        setupEnvironment("cloud-redis");
        assertThat(policyEvaluationCacheConfig.cache(true), instanceOf(RedisPolicyEvaluationCache.class));
    }

    private void setupEnvironment(final String springProfileActive) {
        String[] redisEnvironment = {springProfileActive};
        Mockito.doReturn(redisEnvironment).when(mockEnvironment).getActiveProfiles();
    }

}
