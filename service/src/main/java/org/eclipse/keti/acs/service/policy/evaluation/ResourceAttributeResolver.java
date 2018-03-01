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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.UriTemplate;

import org.eclipse.keti.acs.attribute.readers.ResourceAttributeReader;
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.model.Policy;
import org.eclipse.keti.acs.service.policy.matcher.UriTemplateVariableResolver;

public class ResourceAttributeResolver {

    private static final String ATTRIBUTE_URI_TEMPLATE_VARIABLE = "attribute_uri";

    private final Map<String, Set<Attribute>> resourceAttributeMap = new HashMap<>();
    private final ResourceAttributeReader resourceAttributeReader;
    private final Set<Attribute> supplementalResourceAttributes;
    private final String requestResourceUri;
    private final UriTemplateVariableResolver uriTemplateVariableResolver = new UriTemplateVariableResolver();

    /**
     * @param requestResourceUri
     *            URI of the resource from the policy evaluation request
     */
    public ResourceAttributeResolver(final ResourceAttributeReader resourceAttributeReader,
            final String requestResourceUri, final Set<Attribute> supplementalResourceAttributes) {
        this.resourceAttributeReader = resourceAttributeReader;
        this.requestResourceUri = requestResourceUri;
        if (null == supplementalResourceAttributes) {
            this.supplementalResourceAttributes = Collections.emptySet();
        } else {
            this.supplementalResourceAttributes = supplementalResourceAttributes;
        }
    }

    public ResourceAttributeResolverResult getResult(final Policy policy) {
        String resolvedResourceUri = resolveResourceURI(policy);
        boolean uriTemplateExists = true;
        if (null == resolvedResourceUri) {
            resolvedResourceUri = this.requestResourceUri;
            uriTemplateExists = false;
        }
        Set<Attribute> resourceAttributes = this.resourceAttributeMap.get(resolvedResourceUri);
        if (null == resourceAttributes) {
            resourceAttributes = new HashSet<>(this.resourceAttributeReader.getAttributes(resolvedResourceUri));
            resourceAttributes.addAll(this.supplementalResourceAttributes);
            this.resourceAttributeMap.put(resolvedResourceUri, resourceAttributes);
        }
        return new ResourceAttributeResolverResult(resourceAttributes, resolvedResourceUri, uriTemplateExists);
    }

    String resolveResourceURI(final Policy policy) {
        if (attributeUriTemplateExists(policy)) {
            String attributeUriTemplate = policy.getTarget().getResource().getAttributeUriTemplate();
            UriTemplate uriTemplate = new UriTemplate(attributeUriTemplate);
            return this.uriTemplateVariableResolver.resolve(this.requestResourceUri, uriTemplate,
                    ATTRIBUTE_URI_TEMPLATE_VARIABLE);
        }
        return null;
    }

    private boolean attributeUriTemplateExists(final Policy policy) {
        if (policy != null && policy.getTarget() != null && policy.getTarget().getResource() != null) {
            return StringUtils.isNotBlank(policy.getTarget().getResource().getAttributeUriTemplate());
        }
        return false;
    }

    public static class ResourceAttributeResolverResult {
        private final Set<Attribute> resourceAttributes;
        private final String resovledResourceUri;
        private final boolean attributeUriTemplateFound;

        public ResourceAttributeResolverResult(final Set<Attribute> resourceAttributes,
                final String resovledResourceUri, final boolean attributeUriTemplateFound) {
            this.resourceAttributes = resourceAttributes;
            this.resovledResourceUri = resovledResourceUri;
            this.attributeUriTemplateFound = attributeUriTemplateFound;
        }

        public Set<Attribute> getResourceAttributes() {
            return this.resourceAttributes;
        }

        public String getResovledResourceUri() {
            return this.resovledResourceUri;
        }

        public boolean isAttributeUriTemplateFound() {
            return this.attributeUriTemplateFound;
        }
    }
}
