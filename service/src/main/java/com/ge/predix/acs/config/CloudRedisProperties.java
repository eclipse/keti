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
