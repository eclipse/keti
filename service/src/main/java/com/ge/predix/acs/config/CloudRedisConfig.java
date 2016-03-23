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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * DataSourceConfig used for all cloud profiles.
 *
 * @author 212406427
 */
@Configuration
@Profile({ "cloud-redis" })
@EnableAutoConfiguration
public class CloudRedisConfig extends AbstractCloudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudRedisConfig.class);

    @Value("${ACS_REDIS:acs-redis}")
    private String acsRedis;

    @Value("${MIN_ACTIVE:0}")
    private int minActive;
    @Value("${MAX_ACTIVE:100}")
    private int maxActive;
    @Value("${MAX_WAIT_TIME:30000}")
    private int maxWaitTime;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        PoolConfig poolConfig = new PoolConfig(this.minActive, this.maxActive, this.maxWaitTime);
        RedisConnectionFactory connFactory = connectionFactory().redisConnectionFactory(this.acsRedis,
                new PooledServiceConnectorConfig(poolConfig));
        LOGGER.info("Successfully created Redis connection factory.");
        return connFactory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        LOGGER.info("Successfully created Redis template.");
        return redisTemplate;
    }
}
