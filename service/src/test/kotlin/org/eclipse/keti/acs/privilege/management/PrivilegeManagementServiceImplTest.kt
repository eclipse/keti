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

package org.eclipse.keti.acs.privilege.management

import org.eclipse.keti.acs.SpringSecurityPolicyContextResolver
import org.eclipse.keti.acs.attribute.cache.AttributeCacheFactory
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorServiceImpl
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader
import org.eclipse.keti.acs.config.GraphBeanDefinitionRegistryPostProcessor
import org.eclipse.keti.acs.config.GraphConfig
import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepositoryProxy
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepositoryProxy
import org.eclipse.keti.acs.request.context.AcsRequestContextHolder
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.Parent
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.zone.management.ZoneService
import org.eclipse.keti.acs.zone.management.ZoneServiceImpl
import org.eclipse.keti.acs.zone.resolver.SpringSecurityZoneResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.Arrays
import java.util.Arrays.asList
import java.util.Collections
import java.util.HashSet

@ContextConfiguration(
    classes = [
        AcsRequestContextHolder::class,
        InMemoryDataSourceConfig::class,
        InMemoryPolicyEvaluationCache::class,
        AttributeCacheFactory::class,
        PrivilegeManagementServiceImpl::class,
        SpringSecurityPolicyContextResolver::class,
        SpringSecurityZoneResolver::class,
        ZoneServiceImpl::class,
        GraphBeanDefinitionRegistryPostProcessor::class,
        GraphConfig::class,
        SubjectRepositoryProxy::class,
        ResourceRepositoryProxy::class,
        AttributeConnectorServiceImpl::class,
        AttributeReaderFactory::class,
        PrivilegeServiceResourceAttributeReader::class,
        PrivilegeServiceSubjectAttributeReader::class
    ]
)
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@TestPropertySource("classpath:application.properties")
class PrivilegeManagementServiceImplTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    private lateinit var service: PrivilegeManagementService

    private val attributesUtilities = AttributesUtilities()

    private var fixedAttributes: Set<Attribute>? = null

    @Autowired
    private lateinit var zoneService: ZoneService

    private val testUtils = TestUtils()
    private var testZone: Zone? = null

    @BeforeClass
    fun beforeClass() {
        this.testZone = this.testUtils.setupTestZone("PrivilegeManagementServiceImplTest", this.zoneService)
    }

    @AfterClass
    fun cleanupAfterClass() {
        this.zoneService.deleteZone(this.testZone!!.name!!)
    }

    @BeforeMethod
    fun setup() {
        this.fixedAttributes = this.attributesUtilities.getSetOfAttributes(Attribute("acs", "group", "admin"))
    }

    @Test
    fun testAppendResources() {
        doAppendResourcesAndAssert("/asset/sanramon", "/asset/ny")
    }

    @Test(dataProvider = "emptyIdDataProvider", expectedExceptions = [(PrivilegeManagementException::class)])
    fun testAppendResourceWithEmptyResourceId(resourceIdentifier: String?) {
        val resource = createResource(resourceIdentifier)
        this.service.appendResources(asList(resource))
    }

    @DataProvider(name = "emptyIdDataProvider")
    private fun emptyIdDataProvider(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(
            arrayOf(""), arrayOf(null)
        )
    }

    @Test(expectedExceptions = [(PrivilegeManagementException::class)])
    fun testAppendNullResources() {
        this.service.appendResources(null)
    }

    @Test(expectedExceptions = [(PrivilegeManagementException::class)])
    fun testAppendEmptyResources() {
        this.service.appendResources(Collections.emptyList())
    }

    @Test
    fun testCreateResource() {
        doCreateResourceAndAssert("/asset/sanrafael")
    }

    @Test(expectedExceptions = [(PrivilegeManagementException::class)])
    fun testCreateNullResources() {
        this.service.upsertResource(null)
    }

    @Test
    fun testUpdateResource() {
        val resourceIdentifier = "/asset/sananselmo"
        val resource = doCreateResourceAndAssert(resourceIdentifier)

        val resourceAttributes = resource.attributes!!.toMutableSet()
        resourceAttributes.add(Attribute("acs", "group", "analyst"))
        resource.attributes = resourceAttributes

        val created = this.service.upsertResource(resource)
        Assert.assertFalse(created)
        val savedResource = this.service.getByResourceIdentifier(resource.resourceIdentifier!!)
        assertResource(savedResource, resource, 2)
    }

    @Test
    fun testDeleteResource() {
        val resourceIdentifier = "/asset/santarita"
        val resource = doCreateResourceAndAssert(resourceIdentifier)
        val id = resource.resourceIdentifier

        val deleted = this.service.deleteResource(id!!)
        Assert.assertTrue(deleted)
        Assert.assertNull(this.service.getByResourceIdentifier(id))
    }

    @Test
    fun testDeleteInvalidResource() {
        Assert.assertFalse(this.service.deleteResource("invalid_id"))
    }

    @Test
    fun testAppendSubjects() {
        try {
            val s1 = createSubject("dave", this.fixedAttributes)
            val s2 = createSubject("sanjeev", this.fixedAttributes)

            this.service.appendSubjects(asList(s1, s2))

            // able to get subject by identifier
            Assert.assertNotNull(this.service.getBySubjectIdentifier(s1.subjectIdentifier!!))
            Assert.assertNotNull(this.service.getBySubjectIdentifier(s2.subjectIdentifier!!))
        } finally {
            this.service.deleteSubject("dave")
            this.service.deleteSubject("sanjeev")
        }
    }

    @Test(expectedExceptions = [(PrivilegeManagementException::class)])
    fun testAppendNullSubjects() {
        this.service.appendSubjects(null)
    }

    @Test(expectedExceptions = [(PrivilegeManagementException::class)])
    fun testAppendEmptySubjects() {
        this.service.appendSubjects(Collections.emptyList())
    }

    @Test
    fun testCreateSubject() {
        val subjectIdentifier = "marissa"
        try {
            val subject = createSubject(subjectIdentifier, this.fixedAttributes)

            val created = this.service.upsertSubject(subject)
            Assert.assertTrue(created)
            Assert.assertTrue(this.service.getBySubjectIdentifier(subjectIdentifier) == subject)
        } finally {
            this.service.deleteSubject("marissa")
        }
    }

    @Test
    fun testCreateSubjectWithParent() {
        val testSubjectId = "marissa"
        val parentSubjectId = "bob"
        try {
            val marissa = createSubject(testSubjectId, this.fixedAttributes)

            val bob = createSubject(
                parentSubjectId,
                this.attributesUtilities.getSetOfAttributes(Attribute("acs", "group", "parent"))
            )
            this.service.upsertSubject(bob)
            marissa.parents = HashSet(Arrays.asList(Parent(parentSubjectId)))
            this.service.upsertSubject(marissa)

            Assert.assertEquals(this.service.getBySubjectIdentifier(testSubjectId), marissa)
            Assert.assertEquals(
                this.service.getBySubjectIdentifier(testSubjectId)!!.attributes,
                marissa.attributes
            )
        } finally {
            this.service.deleteSubject(testSubjectId)
            this.service.deleteSubject(parentSubjectId)
        }
    }

    @Test(expectedExceptions = [(PrivilegeManagementException::class)])
    fun testCreateNullSubject() {
        this.service.upsertSubject(null)
    }

    // TODO enable it back when the zone resolver is fully implemented
    @Test(expectedExceptions = [(SecurityException::class)], enabled = false)
    fun testCreateSubjectAndGetWithDifferentClientId() {
        val subjectIdentifier = "Dave-ID123"
        val subject = createSubject(subjectIdentifier, this.fixedAttributes)

        val created = this.service.upsertSubject(subject)
        Assert.assertTrue(created)
        Assert.assertTrue(this.service.getBySubjectIdentifier(subjectIdentifier) == subject)

        mockSecurityContext(this.testZone)
        val returnedSubject = this.service.getBySubjectIdentifier(subjectIdentifier)
        Assert.assertNull(returnedSubject)
    }

    // TODO enable it back when the zone resolver is fully implemented
    @Test(expectedExceptions = [(SecurityException::class)], enabled = false)
    fun testCreateResourceAndGetWithDifferentClientId() {
        val resourceIdentifier = "Dave-ID123"
        val resource = createResource(resourceIdentifier)

        val created = this.service.upsertResource(resource)
        Assert.assertTrue(created)
        Assert.assertTrue(this.service.getByResourceIdentifier(resourceIdentifier) == resource)

        mockSecurityContext(this.testZone)
        val returnedResource = this.service.getByResourceIdentifier(resourceIdentifier)
        Assert.assertNull(returnedResource)
    }

    /*
     * TODO Need this test when we start supporting multiple issuers
     *
     * public void testCreateSubjectAndGetWithDifferentIssuerId(){ String subjectIdentifier = "Dave-ID123"; Subject
     * subject = createSubject(subjectIdentifier); boolean created = this.service.upsertSubject(subject);
     * Assert.assertTrue(created);
     *
     * PolicyContextResloverUtilTest scope = new PolicyContextResloverUtilTest();
     * scope.mockSecurityContext("ISSUER_1234", CLIENT_ID); }
     */

    @Test
    fun testUpdateSubject() {
        val subjectIdentifier = "/asset/sananselmo"
        val subject = createSubject(subjectIdentifier, this.fixedAttributes)

        var created = this.service.upsertSubject(subject)
        Assert.assertTrue(created)
        Assert.assertTrue(this.service.getBySubjectIdentifier(subjectIdentifier) == subject)

        val subjectAttributes = subject.attributes!!.toMutableSet()
        subjectAttributes.add(Attribute("acs", "group", "analyst"))
        subject.attributes = subjectAttributes

        created = this.service.upsertSubject(subject)
        Assert.assertFalse(created)

        val savedSubject = this.service.getBySubjectIdentifier(subject.subjectIdentifier!!)
        assertSubject(savedSubject, subject, 2)
    }

    @Test
    fun testDeleteSubject() {
        val subjectIdentifier = "/asset/santarita"
        val subject = createSubject(subjectIdentifier, this.fixedAttributes)
        this.service.upsertSubject(subject)

        val deleted = this.service.deleteSubject(subjectIdentifier)
        Assert.assertTrue(deleted)
        Assert.assertNull(this.service.getBySubjectIdentifier(subjectIdentifier))
    }

    @Test
    fun testDeleteInvalidSubject() {
        Assert.assertFalse(this.service.deleteSubject("invalid_id"))
    }

    @Test
    fun testGetSubjects() {
        val r1 = createSubject("/asset/macfarland", this.fixedAttributes)
        val r2 = createSubject("/asset/oregon", this.fixedAttributes)

        this.service.appendSubjects(asList(r1, r2))
        val subjects = this.service.subjects
        Assert.assertEquals(subjects.size, 2)

    }

    private fun createSubject(
        subjectIdentifier: String,
        attributes: Set<Attribute>?
    ): BaseSubject {
        val subject = BaseSubject()
        subject.subjectIdentifier = subjectIdentifier
        subject.attributes = attributes
        return subject
    }

    private fun createResource(resourceIdentifier: String?): BaseResource {
        val resource = BaseResource()
        resource.resourceIdentifier = resourceIdentifier
        resource.attributes = this.fixedAttributes
        return resource
    }

    private fun assertResource(
        savedResource: BaseResource?,
        resource: BaseResource,
        numOfAttributes: Int
    ) {
        Assert.assertNotNull(savedResource)
        Assert.assertEquals(savedResource!!.resourceIdentifier, resource.resourceIdentifier)
        Assert.assertEquals(savedResource.attributes!!.size, numOfAttributes)
    }

    private fun assertSubject(
        savedSubject: BaseSubject?,
        subject: BaseSubject,
        numOfAttributes: Int
    ) {
        Assert.assertNotNull(savedSubject)
        Assert.assertEquals(savedSubject!!.subjectIdentifier, subject.subjectIdentifier)
        Assert.assertEquals(savedSubject.attributes!!.size, numOfAttributes)
    }

    fun doAppendResourcesAndAssert(
        identifier1: String,
        identifier2: String
    ) {
        val r1 = createResource(identifier1)
        val r2 = createResource(identifier2)

        this.service.appendResources(asList(r1, r2))

        val fetchedResource1 = this.service.getByResourceIdentifier(r1.resourceIdentifier!!)
        val fetchedResource2 = this.service.getByResourceIdentifier(r2.resourceIdentifier!!)

        val resources = this.service.resources
        Assert.assertTrue(resources.size >= 2)

        Assert.assertEquals(fetchedResource1, r1)
        Assert.assertEquals(fetchedResource2, r2)

    }

    private fun doCreateResourceAndAssert(resourceIdentifier: String): BaseResource {
        val resource = createResource(resourceIdentifier)

        val created = this.service.upsertResource(resource)
        Assert.assertTrue(created)

        Assert.assertTrue(this.service.getByResourceIdentifier(resourceIdentifier) == resource)
        return resource
    }

}
