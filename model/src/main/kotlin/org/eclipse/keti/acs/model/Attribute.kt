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

package org.eclipse.keti.acs.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

/**
 *
 * @author acs-engineers@ge.com
 */
@ApiModel(
    description = "Attribute represents a characteristic of a subject or resource in context of a resource" + " request. The attribute values are evaluated against any attributes declared in a access" + " control policy during policy evaluation. For example, a subject may be assigned a 'group'" + "attribute which would be matched against any 'group' attribute conditions in a policy."
)
class Attribute {

    /**
     * @return the issuer
     */
    @get:ApiModelProperty(value = "The entity vouching for this attribute.", required = true)
    var issuer: String? = null

    /**
     * @return the name
     */
    @get:ApiModelProperty(value = "The unique name of this attribute.", required = true)
    var name: String? = null

    @get:ApiModelProperty(
        value = "The value of this attribute. Optional when used in context of a Policy Target.",
        required = true
    )
    var value: String? = null

    constructor() {
        // Default constructor.
    }

    constructor(
        issuer: String,
        name: String,
        value: String
    ) : this(issuer, name) {
        this.value = value
    }

    constructor(
        issuer: String,
        name: String
    ) {
        this.issuer = issuer
        this.name = name
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.issuer).append(this.name).append(this.value).toHashCode()
    }

    override fun equals(other: Any?): Boolean {

        if (other is Attribute) {
            val that = other as Attribute?
            return EqualsBuilder().append(this.issuer, that?.issuer).append(this.name, that?.name)
                .append(this.value, that?.value).isEquals
        }
        return false
    }

    override fun toString(): String {
        return "Attribute [issuer=" + this.issuer + ", name=" + this.name + ", value=" + this.value + "]"
    }
}
