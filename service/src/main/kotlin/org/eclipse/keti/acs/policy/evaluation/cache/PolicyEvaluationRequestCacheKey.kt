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

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import java.util.LinkedHashSet

class PolicyEvaluationRequestCacheKey {

    val request: PolicyEvaluationRequestV1?
    val policySetIds: LinkedHashSet<String?>
    val resourceId: String?
    val subjectId: String?
    val zoneId: String?

    constructor(
        request: PolicyEvaluationRequestV1,
        policySetIds: LinkedHashSet<String?>,
        zoneId: String
    ) {
        this.request = request
        this.policySetIds = policySetIds
        this.resourceId = request.resourceIdentifier
        this.subjectId = request.subjectIdentifier
        this.zoneId = zoneId
    }

    constructor(
        policySetIds: LinkedHashSet<String?>,
        resourceId: String,
        subjectId: String,
        zoneId: String
    ) {
        this.request = null
        this.policySetIds = policySetIds
        this.resourceId = resourceId
        this.subjectId = subjectId
        this.zoneId = zoneId
    }

    fun toDecisionKey(): String {
        val keyBuilder = StringBuilder()
        if (null == this.zoneId) {
            keyBuilder.append("*:")
        } else {
            keyBuilder.append(this.zoneId)
            keyBuilder.append(":")
        }
        if (null == this.subjectId) {
            keyBuilder.append("*:")
        } else {
            keyBuilder.append(Integer.toHexString(this.subjectId.hashCode()))
            keyBuilder.append(":")
        }
        if (null == this.resourceId) {
            keyBuilder.append("*:")
        } else {
            keyBuilder.append(Integer.toHexString(this.resourceId.hashCode()))
            keyBuilder.append(":")
        }
        if (null == this.request) {
            keyBuilder.append("*")
        } else {
            keyBuilder.append(Integer.toHexString(this.request.hashCode()))
        }
        return keyBuilder.toString()
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.request).append(this.zoneId).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is PolicyEvaluationRequestCacheKey) {
            val that = other as PolicyEvaluationRequestCacheKey?
            return EqualsBuilder().append(this.request, that?.request).append(this.zoneId, that?.zoneId).isEquals
        }
        return false
    }

    class Builder {
        private var builderRequest: PolicyEvaluationRequestV1? = null
        private var builderPolicySetIds = LinkedHashSet<String?>()
        private var builderResourceId: String? = null
        private var builderSubjectId: String? = null
        private var builderZoneId: String? = null

        fun request(request: PolicyEvaluationRequestV1): Builder {
            this.builderRequest = request
            return this
        }

        fun policySetIds(policySets: LinkedHashSet<String?>?): Builder {
            if (null != this.builderRequest && !this.builderRequest!!.policySetsEvaluationOrder.isEmpty()) {
                throw IllegalStateException(
                    "Cannot set policy sets evaluation order if set in the policy request."
                )
            }
            if (null != policySets) {
                this.builderPolicySetIds = policySets
            }
            return this
        }

        fun resourceId(resourceId: String): Builder {
            if (null != this.builderRequest) {
                throw IllegalStateException("Cannot set resource id if policy request is set.")
            }
            this.builderResourceId = resourceId
            return this
        }

        fun subjectId(subjectId: String): Builder {
            if (null != this.builderRequest) {
                throw IllegalStateException("Cannot set subject id if policy request is set.")
            }
            this.builderSubjectId = subjectId
            return this
        }

        fun zoneId(zoneId: String): Builder {
            this.builderZoneId = zoneId
            return this
        }

        fun build(): PolicyEvaluationRequestCacheKey {
            return if (null != this.builderRequest) {
                if (this.builderRequest!!.policySetsEvaluationOrder.isEmpty()) {
                    PolicyEvaluationRequestCacheKey(
                        this.builderRequest!!, this.builderPolicySetIds, this.builderZoneId!!
                    )
                } else {
                    PolicyEvaluationRequestCacheKey(
                        this.builderRequest!!, this.builderRequest!!.policySetsEvaluationOrder, this.builderZoneId!!
                    )
                }
            } else PolicyEvaluationRequestCacheKey(
                this.builderPolicySetIds, this.builderResourceId!!, this.builderSubjectId!!, this.builderZoneId!!
            )
        }
    }
}
