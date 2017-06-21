/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/

package com.ge.predix.acs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import redis.clients.jedis.JedisPoolConfig;

/**
 * DataSourceConfig used for all cloud profiles.
 *
 * @author 212406427
 */
@Configuration
@Profile({ "redis" })
public class LocalRedisConnectionFactoryConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRedisConnectionFactoryConfig.class);

    @Value("${DECISION_REDIS_HOST:localhost}")
    private String decisionRedisHost;
    @Value("${DECISION_REDIS_PORT:6379}")
    private int decisionRedisPort;
    @Value("${DECISION_REDIS_MIN_ACTIVE:0}")
    private int decisionMinActive;
    @Value("${DECISION_REDIS_MAX_ACTIVE:100}")
    private int decisionMaxActive;
    @Value("${DECISION_REDIS_MAX_WAIT_TIME:2000}")
    private int decisionMaxWaitTime;
    @Value("${DECISION_REDIS_SOCKET_TIMEOUT:3000}")
    private int decisionSoTimeout;

    @Value("${RESOURCE_REDIS_HOST:localhost}")
    private String resourceRedisHost;
    @Value("${RESOURCE_REDIS_PORT:6379}")
    private int resourceRedisPort;
    @Value("${RESOURCE_REDIS_MIN_ACTIVE:0}")
    private int resourceMinActive;
    @Value("${RESOURCE_REDIS_MAX_ACTIVE:100}")
    private int resourceMaxActive;
    @Value("${RESOURCE_REDIS_MAX_WAIT_TIME:2000}")
    private int resourceMaxWaitTime;
    @Value("${RESOURCE_REDIS_SOCKET_TIMEOUT:3000}")
    private int resourceSoTimeout;

    @Value("${SUBJECT_REDIS_HOST:localhost}")
    private String subjectRedisHost;
    @Value("${SUBJECT_REDIS_PORT:6379}")
    private int subjectRedisPort;
    @Value("${SUBJECT_REDIS_MIN_ACTIVE:0}")
    private int subjectMinActive;
    @Value("${SUBJECT_REDIS_MAX_ACTIVE:100}")
    private int subjectMaxActive;
    @Value("${SUBJECT_REDIS_MAX_WAIT_TIME:2000}")
    private int subjectMaxWaitTime;
    @Value("${SUBJECT_REDIS_SOCKET_TIMEOUT:3000}")
    private int subjectSoTimeout;

    @Bean(name = { "redisConnectionFactory", "decisionRedisConnectionFactory" })
    public RedisConnectionFactory decisionRedisConnectionFactory() {
        LOGGER.info("Successfully created Decision Redis connection factory.");
        return createJedisConnectionFactory(this.decisionMinActive, this.decisionMaxActive, this.decisionMaxWaitTime,
                this.decisionSoTimeout, this.decisionRedisHost, this.decisionRedisPort);
    }

    @Bean(name = { "resourceRedisConnectionFactory" })
    public RedisConnectionFactory resourceRedisConnectionFactory() {
        LOGGER.info("Successfully created Resource Redis connection factory.");
        return createJedisConnectionFactory(this.resourceMinActive, this.resourceMaxActive, this.resourceMaxWaitTime,
                this.resourceSoTimeout, this.resourceRedisHost, this.resourceRedisPort);
    }

    @Bean(name = { "subjectRedisConnectionFactory" })
    public RedisConnectionFactory subjectRedisConnectionFactory() {
        LOGGER.info("Successfully created Subject Redis connection factory.");
        return createJedisConnectionFactory(this.subjectMinActive, this.subjectMaxActive, this.subjectMaxWaitTime,
                this.subjectSoTimeout, this.subjectRedisHost, this.subjectRedisPort);
    }

    private RedisConnectionFactory createJedisConnectionFactory(final int minActive, final int maxActive,
            final int maxWaitTime, final int soTimeout, final String redisHost, final int redisPort) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(minActive);
        poolConfig.setMinIdle(maxActive);
        poolConfig.setMaxWaitMillis(maxWaitTime);
        poolConfig.setTestOnBorrow(false);

        JedisConnectionFactory connFactory = new JedisConnectionFactory(poolConfig);
        connFactory.setUsePool(false);
        connFactory.setTimeout(soTimeout);
        connFactory.setHostName(redisHost);
        connFactory.setPort(redisPort);
        return connFactory;
    }

}
