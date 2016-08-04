package com.ge.predix.acs.policy.evaluation.cache;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

public class HystrixPolicyEvaluationCacheCircuitBreaker implements PolicyEvaluationCacheCircuitBreaker {
    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixPolicyEvaluationCacheCircuitBreaker.class);

    @Autowired
    private PolicyEvaluationCache cacheImpl;
    @Value(value = "${ENABLE_CACHING:false}")
    private boolean cachingEnabled;

    @HystrixCommand(fallbackMethod = "nopGet")
    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey key) {
        if (this.cachingEnabled) {
            return this.cacheImpl.get(key);
        }
        return null;
    }

    public PolicyEvaluationResult nopGet(final PolicyEvaluationRequestCacheKey key) {
        LOGGER.warn("Executing circuit breaker fallback method for cache 'get' method.");
        return null;
    }

    @HystrixCommand(fallbackMethod = "nopSet")
    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        if (this.cachingEnabled) {
            this.cacheImpl.set(key, value);
        }
    }

    public void nopSet(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        // Do nothing.
        LOGGER.warn("Executing circuit breaker fallback method for cache 'set' method.");
    }

    @HystrixCommand(fallbackMethod = "nopSetResourceTranslation")
    @Override
    public void setResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        if (this.cachingEnabled) {
            this.cacheImpl.setResourceTranslation(zoneId, fromResourceId, toResourceId);
        }
    }

    public void nopSetResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        // Do nothing.
        LOGGER.warn("Executing circuit breaker fallback method for cache 'setResourceTranslation' method.");
    }

    @HystrixCommand(fallbackMethod = "nopSetResourceTranslations")
    @Override
    public void setResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        if (this.cachingEnabled) {
            this.cacheImpl.setResourceTranslations(zoneId, fromResourceIds, toResourceId);
        }
    }

    public void nopSetResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        // Do nothing.
        LOGGER.warn("Executing circuit breaker fallback method for cache 'setResourceTranslations' method.");
    }

    @Override
    public void reset() {
        if (this.cachingEnabled) {
            this.cacheImpl.reset();
        }
    }

    @Override
    public void reset(final PolicyEvaluationRequestCacheKey key) {
        if (this.cachingEnabled) {
            this.cacheImpl.reset(key);
        }
    }

    @Override
    public void resetForPolicySet(final String zoneId, final String policySetId) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForPolicySet(zoneId, policySetId);
        }
    }

    @Override
    public void resetForResource(final String zoneId, final String resourceId) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForResource(zoneId, resourceId);
        }
    }

    @Override
    public void resetForResources(final String zoneId, final List<ResourceEntity> entities) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForResources(zoneId, entities);
        }
    }

    @Override
    public void resetForSubject(final String zoneId, final String subjectId) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForSubject(zoneId, subjectId);
        }
    }

    @Override
    public void resetForSubjects(final String zoneId, final List<SubjectEntity> subjectEntities) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForSubjects(zoneId, subjectEntities);
        }
    }

    @Override
    public void setCacheImpl(final PolicyEvaluationCache cacheImpl) {
        this.cacheImpl = cacheImpl;
    }

    public boolean isCachingEnabled() {
        return this.cachingEnabled;
    }

    @Override
    public void setCachingEnabled(final boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }

    @Override
    public void resetForResourcesByIds(final String zoneId, final Set<String> resourceIds) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForResourcesByIds(zoneId, resourceIds);
        }
    }

    @Override
    public void resetForSubjectsByIds(final String zoneId, final Set<String> subjectIds) {
        if (this.cachingEnabled) {
            this.cacheImpl.resetForSubjectsByIds(zoneId, subjectIds);
        }
    }
}
