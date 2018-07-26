/*******************************************************************************
 * Copyright 2018 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.monitoring

import org.mockito.Mockito
import org.springframework.boot.actuate.health.Status
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class CacheHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    @Throws(Exception::class)
    fun testHealth(
        cacheHealthIndicator: CacheHealthIndicator,
        status: Status,
        healthCode: HealthCode
    ) {
        Assert.assertEquals(cacheHealthIndicator.health().status, status)
        Assert.assertEquals(
            cacheHealthIndicator.health().details[DESCRIPTION_KEY],
            CACHE_DESCRIPTION
        )
        if (healthCode === HealthCode.AVAILABLE) {
            Assert.assertFalse(
                cacheHealthIndicator.health().details.containsKey(
                    CODE_KEY
                )
            )
        } else {
            Assert.assertEquals(
                cacheHealthIndicator.health().details[CODE_KEY],
                healthCode
            )
        }
    }

    @DataProvider
    @Throws(Exception::class)
    fun statuses(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                mockCacheWithUp(true, true, DecisionCacheHealthIndicator::class.java),
                Status.UP,
                HealthCode.AVAILABLE
            ),
            arrayOf(
                mockCacheWithUp(true, true, ResourceCacheHealthIndicator::class.java),
                Status.UP,
                HealthCode.AVAILABLE
            ),
            arrayOf(
                mockCacheWithUp(true, true, SubjectCacheHealthIndicator::class.java),
                Status.UP,
                HealthCode.AVAILABLE
            ),
            arrayOf(
                mockCacheWithUp(false, true, DecisionCacheHealthIndicator::class.java),
                Status.UNKNOWN,
                HealthCode.DISABLED
            ),
            arrayOf(
                mockCacheWithUp(false, true, ResourceCacheHealthIndicator::class.java),
                Status.UNKNOWN,
                HealthCode.DISABLED
            ),
            arrayOf(
                mockCacheWithUp(false, true, SubjectCacheHealthIndicator::class.java),
                Status.UNKNOWN,
                HealthCode.DISABLED
            ),
            arrayOf(
                mockCacheWithUp(true, false, DecisionCacheHealthIndicator::class.java),
                Status.UNKNOWN,
                HealthCode.HEALTH_CHECK_DISABLED
            ),
            arrayOf(
                mockCacheWithUp(true, false, ResourceCacheHealthIndicator::class.java),
                Status.UNKNOWN,
                HealthCode.HEALTH_CHECK_DISABLED
            ),
            arrayOf(
                mockCacheWithUp(true, false, SubjectCacheHealthIndicator::class.java),
                Status.UNKNOWN,
                HealthCode.HEALTH_CHECK_DISABLED
            ),
            arrayOf(
                mockCacheWithExceptionWhileGettingConnection(
                    RuntimeException(),
                    DecisionCacheHealthIndicator::class.java
                ), Status.DOWN, HealthCode.ERROR
            ),
            arrayOf(
                mockCacheWithExceptionWhileGettingConnection(
                    RuntimeException(),
                    ResourceCacheHealthIndicator::class.java
                ), Status.DOWN, HealthCode.ERROR
            ),
            arrayOf(
                mockCacheWithExceptionWhileGettingConnection(
                    RuntimeException(),
                    SubjectCacheHealthIndicator::class.java
                ), Status.DOWN, HealthCode.ERROR
            ),
            arrayOf(
                mockCacheWithExceptionWhileGettingInfo(
                    RuntimeException(),
                    DecisionCacheHealthIndicator::class.java
                ), Status.DOWN, HealthCode.ERROR
            ),
            arrayOf(
                mockCacheWithExceptionWhileGettingInfo(
                    RuntimeException(),
                    ResourceCacheHealthIndicator::class.java
                ), Status.DOWN, HealthCode.ERROR
            ),
            arrayOf(
                mockCacheWithExceptionWhileGettingInfo(RuntimeException(), SubjectCacheHealthIndicator::class.java),
                Status.DOWN,
                HealthCode.ERROR
            )
        )
    }

    @Throws(Exception::class)
    private fun mockCache(
        cachingEnabled: Boolean,
        healthCheckEnabled: Boolean,
        clazz: Class<*>
    ): CacheHealthIndicator {
        val redisConnectionFactory = Mockito.mock(RedisConnectionFactory::class.java)
        val cacheHealthIndicator = clazz
            .getConstructor(RedisConnectionFactory::class.java, Boolean::class.javaPrimitiveType)
            .newInstance(redisConnectionFactory, cachingEnabled) as CacheHealthIndicator
        ReflectionTestUtils.setField(cacheHealthIndicator, "healthCheckEnabled", healthCheckEnabled)
        return Mockito.spy(cacheHealthIndicator)
    }

    @Throws(Exception::class)
    private fun mockCache(clazz: Class<*>): CacheHealthIndicator {
        return mockCache(true, true, clazz)
    }

    @Throws(Exception::class)
    private fun mockCacheWithUp(
        cachingEnabled: Boolean,
        healthCheckEnabled: Boolean,
        clazz: Class<*>
    ): CacheHealthIndicator {
        val cacheHealthIndicator = mockCache(cachingEnabled, healthCheckEnabled, clazz)
        Mockito.doReturn(Mockito.mock(RedisConnection::class.java)).`when`(cacheHealthIndicator).redisConnection
        return cacheHealthIndicator
    }

    @Throws(Exception::class)
    private fun mockCacheWithExceptionWhileGettingConnection(
        e: Exception,
        clazz: Class<*>
    ): CacheHealthIndicator {
        val cacheHealthIndicator = mockCache(clazz)
        Mockito.doAnswer { _ -> throw e }.`when`(cacheHealthIndicator).redisConnection
        return cacheHealthIndicator
    }

    @Throws(Exception::class)
    private fun mockCacheWithExceptionWhileGettingInfo(
        e: Exception,
        clazz: Class<*>
    ): CacheHealthIndicator {
        val decisionCacheHealthIndicator = mockCache(clazz)
        val redisConnection = Mockito.mock(RedisConnection::class.java)
        Mockito.doReturn(redisConnection).`when`(decisionCacheHealthIndicator).redisConnection
        Mockito.doAnswer { _ -> throw e }.`when`(redisConnection).info()
        return decisionCacheHealthIndicator
    }
}
