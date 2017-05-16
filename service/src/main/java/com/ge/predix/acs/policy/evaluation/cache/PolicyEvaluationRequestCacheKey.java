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

import java.util.LinkedHashSet;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;

public class PolicyEvaluationRequestCacheKey {

    private final PolicyEvaluationRequestV1 request;
    private final LinkedHashSet<String> policySetIds;
    private final String resourceId;
    private final String subjectId;
    private final String zoneId;

    public PolicyEvaluationRequestCacheKey(final PolicyEvaluationRequestV1 request,
            final LinkedHashSet<String> policySetIds, final String zoneId) {
        this.request = request;
        this.policySetIds = policySetIds;
        this.resourceId = request.getResourceIdentifier();
        this.subjectId = request.getSubjectIdentifier();
        this.zoneId = zoneId;
    }

    public PolicyEvaluationRequestCacheKey(final LinkedHashSet<String> policySetIds, final String resourceId,
            final String subjectId, final String zoneId) {
        this.request = null;
        this.policySetIds = policySetIds;
        this.resourceId = resourceId;
        this.subjectId = subjectId;
        this.zoneId = zoneId;
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

    public static class Builder {
        private PolicyEvaluationRequestV1 builderRequest;
        private LinkedHashSet<String> builderPolicySetIds = new LinkedHashSet<>();
        private String builderResourceId;
        private String builderSubjectId;
        private String builderZoneId;

        public Builder request(final PolicyEvaluationRequestV1 request) {
            this.builderRequest = request;
            return this;
        }

        public Builder policySetIds(final LinkedHashSet<String> policySets) {
            if (null != this.builderRequest && !this.builderRequest.getPolicySetsEvaluationOrder().isEmpty()) {
                throw new IllegalStateException(
                        "Cannot set policy sets evaluation order if set in the policy request.");
            }
            if (null != policySets) {
                this.builderPolicySetIds = policySets;
            }
            return this;
        }

        public Builder resourceId(final String resourceId) {
            if (null != this.builderRequest) {
                throw new IllegalStateException("Cannot set resource id if policy request is set.");
            }
            this.builderResourceId = resourceId;
            return this;
        }

        public Builder subjectId(final String subjectId) {
            if (null != this.builderRequest) {
                throw new IllegalStateException("Cannot set subject id if policy request is set.");
            }
            this.builderSubjectId = subjectId;
            return this;
        }

        public Builder zoneId(final String zoneId) {
            this.builderZoneId = zoneId;
            return this;
        }

        public PolicyEvaluationRequestCacheKey build() {
            if (null != this.builderRequest) {
                if (this.builderRequest.getPolicySetsEvaluationOrder().isEmpty()) {
                    return new PolicyEvaluationRequestCacheKey(this.builderRequest, this.builderPolicySetIds,
                            this.builderZoneId);
                } else {
                    return new PolicyEvaluationRequestCacheKey(this.builderRequest,
                            this.builderRequest.getPolicySetsEvaluationOrder(), this.builderZoneId);
                }
            }
            return new PolicyEvaluationRequestCacheKey(this.builderPolicySetIds, this.builderResourceId,
                    this.builderSubjectId, this.builderZoneId);
        }
    }
}
