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

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.ge.predix.acs.model.Attribute;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@SuppressWarnings({ "javadoc", "nls" })
@ApiModel(description = "Policy evaluation request for V1.")
public class PolicyEvaluationRequestV1 {

    private String resourceIdentifier;

    private String subjectIdentifier;

    private List<Attribute> subjectAttributes;

    private List<Attribute> resourceAttributes;

    private String action;

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
    public List<Attribute> getResourceAttributes() {
        return this.resourceAttributes;
    }

    public void setResourceAttributes(final List<Attribute> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    /**
     * @return the subjectAttributes
     */
    @ApiModelProperty(value = "Supplemental subject attributes provided by the requestor")
    public List<Attribute> getSubjectAttributes() {
        return this.subjectAttributes;
    }

    /**
     * @param subjectAttributes
     *            the subjectAttributes to set
     */
    public void setSubjectAttributes(final List<Attribute> subjectAttributes) {
        this.subjectAttributes = subjectAttributes;
    }

    @ApiModelProperty(value = "The action on the given resource URI", required = true)
    public String getAction() {
        return this.action;
    }

    public void setAction(final String action) {
        this.action = action;
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
        return hashCodeBuilder.toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj instanceof PolicyEvaluationRequestV1) {
            final PolicyEvaluationRequestV1 other = (PolicyEvaluationRequestV1) obj;
            EqualsBuilder equalsBuilder = new EqualsBuilder();
            if ((null == this.subjectAttributes) && (null != other.subjectAttributes)) {
                return false;
            }
            if ((null != this.subjectAttributes) && (null == other.subjectAttributes)) {
                return false;
            }
            if ((null != this.subjectAttributes) && (this.subjectAttributes.size() != other.subjectAttributes.size())) {
                return false;
            }
            if ((null != this.subjectAttributes) && (this.subjectAttributes.size() == other.subjectAttributes.size())) {
                for (int i = 0; i < this.subjectAttributes.size(); i++) {
                    equalsBuilder.append(this.subjectAttributes.get(i), other.subjectAttributes.get(i));
                }
            }
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
