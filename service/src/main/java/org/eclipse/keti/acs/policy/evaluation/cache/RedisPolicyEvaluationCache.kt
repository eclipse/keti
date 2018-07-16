/*******************************************************************************
 * Copyright 2017 General Electric Company
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

package org.eclipse.keti.acs.policy.evaluation.cache

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val LOGGER = LoggerFactory.getLogger(RedisPolicyEvaluationCache::class.java)

@Component
@Profile("cloud-redis", "redis")
class RedisPolicyEvaluationCache : AbstractPolicyEvaluationCache(), InitializingBean {

    @Value("\${CACHED_EVAL_TTL_SECONDS:600}")
    private val cachedEvalTimeToLiveSeconds: Long = 600

    @Autowired
    private lateinit var decisionCacheRedisTemplate: RedisTemplate<String, String>

    override fun afterPropertiesSet() {
        LOGGER.info("Starting Redis policy evaluation cache.")
        try {
            val pingResult = this.decisionCacheRedisTemplate.connectionFactory.connection.ping()
            LOGGER.info("Redis server ping: {}", pingResult)
        } catch (ex: RedisConnectionFailureException) {
            LOGGER.error("Redis server ping failed.", ex)
        }

    }

    override fun delete(key: String) {
        this.decisionCacheRedisTemplate.delete(key)
    }

    override fun delete(keys: Collection<String>) {
        this.decisionCacheRedisTemplate.delete(keys)
    }

    override fun flushAll() {
        this.decisionCacheRedisTemplate.connectionFactory.connection.flushAll()
    }

    override fun keys(key: String): Set<String> {
        return this.decisionCacheRedisTemplate.keys(key)
    }

    override fun multiGet(keys: List<String>): List<String?> {
        return this.decisionCacheRedisTemplate.opsForValue().multiGet(keys)
    }

    override fun multiSet(map: Map<String, String>) {
        this.decisionCacheRedisTemplate.opsForValue().multiSet(map)
    }

    override fun set(
        key: String,
        value: String
    ) {
        if (isPolicyEvalResultKey(key)) {
            this.decisionCacheRedisTemplate.opsForValue().set(
                key, value, this.cachedEvalTimeToLiveSeconds, TimeUnit.SECONDS
            )
        } else {
            this.decisionCacheRedisTemplate.opsForValue().set(key, value)
        }
    }

    override fun setIfNotExists(
        key: String,
        value: String
    ) {
        this.decisionCacheRedisTemplate.boundValueOps(key).setIfAbsent(value)
    }
}
