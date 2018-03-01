/*******************************************************************************
 * Copyright 2017 General Electric Company
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

package org.eclipse.keti.acs.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.keti.acs.model.Attribute;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a Resource in the system identified by a subjectIdentifier.
 */
@ApiModel(description = "Represents a managed protected resource for V1.")
public class BaseResource {

    @ApiModelProperty(value = "The unique resource identifier, ex: \"/asset\"/sanramon", required = true)
    private String resourceIdentifier;
    @ApiModelProperty(value = "The list of attribute values being assigned", required = true)
    private Set<Attribute> attributes = Collections.emptySet();
    @ApiModelProperty(value = "A set that identifies resources whose attributes also apply to this resource during "
            + "policy evaluation.")
    private Set<Parent> parents = Collections.emptySet();

    public BaseResource() {
        super();
    }

    public BaseResource(final String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public BaseResource(final String resourceIdentifier, final Set<Attribute> attributes) {
        this.resourceIdentifier = resourceIdentifier;
        this.attributes = attributes;
    }

    public Set<Attribute> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getResourceIdentifier() {
        return this.resourceIdentifier;
    }

    public void setResourceIdentifier(final String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public Set<Parent> getParents() {
        return this.parents;
    }

    public void setParents(final Set<Parent> parents) {
        this.parents = parents;
    }

    @JsonIgnore
    public boolean isIdentifierValid() {
        return StringUtils.isNotBlank(getResourceIdentifier());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.resourceIdentifier).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof BaseResource) {
            final BaseResource other = (BaseResource) obj;
            return new EqualsBuilder().append(this.resourceIdentifier, other.getResourceIdentifier()).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "BaseResource [resourceIdentifier=" + resourceIdentifier + ", attributes=" + attributes + ", parents="
                + parents + "]";
    }
}
