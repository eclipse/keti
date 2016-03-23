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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;

public class PolicyEvaluationRequestCacheKey {

    private final PolicyEvaluationRequestV1 request;
    private final String policySetId;
    private final String resourceId;
    private final String subjectId;
    private final String zoneId;

    public PolicyEvaluationRequestCacheKey(final PolicyEvaluationRequestV1 request, final String policySetId,
            final String zoneId) {
        this.request = request;
        this.policySetId = policySetId;
        this.resourceId = request.getResourceIdentifier();
        this.subjectId = request.getSubjectIdentifier();
        this.zoneId = zoneId;
    }

    public PolicyEvaluationRequestCacheKey(final String policySetId, final String resourceId, final String subjectId,
            final String zoneId) {
        this.request = null;
        this.policySetId = policySetId;
        this.resourceId = resourceId;
        this.subjectId = subjectId;
        this.zoneId = zoneId;
    }

    public String toRedisKey() {
        StringBuilder keyBuilder = new StringBuilder();
        if (null == this.zoneId) {
            keyBuilder.append("*:");
        } else {
            keyBuilder.append(this.zoneId);
            keyBuilder.append(":");
        }
        if (null == this.policySetId) {
            keyBuilder.append("*:");
        } else {
            keyBuilder.append(Integer.toHexString(this.policySetId.hashCode()));
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
        return new HashCodeBuilder().append(this.request).append(this.policySetId).append(this.zoneId).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PolicyEvaluationRequestCacheKey) {
            final PolicyEvaluationRequestCacheKey other = (PolicyEvaluationRequestCacheKey) obj;
            return new EqualsBuilder().append(this.request, other.request).append(this.policySetId, other.policySetId)
                    .append(this.zoneId, other.zoneId).isEquals();
        }
        return false;
    }

    public PolicyEvaluationRequestV1 getRequest() {
        return this.request;
    }

    public String getPolicySetId() {
        return this.policySetId;
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
        private String builderPolicySetId;
        private String builderResourceId;
        private String builderSubjectId;
        private String builderZoneId;

        public Builder request(final PolicyEvaluationRequestV1 request) {
            this.builderRequest = request;
            return this;
        }

        public Builder policySetId(final String policySet) {
            this.builderPolicySetId = policySet;
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
                return new PolicyEvaluationRequestCacheKey(this.builderRequest, this.builderPolicySetId,
                        this.builderZoneId);
            }
            return new PolicyEvaluationRequestCacheKey(this.builderPolicySetId, this.builderResourceId,
                    this.builderSubjectId, this.builderZoneId);
        }
    }
}
