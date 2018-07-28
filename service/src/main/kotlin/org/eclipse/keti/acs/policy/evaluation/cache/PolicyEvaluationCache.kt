/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.policy.evaluation.cache

import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.rest.PolicyEvaluationResult

const val DECISION = "decision"

interface PolicyEvaluationCache {

    operator fun get(key: PolicyEvaluationRequestCacheKey): PolicyEvaluationResult?

    operator fun set(
        key: PolicyEvaluationRequestCacheKey,
        value: PolicyEvaluationResult
    )

    fun reset()

    fun reset(key: PolicyEvaluationRequestCacheKey)

    fun resetForPolicySet(
        zoneId: String,
        policySetId: String
    )

    fun resetForResource(
        zoneId: String,
        resourceId: String
    )

    fun resetForResources(
        zoneId: String,
        entities: List<ResourceEntity>
    )

    fun resetForResourcesByIds(
        zoneId: String,
        resourceIds: Set<String>
    )

    fun resetForSubject(
        zoneId: String,
        subjectId: String
    )

    fun resetForSubjects(
        zoneId: String,
        subjectEntities: List<SubjectEntity>
    )

    fun resetForSubjectsByIds(
        zoneId: String,
        subjectIds: Set<String>
    )
}
