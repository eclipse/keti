/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/
package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Policy;
import com.ge.predix.acs.model.ResourceType;
import com.ge.predix.acs.model.Target;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;

@Test
public class ResourceAttributeResolverTest {

    @Mock
    private PrivilegeManagementService privilegeManagementService;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
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
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.privilegeManagementService, resourceURI,
                null);

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
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.privilegeManagementService, "/a/b",
                null);
        Assert.assertEquals(resolver.resolveResourceURI(null), null);
    }

    public void testResolveResourceUriNoTarget() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.privilegeManagementService, "/a/b",
                null);
        Assert.assertEquals(resolver.resolveResourceURI(new Policy()), null);
    }

    public void testResolveResourceUriNoResource() {
        Target t = new Target();
        t.setAction("GET");
        Policy p = new Policy();
        p.setTarget(t);
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.privilegeManagementService, "/a/b",
                null);
        Assert.assertEquals(resolver.resolveResourceURI(new Policy()), null);
    }

    public void testGetResourceAttributes() {
        BaseResource testResource = new BaseResource();
        testResource.setResourceIdentifier("/test/resource");
        Set<Attribute> testResourceAttributes = new HashSet<>();
        testResourceAttributes.add(new Attribute("issuer1", "test-attr"));
        testResource.setAttributes(testResourceAttributes);

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getByResourceIdentifier(testResource.getResourceIdentifier()))
                .thenReturn(testResource);
        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(
                testResource.getResourceIdentifier())).thenReturn(testResource);
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.privilegeManagementService,
                testResource.getResourceIdentifier(), null);
        Assert.assertEquals(resolver.getResourceAttributes(getPolicy(null)), testResourceAttributes);
    }

    public void testGetResourceAttributesForNonExistingURI() {
        ResourceAttributeResolver resolver = new ResourceAttributeResolver(this.privilegeManagementService,
                "NonExistingURI", null);
        Assert.assertEquals(resolver.getResourceAttributes(getPolicy(null)).size(), 0);
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
