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

public interface PolicyEvaluationCache {
    String DECISION = "decision";


    PolicyEvaluationResult get(PolicyEvaluationRequestCacheKey key);

    void set(PolicyEvaluationRequestCacheKey key, PolicyEvaluationResult value);

    void reset();

    void reset(PolicyEvaluationRequestCacheKey key);

    void resetForPolicySet(String zoneId, String policySetId);

    void resetForResource(String zoneId, String resourceId);

    void resetForResources(String zoneId, List<ResourceEntity> entities);

    void resetForResourcesByIds(String zoneId, Set<String> resourceIds);

    void resetForSubject(String zoneId, String subjectId);

    void resetForSubjects(String zoneId, List<SubjectEntity> subjectEntities);

    void resetForSubjectsByIds(String zoneId, Set<String> subjectIds);
}
