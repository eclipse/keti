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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

fun newInstance(other: AttributeConnector): AttributeConnector {
    val attributeConnector = AttributeConnector()
    attributeConnector.isActive = other.isActive
    attributeConnector.maxCachedIntervalMinutes = other.maxCachedIntervalMinutes
    attributeConnector.adapters = other.adapters!!.map { AttributeAdapterConnection(it) }.toSet()
    return attributeConnector
}

@ApiModel(description = "Connector configuration for external resource or subject attributes.")
class AttributeConnector {

    @JsonProperty("isActive")
    @get:ApiModelProperty(value = "A flag to enable or disable the retrieval of remote attributes. Disabled by default.")
    var isActive = false

    @JsonProperty(required = false)
    @get:ApiModelProperty(value = "Maximum time in minutes before remote attributes are refreshed. Set to 480 minutes by default")
    var maxCachedIntervalMinutes = 480 // default value

    @get:ApiModelProperty(
        value = "A set of adapters used to retrieve attributes from. Only one adapter is currently supported",
        required = true
    )
    var adapters: Set<AttributeAdapterConnection>? = null

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.isActive).append(this.maxCachedIntervalMinutes).append(this.adapters)
            .toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is AttributeConnector) {
            val that = other as AttributeConnector?
            return EqualsBuilder().append(this.isActive, that?.isActive)
                .append(this.maxCachedIntervalMinutes, that?.maxCachedIntervalMinutes)
                .append(this.adapters, that?.adapters).isEquals
        }
        return false
    }
}
