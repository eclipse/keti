package com.ge.predix.acs.policy.evaluation.cache;

import java.util.List;
import java.util.Set;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

public class NonCachingPolicyEvaluationCache implements PolicyEvaluationCache {

    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey key) {
        return null;
    }

    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult value) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void reset() {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void reset(final PolicyEvaluationRequestCacheKey key) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForPolicySet(final String zoneId, final String policySetId) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForResource(final String zoneId, final String resourceId) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForResources(final String zoneId, final List<ResourceEntity> entities) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForResourcesByIds(final String zoneId, final Set<String> resourceIds) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForSubject(final String zoneId, final String subjectId) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForSubjects(final String zoneId, final List<SubjectEntity> subjectEntities) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    @Override
    public void resetForSubjectsByIds(final String zoneId, final Set<String> subjectIds) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }
}
