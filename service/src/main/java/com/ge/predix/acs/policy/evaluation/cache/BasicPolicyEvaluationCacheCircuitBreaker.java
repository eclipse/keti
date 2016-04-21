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

public class BasicPolicyEvaluationCacheCircuitBreaker implements PolicyEvaluationCacheCircuitBreaker {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicPolicyEvaluationCacheCircuitBreaker.class);

    @Autowired
    private PolicyEvaluationCache cacheImpl;

    @Value(value = "${ENABLE_CACHING:false}")
    private boolean cachingEnabled;

    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey key) {
        if (this.cachingEnabled) {
            try {
                return this.cacheImpl.get(key);
            } catch (RuntimeException e) {
                return nopGet(key);
            }
        }
        return null;
    }

    public PolicyEvaluationResult nopGet(final PolicyEvaluationRequestCacheKey key) {
        LOGGER.warn("Executing fallback method for cache 'get' method.");
        return null;
    }

    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        if (this.cachingEnabled) {
            try {
                this.cacheImpl.set(key, value);
            } catch (RuntimeException e) {
                nopSet(key, value);
            }
        }
    }

    public void nopSet(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        // Do nothing.
        LOGGER.warn("Executing fallback method for cache 'set' method.");
    }

    @Override
    public void setResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        if (this.cachingEnabled) {
            try {
                this.cacheImpl.setResourceTranslation(zoneId, fromResourceId, toResourceId);
            } catch (RuntimeException e) {
                nopSetResourceTranslation(zoneId, fromResourceId, toResourceId);
            }
        }
    }

    public void nopSetResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        // Do nothing.
        LOGGER.warn("Executing fallback method for cache 'setResourceTranslation' method.");
    }

    @Override
    public void setResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        if (this.cachingEnabled) {
            try {
                this.cacheImpl.setResourceTranslations(zoneId, fromResourceIds, toResourceId);
            } catch (RuntimeException e) {
                nopSetResourceTranslations(zoneId, fromResourceIds, toResourceId);
            }
        }
    }

    public void nopSetResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        // Do nothing.
        LOGGER.warn("Executing fallback method for cache 'setResourceTranslations' method.");
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
}
