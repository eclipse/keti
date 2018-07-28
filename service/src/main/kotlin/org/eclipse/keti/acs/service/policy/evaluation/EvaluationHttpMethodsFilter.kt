/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.service.policy.evaluation

import org.eclipse.keti.acs.security.AbstractHttpMethodsFilter
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import java.util.LinkedHashMap

private const val EVALUATION_URI_REGEX = "\\A/v1/policy-evaluation/??\\Z"

private fun uriPatternsAndAllowedHttpMethods(): Map<String, Set<HttpMethod>> {
    val uriPatternsAndAllowedHttpMethods = LinkedHashMap<String, Set<HttpMethod>>()
    uriPatternsAndAllowedHttpMethods[EVALUATION_URI_REGEX] = setOf(HttpMethod.POST)
    return uriPatternsAndAllowedHttpMethods
}

@Component
class EvaluationHttpMethodsFilter : AbstractHttpMethodsFilter(uriPatternsAndAllowedHttpMethods())
