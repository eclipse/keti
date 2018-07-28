/*******************************************************************************
 * Copyright 2018 General Electric Company
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
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.eclipse.keti.acs.zone.management.ZoneServiceImpl
import org.eclipse.keti.acs.zone.resolver.SpringSecurityZoneResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.ArrayList
import java.util.concurrent.ExecutionException

@ContextConfiguration(
    classes = [
        AcsRequestContextHolder::class,
        InMemoryDataSourceConfig::class,
        InMemoryPolicyEvaluationCache::class,
        AttributeCacheFactory::class,
        PrivilegeManagementServiceImpl::class,
        GraphBeanDefinitionRegistryPostProcessor::class,
        GraphConfig::class,
        ZoneServiceImpl::class,
        SpringSecurityPolicyContextResolver::class,
        SpringSecurityZoneResolver::class,
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
@Test
class PrivilegeManagementNoRollbackTest : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var service: PrivilegeManagementService

    @Autowired
    private lateinit var zoneService: ZoneService

    private val attributesUtilities = AttributesUtilities()
    private var fixedAttributes: Set<Attribute>? = null
    private val testUtils = TestUtils()
    private var testZone: Zone? = null

    @BeforeClass
    @Throws(InterruptedException::class, ExecutionException::class)
    fun setup() {
        this.testZone = this.testUtils.setupTestZone("PrivilegeManagementNoRollbackTest", this.zoneService)
        this.fixedAttributes = this.attributesUtilities.getSetOfAttributes(Attribute("acs", "group", "admin"))
    }

    @AfterClass
    fun cleanup() {
        this.zoneService.deleteZone(this.testZone!!.name!!)
    }

    fun testCreateMultipleSubjectWithConstraintViolationSubjectIdentifier() {
        val subjectIdentifier = "Dave-ID123"

        val subjects = ArrayList<BaseSubject>()
        subjects.add(createSubject(subjectIdentifier))
        subjects.add(createSubject(subjectIdentifier))
        try {
            this.service.appendSubjects(subjects)
        } catch (e: PrivilegeManagementException) {
            // not checking id in toString(), just validating rest of error
            // message due to id mismatch on CI
            val checkMessage = e.message?.contains("Unable to persist Subject(s) for zone")!! ||
                e.message?.contains("Duplicate Subject(s)")!!
            Assert.assertTrue(checkMessage, "Invalid Error Message: " + e.message)
            Assert.assertEquals(this.service.subjects.size, 0)
            return
        }

        Assert.fail("Expected PrivilegeManagementException to be thrown.")
    }

    fun testCreateMultipleResourceWithConstraintViolationResourceIdentifier() {

        val resourceList = ArrayList<BaseResource>()

        val resourceIdentifier = "Brittany123"

        resourceList.add(createResource(resourceIdentifier))
        resourceList.add(createResource(resourceIdentifier))

        try {
            this.service.appendResources(resourceList)
        } catch (e: PrivilegeManagementException) {
            val checkMessage = e.message?.contains("Unable to persist Resource(s) for zone")!! ||
                e.message?.contains("Duplicate Resource(s)")!!
            Assert.assertTrue(checkMessage, "Invalid Error Message: " + e.message)
            Assert.assertEquals(this.service.resources.size, 0)
            return
        }

        Assert.fail("Expected PrivilegeManagementException to be thrown.")
    }

    private fun createSubject(subjectIdentifier: String): BaseSubject {
        val subject = BaseSubject()
        subject.subjectIdentifier = subjectIdentifier
        subject.attributes = this.fixedAttributes
        return subject
    }

    private fun createResource(resourceIdentifier: String): BaseResource {
        val resource = BaseResource()
        resource.resourceIdentifier = resourceIdentifier
        resource.attributes = this.fixedAttributes
        return resource
    }
}
