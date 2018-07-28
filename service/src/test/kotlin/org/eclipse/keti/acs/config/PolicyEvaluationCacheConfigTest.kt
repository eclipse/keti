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

package org.eclipse.keti.acs.config

import org.eclipse.keti.acs.policy.evaluation.cache.NonCachingPolicyEvaluationCache
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.policy.evaluation.cache.RedisPolicyEvaluationCache
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.core.env.Environment
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class PolicyEvaluationCacheConfigTest {

    @Mock
    private lateinit var mockEnvironment: Environment

    @InjectMocks
    private lateinit var policyEvaluationCacheConfig: PolicyEvaluationCacheConfig

    @BeforeClass
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testPolicyEvaluationCacheConfigDisabled() {
        setupEnvironment(null)
        assertThat<PolicyEvaluationCache>(
            policyEvaluationCacheConfig.cache(false),
            instanceOf<PolicyEvaluationCache>(NonCachingPolicyEvaluationCache::class.java)
        )
    }

    @Test
    fun testPolicyEvaluationCacheConfigRedis() {
        setupEnvironment("redis")
        assertThat<PolicyEvaluationCache>(
            policyEvaluationCacheConfig.cache(true),
            instanceOf<PolicyEvaluationCache>(RedisPolicyEvaluationCache::class.java)
        )
    }

    @Test
    fun testPolicyEvaluationCacheConfigCloudRedis() {
        setupEnvironment("cloud-redis")
        assertThat<PolicyEvaluationCache>(
            policyEvaluationCacheConfig.cache(true),
            instanceOf<PolicyEvaluationCache>(RedisPolicyEvaluationCache::class.java)
        )
    }

    private fun setupEnvironment(springProfileActive: String?) {
        val redisEnvironment = arrayOf(springProfileActive)
        Mockito.doReturn(redisEnvironment).`when`<Environment>(mockEnvironment).activeProfiles
    }
}
