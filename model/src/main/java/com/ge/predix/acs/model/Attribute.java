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

package com.ge.predix.acs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author 212314537
 */
@ApiModel(
        description = "Attribute represents a characteristic of a subject or resource in context of a resource"
                + " request. The attribute values are evaluated against any attributes declared in a access"
                + " control policy during policy evaluation. For example, a subject may be assigned a 'group'"
                + "attribute which would be matched against any 'group' attribute conditions in a policy.")
@SuppressWarnings({ "javadoc", "nls" })
public class Attribute {
    private String issuer;
    private String name;
    private String value;

    public Attribute() {
        // Default constructor.
    }

    public Attribute(final String issuer, final String name, final String value) {
        this(issuer, name);
        this.value = value;
    }

    public Attribute(final String issuer, final String name) {
        this.issuer = issuer;
        this.name = name;
    }

    /**
     * @return the issuer
     */
    @ApiModelProperty(value = "The entity vouching for this attribute.", required = true)
    public String getIssuer() {
        return this.issuer;
    }

    /**
     * @param issuer
     *            the issuer to set
     */
    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    /**
     * @return the name
     */
    @ApiModelProperty(value = "The unique name of this attribute.", required = true)
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    @ApiModelProperty(
            value = "The value of this attribute. Optional when used in context of a Policy Target.",
            required = true)
    public String getValue() {
        return this.value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.issuer).append(this.name).append(this.value).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj instanceof Attribute) {
            final Attribute other = (Attribute) obj;
            return new EqualsBuilder().append(this.issuer, other.issuer).append(this.name, other.name)
                    .append(this.value, other.value).isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "Attribute [issuer=" + this.issuer + ", name=" + this.name + ", value=" + this.value + "]";
    }
}
