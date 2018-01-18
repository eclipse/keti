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

package org.eclipse.keti.acs.privilege.management;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import org.eclipse.keti.acs.security.AbstractHttpMethodsFilter;

@Component
public class ResourceHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String UPDATE_RESOURCE_URI_REGEX = "\\A/v1/resource/[^/]+?/??\\Z";
    private static final String CREATE_GET_RESOURCE_URI_REGEX = "\\A/v1/resource/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(UPDATE_RESOURCE_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.GET, HttpMethod.DELETE, HttpMethod.HEAD)));
        uriPatternsAndAllowedHttpMethods.put(CREATE_GET_RESOURCE_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public ResourceHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
