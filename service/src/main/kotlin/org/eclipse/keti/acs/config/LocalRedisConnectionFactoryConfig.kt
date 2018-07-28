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

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import redis.clients.jedis.JedisPoolConfig
import javax.annotation.PostConstruct

private val LOGGER = LoggerFactory.getLogger(LocalRedisConnectionFactoryConfig::class.java)

/**
 * DataSourceConfig used for all cloud profiles.
 *
 * @author acs-engineers@ge.com
 */
@Configuration
@Profile("redis")
class LocalRedisConnectionFactoryConfig {

    @Autowired
    private lateinit var environment: Environment

    private lateinit var decisionRedisProperties: LocalRedisProperties
    private lateinit var resourceRedisProperties: LocalRedisProperties
    private lateinit var subjectRedisProperties: LocalRedisProperties

    @PostConstruct
    private fun setupProperties() {
        this.decisionRedisProperties = LocalRedisProperties(this.environment, "DECISION")
        this.resourceRedisProperties = LocalRedisProperties(this.environment, "RESOURCE")
        this.subjectRedisProperties = LocalRedisProperties(this.environment, "SUBJECT")
    }

    @Bean(name = ["redisConnectionFactory", "decisionRedisConnectionFactory"])
    fun decisionRedisConnectionFactory(): RedisConnectionFactory {
        LOGGER.info("Successfully created Decision Redis connection factory.")
        return createJedisConnectionFactory(this.decisionRedisProperties)
    }

    @Bean(name = ["resourceRedisConnectionFactory"])
    fun resourceRedisConnectionFactory(): RedisConnectionFactory {
        LOGGER.info("Successfully created Resource Redis connection factory.")
        return createJedisConnectionFactory(this.resourceRedisProperties)
    }

    @Bean(name = ["subjectRedisConnectionFactory"])
    fun subjectRedisConnectionFactory(): RedisConnectionFactory {
        LOGGER.info("Successfully created Subject Redis connection factory.")
        return createJedisConnectionFactory(subjectRedisProperties)
    }

    private fun createJedisConnectionFactory(redisProperties: LocalRedisProperties): RedisConnectionFactory {
        val poolConfig = JedisPoolConfig()
        poolConfig.maxTotal = redisProperties.minActive
        poolConfig.minIdle = redisProperties.maxActive
        poolConfig.maxWaitMillis = redisProperties.maxWaitTime.toLong()
        poolConfig.testOnBorrow = false

        val connFactory = JedisConnectionFactory(poolConfig)
        connFactory.usePool = false
        connFactory.timeout = redisProperties.soTimeout
        connFactory.hostName = redisProperties.redisHost
        connFactory.port = redisProperties.redisPort
        return connFactory
    }
}
