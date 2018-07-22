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

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.eclipse.keti.acs.model.Attribute
import org.springframework.util.Assert

/**
 * Represents a source of inherited attributes.
 */
@ApiModel(description = "Represents a source of inherited attributes.")
class Parent {

    @ApiModelProperty(
        value = "The unique identifier of the parent, e.g. \"tom@ge.com\" or \"/site/sanramon\".",
        required = true
    )
    var identifier: String? = null

    @ApiModelProperty(
        value = "Restrictions on when a subject inherits parent attributes based on the resource accessed," + " i.e. a subject may only inherit the attributes from a parent subject if the user accesses" + " a resource with specific attributes. e.g. \"tom@ge.com\" inherits attributes from the" + " \"analysts\" group only when accessing resources where the \"site\" is \"sanramon\".",
        required = true
    )
    var scopes = emptySet<Attribute>()
        set(scopes) {
            Assert.isTrue(scopes.size < 2, "Multiple scope attributes are not supported, yet.")
            field = scopes
        }

    constructor() {
        // Default constructor.
    }

    constructor(identifier: String) {
        this.identifier = identifier
    }

    constructor(
        identifier: String,
        scopes: Set<Attribute>
    ) {
        this.scopes = scopes
        this.identifier = identifier
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.identifier).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Parent) {
            val that = other as Parent?
            return EqualsBuilder().append(this.identifier, that?.identifier).isEquals
        }
        return false
    }

    override fun toString(): String {
        return "Parent [identifier=" + identifier + ", scopes=" + this.scopes + "]"
    }

}
