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
package com.ge.predix.acs.rest;

import com.ge.predix.acs.model.Attribute;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings({ "javadoc", "nls" })
@ApiModel(description = "Policy evaluation request for V1.")
public class PolicyEvaluationRequestV1 {
    public static final LinkedHashSet<String> EMPTY_POLICY_EVALUATION_ORDER = new LinkedHashSet<String>();

    private String resourceIdentifier;

    private String subjectIdentifier;

    private Set<Attribute> subjectAttributes;

    private Set<Attribute> resourceAttributes;

    private String action;

    private LinkedHashSet<String> policySetsEvaluationOrder = EMPTY_POLICY_EVALUATION_ORDER;

    @ApiModelProperty(value = "The resource URI to be consumed", required = true)
    public String getResourceIdentifier() {
        return this.resourceIdentifier;
    }

    public void setResourceIdentifier(final String resourceUri) {
        this.resourceIdentifier = resourceUri;
    }

    @ApiModelProperty(value = "The subject identifier", required = true)
    public String getSubjectIdentifier() {
        return this.subjectIdentifier;
    }

    public void setSubjectIdentifier(final String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    @ApiModelProperty(value = "Supplemental resource attributes provided by the requestor")
    public Set<Attribute> getResourceAttributes() {
        return this.resourceAttributes;
    }

    public void setResourceAttributes(final Set<Attribute> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    /**
     * @return the subjectAttributes
     */
    @ApiModelProperty(value = "Supplemental subject attributes provided by the requestor")
    public Set<Attribute> getSubjectAttributes() {
        return this.subjectAttributes;
    }

    /**
     * @param subjectAttributes
     *            the subjectAttributes to set
     */
    public void setSubjectAttributes(final Set<Attribute> subjectAttributes) {
        this.subjectAttributes = subjectAttributes;
    }

    @ApiModelProperty(value = "The action on the given resource URI", required = true)
    public String getAction() {
        return this.action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    @ApiModelProperty(
            value = "This list of policy set IDs specifies the order in which the service will evaluate policies. "
                    + "Evaluation stops when a policy with matching target is found and the condition returns true, "
                    + "Or all policies are exhausted.")
    public LinkedHashSet<String> getPolicySetsEvaluationOrder() {
        return this.policySetsEvaluationOrder;
    }

    public void setPolicySetsEvaluationOrder(final LinkedHashSet<String> policySetIds) {
        if (policySetIds != null) {
            this.policySetsEvaluationOrder = policySetIds;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(this.action).append(this.resourceIdentifier).append(this.subjectIdentifier);
        if (null != this.subjectAttributes) {
            for (Attribute attribute : this.subjectAttributes) {
                hashCodeBuilder.append(attribute);
            }
        }
        for (String policyID : this.policySetsEvaluationOrder) {
            hashCodeBuilder.append(policyID);
        }
        return hashCodeBuilder.toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj instanceof PolicyEvaluationRequestV1) {
            final PolicyEvaluationRequestV1 other = (PolicyEvaluationRequestV1) obj;
            EqualsBuilder equalsBuilder = new EqualsBuilder();

            // Element by element comparison may produce true negative in Sets so use built in equals
            // From AbstractSet's (HashSet's ancestor) documentation
            // This implementation first checks if the specified object is this set; if so it returns true.
            // Then, it checks if the specified object is a set whose size is identical to the size of this set;
            // if not, it returns false. If so, it returns containsAll((Collection) o).
            equalsBuilder.append(this.subjectAttributes, other.subjectAttributes);
            equalsBuilder.append(this.policySetsEvaluationOrder, other.policySetsEvaluationOrder);

            equalsBuilder.append(this.action, other.action).append(this.resourceIdentifier, other.resourceIdentifier)
                    .append(this.subjectIdentifier, other.subjectIdentifier);
            return equalsBuilder.isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "PolicyEvaluationRequest [resourceIdentifier=" + this.resourceIdentifier + ", subjectIdentifier="
                + this.subjectIdentifier + ", action=" + this.action + "]";
    }

}
