package com.ge.predix.acs.policy.evaluation.cache;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Component
public class DefaultPolicyEvaluationCacheCircuitBreaker implements PolicyEvaluationCacheCircuitBreaker {

    @Autowired
    private PolicyEvaluationCache cacheImpl;
    @Value(value = "${DISABLE_CACHING:false}")
    private boolean disableCaching;

    @HystrixCommand(fallbackMethod = "nopGet")
    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey key) {
        if (!this.disableCaching) {
            return this.cacheImpl.get(key);
        }
        return null;
    }

    public PolicyEvaluationResult nopGet(final PolicyEvaluationRequestCacheKey key) {
        return null;
    }

    @HystrixCommand(fallbackMethod = "nopSet")
    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        if (!this.disableCaching) {
            this.cacheImpl.set(key, value);
        }
    }

    public void nopSet(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        // Do nothing.
    }

    @HystrixCommand(fallbackMethod = "nopSetResourceTranslation")
    @Override
    public void setResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        if (!this.disableCaching) {
            this.cacheImpl.setResourceTranslation(zoneId, fromResourceId, toResourceId);
        }
    }

    public void nopSetResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        // Do nothing.
    }

    @HystrixCommand(fallbackMethod = "nopSetResourceTranslations")
    @Override
    public void setResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        if (!this.disableCaching) {
            this.cacheImpl.setResourceTranslations(zoneId, fromResourceIds, toResourceId);
        }
    }

    public void nopSetResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        // Do nothing.
    }

    @Override
    public void reset() {
        if (!this.disableCaching) {
            this.cacheImpl.reset();
        }
    }

    @Override
    public void reset(final PolicyEvaluationRequestCacheKey key) {
        if (!this.disableCaching) {
            this.cacheImpl.reset(key);
        }
    }

    @Override
    public void resetForPolicySet(final String zoneId, final String policySetId) {
        if (!this.disableCaching) {
            this.cacheImpl.resetForPolicySet(zoneId, policySetId);
        }
    }

    @Override
    public void resetForResource(final String zoneId, final String resourceId) {
        if (!this.disableCaching) {
            this.cacheImpl.resetForResource(zoneId, resourceId);
        }
    }

    @Override
    public void resetForResources(final String zoneId, final List<ResourceEntity> entities) {
        if (!this.disableCaching) {
            this.cacheImpl.resetForResources(zoneId, entities);
        }
    }

    @Override
    public void resetForSubject(final String zoneId, final String subjectId) {
        if (!this.disableCaching) {
            this.cacheImpl.resetForSubject(zoneId, subjectId);
        }
    }

    @Override
    public void resetForSubjects(final String zoneId, final List<SubjectEntity> subjectEntities) {
        if (!this.disableCaching) {
            this.cacheImpl.resetForSubjects(zoneId, subjectEntities);
        }
    }

    @Override
    public void setCacheImpl(final PolicyEvaluationCache cacheImpl) {
        this.cacheImpl = cacheImpl;
    }
}
