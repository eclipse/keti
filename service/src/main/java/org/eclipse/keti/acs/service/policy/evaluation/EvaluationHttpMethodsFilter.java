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

package org.eclipse.keti.acs.service.policy.evaluation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import org.eclipse.keti.acs.security.AbstractHttpMethodsFilter;

@Component
public class EvaluationHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String EVALUATION_URI_REGEX = "\\A/v1/policy-evaluation/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(EVALUATION_URI_REGEX, Collections.singleton(HttpMethod.POST));
        return uriPatternsAndAllowedHttpMethods;
    }

    public EvaluationHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
