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

package org.eclipse.keti.acs.attribute.cache

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsInstanceOf.instanceOf

import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.core.env.Environment
import org.springframework.test.util.ReflectionTestUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class AttributeCacheFactoryTest {
    @Mock
    private lateinit var mockEnvironment: Environment

    @InjectMocks
    private lateinit var cacheFactory: AttributeCacheFactory

    @BeforeClass
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testAttributeCacheDisabled() {
        setupEnvironment(false, null)
        assertThat(
            cacheFactory.createResourceAttributeCache(180, "myzone", null),
            instanceOf(NonCachingAttributeCache::class.java)
        )
        assertThat(
            cacheFactory.createSubjectAttributeCache(180, "myzone", null),
            instanceOf(NonCachingAttributeCache::class.java)
        )
    }

    @Test
    fun testAttributeCacheRedis() {
        setupEnvironment(true, "redis")
        assertThat(
            cacheFactory.createResourceAttributeCache(180, "myzone", null),
            instanceOf(RedisAttributeCache::class.java)
        )
        assertThat(
            cacheFactory.createSubjectAttributeCache(180, "myzone", null),
            instanceOf(RedisAttributeCache::class.java)
        )
    }

    @Test
    fun testAttributeCacheCloudRedis() {
        setupEnvironment(true, "cloud-redis")
        assertThat(
            cacheFactory.createResourceAttributeCache(180, "myzone", null),
            instanceOf(RedisAttributeCache::class.java)
        )
        assertThat(
            cacheFactory.createSubjectAttributeCache(180, "myzone", null),
            instanceOf(RedisAttributeCache::class.java)
        )
    }

    @Test
    fun testAttributeCacheInMemoty() {
        setupEnvironment(true, null)
        assertThat(
            cacheFactory.createResourceAttributeCache(180, "myzone", null),
            instanceOf(InMemoryAttributeCache::class.java)
        )
        assertThat(
            cacheFactory.createSubjectAttributeCache(180, "myzone", null),
            instanceOf(InMemoryAttributeCache::class.java)
        )
    }

    private fun setupEnvironment(
        cachingEnabled: Boolean,
        springProfileActive: String?
    ) {
        ReflectionTestUtils.setField(cacheFactory, "resourceCachingEnabled", cachingEnabled)
        ReflectionTestUtils.setField(cacheFactory, "subjectCachingEnabled", cachingEnabled)
        val redisEnvironment = arrayOf(springProfileActive)
        Mockito.doReturn(redisEnvironment).`when`<Environment>(mockEnvironment).activeProfiles
    }
}
