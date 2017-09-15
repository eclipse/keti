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

package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.readers.PrivilegeServiceResourceAttributeReader;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Policy;
import com.ge.predix.acs.model.ResourceType;
import com.ge.predix.acs.model.Target;
import com.ge.predix.acs.rest.BaseResource;

@Test
public class ResourceAttributeResolverTest {
    @Mock
    private PrivilegeServiceResourceAttributeReader defaultResourceAttributeReader;

    private BaseResource testResource;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);

        this.testResource = new BaseResource("/test/resource");
        when(this.defaultResourceAttributeReader.getAttributes(eq(this.testResource.getResourceIdentifier())))
                .thenReturn(Collections.emptySet());
    }

    /**
     * @param resourceURI
     *            in the evaluation request
     * @param resolvedResourceURI
     *            expected resource URI after attribute template is applied
     */
    @Test(dataProvider = "resourceUriProvider")
    public void testResolveResourceUri(final String resourceURI, final String attributeUriTemplate,
            final String resolvedResourceURI) throws Exception {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(
            this.defaultResourceAttributeReader, resourceURI, null);

        Assert.assertEquals(resolver.resolveResourceURI(getPolicy(attributeUriTemplate)), resolvedResourceURI);
    }

    @DataProvider
    public Object[][] resourceUriProvider() {
        return new Object[][] { { "/v1/site/123/asset/456", "/v1{attribute_uri}/asset/{asset-id}", "/site/123" },
                { "/v1/site/123/asset/456", "/v2/DoesNotExist/{attribute_uri}", null },
                // attributeUriTemplate not defined
                { "/v1/site/123/asset/456", null, null },
                // attributeUriTemplate defined as " "
                { "/v1/site/123/asset/456", " ", null } };
    }

    public void testResolveResourceUriNoPolicy() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader, "/a/b",
                null);
        Assert.assertEquals(resolver.resolveResourceURI(null), null);
    }

    public void testResolveResourceUriNoTarget() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader, "/a/b",
                null);
        Assert.assertEquals(resolver.resolveResourceURI(new Policy()), null);
    }

    public void testResolveResourceUriNoResource() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader, "/a/b",
                null);
        Assert.assertEquals(resolver.resolveResourceURI(new Policy()), null);
    }

    public void testGetResourceAttributes() {
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("issuer1", "test-attr"));
        this.testResource.setAttributes(resourceAttributes);

        Set<Attribute> supplementalResourceAttributes = new HashSet<>();
        supplementalResourceAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.defaultResourceAttributeReader.getAttributes(this.testResource.getResourceIdentifier()))
                .thenReturn(resourceAttributes);
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader,
                this.testResource.getResourceIdentifier(), supplementalResourceAttributes);

        Set<Attribute> combinedResourceAttributes = resolver.getResult(getPolicy(null)).getResourceAttributes();
        Assert.assertNotNull(combinedResourceAttributes);
        Assert.assertTrue(combinedResourceAttributes.containsAll(resourceAttributes));
        Assert.assertTrue(combinedResourceAttributes.containsAll(supplementalResourceAttributes));
    }

    public void testGetResourceAttributesNoResourceFoundAndNoSupplementalAttributes() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader,
                "NonExistingURI", null);

        Set<Attribute> combinedResourceAttributes = resolver.getResult(getPolicy(null)).getResourceAttributes();
        Assert.assertTrue(combinedResourceAttributes.isEmpty());
    }

    @Test
    public void testGetResourceAttributesResourceFoundWithNoAttributesAndNoSupplementalAttributes() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader,
                this.testResource.getResourceIdentifier(), null);

        Set<Attribute> combinedResourceAttributes = resolver.getResult(getPolicy(null)).getResourceAttributes();
        Assert.assertTrue(combinedResourceAttributes.isEmpty());
    }

    @Test
    public void testGetResourceAttributesResourceFoundWithAttributesAndNoSupplementalAttributes() {
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        this.testResource.setAttributes(resourceAttributes);

        when(this.defaultResourceAttributeReader.getAttributes(eq(this.testResource.getResourceIdentifier())))
                .thenReturn(resourceAttributes);
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader,
                this.testResource.getResourceIdentifier(), null);

        Set<Attribute> combinedResourceAttributes = resolver.getResult(getPolicy(null)).getResourceAttributes();
        Assert.assertNotNull(combinedResourceAttributes);
        Assert.assertTrue(combinedResourceAttributes.containsAll(resourceAttributes));
    }

    @Test
    public void testGetResourceAttributesSupplementalAttributesOnly() {
        Set<Attribute> supplementalResourceAttributes = new HashSet<>();
        supplementalResourceAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.defaultResourceAttributeReader,
                this.testResource.getResourceIdentifier(), supplementalResourceAttributes);

        Set<Attribute> combinedResourceAttributes = resolver.getResult(getPolicy(null)).getResourceAttributes();
        Assert.assertNotNull(combinedResourceAttributes);
        Assert.assertTrue(combinedResourceAttributes.containsAll(supplementalResourceAttributes));
    }

    private Policy getPolicy(final String attributeTemplate) {
        ResourceType r = new ResourceType();
        r.setAttributeUriTemplate(attributeTemplate);
        Target t = new Target();
        t.setResource(r);
        Policy p = new Policy();
        p.setTarget(t);
        return p;
    }

    // attrTemplate does not match uriTemplate
    // no attrTemplate
}
