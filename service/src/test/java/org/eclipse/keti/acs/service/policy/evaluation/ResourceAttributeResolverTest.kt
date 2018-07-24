/*******************************************************************************
 * Copyright 2018 General Electric Company
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

import com.nhaarman.mockito_kotlin.eq
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceResourceAttributeReader
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Policy
import org.eclipse.keti.acs.model.ResourceType
import org.eclipse.keti.acs.model.Target
import org.eclipse.keti.acs.rest.BaseResource
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.HashSet

@Test
class ResourceAttributeResolverTest {

    @Mock
    private lateinit var defaultResourceAttributeReader: PrivilegeServiceResourceAttributeReader

    private var testResource: BaseResource? = null

    @BeforeMethod
    fun beforeMethod() {
        MockitoAnnotations.initMocks(this)

        this.testResource = BaseResource("/test/resource")
        `when`(this.defaultResourceAttributeReader.getAttributes(eq(this.testResource!!.resourceIdentifier!!)))
            .thenReturn(emptySet())
    }

    /**
     * @param resourceURI
     * in the evaluation request
     * @param resolvedResourceURI
     * expected resource URI after attribute template is applied
     */
    @Test(dataProvider = "resourceUriProvider")
    @Throws(Exception::class)
    fun testResolveResourceUri(
        resourceURI: String,
        attributeUriTemplate: String?,
        resolvedResourceURI: String?
    ) {
        val resolver = ResourceAttributeResolver(
            this.defaultResourceAttributeReader, resourceURI, null
        )

        Assert.assertEquals(resolver.resolveResourceURI(getPolicy(attributeUriTemplate)), resolvedResourceURI)
    }

    @DataProvider
    fun resourceUriProvider(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf<Any?>("/v1/site/123/asset/456", "/v1{attribute_uri}/asset/{asset-id}", "/site/123"),
            arrayOf<Any?>("/v1/site/123/asset/456", "/v2/DoesNotExist/{attribute_uri}", null),
            // attributeUriTemplate not defined
            arrayOf<Any?>("/v1/site/123/asset/456", null, null),
            // attributeUriTemplate defined as " "
            arrayOf<Any?>("/v1/site/123/asset/456", " ", null)
        )
    }

    fun testResolveResourceUriNoPolicy() {
        val resolver = ResourceAttributeResolver(this.defaultResourceAttributeReader, "/a/b", null)
        Assert.assertEquals(resolver.resolveResourceURI(null), null)
    }

    fun testResolveResourceUriNoTarget() {
        val resolver = ResourceAttributeResolver(this.defaultResourceAttributeReader, "/a/b", null)
        Assert.assertEquals(resolver.resolveResourceURI(Policy()), null)
    }

    fun testResolveResourceUriNoResource() {
        val resolver = ResourceAttributeResolver(this.defaultResourceAttributeReader, "/a/b", null)
        Assert.assertEquals(resolver.resolveResourceURI(Policy()), null)
    }

    fun testGetResourceAttributes() {
        val resourceAttributes = HashSet<Attribute>()
        resourceAttributes.add(Attribute("issuer1", "test-attr"))
        this.testResource!!.attributes = resourceAttributes

        val supplementalResourceAttributes = HashSet<Attribute>()
        supplementalResourceAttributes.add(Attribute("https://acs.attributes.int", "site", "sanramon"))

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        `when`(this.defaultResourceAttributeReader.getAttributes(this.testResource!!.resourceIdentifier!!))
            .thenReturn(resourceAttributes)
        val resolver = ResourceAttributeResolver(
            this.defaultResourceAttributeReader,
            this.testResource!!.resourceIdentifier!!, supplementalResourceAttributes
        )

        val combinedResourceAttributes = resolver.getResult(getPolicy(null)).resourceAttributes
        Assert.assertNotNull(combinedResourceAttributes)
        Assert.assertTrue(combinedResourceAttributes.containsAll(resourceAttributes))
        Assert.assertTrue(combinedResourceAttributes.containsAll(supplementalResourceAttributes))
    }

    fun testGetResourceAttributesNoResourceFoundAndNoSupplementalAttributes() {
        val resolver = ResourceAttributeResolver(
            this.defaultResourceAttributeReader,
            "NonExistingURI", null
        )

        val combinedResourceAttributes = resolver.getResult(getPolicy(null)).resourceAttributes
        Assert.assertTrue(combinedResourceAttributes.isEmpty())
    }

    @Test
    fun testGetResourceAttributesResourceFoundWithNoAttributesAndNoSupplementalAttributes() {
        val resolver = ResourceAttributeResolver(
            this.defaultResourceAttributeReader,
            this.testResource!!.resourceIdentifier!!, null
        )

        val combinedResourceAttributes = resolver.getResult(getPolicy(null)).resourceAttributes
        Assert.assertTrue(combinedResourceAttributes.isEmpty())
    }

    @Test
    fun testGetResourceAttributesResourceFoundWithAttributesAndNoSupplementalAttributes() {
        val resourceAttributes = HashSet<Attribute>()
        resourceAttributes.add(Attribute("https://acs.attributes.int", "role", "administrator"))
        this.testResource!!.attributes = resourceAttributes

        `when`(this.defaultResourceAttributeReader.getAttributes(eq(this.testResource!!.resourceIdentifier!!)))
            .thenReturn(resourceAttributes)
        val resolver = ResourceAttributeResolver(
            this.defaultResourceAttributeReader,
            this.testResource!!.resourceIdentifier!!, null
        )

        val combinedResourceAttributes = resolver.getResult(getPolicy(null)).resourceAttributes
        Assert.assertNotNull(combinedResourceAttributes)
        Assert.assertTrue(combinedResourceAttributes.containsAll(resourceAttributes))
    }

    @Test
    fun testGetResourceAttributesSupplementalAttributesOnly() {
        val supplementalResourceAttributes = HashSet<Attribute>()
        supplementalResourceAttributes.add(Attribute("https://acs.attributes.int", "site", "sanramon"))

        val resolver = ResourceAttributeResolver(
            this.defaultResourceAttributeReader,
            this.testResource!!.resourceIdentifier!!, supplementalResourceAttributes
        )

        val combinedResourceAttributes = resolver.getResult(getPolicy(null)).resourceAttributes
        Assert.assertNotNull(combinedResourceAttributes)
        Assert.assertTrue(combinedResourceAttributes.containsAll(supplementalResourceAttributes))
    }

    private fun getPolicy(attributeTemplate: String?): Policy {
        val r = ResourceType()
        r.attributeUriTemplate = attributeTemplate
        val t = Target()
        t.resource = r
        val p = Policy()
        p.target = t
        return p
    }

    // attrTemplate does not match uriTemplate
    // no attrTemplate
}
