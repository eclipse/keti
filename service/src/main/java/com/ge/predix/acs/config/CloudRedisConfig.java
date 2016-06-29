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
import org.springframework.cloud.service.common.RedisServiceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

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

    @Value("${REDIS_MIN_ACTIVE:0}")
    private int minActive;
    @Value("${REDIS_MAX_ACTIVE:100}")
    private int maxActive;
    @Value("${REDIS_MAX_WAIT_TIME:2000}")
    private int maxWaitTime;

    @Value("${REDIS_SOCKET_TIMEOUT:3000}")
    private int soTimeout;

    @Value("${REDIS_USE_JEDIS_FACTORY:false}")
    private boolean useJedisFactory;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory connFactory;
        if (this.useJedisFactory) {
            connFactory = createJedisConnectionFactory();
        } else {
            connFactory = createSpringCloudConnectionFactory();
        }
        LOGGER.info("Successfully created cloud Redis connection factory.");
        return connFactory;
    }

    private RedisConnectionFactory createSpringCloudConnectionFactory() {
        PoolConfig poolConfig = new PoolConfig(this.minActive, this.maxActive, this.maxWaitTime);
        RedisConnectionFactory connFactory = connectionFactory().redisConnectionFactory(this.acsRedis,
                new PooledServiceConnectorConfig(poolConfig));
        return connFactory;
    }

    public RedisConnectionFactory createJedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(this.maxActive);
        poolConfig.setMinIdle(this.minActive);
        poolConfig.setMaxWaitMillis(this.maxWaitTime);
        poolConfig.setTestOnBorrow(false);

        RedisServiceInfo redisServiceInfo = (RedisServiceInfo) cloud().getServiceInfo(this.acsRedis);

        JedisConnectionFactory connFactory = new JedisConnectionFactory(poolConfig);
        connFactory.setUsePool(true);
        connFactory.setTimeout(this.soTimeout);
        connFactory.setHostName(redisServiceInfo.getHost());
        connFactory.setPort(redisServiceInfo.getPort());
        connFactory.setPassword(redisServiceInfo.getPassword());
        return connFactory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        LOGGER.info("Successfully created Redis template.");
        return redisTemplate;
    }
}
