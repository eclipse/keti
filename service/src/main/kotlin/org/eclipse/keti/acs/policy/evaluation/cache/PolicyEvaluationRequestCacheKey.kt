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

const val ANY_POLICY_SET_KEY = "any-policy-set-1f9c788b-e25d-4075-8fad-73bedcd67c2b"
val EVALUATION_ORDER_ANY_POLICY_SET_KEY: LinkedHashSet<String?> = LinkedHashSet(listOf(ANY_POLICY_SET_KEY))

class PolicyEvaluationRequestCacheKey(
    request: PolicyEvaluationRequestV1,
    zoneId: String
) {

    val request: PolicyEvaluationRequestV1? = request
    val policySetIds: LinkedHashSet<String?>
    val resourceId: String? = request.resourceIdentifier
    val subjectId: String? = request.subjectIdentifier
    val zoneId: String? = zoneId

    init {
        if (request.policySetsEvaluationOrder.isEmpty()) {
            // When policySetEvaluationOrder is not specified in the request, cached decision is invalidated if any
            // policy set in this zone was modified. This simplifies the logic of invalidating cached decisions for a
            // zone with a single policy set. It also supports decision invalidation logic for use cases when a second
            // (or more) policy sets are added/removed to a zone which initially had only one policy set.
            this.policySetIds = EVALUATION_ORDER_ANY_POLICY_SET_KEY
        } else {
            this.policySetIds = request.policySetsEvaluationOrder
        }
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
}
