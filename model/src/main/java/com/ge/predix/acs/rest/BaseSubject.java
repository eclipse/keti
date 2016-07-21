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

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ge.predix.acs.model.Attribute;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a Subject in the system identified by a subjectIdentifier.
 */
@ApiModel(description = "Represents a managed protected subject for V1.")
public class BaseSubject {

    @ApiModelProperty(value = "The unique subject identifier, ex: \"joe@ge.com\"", required = true)
    private String subjectIdentifier;

    @ApiModelProperty(value = "The list of attribute values being assigned", required = true)
    private Set<Attribute> attributes = Collections.emptySet();

    @ApiModelProperty(value = "A set that identifies subjects whose attributes also apply to this subject during"
            + "policy evaluation.")
    private Set<Parent> parents = Collections.emptySet();

    public BaseSubject() {
        super();
    }

    public BaseSubject(final String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    public BaseSubject(final String subjectIdentifier, final Set<Attribute> attributes) {
        this.subjectIdentifier = subjectIdentifier;
        this.attributes = attributes;
    }

    public Set<Attribute> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getSubjectIdentifier() {
        return this.subjectIdentifier;
    }

    public void setSubjectIdentifier(final String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    public Set<Parent> getParents() {
        return this.parents;
    }

    public void setParents(final Set<Parent> parents) {
        this.parents = parents;
    }

    @JsonIgnore
    public boolean isIdentifierValid() {
        return StringUtils.isNotBlank(getSubjectIdentifier());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.subjectIdentifier).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof BaseSubject) {
            final BaseSubject other = (BaseSubject) obj;
            return new EqualsBuilder().append(this.subjectIdentifier, other.getSubjectIdentifier()).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "Subject [subjectIdentifier=" + this.subjectIdentifier + ", attributes=" + attributes + "]";
    }

}
