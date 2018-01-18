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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.policy.evaluation.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({ "cloud-redis", "redis" })
public class RedisPolicyEvaluationCache extends AbstractPolicyEvaluationCache implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPolicyEvaluationCache.class);

    @Value("${CACHED_EVAL_TTL_SECONDS:600}")
    private long cachedEvalTimeToLiveSeconds;

    @Autowired
    private RedisTemplate<String, String> decisionCacheRedisTemplate;

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("Starting Redis policy evaluation cache.");
        try {
            String pingResult = this.decisionCacheRedisTemplate.getConnectionFactory().getConnection().ping();
            LOGGER.info("Redis server ping: {}", pingResult);
        } catch (RedisConnectionFailureException ex) {
            LOGGER.error("Redis server ping failed.", ex);
        }
    }

    @Override
    void delete(final String key) {
        this.decisionCacheRedisTemplate.delete(key);
    }

    @Override
    void delete(final Collection<String> keys) {
        this.decisionCacheRedisTemplate.delete(keys);
    }

    @Override
    void flushAll() {
        this.decisionCacheRedisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Override
    Set<String> keys(final String key) {
        return this.decisionCacheRedisTemplate.keys(key);
    }

    @Override
    List<String> multiGet(final List<String> keys) {
        return this.decisionCacheRedisTemplate.opsForValue().multiGet(keys);
    }

    void multiSet(final Map<String, String> map) {
        this.decisionCacheRedisTemplate.opsForValue().multiSet(map);
    }

    @Override
    void set(final String key, final String value) {
        if (isPolicyEvalResultKey(key)) {
            this.decisionCacheRedisTemplate.opsForValue().set(key, value, this.cachedEvalTimeToLiveSeconds,
                    TimeUnit.SECONDS);
        } else {
            this.decisionCacheRedisTemplate.opsForValue().set(key, value);
        }
    }

    public void setCachedEvalTimeToLiveSeconds(final long cachedEvalTimeToLiveSeconds) {
        this.cachedEvalTimeToLiveSeconds = cachedEvalTimeToLiveSeconds;
    }

    void setIfNotExists(final String key, final String value) {
        this.decisionCacheRedisTemplate.boundValueOps(key).setIfAbsent(value);
    }
}
