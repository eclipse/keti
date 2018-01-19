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

import java.util.List;
import java.util.Set;

import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity;
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity;
import org.eclipse.keti.acs.rest.PolicyEvaluationResult;

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
