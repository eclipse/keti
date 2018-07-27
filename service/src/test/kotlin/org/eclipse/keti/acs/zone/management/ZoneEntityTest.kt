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

package org.eclipse.keti.acs.zone.management

import org.eclipse.keti.acs.SpringSecurityPolicyContextResolver
import org.eclipse.keti.acs.config.GraphBeanDefinitionRegistryPostProcessor
import org.eclipse.keti.acs.config.GraphConfig
import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepository
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepository
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.service.policy.admin.dao.PolicySetEntity
import org.eclipse.keti.acs.service.policy.admin.dao.PolicySetRepository
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.eclipse.keti.acs.zone.resolver.SpringSecurityZoneResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@ContextConfiguration(
    classes = [
        GraphBeanDefinitionRegistryPostProcessor::class,
        GraphConfig::class,
        InMemoryDataSourceConfig::class,
        SpringSecurityPolicyContextResolver::class,
        SpringSecurityZoneResolver::class,
        ZoneServiceImpl::class
    ]
)
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test(singleThreaded = true)
class ZoneEntityTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    private lateinit var zoneRepository: ZoneRepository

    @Autowired
    private lateinit var subjectRepository: SubjectRepository

    @Autowired
    private lateinit var resourceRepository: ResourceRepository

    @Autowired
    private lateinit var policySetRepository: PolicySetRepository

    @Autowired
    private lateinit var service: ZoneService

    // zone with 1 issuer - issuer1
    private var zone1: Zone? = null

    // Some tests modify this standard setup. Reset before every test.
    @BeforeMethod
    fun setup() {
        this.zone1 = Zone("zone1", "zone1", "description")
    }

    fun testCreateZoneWithConstraintViolationSubdomain() {
        val testzone1 = Zone("zone1", "subdomain1", "description")
        val zone2SameSubdomain = Zone("zone2", "subdomain1", "description")

        try {
            this.service.upsertZone(testzone1)
            this.service.upsertZone(zone2SameSubdomain)
        } catch (e: ZoneManagementException) {

            Assert.assertEquals(e.message, "Subdomain subdomain1 for zoneName = zone2 is already being used.")
            this.service.deleteZone(testzone1.name!!)
            this.service.deleteZone(zone2SameSubdomain.name!!)
            return
        }

        this.service.deleteZone(testzone1.name!!)
        this.service.deleteZone(zone2SameSubdomain.name!!)
        Assert.fail("Expected ZoneManagementException to be thrown.")
    }

    fun testUpdateZoneSubdomain() {
        this.service.upsertZone(this.zone1!!)
        val updatedZone1 = Zone("zone1", "updated-subdomain", "description")
        this.service.upsertZone(updatedZone1)
        val actualZone = this.zoneRepository.getByName(this.zone1!!.name!!)
        Assert.assertEquals(actualZone!!.subdomain, "updated-subdomain")
    }

    fun testCreateZone() {
        this.service.upsertZone(this.zone1!!)
        val zone1Actual = this.service.retrieveZone(this.zone1!!.name!!)
        Assert.assertEquals(zone1Actual, this.zone1)
    }

    fun testDeleteZoneWithCascade() {

        // create test zone
        val testZone = Zone("test-zone", "test-zone-subdomain", "description")
        this.service.upsertZone(testZone)
        var testZoneEntity = this.zoneRepository.getByName(testZone.name!!)

        // put subject, resource, and policy
        val subjectEntity = SubjectEntity(testZoneEntity!!, "bob")
        subjectEntity.attributesAsJson = "{}"
        this.subjectRepository.save(subjectEntity)

        val resourceEntity = ResourceEntity(testZoneEntity, "bob")
        resourceEntity.attributesAsJson = "[]"
        this.resourceRepository.save(resourceEntity)

        val policySetEntity = PolicySetEntity(testZoneEntity, "policy-set-2", "{}")
        this.policySetRepository.save(policySetEntity)

        // Check if in repo
        Assert.assertEquals(this.subjectRepository.getByZoneAndSubjectIdentifier(testZoneEntity, "bob"), subjectEntity)
        Assert.assertEquals(
            this.resourceRepository.getByZoneAndResourceIdentifier(testZoneEntity, "bob"),
            resourceEntity
        )
        Assert.assertEquals(
            this.policySetRepository.getByZoneAndPolicySetId(testZoneEntity, "policy-set-2"),
            policySetEntity
        )
        Assert.assertEquals(testZoneEntity.name, testZone.name)

        // delete zone and assert proper cascading
        this.service.deleteZone(testZone.name!!)
        Assert.assertEquals(this.subjectRepository.getByZoneAndSubjectIdentifier(testZoneEntity, "bob"), null)
        Assert.assertEquals(this.resourceRepository.getByZoneAndResourceIdentifier(testZoneEntity, "bob"), null)
        Assert.assertEquals(this.policySetRepository.getByZoneAndPolicySetId(testZoneEntity, "policy-set-2"), null)
        testZoneEntity = this.zoneRepository.getByName(testZone.name!!)
        Assert.assertEquals(testZoneEntity, null)
    }
}
