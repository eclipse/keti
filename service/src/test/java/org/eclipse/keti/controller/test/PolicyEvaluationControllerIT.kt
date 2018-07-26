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

package org.eclipse.keti.controller.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementService
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.service.policy.admin.PolicyManagementService
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedHashSet

private val OBJECT_MAPPER = ObjectMapper()
private const val POLICY_EVAL_URL = "v1/policy-evaluation"

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test
class PolicyEvaluationControllerIT : AbstractTestNGSpringContextTests() {

    private val jsonUtils = JsonUtils()
    private val testUtils = TestUtils()
    private var testZone: Zone? = null
    private var testSubject: BaseSubject? = null
    private var testResource: BaseResource? = null
    private var denyPolicySet: List<PolicySet>? = null
    private var notApplicableAndDenyPolicySets: List<PolicySet>? = null

    @Autowired
    private lateinit var wac: WebApplicationContext

    @Autowired
    private lateinit var zoneService: ZoneService

    @Autowired
    private lateinit var privilegeManagementService: PrivilegeManagementService

    @Autowired
    private lateinit var policyManagementService: PolicyManagementService

    @BeforeClass
    fun setup() {

        this.testZone = TestUtils().setupTestZone("PolicyEvaluationControllerITZone", zoneService)
        this.testSubject = BaseSubject("testSubject")
        this.testResource = BaseResource("testResource")
        Assert.assertTrue(this.privilegeManagementService.upsertResource(this.testResource))
        Assert.assertTrue(this.privilegeManagementService.upsertSubject(this.testSubject))

        this.denyPolicySet = createDenyPolicySet()
        this.notApplicableAndDenyPolicySets = createNotApplicableAndDenyPolicySets()
    }

    @AfterMethod
    fun testCleanup() {
        val policySets = this.policyManagementService.allPolicySets
        policySets.forEach { policySet -> this.policyManagementService.deletePolicySet(policySet.name) }
    }

    @Test
    @Throws(Exception::class)
    fun testPolicyZoneDoesNotExistException() {
        mockSecurityContext(null)
        mockAcsRequestContext()
        val policyEvalRequest = createPolicyEvalRequest(
            this.testResource!!.resourceIdentifier, this.testSubject!!.subjectIdentifier, LinkedHashSet()
        )
        val postPolicyEvalContext =
            this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, POLICY_EVAL_URL)
        val resultActions = postPolicyEvalContext.mockMvc.perform(
            postPolicyEvalContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    policyEvalRequest
                )
            )
        )
        resultActions.andReturn().response.contentAsString!!.contentEquals("Zone not found")
        resultActions.andExpect(status().isBadRequest)

        mockSecurityContext(this.testZone)
        mockAcsRequestContext()
    }

    @Test
    @Throws(Exception::class)
    fun testPolicyInvalidMediaTypeResponseStatusCheck() {

        val postPolicyEvalContext =
            this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, POLICY_EVAL_URL)
        postPolicyEvalContext.mockMvc.perform(
            postPolicyEvalContext.builder.contentType(MediaType.IMAGE_GIF_VALUE).content("testString")
        ).andExpect(status().isUnsupportedMediaType)
    }

    @Test(dataProvider = "policyEvalDataProvider")
    @Throws(Exception::class)
    fun testPolicyEvaluation(
        policyEvalRequest: PolicyEvaluationRequestV1,
        policySets: List<PolicySet>?,
        expectedEffect: Effect
    ) {

        if (policySets != null) {
            upsertMultiplePolicySets(policySets)
        }

        val postPolicyEvalContext =
            this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, POLICY_EVAL_URL)
        val mvcResult = postPolicyEvalContext.mockMvc.perform(
            postPolicyEvalContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    policyEvalRequest
                )
            )
        ).andExpect(status().isOk).andReturn()
        val policyEvalResult =
            OBJECT_MAPPER.readValue(mvcResult.response.contentAsByteArray, PolicyEvaluationResult::class.java)

        assertThat(policyEvalResult.effect, equalTo(expectedEffect))
    }

    @Test(dataProvider = "policyEvalBadRequestDataProvider")
    @Throws(Exception::class)
    fun testPolicyEvaluationBadRequest(
        policyEvalRequest: PolicyEvaluationRequestV1,
        policySets: List<PolicySet>
    ) {

        upsertMultiplePolicySets(policySets)

        val postPolicyEvalContext =
            this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, POLICY_EVAL_URL)
        postPolicyEvalContext.mockMvc.perform(
            postPolicyEvalContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    policyEvalRequest
                )
            )
        ).andExpect(status().isBadRequest)
    }

    @DataProvider(name = "policyEvalDataProvider")
    private fun policyEvalDataProvider(): Array<Array<Any?>> {
        return arrayOf(
            requestEvaluationWithEmptyPolicySet(),
            requestEvaluationWithOnePolicySetAndEmptyPriorityList(),
            requestEvaluationWithOnePolicySetAndPriorityList(),
            requestEvaluationWithAllOfTwoPolicySets(),
            requestEvaluationWithFirstOfTwoPolicySets(),
            requestEvaluationWithSecondOfTwoPolicySets()
        )
    }

    private fun requestEvaluationWithEmptyPolicySet(): Array<Any?> {
        return arrayOf(
            createPolicyEvalRequest(
                this.testResource!!.resourceIdentifier,
                this.testSubject!!.subjectIdentifier,
                LinkedHashSet()
            ), emptyList<Any>(), Effect.NOT_APPLICABLE
        )
    }

    private fun requestEvaluationWithSecondOfTwoPolicySets(): Array<Any?> {
        return arrayOf(
            createPolicyEvalRequest(this.testResource!!.resourceIdentifier,
                this.testSubject!!.subjectIdentifier,
                LinkedHashSet(Arrays.asList(this.notApplicableAndDenyPolicySets!![1].name))
            ), this.notApplicableAndDenyPolicySets, Effect.DENY
        )
    }

    private fun requestEvaluationWithFirstOfTwoPolicySets(): Array<Any?> {
        return arrayOf(
            createPolicyEvalRequest(this.testResource!!.resourceIdentifier,
                this.testSubject!!.subjectIdentifier,
                LinkedHashSet(Arrays.asList(this.notApplicableAndDenyPolicySets!![0].name))
            ), this.notApplicableAndDenyPolicySets, Effect.NOT_APPLICABLE
        )
    }

    private fun requestEvaluationWithOnePolicySetAndPriorityList(): Array<Any?> {
        return arrayOf(
            createPolicyEvalRequest(this.testResource!!.resourceIdentifier,
                this.testSubject!!.subjectIdentifier,
                LinkedHashSet(Arrays.asList(this.denyPolicySet!![0].name))
            ), this.denyPolicySet, Effect.DENY
        )
    }

    private fun requestEvaluationWithOnePolicySetAndEmptyPriorityList(): Array<Any?> {
        return arrayOf(
            createPolicyEvalRequest(
                this.testResource!!.resourceIdentifier,
                this.testSubject!!.subjectIdentifier,
                LinkedHashSet()
            ), this.denyPolicySet, Effect.DENY
        )
    }

    private fun requestEvaluationWithAllOfTwoPolicySets(): Array<Any?> {
        return arrayOf(createPolicyEvalRequest(this.testResource!!.resourceIdentifier,
            this.testSubject!!.subjectIdentifier,
            LinkedHashSet(Arrays.asList(this.notApplicableAndDenyPolicySets!![0].name,
                this.notApplicableAndDenyPolicySets!![1].name))
        ), this.notApplicableAndDenyPolicySets, Effect.DENY
        )

    }

    @DataProvider(name = "policyEvalBadRequestDataProvider")
    private fun policyEvalBadRequestDataProvider(): Array<Array<Any?>> {
        return arrayOf(
            requestEvaluationWithNonExistentPolicySet(),
            requestEvaluationWithTwoPolicySetsAndNoPriorityList(),
            requestEvaluationWithExistentAndNonExistentPolicySets()
        )
    }

    private fun requestEvaluationWithExistentAndNonExistentPolicySets(): Array<Any?> {
        return arrayOf(createPolicyEvalRequest(this.testResource!!.resourceIdentifier,
            this.testSubject!!.subjectIdentifier,
            LinkedHashSet(Arrays.asList(this.notApplicableAndDenyPolicySets!![0].name,
                "noexistent-policy-set"))
        ), this.notApplicableAndDenyPolicySets
        )
    }

    private fun requestEvaluationWithTwoPolicySetsAndNoPriorityList(): Array<Any?> {
        return arrayOf(
            createPolicyEvalRequest(
                this.testResource!!.resourceIdentifier,
                this.testSubject!!.subjectIdentifier,
                LinkedHashSet()
            ), this.notApplicableAndDenyPolicySets
        )
    }

    private fun requestEvaluationWithNonExistentPolicySet(): Array<Any?> {
        return arrayOf(createPolicyEvalRequest(this.testResource!!.resourceIdentifier,
            this.testSubject!!.subjectIdentifier,
            LinkedHashSet(Arrays.asList("nonexistent-policy-set"))
        ), this.denyPolicySet
        )
    }

    private fun createPolicyEvalRequest(
        resourceIdentifier: String?,
        subjectIdentifier: String?,
        policySetsPriority: LinkedHashSet<String?>
    ): PolicyEvaluationRequestV1 {
        val policyEvalRequest = PolicyEvaluationRequestV1()
        policyEvalRequest.action = "GET"
        policyEvalRequest.resourceIdentifier = resourceIdentifier
        policyEvalRequest.subjectIdentifier = subjectIdentifier
        policyEvalRequest.policySetsEvaluationOrder = policySetsPriority
        return policyEvalRequest
    }

    private fun createDenyPolicySet(): List<PolicySet> {
        val policySets = ArrayList<PolicySet>()
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet::class.java)!!)
        Assert.assertNotNull(policySets, "Policy set file is not found or invalid")
        return policySets
    }

    private fun createNotApplicableAndDenyPolicySets(): List<PolicySet> {
        val policySets = ArrayList<PolicySet>()
        policySets
            .add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalNotApplicable.json", PolicySet::class.java)!!)
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet::class.java)!!)
        Assert.assertNotNull(policySets, "Policy set files are not found or invalid")
        Assert.assertTrue(policySets.size == 2, "One or more policy set files are not found or invalid")
        return policySets
    }

    private fun upsertPolicySet(policySet: PolicySet) {
        this.policyManagementService.upsertPolicySet(policySet)
        Assert.assertNotNull(this.policyManagementService.getPolicySet(policySet.name!!))
    }

    private fun upsertMultiplePolicySets(policySets: List<PolicySet>) {
        policySets.forEach { this.upsertPolicySet(it) }
    }
}
