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

import org.springframework.core.env.Environment;

public class CloudRedisProperties {

    private String cacheName;
    private int minActive;
    private int maxActive;
    private int maxWaitTime;
    private int soTimeout;
    private boolean useJedisFactory;

    public CloudRedisProperties(final Environment env, final String cacheType) {
        this.cacheName = env.getProperty(cacheType + "_CACHE_NAME", String.class, cacheType.toLowerCase() + "-redis");
        this.minActive = env.getProperty(cacheType + "_REDIS_MIN_ACTIVE", Integer.class, 0);
        this.maxActive = env.getProperty(cacheType + "_REDIS_MAX_ACTIVE", Integer.class, 100);
        this.maxWaitTime = env.getProperty(cacheType + "_REDIS_MAX_WAIT_TIME", Integer.class, 2000);
        this.soTimeout = env.getProperty(cacheType + "_REDIS_SOCKET_TIMEOUT", Integer.class, 3000);
        this.useJedisFactory = env.getProperty(cacheType + "_REDIS_USE_JEDIS_FACTORY", Boolean.class, false);
    }

    public String getCacheName() {
        return cacheName;
    }

    public int getMinActive() {
        return minActive;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public boolean useJedisFactory() {
        return useJedisFactory;
    }
}
