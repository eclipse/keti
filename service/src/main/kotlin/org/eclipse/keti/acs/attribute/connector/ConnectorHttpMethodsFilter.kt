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

package org.eclipse.keti.acs.attribute.connector

import org.eclipse.keti.acs.security.AbstractHttpMethodsFilter
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

private const val CHANGE_GET_RESOURCE_CONNECTOR_URI_REGEX = "\\A/v1/connector/resource/??\\Z"
private const val CHANGE_GET_SUBJECT_CONNECTOR_URI_REGEX = "\\A/v1/connector/subject/??\\Z"

private fun uriPatternsAndAllowedHttpMethods(): Map<String, Set<HttpMethod>> {
    return linkedMapOf(
        CHANGE_GET_RESOURCE_CONNECTOR_URI_REGEX to setOf(
            HttpMethod.PUT,
            HttpMethod.GET,
            HttpMethod.DELETE,
            HttpMethod.HEAD
        ),
        CHANGE_GET_SUBJECT_CONNECTOR_URI_REGEX to setOf(
            HttpMethod.PUT,
            HttpMethod.GET,
            HttpMethod.DELETE,
            HttpMethod.HEAD
        )
    )
}

@Component
class ConnectorHttpMethodsFilter : AbstractHttpMethodsFilter(uriPatternsAndAllowedHttpMethods())
