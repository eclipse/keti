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

package org.eclipse.keti.acs.service.policy.admin

import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.SpringSecurityPolicyContextResolver
import org.eclipse.keti.acs.attribute.cache.AttributeCacheFactory
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorServiceImpl
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.policy.evaluation.cache.InMemoryPolicyEvaluationCache
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementServiceImpl
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepositoryProxy
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepositoryProxy
import org.eclipse.keti.acs.service.InvalidACSRequestException
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidator
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidatorImpl
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.eclipse.keti.acs.zone.resolver.SpringSecurityZoneResolver
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

private const val SUBDOMAIN1 = "tenant1"
private const val SUBDOMAIN2 = "tenant2"
private const val DEFAULT_SUBDOMAIN = "defaultTenant"

@Test
@TestPropertySource("classpath:application.properties")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@ContextConfiguration(
    classes = [
        InMemoryPolicyEvaluationCache::class,
        InMemoryDataSourceConfig::class,
        AttributeCacheFactory::class,
        PolicyManagementServiceImpl::class,
        SpringSecurityPolicyContextResolver::class,
        PolicySetValidatorImpl::class,
        GroovyConditionShell::class,
        SpringSecurityZoneResolver::class,
        GroovyConditionCache::class,
        AttributeConnectorServiceImpl::class,
        AttributeReaderFactory::class,
        PrivilegeServiceResourceAttributeReader::class,
        PrivilegeServiceSubjectAttributeReader::class,
        PrivilegeManagementServiceImpl::class,
        SubjectRepositoryProxy::class,
        ResourceRepositoryProxy::class
    ]
)
class PolicyManagementServiceTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    @Spy
    private lateinit var policySetValidator: PolicySetValidator

    @Autowired
    @InjectMocks
    private lateinit var policyService: PolicyManagementServiceImpl

    @Autowired
    private lateinit var zoneRepository: ZoneRepository

    @Mock
    private val mockZoneResolver = mock(ZoneResolver::class.java)

    private val jsonUtils = JsonUtils()

    private val zone1 = this.createZone("zone1", SUBDOMAIN1, "description for Zone1")
    private val zone2 = this.createZone("zone2", SUBDOMAIN2, "description for Zone2")
    private val defaultZone = this
        .createZone("defaultZone", DEFAULT_SUBDOMAIN, "description for defaultZone")

    @BeforeClass
    fun beforeClass() {
        this.zoneRepository.save(this.zone1)
        this.zoneRepository.save(this.zone2)
        this.zoneRepository.save(this.defaultZone)
    }

    @AfterClass
    fun afterClass() {

        this.zoneRepository.delete(this.defaultZone)
        this.zoneRepository.delete(this.zone1)
        this.zoneRepository.delete(this.zone2)
    }

    @BeforeMethod
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        ReflectionTestUtils.setField(this.policyService, "policySetValidator", this.policySetValidator)
        initializeDefaultResolverBehavior()
    }

    fun testDeleteWhenPolicySetExists() {
        val policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)
        this.policyService.upsertPolicySet(policySet!!)
        this.policyService.deletePolicySet(policySet.name)
        Mockito.verify<PolicySetValidator>(this.policySetValidator, Mockito.times(1))
            .removeCachedConditions(any())
        val retrievedPolicySet = this.policyService.getPolicySet(policySet.name!!)
        Assert.assertNull(retrievedPolicySet)
    }

    fun testDeleteWhenPolicySetDoesNotExists() {
        this.policyService.deletePolicySet("policyId")
        Assert.assertTrue(true) // no exception throw means, the test passed
    }

    fun testDeleteWhenPolicySetIdIsNull() {
        this.policyService.deletePolicySet(null)
        Assert.assertTrue(true) // no exception throw means, the test passed
    }

    fun testCreatePolicySetPositive() {
        val policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)
        val policyName = policySet!!.name
        this.policyService.upsertPolicySet(policySet)
        val savedPolicySet = this.policyService.getPolicySet(policyName!!)
        Assert.assertNotNull(savedPolicySet)
        Assert.assertEquals(savedPolicySet!!.policies.size, 1)
        Assert.assertEquals(
            savedPolicySet.policies[0].target!!.resource!!.uriTemplate,
            "/secured-by-value/sites/sanramon"
        )
        this.policyService.deletePolicySet(policyName)
        Assert.assertEquals(this.policyService.allPolicySets.size, 0)
    }

    @Test
    fun testCreateApmPolicySetPositive() {
        val policySet = this.jsonUtils
            .deserializeFromFile("testApmPolicySetLoadsSuccessfully.json", PolicySet::class.java)
        val policyName = policySet!!.name
        this.policyService.upsertPolicySet(policySet)
        val savedPolicySet = this.policyService.getPolicySet(policyName!!)
        Assert.assertNotNull(savedPolicySet)
        Assert.assertEquals(savedPolicySet!!.name, policyName)
        this.policyService.deletePolicySet(policyName)
        Assert.assertEquals(this.policyService.allPolicySets.size, 0)
    }

    fun testUpdatePolicySet() {
        val policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)

        this.policyService.upsertPolicySet(policySet!!)

        var retPolicySet = this.policyService.getPolicySet(policySet.name!!)
        Assert.assertEquals(retPolicySet!!.name, policySet.name)
        Assert.assertEquals(retPolicySet.policies.size, policySet.policies.size)

        // Now we want to update
        policySet.policies[0].effect = Effect.DENY
        this.policyService.upsertPolicySet(policySet)

        // get the policy back
        retPolicySet = this.policyService.getPolicySet(policySet.name!!)

        Assert.assertEquals(retPolicySet!!.policies[0].effect, Effect.DENY)

        this.policyService.deletePolicySet(policySet.name)
        Assert.assertEquals(this.policyService.allPolicySets.size, 0)
    }

    fun testCreateMultiplePolicySets() {
        val policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)
        val policySet2 = this.jsonUtils
            .deserializeFromFile("policy-set-with-one-policy-one-condition.json", PolicySet::class.java)

        this.policyService.upsertPolicySet(policySet!!)
        try {
            this.policyService.upsertPolicySet(policySet2!!)
            val expectedPolicySets = this.policyService.allPolicySets
            Assert.assertEquals(expectedPolicySets.size, 2)
        } catch (e: PolicyManagementException) {
            Assert.fail("Creation of 2nd policySet failed.")
        } finally {
            this.policyService.deletePolicySet(policySet.name)
            this.policyService.deletePolicySet(policySet2!!.name)
            Assert.assertEquals(this.policyService.allPolicySets.size, 0)
        }
    }

    @Test(expectedExceptions = [(PolicyManagementException::class)])
    fun testCreatePolicySetWithInvalidConditions() {
        val policySet = this.jsonUtils
            .deserializeFromFile("policy-set-with-one-policy-invalid-condition.json", PolicySet::class.java)
        this.policyService.upsertPolicySet(policySet!!)
    }

    @Test(expectedExceptions = [(PolicyManagementException::class)])
    fun testCreatePolicySetWithInvalidJson() {
        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/missing-effect-policy.json", PolicySet::class.java)
        this.policyService.upsertPolicySet(policySet!!)
    }

    @Test(expectedExceptions = [(PolicyManagementException::class)])
    fun testCreatePolicySetWithMissingClientId() {
        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail)
            .thenAnswer { _ -> throw InvalidACSRequestException() }
        this.createSimplePolicySet()
    }

    @Test(expectedExceptions = [(PolicyManagementException::class)])
    fun testCreatePolicySetWithMissingIssuer() {
        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail)
            .thenAnswer { _ -> throw InvalidACSRequestException() }
        this.createSimplePolicySet()
    }

    fun testCreatePolicySetsForMultipleApplications() {
        val client1PolicySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)
        val client2PolicySet = this.jsonUtils
            .deserializeFromFile("policy-set-with-one-policy-one-condition.json", PolicySet::class.java)

        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone1)

        this.policyService.upsertPolicySet(client1PolicySet!!)
        Assert.assertEquals(this.policyService.allPolicySets.size, 1)
        Assert.assertEquals(this.policyService.allPolicySets[0].name, client1PolicySet.name)

        // Add and assert policyset for client2, with client1 policySet already
        // created

        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone2)
        this.policyService.upsertPolicySet(client2PolicySet!!)
        Assert.assertEquals(this.policyService.allPolicySets.size, 1)
        Assert.assertEquals(this.policyService.allPolicySets[0].name, client2PolicySet.name)

        this.policyService.deletePolicySet(client2PolicySet.name)

        // Cleanup PolicySet for client1
        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone1)
        this.policyService.deletePolicySet(client1PolicySet.name)
        Assert.assertEquals(this.policyService.allPolicySets.size, 0)
    }

    // TODO Enable this test once the service is updated to use real zones
    @Test(enabled = false)
    fun testGetAllPolicySetAndReturnEmptyList() {
        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone1)

        val allPolicySets = this.policyService.allPolicySets
        Assert.assertEquals(allPolicySets.size, 0)
    }

    fun testCreatePolicySetsForMultipleZones() {
        val issuer1PolicySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)
        val issuer2PolicySet = this.jsonUtils
            .deserializeFromFile("policy-set-with-one-policy-one-condition.json", PolicySet::class.java)

        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone1)
        this.policyService.upsertPolicySet(issuer1PolicySet!!)

        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone2)
        this.policyService.upsertPolicySet(issuer2PolicySet!!)

        Assert.assertEquals(this.policyService.allPolicySets.size, 1)

        this.policyService.deletePolicySet(issuer2PolicySet.name)
        // need this to delete issuer1PolicySet properly (policy-set-id and
        // zone_id are used to find the row)
        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.zone1)
        this.policyService.deletePolicySet(issuer1PolicySet.name)
        Assert.assertEquals(this.policyService.allPolicySets.size, 0)
    }

    private fun createSimplePolicySet() {
        val policySet = this.jsonUtils.deserializeFromFile("set-with-1-policy.json", PolicySet::class.java)
        this.policyService.upsertPolicySet(policySet!!)
    }

    private fun initializeDefaultResolverBehavior() {
        Mockito.`when`(this.mockZoneResolver.zoneEntityOrFail).thenReturn(this.defaultZone)
        ReflectionTestUtils.setField(this.policyService, "zoneResolver", this.mockZoneResolver)
    }

    private fun createZone(
        name: String,
        subdomain: String,
        description: String
    ): ZoneEntity {
        val zone = ZoneEntity()
        zone.name = name
        zone.subdomain = subdomain
        zone.description = description
        return zone
    }
}
