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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.commons.web.POLICY_SET_URL
import org.eclipse.keti.acs.commons.web.expand
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.hamcrest.Matchers.`is`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

private const val VERSION = "v1/"

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test
class PolicyManagementControllerIT : AbstractTestNGSpringContextTests() {

    private val objectWriter = ObjectMapper().writer().withDefaultPrettyPrinter()

    @Autowired
    private lateinit var zoneService: ZoneService

    @Autowired
    private lateinit var wac: WebApplicationContext

    private var policySet: PolicySet? = null

    private val jsonUtils = JsonUtils()
    private val testUtils = TestUtils()
    private var testZone: Zone? = null
    private var testZone2: Zone? = null

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.testZone = TestUtils().setupTestZone("PolicyMgmtControllerIT", zoneService)
        this.policySet = this.jsonUtils.deserializeFromFile(
            "controller-test/complete-sample-policy-set.json", PolicySet::class.java
        )
        Assert.assertNotNull(this.policySet, "complete-sample-policy-set.json file not found or invalid")
    }

    @Throws(Exception::class)
    fun testCreatePolicyInvalidMediaTypeResponseStatusCheck() {

        val thisUri = VERSION + "/policy-set/" + this.policySet!!.name
        // create policy-set in first zone
        val putContext =
            this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_XML_VALUE).content("testString")
        ).andExpect(status().isUnsupportedMediaType)
    }

    @Throws(Exception::class)
    fun policyZoneDoesNotExistException() {
        // NOTE: To throw a ZoneDoesNotExistException, we must ensure that the AcsRequestContext in the
        //       SpringSecurityZoneResolver class returns a null ZoneEntity
        mockSecurityContext(null)
        mockAcsRequestContext()
        val thisUri = VERSION + "/policy-set/" + this.policySet!!.name
        // create policy-set in first zone
        val putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, thisUri
        )
        val resultActions = putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(this.objectWriter.writeValueAsString(this.policySet))
        )
        resultActions.andExpect(status().isUnprocessableEntity)
        resultActions.andReturn().response.contentAsString.contains("zone 'null' does not exist")

        mockSecurityContext(this.testZone)
        mockAcsRequestContext()
    }

    @Throws(Exception::class)
    fun testCreateSamePolicyDifferentZones() {
        val thisUri = VERSION + "/policy-set/" + this.policySet!!.name
        // create policy-set in first zone
        var putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, thisUri
        )
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(this.objectWriter.writeValueAsString(this.policySet))
        ).andExpect(status().isCreated)

        // create policy set in second zone
        this.testZone2 = TestUtils().createTestZone("PolicyMgmtControllerIT2")
        this.zoneService.upsertZone(this.testZone2!!)
        mockSecurityContext(this.testZone2)
        putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone2!!.subdomain, thisUri
        )
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(this.objectWriter.writeValueAsString(this.policySet))
        ).andExpect(status().isCreated)
        // we expect both policy sets to be create in each zone
        // set security context back to first test zone
        mockSecurityContext(this.testZone)
        mockAcsRequestContext()
    }

    @Throws(Exception::class)
    fun testCreatePolicy() {
        val policySetName = upsertPolicySet(this.policySet)
        val mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/" + policySetName
        )
        mockMvcContext.mockMvc.perform(mockMvcContext.builder).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("name").value(policySetName)).andExpect(jsonPath("policies").isArray)
            .andExpect(jsonPath("policies[1].target.resource.attributes[0].name").value("group"))
    }

    @Test
    @Throws(Exception::class)
    fun testCreateMultiplePolicySets() {
        // create first policy set
        val policySetName = upsertPolicySet(this.policySet)

        var mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/" + policySetName
        )

        // assert first policy set
        mockMvcContext.mockMvc.perform(mockMvcContext.builder).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("name").value(policySetName)).andExpect(jsonPath("policies").isArray)
            .andExpect(jsonPath("policies[1].target.resource.attributes[0].name").value("group"))

        var policySet2Name: String? = ""
        try {
            // create second policy set
            val policySet2 = this.jsonUtils.deserializeFromFile(
                "controller-test/multiple-policy-set-test.json", PolicySet::class.java
            )
            Assert.assertNotNull(policySet2, "multiple-policy-set-test.json file not found or invalid")
            policySet2Name = upsertPolicySet(policySet2)

            mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(
                this.wac, this.testZone!!.subdomain, VERSION + "policy-set/" + policySet2Name
            )

            // assert second policy set
            mockMvcContext.mockMvc.perform(mockMvcContext.builder).andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(policySet2Name))

            // assert that policy evaluation fails
            val evalRequest = PolicyEvaluationRequestV1()
            evalRequest.action = "GET"
            evalRequest.subjectIdentifier = "test-user"
            evalRequest.resourceIdentifier = "/app/testuri"
            val evalRequestJson = this.objectWriter.writeValueAsString(evalRequest)
            mockMvcContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
                this.wac, this.testZone!!.subdomain, VERSION + "policy-evaluation"
            )
            mockMvcContext.mockMvc.perform(
                mockMvcContext.builder.content(evalRequestJson).contentType(MediaType.APPLICATION_JSON)
            ).andExpect(status().isBadRequest).andExpect(
                jsonPath("ErrorDetails.errorMessage").value("More than one policy set exists for this zone. Please provide an ordered list " + "of policy set names to consider for this evaluation and resubmit the request.")
            )
        } finally {
            mockMvcContext = this.testUtils.createWACWithCustomDELETERequestBuilder(
                this.wac, this.testZone!!.subdomain, VERSION + "policy-set/" + policySet2Name
            )
            mockMvcContext.mockMvc.perform(mockMvcContext.builder).andExpect(status().is2xxSuccessful)
        }
    }

    @Throws(Exception::class)
    fun testGetNonExistentPolicySet() {
        val mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, "$VERSION/policy-set/non-existent"
        )
        mockMvcContext.mockMvc.perform(mockMvcContext.builder.accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Throws(Exception::class)
    fun testCreatePolicyWithNoPolicySet() {
        val ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/policyNoBody"
        )
        ctxt.mockMvc.perform(ctxt.builder.contentType(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isBadRequest)
    }

    @Throws(Exception::class)
    fun testDeletePolicySet() {
        val policySetName = upsertPolicySet(this.policySet)
        val ctxt = this.testUtils.createWACWithCustomDELETERequestBuilder(
            this.wac, this.testZone!!.subdomain, "$VERSION/policy-set/$policySetName"
        )
        ctxt.mockMvc.perform(ctxt.builder).andExpect(status().isNoContent)

        // assert policy is gone
        val getContext = this.testUtils.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, "$VERSION/policy-set/$policySetName"
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isNotFound)
    }

    @Throws(Exception::class)
    fun testGetAllPolicySets() {
        val firstPolicySetName = upsertPolicySet(this.policySet)
        val mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set"
        )
        mockMvcContext.mockMvc.perform(mockMvcContext.builder.accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("$[0].name", `is`<String>(firstPolicySetName)))
    }

    @Throws(JsonProcessingException::class, Exception::class)
    private fun upsertPolicySet(myPolicySet: PolicySet?): String? {

        val policySetContent = this.objectWriter.writeValueAsString(myPolicySet)
        val policySetName = myPolicySet!!.name
        val ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/" + myPolicySet.name
        )
        val policySetUri = expand(POLICY_SET_URL, "policySetId:" + policySetName!!)
        val policySetPath = policySetUri.path

        ctxt.mockMvc.perform(ctxt.builder.content(policySetContent).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated).andExpect(header().string("Location", policySetPath))

        return policySetName
    }

    @Throws(Exception::class)
    fun testCreatePolicyEmptyPolicySetName() {
        val simplePolicyEmptyName = this.jsonUtils
            .deserializeFromFile("controller-test/simple-policy-set-empty-name.json", PolicySet::class.java)
        Assert.assertNotNull(simplePolicyEmptyName, "simple-policy-set-empty-name.json file not found or invalid")

        val ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/policyWithEmptyName"
        )

        val policySetPayload = this.jsonUtils.serialize(simplePolicyEmptyName!!)
        ctxt.mockMvc.perform(ctxt.builder.contentType(MediaType.APPLICATION_JSON).content(policySetPayload!!))
            .andExpect(status().isUnprocessableEntity)
    }

    @Throws(Exception::class)
    fun testCreatePolicyNoPolicySetName() {
        val simplePolicyNoName =
            this.jsonUtils.deserializeFromFile("controller-test/simple-policy-set-no-name.json", PolicySet::class.java)
        Assert.assertNotNull(simplePolicyNoName, "simple-policy-set-no-name.json file not found or invalid")

        val ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/policyWithNoName"
        )
        val policySetPayload = this.jsonUtils.serialize(simplePolicyNoName!!)
        ctxt.mockMvc.perform(ctxt.builder.contentType(MediaType.APPLICATION_JSON).content(policySetPayload!!))
            .andExpect(status().isUnprocessableEntity)
    }

    @Throws(Exception::class)
    fun testCreatePolicyUriPolicySetIdMismatch() {

        val policySetPayload = this.jsonUtils.serialize(this.policySet!!)
        val ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, VERSION + "policy-set/mismatchWithPolicy"
        )
        ctxt.mockMvc.perform(ctxt.builder.contentType(MediaType.APPLICATION_JSON).content(policySetPayload!!))
            .andExpect(status().isUnprocessableEntity)
    }
}
