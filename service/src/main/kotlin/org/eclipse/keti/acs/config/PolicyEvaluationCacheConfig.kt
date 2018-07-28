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

import org.eclipse.keti.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache
import org.eclipse.keti.acs.policy.evaluation.cache.NonCachingPolicyEvaluationCache
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.policy.evaluation.cache.RedisPolicyEvaluationCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

private val LOGGER = LoggerFactory.getLogger(PolicyEvaluationCacheConfig::class.java)

@Configuration
class PolicyEvaluationCacheConfig {

    @Autowired
    private lateinit var environment: Environment

    @Bean
    fun cache(
        @Value("\${ENABLE_DECISION_CACHING:true}")
        cachingEnabled: Boolean = true
    ): PolicyEvaluationCache {
        if (!cachingEnabled) {
            LOGGER.info("Caching disabled for policy evaluation")
            return NonCachingPolicyEvaluationCache()
        }
        val activeProfiles = listOf(*this.environment.activeProfiles)
        if (activeProfiles.contains("redis") || activeProfiles.contains("cloud-redis")) {
            LOGGER.info("Redis caching enabled for policy evaluation.")
            return RedisPolicyEvaluationCache()
        }
        LOGGER.info("In-memory caching enabled for policy evaluation.")
        return InMemoryPolicyEvaluationCache()
    }
}
