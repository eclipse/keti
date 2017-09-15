/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *******************************************************************************/

package com.ge.predix.acs.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
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

    @Autowired
    private Environment environment;

    private LocalRedisProperties decisionRedisProperties;
    private LocalRedisProperties resourceRedisProperties;
    private LocalRedisProperties subjectRedisProperties;

    @PostConstruct
    private void setupProperties() {
        this.decisionRedisProperties = new LocalRedisProperties(this.environment, "DECISION");
        this.resourceRedisProperties = new LocalRedisProperties(this.environment, "RESOURCE");
        this.subjectRedisProperties = new LocalRedisProperties(this.environment, "SUBJECT");
    }

    @Bean(name = { "redisConnectionFactory", "decisionRedisConnectionFactory" })
    public RedisConnectionFactory decisionRedisConnectionFactory() {
        LOGGER.info("Successfully created Decision Redis connection factory.");
        return createJedisConnectionFactory(this.decisionRedisProperties);
    }

    @Bean(name = { "resourceRedisConnectionFactory" })
    public RedisConnectionFactory resourceRedisConnectionFactory() {
        LOGGER.info("Successfully created Resource Redis connection factory.");
        return createJedisConnectionFactory(this.resourceRedisProperties);
    }

    @Bean(name = { "subjectRedisConnectionFactory" })
    public RedisConnectionFactory subjectRedisConnectionFactory() {
        LOGGER.info("Successfully created Subject Redis connection factory.");
        return createJedisConnectionFactory(subjectRedisProperties);
    }

    private RedisConnectionFactory createJedisConnectionFactory(final LocalRedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisProperties.getMinActive());
        poolConfig.setMinIdle(redisProperties.getMaxActive());
        poolConfig.setMaxWaitMillis(redisProperties.getMaxWaitTime());
        poolConfig.setTestOnBorrow(false);

        JedisConnectionFactory connFactory = new JedisConnectionFactory(poolConfig);
        connFactory.setUsePool(false);
        connFactory.setTimeout(redisProperties.getSoTimeout());
        connFactory.setHostName(redisProperties.getRedisHost());
        connFactory.setPort(redisProperties.getRedisPort());
        return connFactory;
    }

}
