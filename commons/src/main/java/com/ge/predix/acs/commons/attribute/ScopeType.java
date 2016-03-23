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

public class ScopeType {

    private final ResourceHandler resourceHandler;
    private final AttributeType attributeType;

    public ScopeType(final ResourceHandler resourceHandler, final AttributeType attributeType) {
        this.resourceHandler = resourceHandler;
        this.attributeType = attributeType;
    }

    public ResourceHandler getResourceHandler() {
        return this.resourceHandler;
    }

    public AttributeType getAttributeType() {
        return this.attributeType;
    }

    public static ScopeType scope(final ResourceHandler resourceHandler, final String issuer, final String name) {
        AttributeType attrType = new AttributeType(issuer, name);
        return new ScopeType(resourceHandler, attrType);
    }

    @Override
    public String toString() {
        return "ScopeType [resourceHandler=" + this.resourceHandler.getName() + ", attributeType=" + this.attributeType
                + "]";
    }

}
