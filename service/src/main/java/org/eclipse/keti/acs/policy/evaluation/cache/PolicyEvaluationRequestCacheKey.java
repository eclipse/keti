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

package org.eclipse.keti.acs.policy.evaluation.cache;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;

public class PolicyEvaluationRequestCacheKey {
    public static final String ANY_POLICY_SET_KEY = "any-policy-set-1f9c788b-e25d-4075-8fad-73bedcd67c2b";
    public static final LinkedHashSet<String> EVALUATION_ORDER_ANY_POLICY_SET_KEY = new LinkedHashSet<>(
            Collections.singleton(ANY_POLICY_SET_KEY));

    private final PolicyEvaluationRequestV1 request;
    private final LinkedHashSet<String> policySetIds;
    private final String resourceId;
    private final String subjectId;
    private final String zoneId;

    public PolicyEvaluationRequestCacheKey(final PolicyEvaluationRequestV1 request, final String zoneId) {
        this.request = request;
        this.resourceId = request.getResourceIdentifier();
        this.subjectId = request.getSubjectIdentifier();
        this.zoneId = zoneId;

        if (request.getPolicySetsEvaluationOrder().isEmpty()) {
            // When policySetEvaluationOrder is not specified in the request, cached decision is invalidated if any
            // policy set in this zone was modified. This simplifies the logic of invalidating cached decisions for a
            // zone with a single policy set. It also supports decision invalidation logic for use cases when a second
            // (or more) policy sets are added/removed to a zone which initially had only one policy set.
            this.policySetIds = EVALUATION_ORDER_ANY_POLICY_SET_KEY;
        } else {
            this.policySetIds = request.getPolicySetsEvaluationOrder();
        }
    }

    public String toDecisionKey() {
        StringBuilder keyBuilder = new StringBuilder();
        if (null == this.zoneId) {
            keyBuilder.append("*:");
        } else {
            keyBuilder.append(this.zoneId);
            keyBuilder.append(":");
        }
        if (null == this.subjectId) {
            keyBuilder.append("*:");
        } else {
            keyBuilder.append(Integer.toHexString(this.subjectId.hashCode()));
            keyBuilder.append(":");
        }
        if (null == this.resourceId) {
            keyBuilder.append("*:");
        } else {
            keyBuilder.append(Integer.toHexString(this.resourceId.hashCode()));
            keyBuilder.append(":");
        }
        if (null == this.request) {
            keyBuilder.append("*");
        } else {
            keyBuilder.append(Integer.toHexString(this.request.hashCode()));
        }
        return keyBuilder.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.request).append(this.zoneId).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PolicyEvaluationRequestCacheKey) {
            final PolicyEvaluationRequestCacheKey other = (PolicyEvaluationRequestCacheKey) obj;
            return new EqualsBuilder().append(this.request, other.request).append(this.zoneId, other.zoneId).isEquals();
        }
        return false;
    }

    public PolicyEvaluationRequestV1 getRequest() {
        return this.request;
    }

    public LinkedHashSet<String> getPolicySetIds() {
        return this.policySetIds;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public String getSubjectId() {
        return this.subjectId;
    }

    public String getZoneId() {
        return this.zoneId;
    }
}
