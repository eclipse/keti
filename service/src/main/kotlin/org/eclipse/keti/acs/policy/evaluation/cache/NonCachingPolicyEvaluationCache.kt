/*******************************************************************************
 * Copyright 2018 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.policy.evaluation.cache

import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.rest.PolicyEvaluationResult

class NonCachingPolicyEvaluationCache : PolicyEvaluationCache {

    override fun get(key: PolicyEvaluationRequestCacheKey): PolicyEvaluationResult? {
        return null
    }

    override fun set(
        key: PolicyEvaluationRequestCacheKey,
        value: PolicyEvaluationResult
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun reset() {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun reset(key: PolicyEvaluationRequestCacheKey) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForPolicySet(
        zoneId: String,
        policySetId: String
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForResource(
        zoneId: String,
        resourceId: String
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForResources(
        zoneId: String,
        entities: List<ResourceEntity>
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForResourcesByIds(
        zoneId: String,
        resourceIds: Set<String>
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForSubject(
        zoneId: String,
        subjectId: String
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForSubjects(
        zoneId: String,
        subjectEntities: List<SubjectEntity>
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }

    override fun resetForSubjectsByIds(
        zoneId: String,
        subjectIds: Set<String>
    ) {
        // Purposely empty since it's required by the PolicyEvaluationCache interface but unused here
    }
}
