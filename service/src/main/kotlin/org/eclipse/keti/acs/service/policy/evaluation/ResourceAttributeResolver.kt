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

package org.eclipse.keti.acs.service.policy.evaluation

import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.attribute.readers.ResourceAttributeReader
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Policy
import org.eclipse.keti.acs.service.policy.matcher.UriTemplateVariableResolver
import org.springframework.web.util.UriTemplate
import java.util.HashMap
import java.util.HashSet

private const val ATTRIBUTE_URI_TEMPLATE_VARIABLE = "attribute_uri"

/**
 * @param requestResourceUri
 * URI of the resource from the policy evaluation request
 */
class ResourceAttributeResolver(
    private val resourceAttributeReader: ResourceAttributeReader,
    private val requestResourceUri: String,
    supplementalResourceAttributes: Set<Attribute>?
) {

    private val resourceAttributeMap = HashMap<String, Set<Attribute>>()
    private val supplementalResourceAttributes: Set<Attribute>
    private val uriTemplateVariableResolver = UriTemplateVariableResolver()

    init {
        if (null == supplementalResourceAttributes) {
            this.supplementalResourceAttributes = emptySet()
        } else {
            this.supplementalResourceAttributes = supplementalResourceAttributes
        }
    }

    fun getResult(policy: Policy): ResourceAttributeResolverResult {
        var resolvedResourceUri = resolveResourceURI(policy)
        var uriTemplateExists = true
        if (null == resolvedResourceUri) {
            resolvedResourceUri = this.requestResourceUri
            uriTemplateExists = false
        }
        var resourceAttributes: MutableSet<Attribute>? = this.resourceAttributeMap[resolvedResourceUri]?.toMutableSet()
        if (null == resourceAttributes) {
            resourceAttributes = HashSet(this.resourceAttributeReader.getAttributes(resolvedResourceUri)!!)
            resourceAttributes.addAll(this.supplementalResourceAttributes)
            this.resourceAttributeMap[resolvedResourceUri] = resourceAttributes
        }
        return ResourceAttributeResolverResult(resourceAttributes, resolvedResourceUri, uriTemplateExists)
    }

    fun resolveResourceURI(policy: Policy?): String? {
        if (attributeUriTemplateExists(policy)) {
            val attributeUriTemplate = policy!!.target!!.resource?.attributeUriTemplate
            val uriTemplate = UriTemplate(attributeUriTemplate)
            return this.uriTemplateVariableResolver.resolve(
                this.requestResourceUri, uriTemplate, ATTRIBUTE_URI_TEMPLATE_VARIABLE
            )
        }
        return null
    }

    private fun attributeUriTemplateExists(policy: Policy?): Boolean {
        return if (policy?.target?.resource != null) {
            StringUtils.isNotBlank(policy.target!!.resource!!.attributeUriTemplate)
        } else false
    }

    class ResourceAttributeResolverResult(
        val resourceAttributes: Set<Attribute>,
        val resovledResourceUri: String,
        val isAttributeUriTemplateFound: Boolean
    )
}
