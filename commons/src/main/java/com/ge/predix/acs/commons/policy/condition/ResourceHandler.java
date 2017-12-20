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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.commons.policy.condition;

import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplate;

import com.ge.predix.acs.model.Attribute;

/**
 * DTO to represent resource in the groovy condition.
 *
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("nls")
public class ResourceHandler extends AbstractHandler {

    private final String resourceURI;
    private final String resourceURITemplate;

    /**
     * Creates a resource handler.
     *
     * @param attributeSet
     *            attributes
     * @param resourceURI
     *            resourceURI of the requested resource
     * @param resourceURLTemplate
     *            resource URI template in the policy
     */
    public ResourceHandler(final Set<Attribute> attributeSet, final String resourceURI,
            final String resourceURLTemplate) {
        super("Resource", attributeSet);
        this.resourceURI = resourceURI;
        this.resourceURITemplate = resourceURLTemplate;
    }

    /**
     * @param pathVariable
     *            name of path parameter in the URL
     * @return path parameter value
     */
    public String uriVariable(final String pathVariable) {
        if (StringUtils.isEmpty(this.resourceURITemplate) || StringUtils.isEmpty(this.resourceURI)
                || StringUtils.isEmpty(pathVariable)) {
            return "";
        }
        UriTemplate template = new UriTemplate(this.resourceURITemplate);
        Map<String, String> match = template.match(this.resourceURI);
        String pathVariableValue = match.get(pathVariable);
        return pathVariableValue != null ? pathVariableValue : "";
    }

}
