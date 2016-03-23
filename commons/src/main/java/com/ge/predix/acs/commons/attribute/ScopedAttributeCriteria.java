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

public class ScopedAttributeCriteria {
    private final Attribute attribute;
    private final ScopeType scopeType;

    public ScopedAttributeCriteria(final Attribute attribute, final ScopeType scopeType) {
        this.attribute = attribute;
        this.scopeType = scopeType;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public ScopeType getScopeType() {
        return this.scopeType;
    }

    @Override
    public String toString() {
        return "ScopedAttributeCriteria [attribute=" + this.attribute + ", scopeType=" + this.scopeType + "]";
    }

}
