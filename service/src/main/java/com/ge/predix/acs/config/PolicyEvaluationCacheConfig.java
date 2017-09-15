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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.ge.predix.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.NonCachingPolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.RedisPolicyEvaluationCache;

@Configuration
public class PolicyEvaluationCacheConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEvaluationCacheConfig.class);

    @Autowired
    private Environment environment;

    @Bean
    public PolicyEvaluationCache cache(@Value("${ENABLE_DECISION_CACHING:true}") final boolean cachingEnabled) {
        if (!cachingEnabled) {
            LOGGER.info("Caching disabled for policy evaluation");
            return new NonCachingPolicyEvaluationCache();
        }
        List<String> activeProfiles = Arrays.asList(this.environment.getActiveProfiles());
        if (activeProfiles.contains("redis") || activeProfiles.contains("cloud-redis")) {
            LOGGER.info("Redis caching enabled for policy evaluation.");
            return new RedisPolicyEvaluationCache();
        }
        LOGGER.info("In-memory caching enabled for policy evaluation.");
        return new InMemoryPolicyEvaluationCache();
    }
}
