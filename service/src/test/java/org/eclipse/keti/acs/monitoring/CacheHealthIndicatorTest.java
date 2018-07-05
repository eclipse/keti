/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.monitoring;

import static org.eclipse.keti.acs.monitoring.AbstractCacheHealthIndicatorKt.CACHE_DESCRIPTION;
import static org.eclipse.keti.acs.monitoring.AcsMonitoringUtilitiesKt.CODE_KEY;
import static org.eclipse.keti.acs.monitoring.AcsMonitoringUtilitiesKt.DESCRIPTION_KEY;

import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CacheHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    public void testHealth(final CacheHealthIndicator cacheHealthIndicator, final Status status,
            final HealthCode healthCode) throws Exception {
        Assert.assertEquals(cacheHealthIndicator.health().getStatus(), status);
        Assert.assertEquals(cacheHealthIndicator.health().getDetails().get(
                DESCRIPTION_KEY),
                            CACHE_DESCRIPTION);
        if (healthCode == HealthCode.AVAILABLE) {
            Assert.assertFalse(cacheHealthIndicator.health().getDetails().containsKey(
                    CODE_KEY));
        } else {
            Assert.assertEquals(cacheHealthIndicator.health().getDetails().get(
                    CODE_KEY),
                    healthCode);
        }
    }

    @DataProvider
    public Object[][] statuses() throws Exception {
        return new Object[][] {
                new Object[] { mockCacheWithUp(true, true, DecisionCacheHealthIndicator.class), Status.UP,
                        HealthCode.AVAILABLE },
                { mockCacheWithUp(true, true, ResourceCacheHealthIndicator.class), Status.UP,
                        HealthCode.AVAILABLE },
                { mockCacheWithUp(true, true, SubjectCacheHealthIndicator.class), Status.UP,
                        HealthCode.AVAILABLE },
                { mockCacheWithUp(false, true, DecisionCacheHealthIndicator.class), Status.UNKNOWN,
                        HealthCode.DISABLED },
                { mockCacheWithUp(false, true, ResourceCacheHealthIndicator.class), Status.UNKNOWN,
                        HealthCode.DISABLED },
                { mockCacheWithUp(false, true, SubjectCacheHealthIndicator.class), Status.UNKNOWN,
                        HealthCode.DISABLED },
                { mockCacheWithUp(true, false, DecisionCacheHealthIndicator.class), Status.UNKNOWN,
                        HealthCode.HEALTH_CHECK_DISABLED },
                { mockCacheWithUp(true, false, ResourceCacheHealthIndicator.class), Status.UNKNOWN,
                        HealthCode.HEALTH_CHECK_DISABLED },
                { mockCacheWithUp(true, false, SubjectCacheHealthIndicator.class), Status.UNKNOWN,
                        HealthCode.HEALTH_CHECK_DISABLED },
                { mockCacheWithExceptionWhileGettingConnection(new RuntimeException(),
                        DecisionCacheHealthIndicator.class), Status.DOWN,
                        HealthCode.ERROR },
                { mockCacheWithExceptionWhileGettingConnection(new RuntimeException(),
                        ResourceCacheHealthIndicator.class), Status.DOWN, HealthCode.ERROR },
                { mockCacheWithExceptionWhileGettingConnection(new RuntimeException(),
                        SubjectCacheHealthIndicator.class), Status.DOWN, HealthCode.ERROR },
                { mockCacheWithExceptionWhileGettingInfo(new RuntimeException(), DecisionCacheHealthIndicator.class),
                        Status.DOWN, HealthCode.ERROR },
                { mockCacheWithExceptionWhileGettingInfo(new RuntimeException(), ResourceCacheHealthIndicator.class),
                        Status.DOWN, HealthCode.ERROR },
                { mockCacheWithExceptionWhileGettingInfo(new RuntimeException(), SubjectCacheHealthIndicator.class),
                        Status.DOWN, HealthCode.ERROR } };
    }

    @SuppressWarnings("unchecked")
    private static CacheHealthIndicator mockCache(final boolean cachingEnabled, final boolean healthCheckEnabled,
            final Class clazz) throws Exception {
        RedisConnectionFactory redisConnectionFactory = Mockito.mock(RedisConnectionFactory.class);
        CacheHealthIndicator cacheHealthIndicator = (CacheHealthIndicator) clazz
                .getConstructor(RedisConnectionFactory.class, boolean.class)
                .newInstance(redisConnectionFactory, cachingEnabled);
        ReflectionTestUtils.setField(cacheHealthIndicator, "healthCheckEnabled", healthCheckEnabled);
        return Mockito.spy(cacheHealthIndicator);
    }

    private static CacheHealthIndicator mockCache(final Class clazz) throws Exception {
            return mockCache(true, true, clazz);
    }

    private CacheHealthIndicator mockCacheWithUp(final boolean cachingEnabled, final boolean healthCheckEnabled,
            final Class clazz) throws Exception {
        CacheHealthIndicator cacheHealthIndicator = mockCache(cachingEnabled, healthCheckEnabled, clazz);
        Mockito.doReturn(Mockito.mock(RedisConnection.class)).when(cacheHealthIndicator).getRedisConnection();
        return cacheHealthIndicator;
    }


    private CacheHealthIndicator mockCacheWithExceptionWhileGettingConnection(final Exception e, final Class clazz)
            throws Exception {
        CacheHealthIndicator cacheHealthIndicator = mockCache(clazz);
        Mockito.doAnswer(invocation -> {
            throw e;
        }).when(cacheHealthIndicator).getRedisConnection();
        return cacheHealthIndicator;
    }

    private CacheHealthIndicator mockCacheWithExceptionWhileGettingInfo(final Exception e, final Class clazz)
            throws Exception {
        CacheHealthIndicator decisionCacheHealthIndicator = mockCache(clazz);
        RedisConnection redisConnection = Mockito.mock(RedisConnection.class);
        Mockito.doReturn(redisConnection).when(decisionCacheHealthIndicator).getRedisConnection();
        Mockito.doAnswer(invocation -> {
            throw e;
        }).when(redisConnection).info();
        return decisionCacheHealthIndicator;
    }
}
