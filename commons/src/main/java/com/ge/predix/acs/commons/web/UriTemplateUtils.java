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

package com.ge.predix.acs.commons.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.util.UriTemplate;

/**
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("nls")
public final class UriTemplateUtils {

    private UriTemplateUtils() {
        // Prevents instantiation.
    }

    /**
     * Generates an instance of the URI according to the template given by uriTemplate, by expanding the variables
     * with the values provided by keyValues.
     *
     * @param uriTemplate
     *            The URI template
     * @param keyValues
     *            Dynamic list of string of the form "key:value"
     * @return The corresponding URI instance
     */
    public static URI expand(final String uriTemplate, final String... keyValues) {

        UriTemplate template = new UriTemplate(uriTemplate);
        Map<String, String> uriVariables = new HashMap<>();

        for (String kv : keyValues) {
            String[] keyValue = kv.split(":");
            uriVariables.put(keyValue[0], keyValue[1]);
        }
        return template.expand(uriVariables);
    }

    public static boolean isCanonicalMatch(final String uriTemplateDef, final String resourceUri) {
        String canonicalResourceURI = URI.create(resourceUri).normalize().toString();
        UriTemplate uriTemplate = new UriTemplate(appendTrailingSlash(uriTemplateDef));
        return uriTemplate.matches(appendTrailingSlash(canonicalResourceURI));
    }

    public static String appendTrailingSlash(final String s) {
        if (!s.endsWith("/")) {
            return new StringBuilder(s).append("/").toString();
        }
        return s;
    }

}
