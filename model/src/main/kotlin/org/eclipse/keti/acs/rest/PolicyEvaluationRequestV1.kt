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

package org.eclipse.keti.acs.rest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.model.Attribute
import java.util.LinkedHashSet

@ApiModel(description = "Policy evaluation request for V1.")
class PolicyEvaluationRequestV1 {

    @get:ApiModelProperty(value = "The resource URI to be consumed", required = true)
    var resourceIdentifier: String? = null

    @get:ApiModelProperty(value = "The subject identifier", required = true)
    var subjectIdentifier: String? = null

    /**
     * @return the subjectAttributes
     */
    @get:ApiModelProperty(value = "Supplemental subject attributes provided by the requestor")
    var subjectAttributes: Set<Attribute>? = null

    @get:ApiModelProperty(value = "Supplemental resource attributes provided by the requestor")
    var resourceAttributes: Set<Attribute>? = null

    @get:ApiModelProperty(value = "The action on the given resource URI", required = true)
    var action: String? = null

    @get:ApiModelProperty(
        value = "This list of policy set IDs specifies the order in which the service will evaluate policies. " + "Evaluation stops when a policy with matching target is found and the condition returns true, " + "Or all policies are exhausted."
    )
    var policySetsEvaluationOrder = LinkedHashSet<String?>()

    override fun hashCode(): Int {
        val hashCodeBuilder = HashCodeBuilder()
        hashCodeBuilder.append(this.action).append(this.resourceIdentifier).append(this.subjectIdentifier)
        if (null != this.subjectAttributes) {
            for (attribute in this.subjectAttributes!!) {
                hashCodeBuilder.append(attribute)
            }
        }
        for (policyID in this.policySetsEvaluationOrder) {
            hashCodeBuilder.append(policyID)
        }
        return hashCodeBuilder.toHashCode()
    }

    override fun equals(other: Any?): Boolean {

        if (other is PolicyEvaluationRequestV1) {
            val that = other as PolicyEvaluationRequestV1?
            val equalsBuilder = EqualsBuilder()

            // Element by element comparison may produce true negative in Sets so use built in equals. Therefore, defer
            // to the underlying set's equals() implementation. For details, refer to AbstractSet's (HashSet's ancestor)
            // documentation.
            equalsBuilder.append(this.subjectAttributes, that?.subjectAttributes)
            equalsBuilder.append(this.policySetsEvaluationOrder, that?.policySetsEvaluationOrder)

            equalsBuilder.append(this.action, that?.action).append(this.resourceIdentifier, that?.resourceIdentifier)
                .append(this.subjectIdentifier, that?.subjectIdentifier)
            return equalsBuilder.isEquals
        }
        return false
    }

    override fun toString(): String {
        return ("PolicyEvaluationRequest [resourceIdentifier=" + this.resourceIdentifier + ", subjectIdentifier=" + this.subjectIdentifier + ", action=" + this.action + "]")
    }
}
