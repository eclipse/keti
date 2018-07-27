/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.rest

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.model.Attribute

/**
 * Represents a Resource in the system identified by a subjectIdentifier.
 */
@ApiModel(description = "Represents a managed protected resource for V1.")
class BaseResource {

    @ApiModelProperty(value = "The unique resource identifier, ex: \"/asset\"/sanramon", required = true)
    var resourceIdentifier: String? = null

    @ApiModelProperty(value = "The list of attribute values being assigned", required = true)
    var attributes: Set<Attribute>? = emptySet()

    @ApiModelProperty(value = "A set that identifies resources whose attributes also apply to this resource during " + "policy evaluation.")
    var parents = emptySet<Parent>()

    val isIdentifierValid: Boolean
        @JsonIgnore get() = StringUtils.isNotBlank(resourceIdentifier)

    constructor() : super()

    constructor(resourceIdentifier: String?) {
        this.resourceIdentifier = resourceIdentifier
    }

    constructor(
        resourceIdentifier: String,
        attributes: Set<Attribute>
    ) {
        this.resourceIdentifier = resourceIdentifier
        this.attributes = attributes
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.resourceIdentifier).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BaseResource) {
            val that = other as BaseResource?
            return EqualsBuilder().append(this.resourceIdentifier, that?.resourceIdentifier).isEquals
        }
        return false
    }

    override fun toString(): String {
        return ("BaseResource [resourceIdentifier=$resourceIdentifier, attributes=$attributes, parents=$parents]")
    }
}
