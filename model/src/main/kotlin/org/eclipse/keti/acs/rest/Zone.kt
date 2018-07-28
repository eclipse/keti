/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.rest

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * A zone encapsulates all policies and privilege data maintained by ACS for its users. It is a mechanism to define
 * partitioned of authorization data for users of ACS.
 *
 * @author acs-engineers@ge.com
 */
class Zone {

    /**
     * Unique name for this zone.
     *
     * @return
     */
    @get:JsonProperty(required = true)
    var name: String? = null

    var description: String? = null

    /**
     * DNS subdomain for accessing this zone on ACS service.
     *
     * @return
     */
    @get:JsonProperty(required = true)
    var subdomain: String? = null

    constructor() {
        // Intentionally left blank to setup this object with setters
    }

    constructor(
        name: String,
        subdomain: String,
        description: String
    ) {
        this.name = name
        this.subdomain = subdomain
        this.description = description
    }

    override fun hashCode(): Int {
        return HashCodeBuilder().append(this.name).append(this.description).append(this.subdomain).toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Zone) {
            val that = other as Zone?
            return EqualsBuilder().append(this.name, that?.name).append(this.description, that?.description)
                .append(this.subdomain, that?.subdomain).isEquals
        }
        return false
    }

    override fun toString(): String {
        return "Zone [name=" + this.name + ", description=" + this.description + ", subdomain=" + this.subdomain + "]"
    }
}
