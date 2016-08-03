package com.ge.predix.acs.policy.evaluation.cache;

import java.util.List;
import java.util.Set;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

public class MockBrokenPolicyEvaluationCache implements PolicyEvaluationCache {

    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey key) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void setResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void setResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void reset() {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void reset(final PolicyEvaluationRequestCacheKey key) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForPolicySet(final String zoneId, final String policySetId) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForResource(final String zoneId, final String resourceId) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForResources(final String zoneId, final List<ResourceEntity> entities) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForSubject(final String zoneId, final String subjectId) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForSubjects(final String zoneId, final List<SubjectEntity> subjectEntities) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForResourcesByIds(String zoneId, Set<String> resourceIds) {
        throw new IllegalStateException("Broken cache.");
    }

    @Override
    public void resetForSubjectsByIds(String zoneId, Set<String> subjectIds) {
        throw new IllegalStateException("Broken cache.");
    }
}
