/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import java.util.List;
import java.util.Set;

import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

public interface PolicyEvaluationCacheCircuitBreaker {

    PolicyEvaluationResult get(PolicyEvaluationRequestCacheKey key);

    void set(PolicyEvaluationRequestCacheKey key, PolicyEvaluationResult value);

    void setResourceTranslation(String zoneId, String fromResourceId, String toResourceId);

    void setResourceTranslations(String zoneId, Set<String> fromResourceIds, String toResourceId);

    void reset();

    void reset(PolicyEvaluationRequestCacheKey key);

    void resetForPolicySet(String zoneId, String policySetId);

    void resetForResource(String zoneId, String resourceId);

    void resetForResources(String zoneId, List<ResourceEntity> entities);

    void resetForSubject(String zoneId, String subjectId);

    void resetForSubjects(String zoneId, List<SubjectEntity> subjectEntities);

    void setCacheImpl(final PolicyEvaluationCache cacheImpl);

    void setCachingEnabled(final boolean cachingEnabled);
}
