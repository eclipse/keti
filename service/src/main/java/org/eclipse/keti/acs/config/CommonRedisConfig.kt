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

package org.eclipse.keti.acs.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

private val LOGGER = LoggerFactory.getLogger(CommonRedisConfig::class.java)

@Configuration
@Profile("cloud-redis", "redis")
class CommonRedisConfig @Autowired constructor(
    private val decisionRedisConnectionFactory: RedisConnectionFactory,
    private val resourceRedisConnectionFactory: RedisConnectionFactory,
    private val subjectRedisConnectionFactory: RedisConnectionFactory
) {

    @Bean(name = ["redisTemplate", "decisionCacheRedisTemplate"])
    fun decisionCacheRedisTemplate(): RedisTemplate<String, String> {
        return createCacheRedisTemplate(this.decisionRedisConnectionFactory, "Decision")

    }

    @Bean(name = ["resourceCacheRedisTemplate"])
    fun resourceCacheRedisTemplate(): RedisTemplate<String, String> {
        return createCacheRedisTemplate(this.resourceRedisConnectionFactory, "Resource")

    }

    @Bean(name = ["subjectCacheRedisTemplate"])
    fun subjectCacheRedisTemplate(): RedisTemplate<String, String> {
        return createCacheRedisTemplate(this.subjectRedisConnectionFactory, "Subject")
    }

    private fun createCacheRedisTemplate(
        redisConnectionFactory: RedisConnectionFactory,
        redisTemplateType: String
    ): RedisTemplate<String, String> {
        val redisTemplate = RedisTemplate<String, String>()
        redisTemplate.connectionFactory = redisConnectionFactory
        redisTemplate.defaultSerializer = StringRedisSerializer()
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.valueSerializer = StringRedisSerializer()
        LOGGER.info("Successfully created {} Redis template.", redisTemplateType)
        return redisTemplate
    }
}
