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

package com.ge.predix.acs.monitoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.security.AbstractHttpMethodsFilter;

@Component
public class MonitoringHttpMethodsFilter extends AbstractHttpMethodsFilter {

    private static final String HEARTBEAT_URI_REGEX = "\\A/monitoring/heartbeat/??\\Z";

    private static Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods() {
        Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods = new LinkedHashMap<>();
        uriPatternsAndAllowedHttpMethods.put(HEARTBEAT_URI_REGEX,
                new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD)));
        return uriPatternsAndAllowedHttpMethods;
    }

    public MonitoringHttpMethodsFilter() {
        super(uriPatternsAndAllowedHttpMethods());
    }
}
