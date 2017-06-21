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

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.common.RedisServiceInfo;
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
@Profile({ "cloud-redis" })
public class CloudRedisConnectionFactoryConfig extends AbstractCloudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudRedisConnectionFactoryConfig.class);

    @Value("${DECISION_CACHE_NAME:acs-redis}")
    private String decisionCacheName;
    @Value("${DECISION_REDIS_MIN_ACTIVE:0}")
    private int decisionMinActive;
    @Value("${DECISION_REDIS_MAX_ACTIVE:100}")
    private int decisionMaxActive;
    @Value("${DECISION_REDIS_MAX_WAIT_TIME:2000}")
    private int decisionMaxWaitTime;
    @Value("${DECISION_REDIS_SOCKET_TIMEOUT:3000}")
    private int decisionSoTimeout;
    @Value("${DECISION_REDIS_USE_JEDIS_FACTORY:false}")
    private boolean decisionUseJedisFactory;

    @Value("${RESOURCE_CACHE_NAME:acs-resource-redis}")
    private String resourceCacheName;
    @Value("${RESOURCE_REDIS_MIN_ACTIVE:0}")
    private int resourceMinActive;
    @Value("${RESOURCE_REDIS_MAX_ACTIVE:100}")
    private int resourceMaxActive;
    @Value("${RESOURCE_REDIS_MAX_WAIT_TIME:2000}")
    private int resourceMaxWaitTime;
    @Value("${RESOURCE_REDIS_SOCKET_TIMEOUT:3000}")
    private int resourceSoTimeout;
    @Value("${RESOURCE_REDIS_USE_JEDIS_FACTORY:false}")
    private boolean resourceUseJedisFactory;

    @Value("${SUBJECT_CACHE_NAME:acs-subject-redis}")
    private String subjectCacheName;
    @Value("${SUBJECT_REDIS_MIN_ACTIVE:0}")
    private int subjectMinActive;
    @Value("${SUBJECT_REDIS_MAX_ACTIVE:100}")
    private int subjectMaxActive;
    @Value("${SUBJECT_REDIS_MAX_WAIT_TIME:2000}")
    private int subjectMaxWaitTime;
    @Value("${SUBJECT_REDIS_SOCKET_TIMEOUT:3000}")
    private int subjectSoTimeout;
    @Value("${SUBJECT_REDIS_USE_JEDIS_FACTORY:false}")
    private boolean subjectUseJedisFactory;

    @Bean(name = { "redisConnectionFactory", "decisionRedisConnectionFactory" })
    public RedisConnectionFactory decisionRedisConnectionFactory() {
        return createRedisConnectionFactory(this.decisionUseJedisFactory, this::createDecisionJedisConnectionFactory,
                this::createDecisionSpringCloudConnectionFactory, "Decision");
    }

    @Bean(name = { "resourceRedisConnectionFactory" })
    public RedisConnectionFactory resourceRedisConnectionFactory() {
        return createRedisConnectionFactory(this.resourceUseJedisFactory, this::createResourceJedisConnectionFactory,
                this::createResourceSpringCloudConnectionFactory, "Resource");
    }

    @Bean(name = { "subjectRedisConnectionFactory" })
    public RedisConnectionFactory subjectRedisConnectionFactory() {
        return createRedisConnectionFactory(this.subjectUseJedisFactory, this::createSubjectJedisConnectionFactory,
                this::createSubjectSpringCloudConnectionFactory, "Subject");
    }

    private RedisConnectionFactory createDecisionSpringCloudConnectionFactory() {
        return createSpringCloudConnectionFactory(this.decisionMinActive, this.decisionMaxActive,
                this.decisionMaxWaitTime, this.decisionCacheName);
    }

    private RedisConnectionFactory createResourceSpringCloudConnectionFactory() {
        return createSpringCloudConnectionFactory(this.resourceMinActive, this.resourceMaxActive,
                this.resourceMaxWaitTime, this.resourceCacheName);
    }

    private RedisConnectionFactory createSubjectSpringCloudConnectionFactory() {
        return createSpringCloudConnectionFactory(this.subjectMinActive, this.subjectMaxActive, this.subjectMaxWaitTime,
                this.subjectCacheName);
    }

    private RedisConnectionFactory createDecisionJedisConnectionFactory() {
        return createJedisConnectionFactory(this.decisionMaxActive, this.decisionMinActive, this.decisionMaxWaitTime,
                this.decisionCacheName, this.decisionSoTimeout);
    }

    private RedisConnectionFactory createResourceJedisConnectionFactory() {
        return createJedisConnectionFactory(this.resourceMaxActive, this.resourceMinActive, this.resourceMaxWaitTime,
                this.resourceCacheName, this.resourceSoTimeout);
    }

    private RedisConnectionFactory createSubjectJedisConnectionFactory() {
        return createJedisConnectionFactory(this.subjectMaxActive, this.subjectMinActive, this.subjectMaxWaitTime,
                this.subjectCacheName, this.subjectSoTimeout);
    }

    private RedisConnectionFactory createRedisConnectionFactory(final boolean useJedis,
            final Supplier<RedisConnectionFactory> jedisConnectionFactory,
            final Supplier<RedisConnectionFactory> springCloudConnectionFactory, final String cacheName) {
        RedisConnectionFactory connFactory;
        if (useJedis) {
            connFactory = jedisConnectionFactory.get();
        } else {
            connFactory = springCloudConnectionFactory.get();
        }
        LOGGER.info("Successfully created cloud {} Redis connection factory.", cacheName);
        return connFactory;
    }

    private RedisConnectionFactory createSpringCloudConnectionFactory(final int minActive, final int maxActive,
            final int maxWaitTime, final String cacheName) {
        return connectionFactory().redisConnectionFactory(cacheName,
                new PooledServiceConnectorConfig(new PoolConfig(minActive, maxActive, maxWaitTime)));
    }

    private JedisConnectionFactory createJedisConnectionFactory(final int maxActive, final int minActive,
            final int maxWaitTime, final String cacheName, final int soTimeout) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMinIdle(minActive);
        poolConfig.setMaxWaitMillis(maxWaitTime);
        poolConfig.setTestOnBorrow(false);
        RedisServiceInfo redisServiceInfo = (RedisServiceInfo) cloud().getServiceInfo(cacheName);
        JedisConnectionFactory connFactory = new JedisConnectionFactory(poolConfig);
        connFactory.setUsePool(true);
        connFactory.setTimeout(soTimeout);
        connFactory.setHostName(redisServiceInfo.getHost());
        connFactory.setPort(redisServiceInfo.getPort());
        connFactory.setPassword(redisServiceInfo.getPassword());
        return connFactory;
    }
}
