/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.rest

import org.springframework.web.util.UriTemplate

/**
 * Utility class to extract ids from a given URI according to a given URI template.
 *
 * @author acs-engineers@ge.com
 */
/**
 * Constructor given URI template.
 *
 * @param uriTemplateValue
 * URI template used by the model
 */
class RestModelIdExtractor(uriTemplateValue: String) {

    private val uriTemplate: UriTemplate = UriTemplate(uriTemplateValue)

    /**
     * Extracts the "pathVariable" from the given "uri".
     *
     * @param uri
     * The input uri
     * @param pathVariable
     * The path variable being extracted
     * @return the value of the pathVariable
     */
    fun extractId(
        uri: String,
        pathVariable: String
    ): String? {
        return try {
            val pathVariables = this.uriTemplate.match(uri)
            pathVariables[pathVariable]
        } catch (e: Exception) {
            null
        }
    }
}
