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

package com.ge.predix.acs.commons.attribute;

import com.ge.predix.acs.commons.policy.condition.ResourceHandler;

/**
 *
 * @author 212314537
 */
public class Attribute {
    private AttributeType type;
    private String value;

    /**
     * Default constructor.
     */
    public Attribute() {

    }

    /**
     * @param type
     *            attribute type
     * @param value
     *            attribute value
     */
    public Attribute(final AttributeType type, final String value) {
        this.type = type;
        this.value = value;
    }

    public Attribute(final com.ge.predix.acs.model.Attribute modelAttribute) {
        this.type = new AttributeType(modelAttribute.getIssuer(), modelAttribute.getName());
        this.value = modelAttribute.getValue();
    }

    /**
     * @return the type
     */
    public AttributeType getType() {
        return this.type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final AttributeType type) {
        this.type = type;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
        result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        Attribute other = (Attribute) obj;
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

    public static Attribute attribute(final String issuer, final String name, final String value) {
        AttributeType attrType = new AttributeType(issuer, name);
        return new Attribute(attrType, value);
    }

    public ScopedAttributeCriteria forAny(final ScopeType scopeType) {
        return new ScopedAttributeCriteria(this, scopeType);
    }

    public ScopedAttributeCriteria forAny(final ResourceHandler resourceHandler, final String scopeAttributeIssuer,
            final String scopeAttributeName) {
        AttributeType scopeAttributeType = new AttributeType(scopeAttributeIssuer, scopeAttributeName);
        return new ScopedAttributeCriteria(this, new ScopeType(resourceHandler, scopeAttributeType));
    }

    @Override
    public String toString() {
        return "Attribute [type=" + this.type + ", value=" + this.value + "]";
    }
}
