package com.ge.predix.acs.monitoring;

import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DecisionCacheHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    public void testHealth(final DecisionCacheHealthIndicator decisionCacheHealthIndicator, final Status status,
            final AcsMonitoringUtilities.HealthCode healthCode) throws Exception {
        Assert.assertEquals(status, decisionCacheHealthIndicator.health().getStatus());
        Assert.assertEquals(DecisionCacheHealthIndicator.DESCRIPTION,
                decisionCacheHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.DESCRIPTION_KEY));
        if (healthCode == AcsMonitoringUtilities.HealthCode.AVAILABLE) {
            Assert.assertFalse(
                    decisionCacheHealthIndicator.health().getDetails().containsKey(AcsMonitoringUtilities.CODE_KEY));
        } else {
            Assert.assertEquals(healthCode,
                    decisionCacheHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.CODE_KEY));
        }
    }

    @DataProvider
    public Object[][] statuses() {
        return new Object[][] {
                new Object[] { mockCacheWithUp(true, true), Status.UP, AcsMonitoringUtilities.HealthCode.AVAILABLE },

                { mockCacheWithUp(false, true), Status.UNKNOWN, AcsMonitoringUtilities.HealthCode.DISABLED },

                { mockCacheWithUp(true, false), Status.UNKNOWN,
                        AcsMonitoringUtilities.HealthCode.HEALTH_CHECK_DISABLED },

                { mockCacheWithExceptionWhileGettingConnection(new RuntimeException()), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.ERROR },

                { mockCacheWithExceptionWhileGettingInfo(new RuntimeException()), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.ERROR }, };
    }

    private static void setMockCacheInternalFields(final DecisionCacheHealthIndicator decisionCacheHealthIndicator,
            final boolean cachingEnabled, final boolean healthCheckEnabled) {
        ReflectionTestUtils.setField(decisionCacheHealthIndicator, "cachingEnabled", cachingEnabled);
        ReflectionTestUtils.setField(decisionCacheHealthIndicator, "healthCheckEnabled", healthCheckEnabled);
    }

    private static DecisionCacheHealthIndicator mockCache(final boolean cachingEnabled,
            final boolean healthCheckEnabled) {
        RedisConnectionFactory redisConnectionFactory = Mockito.mock(RedisConnectionFactory.class);
        DecisionCacheHealthIndicator decisionCacheHealthIndicator = new DecisionCacheHealthIndicator(
                redisConnectionFactory);
        setMockCacheInternalFields(decisionCacheHealthIndicator, cachingEnabled, healthCheckEnabled);
        return Mockito.spy(decisionCacheHealthIndicator);
    }

    private static DecisionCacheHealthIndicator mockCache() {
        return mockCache(true, true);
    }

    private DecisionCacheHealthIndicator mockCacheWithUp(final boolean cachingEnabled,
            final boolean healthCheckEnabled) {
        DecisionCacheHealthIndicator decisionCacheHealthIndicator = mockCache(cachingEnabled, healthCheckEnabled);
        Mockito.doReturn(Mockito.mock(RedisConnection.class)).when(decisionCacheHealthIndicator).getRedisConnection();
        return decisionCacheHealthIndicator;
    }

    private DecisionCacheHealthIndicator mockCacheWithExceptionWhileGettingConnection(final Exception e) {
        DecisionCacheHealthIndicator decisionCacheHealthIndicator = mockCache();
        Mockito.doThrow(e).when(decisionCacheHealthIndicator).getRedisConnection();
        return decisionCacheHealthIndicator;
    }

    private DecisionCacheHealthIndicator mockCacheWithExceptionWhileGettingInfo(final Exception e) {
        DecisionCacheHealthIndicator decisionCacheHealthIndicator = mockCache();
        RedisConnection redisConnection = Mockito.mock(RedisConnection.class);
        Mockito.doReturn(redisConnection).when(decisionCacheHealthIndicator).getRedisConnection();
        Mockito.doThrow(e).when(redisConnection).info();
        return decisionCacheHealthIndicator;
    }
}
