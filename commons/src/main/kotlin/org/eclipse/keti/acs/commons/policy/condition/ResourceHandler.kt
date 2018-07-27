/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.commons.policy.condition

import org.eclipse.keti.acs.model.Attribute
import org.springframework.util.StringUtils
import org.springframework.web.util.UriTemplate

/**
 * DTO to represent resource in the groovy condition.
 *
 * @author acs-engineers@ge.com
 */

/**
 * Creates a resource handler.
 *
 * @param attributeSet
 * attributes
 * @param resourceURI
 * resourceURI of the requested resource
 * @param resourceURITemplate
 * resource URI template in the policy
 */
class ResourceHandler(
    attributeSet: Set<Attribute>?,
    private val resourceURI: String?,
    private val resourceURITemplate: String?
) : AbstractHandler("Resource", attributeSet) {

    /**
     * @param pathVariable
     * name of path parameter in the URL
     * @return path parameter value
     */
    fun uriVariable(pathVariable: String?): String {
        if (StringUtils.isEmpty(this.resourceURITemplate) || StringUtils.isEmpty(this.resourceURI) || StringUtils.isEmpty(
                pathVariable
            )
        ) {
            return ""
        }
        val template = UriTemplate(this.resourceURITemplate)
        val match = template.match(this.resourceURI)
        val pathVariableValue = match[pathVariable]
        return pathVariableValue ?: ""
    }
}
