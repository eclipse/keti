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
 *******************************************************************************/

package com.ge.predix.acs.rest;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A zone encapsulates all policies and privilege data maintained by ACS for its users. It is a mechanism to define
 * partitioned of authorization data for users of ACS.
 *
 * @author 212319607
 */
public class Zone {

    private String name;
    private String description;
    private String subdomain;

    public Zone() {
        // Intentionally left blank to setup this object with setters
    }

    public Zone(final String name, final String subdomain, final String description) {
        this.name = name;
        this.subdomain = subdomain;
        this.description = description;
    }

    /**
     * Unique name for this zone.
     *
     * @return
     */
    @JsonProperty(required = true)
    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * DNS subdomain for accessing this zone on ACS service.
     *
     * @return
     */
    @JsonProperty(required = true)
    public String getSubdomain() {
        return this.subdomain;
    }

    public void setSubdomain(final String subdomain) {
        this.subdomain = subdomain;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.name).append(this.description).append(this.subdomain).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Zone) {
            final Zone other = (Zone) obj;
            return new EqualsBuilder().append(this.name, other.name).append(this.description, other.description)
                    .append(this.subdomain, other.subdomain).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "Zone [name=" + this.name + ", description=" + this.description + ", subdomain=" + this.subdomain + "]";
    }

}
