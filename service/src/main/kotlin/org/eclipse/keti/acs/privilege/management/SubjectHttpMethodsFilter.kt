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

package org.eclipse.keti.acs.privilege.management

import org.eclipse.keti.acs.security.AbstractHttpMethodsFilter
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import java.util.Arrays
import java.util.HashSet
import java.util.LinkedHashMap

private const val UPDATE_SUBJECT_URI_REGEX = "\\A/v1/subject/[^/]+?/??\\Z"
private const val CREATE_GET_SUBJECT_URI_REGEX = "\\A/v1/subject/??\\Z"

private fun uriPatternsAndAllowedHttpMethods(): Map<String, Set<HttpMethod>> {
    val uriPatternsAndAllowedHttpMethods = LinkedHashMap<String, Set<HttpMethod>>()
    uriPatternsAndAllowedHttpMethods[UPDATE_SUBJECT_URI_REGEX] =
        HashSet(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD))
    uriPatternsAndAllowedHttpMethods[CREATE_GET_SUBJECT_URI_REGEX] =
        HashSet(Arrays.asList(HttpMethod.POST, HttpMethod.GET, HttpMethod.HEAD))
    return uriPatternsAndAllowedHttpMethods
}

@Component
class SubjectHttpMethodsFilter : AbstractHttpMethodsFilter(uriPatternsAndAllowedHttpMethods())
