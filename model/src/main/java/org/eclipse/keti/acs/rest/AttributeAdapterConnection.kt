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

@ApiModel(description = "Connection configuration for an adapter to retrieve external resource or subject attributes.")
class AttributeAdapterConnection {

    @get:ApiModelProperty(value = "Adapter URL to retrieve attributes from", required = true)
    var adapterEndpoint: String? = null

    @get:ApiModelProperty(value = "UAA URL to request an access token for this adapter", required = true)
    var uaaTokenUrl: String? = null

    @get:ApiModelProperty(value = "OAuth client used to obtain an access token for this adapter", required = true)
    var uaaClientId: String? = null

    @get:ApiModelProperty(
        value = "OAuth client secret used to obtain an access token for this adapter",
        required = true
    )
    var uaaClientSecret: String? = null

    constructor() {
        // Required to be here for Jackson deserialization
    }

    constructor(
        adapterEndpoint: String,
        uaaTokenUrl: String,
        uaaClientId: String,
        uaaClientSecret: String
    ) {
        this.adapterEndpoint = adapterEndpoint
        this.uaaTokenUrl = uaaTokenUrl
        this.uaaClientId = uaaClientId
        this.uaaClientSecret = uaaClientSecret
    }

    constructor(other: AttributeAdapterConnection) {
        this.adapterEndpoint = other.adapterEndpoint
        this.uaaTokenUrl = other.uaaTokenUrl
        this.uaaClientId = other.uaaClientId
        this.uaaClientSecret = other.uaaClientSecret
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.adapterEndpoint).append(this.uaaTokenUrl).append(this.uaaClientId)
            .toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is AttributeAdapterConnection) {
            val that = other as AttributeAdapterConnection?
            return EqualsBuilder().append(this.adapterEndpoint, that?.adapterEndpoint)
                .append(this.uaaTokenUrl, that?.uaaTokenUrl).append(this.uaaClientId, that?.uaaClientId).isEquals
        }
        return false
    }
}
