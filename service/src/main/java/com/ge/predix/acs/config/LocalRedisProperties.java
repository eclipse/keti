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

public class LocalRedisProperties {

    private Environment env;
    private String redisHost;
    private int redisPort;
    private int minActive;
    private int maxActive;
    private int maxWaitTime;
    private int soTimeout;

    public LocalRedisProperties(final Environment environment, final String cacheType) {
        this.env = environment;
        this.redisHost = this.env.getProperty(cacheType + "_REDIS_HOST", String.class, "localhost");
        this.redisPort = this.env.getProperty(cacheType + "_REDIS_PORT", Integer.class, 6379);
        this.minActive = this.env.getProperty(cacheType + "_REDIS_MIN_ACTIVE", Integer.class, 0);
        this.maxActive = this.env.getProperty(cacheType + "_REDIS_MAX_ACTIVE", Integer.class, 100);
        this.maxWaitTime = this.env.getProperty(cacheType + "_REDIS_MAX_WAIT_TIME", Integer.class, 2000);
        this.soTimeout = this.env.getProperty(cacheType + "_REDIS_SOCKET_TIMEOUT", Integer.class, 3000);
    }

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
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
}
