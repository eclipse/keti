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

package com.ge.predix.acs.rest.attribute.adapter;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class AttributesResponse {

    private Set<Attribute> attributes;
    private String id;

    public AttributesResponse() {
        // Default constructor necessary for Jackson
    }

    public AttributesResponse(final Set<Attribute> attributes, final String id) {
        this.attributes = attributes;
        this.id = id;
    }

    public Set<Attribute> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
