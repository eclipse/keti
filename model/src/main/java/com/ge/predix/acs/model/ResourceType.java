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

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;

/**
 *
 * @author 212314537
 */
@ApiModel("The resource the access control operation would read, create, delete, or modify. Typically used"
        + " within a policy definition")
public class ResourceType {

    private String name;
    private String uriTemplate;
    private List<Attribute> attributes;
    private String attributeUriTemplate;

    @SuppressWarnings("javadoc")
    public String getName() {
        return this.name;
    }

    @SuppressWarnings("javadoc")
    public void setName(final String name) {
        this.name = name;
    }

    @SuppressWarnings("javadoc")
    public String getUriTemplate() {
        return this.uriTemplate;
    }

    @SuppressWarnings("javadoc")
    public void setUriTemplate(final String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    @SuppressWarnings("javadoc")
    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    @SuppressWarnings("javadoc")
    public void setAttributes(final List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @ApiParam("If this is not specified, ACS uses resourceURI in the evaluation request as the resource URI to lookup "
            + "resource attributes. This URI template can be used to extract a contiguous subset of resourceURI. "
            + "For example, /region/us/report/asset/12 in evaluation request can be mapped to a resource /asset/12 by "
            + "defining attributeUriTemplate as /region/us/report{attribute_uri}. ACS extracts the value of URI "
            + "Template variable 'attribute_uri' as the resourceURI. ")
    public String getAttributeUriTemplate() {
        return this.attributeUriTemplate;
    }

    public void setAttributeUriTemplate(final String attributeUriTemplate) {
        this.attributeUriTemplate = attributeUriTemplate;
    }

}
