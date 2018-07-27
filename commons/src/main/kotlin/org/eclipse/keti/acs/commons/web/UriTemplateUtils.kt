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

package org.eclipse.keti.acs.commons.web

import org.springframework.web.util.UriTemplate
import java.net.URI
import java.util.HashMap

/**
 * @author acs-engineers@ge.com
 */

/**
 * Generates an instance of the URI according to the template given by uriTemplate, by expanding the variables
 * with the values provided by keyValues.
 *
 * @param uriTemplate
 * The URI template
 * @param keyValues
 * Dynamic list of string of the form "key:value"
 * @return The corresponding URI instance
 */
fun expand(
    uriTemplate: String,
    vararg keyValues: String
): URI {

    val template = UriTemplate(uriTemplate)
    val uriVariables = HashMap<String, String>()

    for (kv in keyValues) {
        val keyValue = kv.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        uriVariables[keyValue[0]] = keyValue[1]
    }
    return template.expand(uriVariables)
}

fun isCanonicalMatch(
    uriTemplateDef: String,
    resourceUri: String
): Boolean {
    val canonicalResourceURI = URI.create(resourceUri).normalize().toString()
    val uriTemplate = UriTemplate(appendTrailingSlash(uriTemplateDef))
    return uriTemplate.matches(appendTrailingSlash(canonicalResourceURI))
}

fun appendTrailingSlash(s: String): String {
    return if (!s.endsWith("/")) {
        StringBuilder(s).append("/").toString()
    } else s
}
