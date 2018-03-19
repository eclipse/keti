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
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.common.RedisServiceInfo;
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
@Profile({ "cloud-redis" })
public class CloudRedisConnectionFactoryConfig extends AbstractCloudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudRedisConnectionFactoryConfig.class);

    @Autowired
    private Environment environment;

    private CloudRedisProperties decisionRedisProperties;
    private CloudRedisProperties resourceRedisProperties;
    private CloudRedisProperties subjectRedisProperties;

    @PostConstruct
    private void setupProperties() {
        this.decisionRedisProperties = new CloudRedisProperties(this.environment, "DECISION");
        this.resourceRedisProperties = new CloudRedisProperties(this.environment, "RESOURCE");
        this.subjectRedisProperties = new CloudRedisProperties(this.environment, "SUBJECT");
    }

    @Bean(name = { "redisConnectionFactory", "decisionRedisConnectionFactory" })
    public RedisConnectionFactory decisionRedisConnectionFactory() {
        return createRedisConnectionFactory(this.decisionRedisProperties);
    }

    @Bean(name = { "resourceRedisConnectionFactory" })
    public RedisConnectionFactory resourceRedisConnectionFactory() {
        return createRedisConnectionFactory(this.resourceRedisProperties);
    }

    @Bean(name = { "subjectRedisConnectionFactory" })
    public RedisConnectionFactory subjectRedisConnectionFactory() {
        return createRedisConnectionFactory(this.subjectRedisProperties);
    }

    private RedisConnectionFactory createRedisConnectionFactory(final CloudRedisProperties redisProperties) {
        RedisConnectionFactory connFactory;
        if (redisProperties.useJedisFactory()) {
            connFactory = createJedisConnectionFactory(redisProperties);
        } else {
            connFactory = createSpringCloudConnectionFactory(redisProperties);
        }
        LOGGER.info("Successfully created cloud {} Redis connection factory.", redisProperties.getCacheName());
        return connFactory;
    }

    private RedisConnectionFactory createSpringCloudConnectionFactory(final CloudRedisProperties redisProperties) {
        return connectionFactory().redisConnectionFactory(redisProperties.getCacheName(),
                new PooledServiceConnectorConfig(
                        new PoolConfig(redisProperties.getMinActive(), redisProperties.getMaxActive(),
                                redisProperties.getMaxWaitTime())));
    }

    private JedisConnectionFactory createJedisConnectionFactory(final CloudRedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisProperties.getMaxActive());
        poolConfig.setMinIdle(redisProperties.getMinActive());
        poolConfig.setMaxWaitMillis(redisProperties.getMaxWaitTime());
        poolConfig.setTestOnBorrow(false);
        RedisServiceInfo redisServiceInfo = (RedisServiceInfo) cloud().getServiceInfo(redisProperties.getCacheName());
        JedisConnectionFactory connFactory = new JedisConnectionFactory(poolConfig);
        connFactory.setUsePool(true);
        connFactory.setTimeout(redisProperties.getSoTimeout());
        connFactory.setHostName(redisServiceInfo.getHost());
        connFactory.setPort(redisServiceInfo.getPort());
        connFactory.setPassword(redisServiceInfo.getPassword());
        return connFactory;
    }
}
