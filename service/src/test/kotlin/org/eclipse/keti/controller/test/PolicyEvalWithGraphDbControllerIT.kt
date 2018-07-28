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

package org.eclipse.keti.controller.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementService
import org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository
import org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepository
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.EVIDENCE_IMPLANT_ID
import org.eclipse.keti.acs.testutils.EVIDENCE_SCULLYS_TESTIMONY_ID
import org.eclipse.keti.acs.testutils.SPECIAL_AGENTS_GROUP_ATTRIBUTE
import org.eclipse.keti.acs.testutils.TOP_SECRET_CLASSIFICATION
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.createScopedSubjectHierarchy
import org.eclipse.keti.acs.testutils.createSubjectHierarchy
import org.eclipse.keti.acs.testutils.createThreeLevelResourceHierarchy
import org.eclipse.keti.acs.testutils.createTwoLevelResourceHierarchy
import org.eclipse.keti.acs.testutils.createTwoParentResourceHierarchy
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.SkipException
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.net.URLEncoder
import java.util.HashSet

private val OBJECT_MAPPER = ObjectMapper()
private val OBJECT_WRITER = ObjectMapper().writer().withDefaultPrettyPrinter()
private const val POLICY_EVAL_URL = "v1/policy-evaluation"
private const val POLICY_SET_URL = "v1/policy-set"
private const val SUBJECT_URL = "v1/subject"
private const val RESOURCE_URL = "v1/resource"
private const val TEST_TRAVERSAL_LIMIT: Long = 2

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class PolicyEvalWithGraphDbControllerIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var privilegeManagementService: PrivilegeManagementService

    @Qualifier("resourceHierarchicalRepository")
    @Autowired
    private lateinit var graphResourceRepository: GraphResourceRepository

    @Qualifier("subjectHierarchicalRepository")
    @Autowired
    private lateinit var graphSubjectRepository: GraphSubjectRepository

    @Autowired
    private lateinit var wac: WebApplicationContext

    @Autowired
    private lateinit var zoneService: ZoneService

    private var policySet: PolicySet? = null

    private val jsonUtils = JsonUtils()
    private val testUtils = TestUtils()
    private var testZone1: Zone? = null
    private var testZone2: Zone? = null

    @Autowired
    private lateinit var env: ConfigurableEnvironment

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        if (!listOf(*this.env.activeProfiles).contains("graph")) {
            throw SkipException("This test only applies when using graph profile.")
        }

        this.testZone1 = TestUtils().createTestZone("PolicyEvalWithGraphDbControllerIT1")
        this.testZone2 = TestUtils().createTestZone("PolicyEvalWithGraphDbControllerIT2")
        this.zoneService.upsertZone(this.testZone1!!)
        this.zoneService.upsertZone(this.testZone2!!)
        mockSecurityContext(this.testZone1)
        mockAcsRequestContext()
        this.policySet = this.jsonUtils.deserializeFromFile("complete-sample-policy-set-2.json", PolicySet::class.java)
        Assert.assertNotNull(this.policySet, "complete-sample-policy-set-2.json file not found or invalid")
    }

    @AfterMethod
    fun testCleanup() {
        for (subject in this.privilegeManagementService.subjects) {
            this.privilegeManagementService.deleteSubject(subject.subjectIdentifier!!)
        }
        for (resource in this.privilegeManagementService.resources) {
            this.privilegeManagementService.deleteResource(resource.resourceIdentifier!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testPolicyInvalidMediaTypeResponseStatusCheck() {

        val uri = "$POLICY_SET_URL/testString"
        val putPolicySetContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, "testZone", uri)
        putPolicySetContext.mockMvc.perform(
            putPolicySetContext.builder.contentType(MediaType.TEXT_HTML_VALUE).content("testString")
        ).andExpect(status().isUnsupportedMediaType)
    }

    @Test(dataProvider = "policyEvalDataProvider")
    @Throws(Exception::class)
    fun testPolicyEvaluation(
        zone: Zone,
        testPolicySet: PolicySet,
        resourceHierarchy: List<BaseResource>?,
        subjectHierarchy: List<BaseSubject>?,
        policyEvalRequest: PolicyEvaluationRequestV1,
        expectedEffect: Effect
    ) {
        // Create policy set.

        val uri = POLICY_SET_URL + "/" + URLEncoder.encode(testPolicySet.name!!, "UTF-8")
        val putPolicySetContext = this.testUtils.createWACWithCustomPUTRequestBuilder(
            this.wac, zone.subdomain, uri
        )
        putPolicySetContext.mockMvc.perform(
            putPolicySetContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_WRITER.writeValueAsString(
                    testPolicySet
                )
            )
        ).andExpect(status().isCreated)

        // Create resource hierarchy.
        if (null != resourceHierarchy) {
            val postResourcesContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
                this.wac, zone.subdomain, RESOURCE_URL
            )
            postResourcesContext.mockMvc.perform(
                postResourcesContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                    OBJECT_MAPPER.writeValueAsString(
                        resourceHierarchy
                    )
                )
            ).andExpect(status().isNoContent)
        }

        // Create subject hierarchy.
        if (null != subjectHierarchy) {
            val postSubjectsContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
                this.wac, zone.subdomain, SUBJECT_URL
            )
            postSubjectsContext.mockMvc.perform(
                postSubjectsContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                    OBJECT_MAPPER.writeValueAsString(
                        subjectHierarchy
                    )
                )
            ).andExpect(status().isNoContent)
        }

        // Request policy evaluation.
        val postPolicyEvalContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
            this.wac, zone.subdomain, POLICY_EVAL_URL
        )
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

    @Test(dataProvider = "policyEvalExceedingAttributeLimitDataProvider")
    @Throws(Exception::class)
    fun testPolicyEvaluationForAttributesExceedingTraversalLimit(
        zone: Zone,
        testPolicySet: PolicySet,
        resourceHierarchy: List<BaseResource>?,
        subjectHierarchy: List<BaseSubject>?,
        policyEvalRequest: PolicyEvaluationRequestV1,
        expectedEffect: Effect,
        expectedMessage: String
    ) {
        val traversalLimit = graphResourceRepository.traversalLimit

        graphResourceRepository.traversalLimit = TEST_TRAVERSAL_LIMIT
        graphSubjectRepository.traversalLimit = TEST_TRAVERSAL_LIMIT

        // Create policy set.

        try {
            val uri = POLICY_SET_URL + "/" + URLEncoder.encode(testPolicySet.name!!, "UTF-8")
            val putPolicySetContext = this.testUtils.createWACWithCustomPUTRequestBuilder(
                this.wac, zone.subdomain, uri
            )
            putPolicySetContext.mockMvc.perform(
                putPolicySetContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                    OBJECT_WRITER.writeValueAsString(
                        testPolicySet
                    )
                )
            ).andExpect(status().isCreated)

            // Create resource hierarchy.
            if (null != resourceHierarchy) {
                val postResourcesContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
                    this.wac, zone.subdomain, RESOURCE_URL
                )
                postResourcesContext.mockMvc.perform(
                    postResourcesContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                        OBJECT_MAPPER.writeValueAsString(
                            resourceHierarchy
                        )
                    )
                ).andExpect(status().isNoContent)
            }

            // Create subject hierarchy.
            if (null != subjectHierarchy) {
                val postSubjectsContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
                    this.wac, zone.subdomain, SUBJECT_URL
                )
                postSubjectsContext.mockMvc.perform(
                    postSubjectsContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                        OBJECT_MAPPER.writeValueAsString(
                            subjectHierarchy
                        )
                    )
                ).andExpect(status().isNoContent)
            }

            // Request policy evaluation.
            val postPolicyEvalContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(
                this.wac, zone.subdomain, POLICY_EVAL_URL
            )
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
            assertThat<String>(policyEvalResult.message, equalTo(expectedMessage))
        } finally {
            graphResourceRepository.traversalLimit = traversalLimit
            graphSubjectRepository.traversalLimit = traversalLimit
        }
    }

    @DataProvider(name = "policyEvalDataProvider")
    private fun policyEvalDataProvider(): Array<Array<Any?>> {
        return arrayOf(
            attributeInheritanceData(),
            scopedAttributeInheritanceData(),
            evaluationWithNoSubjectAndNoResourceData(),
            evaluationWithSupplementalAttributesData()
        )
    }

    @DataProvider(name = "policyEvalExceedingAttributeLimitDataProvider")
    private fun policyEvalExceedingAttributeLimitDataProvider(): Array<Array<Any?>> {
        return arrayOf(
            evaluationWithResourceAttributesExceedingTraversalLimitData(),
            evaluationWithSubjectAttributesExceedingTraversalLimitData()
        )
    }

    /**
     * Test that subjects and resources inherit attributes from their parents. The policy set will permit the request
     * if the subject and resource successfully inherit the required attributes from their respective parents.
     */
    private fun attributeInheritanceData(): Array<Any?> {
        return arrayOf(
            this.testZone1,
            this.policySet,
            createThreeLevelResourceHierarchy(),
            createSubjectHierarchy(),
            createPolicyEvalRequest(EVIDENCE_SCULLYS_TESTIMONY_ID, AGENT_MULDER),
            Effect.PERMIT
        )
    }

    /**
     * Test that subjects inherit attributes only when accessing resources with the right attributes. The policy set
     * will deny the request if the subject accesses a resource that does not allow it inherit the required
     * attributes.
     */
    private fun scopedAttributeInheritanceData(): Array<Any?> {
        return arrayOf(
            this.testZone1,
            this.policySet,
            createTwoParentResourceHierarchy(),
            createScopedSubjectHierarchy(),
            createPolicyEvalRequest(EVIDENCE_IMPLANT_ID, AGENT_MULDER),
            Effect.DENY
        )
    }

    /**
     * Test that evaluation is successful even when the resource and subject do not exist. The policy set will deny
     * the request but return a successful result.
     */
    private fun evaluationWithNoSubjectAndNoResourceData(): Array<Any?> {
        return arrayOf(
            this.testZone1,
            this.policySet,
            null,
            null,
            createPolicyEvalRequest(EVIDENCE_IMPLANT_ID, AGENT_MULDER),
            Effect.DENY
        )
    }

    /**
     * Test that evaluation is successful even when the request provides the attributes. The policy set will return
     * permit because the condition is satisfied by the user provided supplemental attributes.
     */
    private fun evaluationWithSupplementalAttributesData(): Array<Any?> {
        return arrayOf(
            this.testZone1, this.policySet, null, null, createPolicyEvalRequest(
                EVIDENCE_IMPLANT_ID, AGENT_MULDER, HashSet(
                    listOf(
                        SPECIAL_AGENTS_GROUP_ATTRIBUTE, TOP_SECRET_CLASSIFICATION
                    )
                ), HashSet(
                    listOf(
                        SPECIAL_AGENTS_GROUP_ATTRIBUTE, TOP_SECRET_CLASSIFICATION
                    )
                )
            ), Effect.PERMIT
        )
    }

    /**
     * Test that evaluation is successful when the resource and/or subject attributes exceed the length.
     * The policy set will return indeterminate because the traversal limit is exceeded.
     */
    private fun evaluationWithResourceAttributesExceedingTraversalLimitData(): Array<Any?> {
        val errorMessage =
            ("The number of attributes on this resource '$EVIDENCE_SCULLYS_TESTIMONY_ID' has exceeded the maximum limit of $TEST_TRAVERSAL_LIMIT")
        return arrayOf(
            this.testZone1,
            this.policySet,
            createThreeLevelResourceHierarchy(),
            null,
            createPolicyEvalRequest(EVIDENCE_SCULLYS_TESTIMONY_ID, AGENT_MULDER),
            Effect.INDETERMINATE,
            errorMessage
        )
    }

    /**
     * Test that subjects and resources inherit attributes from their parents. The policy set will permit the request
     * if the subject and resource successfully inherit the required attributes from their respective parents.
     */
    private fun evaluationWithSubjectAttributesExceedingTraversalLimitData(): Array<Any?> {
        val errorMessage =
            ("The number of attributes on this subject '$AGENT_MULDER' has exceeded the maximum limit of $TEST_TRAVERSAL_LIMIT")
        return arrayOf(
            this.testZone1,
            this.policySet,
            createTwoLevelResourceHierarchy(),
            createSubjectHierarchy(),
            createPolicyEvalRequest(EVIDENCE_SCULLYS_TESTIMONY_ID, AGENT_MULDER),
            Effect.INDETERMINATE,
            errorMessage
        )
    }

    private fun createPolicyEvalRequest(
        resourceIdentifier: String,
        subjectIdentifier: String
    ): PolicyEvaluationRequestV1 {
        val policyEvalRequest = PolicyEvaluationRequestV1()
        policyEvalRequest.action = "GET"
        policyEvalRequest.resourceIdentifier = resourceIdentifier
        policyEvalRequest.subjectIdentifier = subjectIdentifier
        return policyEvalRequest
    }

    private fun createPolicyEvalRequest(
        resourceIdentifier: String,
        subjectIdentifier: String,
        supplementalResourceAttributes: Set<Attribute>,
        supplementalSubjectAttributes: Set<Attribute>
    ): PolicyEvaluationRequestV1 {
        val policyEvalRequest = PolicyEvaluationRequestV1()
        policyEvalRequest.action = "GET"
        policyEvalRequest.resourceIdentifier = resourceIdentifier
        policyEvalRequest.subjectIdentifier = subjectIdentifier
        policyEvalRequest.resourceAttributes = supplementalResourceAttributes
        policyEvalRequest.subjectAttributes = supplementalSubjectAttributes
        return policyEvalRequest
    }
}
