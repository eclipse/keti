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

package com.ge.predix.acs.commons.attribute;

/**
 * @author 212314537
 */
@SuppressWarnings("javadoc")
public final class AttributeType {
    private final String issuer;
    private final String name;

    public AttributeType(final String issuer, final String name) {
        this.issuer = issuer;
        this.name = name;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.issuer == null) ? 0 : this.issuer.hashCode());
        result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AttributeType other = (AttributeType) obj;
        if (this.issuer == null) {
            if (other.issuer != null) {
                return false;
            }
        } else if (!this.issuer.equals(other.issuer)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "AttributeType [issuer=" + this.issuer + ", name=" + this.name + "]";
    }
}
