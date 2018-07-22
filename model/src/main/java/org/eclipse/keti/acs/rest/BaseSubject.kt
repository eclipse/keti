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
 * Represents a Subject in the system identified by a subjectIdentifier.
 */
@ApiModel(description = "Represents a managed protected subject for V1.")
class BaseSubject {

    @ApiModelProperty(value = "The unique subject identifier, ex: \"joe@ge.com\"", required = true)
    var subjectIdentifier: String? = null

    @ApiModelProperty(value = "The list of attribute values being assigned", required = true)
    var attributes: Set<Attribute>? = emptySet()

    @ApiModelProperty(value = "A set that identifies subjects whose attributes also apply to this subject during" + "policy evaluation.")
    var parents = emptySet<Parent>()

    val isIdentifierValid: Boolean
        @JsonIgnore get() = StringUtils.isNotBlank(subjectIdentifier)

    constructor() : super()

    constructor(subjectIdentifier: String) {
        this.subjectIdentifier = subjectIdentifier
    }

    constructor(
        subjectIdentifier: String,
        attributes: Set<Attribute>
    ) {
        this.subjectIdentifier = subjectIdentifier
        this.attributes = attributes
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.subjectIdentifier).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BaseSubject) {
            val that = other as BaseSubject?
            return EqualsBuilder().append(this.subjectIdentifier, that?.subjectIdentifier).isEquals
        }
        return false
    }

    override fun toString(): String {
        return "Subject [subjectIdentifier=" + this.subjectIdentifier + ", attributes=" + attributes + "]"
    }
}
