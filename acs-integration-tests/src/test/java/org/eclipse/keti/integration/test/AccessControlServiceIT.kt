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

package org.eclipse.keti.integration.test

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.ACSTestUtil
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File
import java.io.IOException

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
class AccessControlServiceIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    @Autowired
    private lateinit var acsTestUtil: ACSTestUtil

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    val subject: Array<Array<Any?>>
        @DataProvider(name = "subjectProvider")
        get() = arrayOf(
            arrayOf(
                MARISSA_V1,
                policyHelper.createEvalRequest(MARISSA_V1.subjectIdentifier, "sanramon"),
                acsitSetUpFactory.acsUrl
            ),
            arrayOf(
                JOE_V1,
                policyHelper.createEvalRequest(JOE_V1.subjectIdentifier, "sanramon"),
                acsitSetUpFactory.acsUrl
            ),
            arrayOf(
                PETE_V1,
                policyHelper.createEvalRequest(PETE_V1.subjectIdentifier, "sanramon"),
                acsitSetUpFactory.acsUrl
            ),
            arrayOf(
                JLO_V1,
                policyHelper.createEvalRequest(JLO_V1.subjectIdentifier, "sanramon"),
                acsitSetUpFactory.acsUrl
            )
        )

    val acsEndpoint: Array<Array<Any?>>
        @DataProvider(name = "endpointProvider")
        get() = arrayOf(arrayOf<Any?>(acsitSetUpFactory.acsUrl))

    @BeforeClass
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()
    }

    @Test(dataProvider = "subjectProvider")
    @Throws(Exception::class)
    fun testPolicyEvalWithFirstMatchDeny(
        subject: BaseSubject,
        policyEvaluationRequest: PolicyEvaluationRequestV1,
        endpoint: String
    ) {

        this.privilegeHelper.putSubject(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, subject, endpoint,
            this.acsitSetUpFactory.zone1Headers, this.privilegeHelper.defaultAttribute
        )

        val policyFile = "src/test/resources/multiple-site-based-policy-set.json"
        val testPolicyName = this.policyHelper.setTestPolicy(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
        )
        val postForEntity = this.acsitSetUpFactory.acsZoneAdminRestTemplate
            .postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsitSetUpFactory.zone1Headers),
                PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        val responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.DENY)

        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
        this.privilegeHelper.deleteSubject(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, subject.subjectIdentifier,
            this.acsitSetUpFactory.zone1Headers
        )
    }

    @Test(dataProvider = "subjectProvider")
    @Throws(Exception::class)
    fun testPolicyEvalPermit(
        subject: BaseSubject,
        policyEvaluationRequest: PolicyEvaluationRequestV1,
        endpoint: String
    ) {
        var testPolicyName: String? = null
        try {

            this.privilegeHelper.putSubject(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate, subject, endpoint,
                this.acsitSetUpFactory.zone1Headers, this.privilegeHelper.defaultAttribute
            )
            val policyFile = "src/test/resources/single-site-based-policy-set.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )

            var postForEntity = this.acsitSetUpFactory.acsZoneAdminRestTemplate
                .postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    HttpEntity(policyEvaluationRequest, this.acsitSetUpFactory.zone1Headers),
                    PolicyEvaluationResult::class.java
                )

            Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
            var responseBody = postForEntity.body
            Assert.assertEquals(responseBody.effect, Effect.PERMIT)

            val policyEvaluationRequest2 = this.policyHelper
                .createEvalRequest(subject.subjectIdentifier, "ny")

            postForEntity = this.acsitSetUpFactory.acsZoneAdminRestTemplate.postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest2, this.acsitSetUpFactory.zone1Headers),
                PolicyEvaluationResult::class.java
            )

            Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
            responseBody = postForEntity.body
            Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)

        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(
                    this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                    this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
                )
            }
        }
    }

    // TODO: Delete resource. Currently it's causing "405 Request method 'DELETE' not supported" error
    // Not deleting resource prevents from running this test repeatedly.
    // @Test
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalWithAttributeUriTemplate() {
        var testPolicyName: String? = null

        // This is the extracted resource URI using attributeUriTemplate. See test policy.
        val testResourceId = "/asset/1223"
        try {
            // OAuth2RestTemplate acsRestTemplate = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();

            // set policy
            val policyFile = "src/test/resources/policies/policy-set-with-attribute-uri-template.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.zone1Headers, this.acsitSetUpFactory.acsUrl, policyFile
            )

            // Policy Eval. without setting required attribute on resource. Should return DENY
            // Note resourceId sent to eval request is the complete URI, from which /asset/1223 will be
            // extracted by ACS, using "attributeUriTemplate": "/v1/region/report{attribute_uri}"
            val evalRequest = this.policyHelper.createEvalRequest(
                "GET", "testSubject",
                "/v1/region/report/asset/1223", null
            )

            var postForEntity = this.acsitSetUpFactory.acsZoneAdminRestTemplate
                .postForEntity(
                    this.acsitSetUpFactory.acsUrl + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    HttpEntity(evalRequest, this.acsitSetUpFactory.zone1Headers),
                    PolicyEvaluationResult::class.java
                )

            Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
            Assert.assertEquals(postForEntity.body.effect, Effect.DENY)

            // Set resource attribute and evaluate again. expect PERMIT
            // createResource adds a 'site' attribute with value 'sanramon' used by our test policy
            val testResource = this.privilegeHelper.createResource(testResourceId)
            this.privilegeHelper.postResources(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.acsUrl, this.acsitSetUpFactory.zone1Headers, testResource
            )

            postForEntity = this.acsitSetUpFactory.acsZoneAdminRestTemplate.postForEntity(
                this.acsitSetUpFactory.acsUrl + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                HttpEntity(evalRequest, this.acsitSetUpFactory.zone1Headers),
                PolicyEvaluationResult::class.java
            )
            Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
            Assert.assertEquals(postForEntity.body.effect, Effect.PERMIT)

        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(
                    this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                    this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
                )
            }
        }

    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testCreationOfValidPolicy(endpoint: String) {
        val policyFile = "src/test/resources/single-site-based-policy-set.json"
        val testPolicyName = this.policyHelper.setTestPolicy(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
        )
        val policySetSaved = this.getPolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            testPolicyName, this.acsitSetUpFactory.zone1Headers, endpoint
        )
        Assert.assertEquals(testPolicyName, policySetSaved.name)
        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyCreationInValidPolicy(endpoint: String) {
        val testPolicyName: String
        try {
            val policyFile = "src/test/resources/missing-policy-set-name-policy.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )
        } catch (e: HttpClientErrorException) {
            this.acsTestUtil.assertExceptionResponseBody(e, "policy set name is missing")
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
        Assert.fail("testPolicyCreationInValidPolicy should have failed")
    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyCreationInValidWithBadPolicySetNamePolicy(endpoint: String) {
        val testPolicyName: String
        try {
            val policyFile = "src/test/resources/policy-set-with-only-name-effect.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )
        } catch (e: HttpClientErrorException) {
            this.acsTestUtil.assertExceptionResponseBody(e, "is not URI friendly")
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
        Assert.fail("testPolicyCreationInValidPolicy should have failed")
    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyCreationJsonSchemaInvalidPolicySet(endpoint: String) {
        val testPolicyName: String
        try {
            val policyFile = "src/test/resources/invalid-json-schema-policy-set.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )
        } catch (e: HttpClientErrorException) {
            this.acsTestUtil.assertExceptionResponseBody(e, "JSON Schema validation")
            Assert.assertEquals(e.statusCode, HttpStatus.UNPROCESSABLE_ENTITY)
            return
        }

        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
        Assert.fail("testPolicyCreationInValidPolicy should have failed")
    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyEvalNotApplicable(endpoint: String) {
        var testPolicyName: String? = null
        try {
            this.privilegeHelper.putSubject(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate, MARISSA_V1,
                this.acsitSetUpFactory.acsUrl, this.acsitSetUpFactory.zone1Headers,
                this.privilegeHelper.defaultAttribute
            )

            val policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )

            val policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.subjectIdentifier, "sanramon")
            val postForEntity = this.acsitSetUpFactory.acsZoneAdminRestTemplate
                .postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    HttpEntity(policyEvaluationRequest, this.acsitSetUpFactory.zone1Headers),
                    PolicyEvaluationResult::class.java
                )

            Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
            val responseBody = postForEntity.body
            Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)
        } catch (e: Exception) {
            Assert.fail("testPolicyEvalNotApplicable should have NOT failed " + endpoint + " " + e.message)
        } finally {
            this.policyHelper.deletePolicySet(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
            )
            this.privilegeHelper.deleteSubject(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.acsUrl, MARISSA_V1.subjectIdentifier,
                this.acsitSetUpFactory.zone1Headers
            )
        }
    }

    @Test(dataProvider = "endpointProvider")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testPolicyUpdateWithNoOauthToken(endpoint: String) {
        val acs = RestTemplate()
        // Use vanilla rest template with no oauth token.
        try {
            val policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json"
            this.policyHelper.setTestPolicy(acs, this.acsitSetUpFactory.zone1Headers, endpoint, policyFile)
            Assert.fail("No exception thrown when making request without token.")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNAUTHORIZED)
        }

    }

    @Test(dataProvider = "endpointProvider")
    fun testPolicyEvalWithNoOauthToken(endpoint: String) {
        val acs = RestTemplate()
        // Use vanilla rest template with no oauth token.
        try {
            acs.postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                HttpEntity(
                    this.policyHelper.createEvalRequest(MARISSA_V1.subjectIdentifier, "sanramon"),
                    this.acsitSetUpFactory.zone1Headers
                ),
                PolicyEvaluationResult::class.java
            )
            Assert.fail("No exception thrown when making policy evaluation request without token.")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNAUTHORIZED)
        }

    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyUpdateWithInsufficientScope(endpoint: String) {
        val testPolicyName: String
        try {
            val policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json"
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsNoPolicyScopeRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )
            this.policyHelper.deletePolicySet(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
            )
            Assert.fail("No exception when trying to create policy set with no acs scope")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.FORBIDDEN)
        }

    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyUpdateWithReadOnlyAccess(endpoint: String) {
        try {
            val policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json"
            this.policyHelper.setTestPolicy(
                this.acsitSetUpFactory.acsReadOnlyRestTemplate,
                this.acsitSetUpFactory.zone1Headers, endpoint, policyFile
            )
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.FORBIDDEN)
        }

    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testPolicyReadWithReadOnlyAccess(endpoint: String) {
        val testPolicyName = this.policyHelper.setTestPolicy(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.zone1Headers, endpoint,
            "src/test/resources/single-site-based-policy-set.json"
        )

        val policySetResponse = this.acsitSetUpFactory.acsReadOnlyRestTemplate.exchange(
            endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName, HttpMethod.GET,
            HttpEntity<Any>(this.acsitSetUpFactory.zone1Headers), PolicySet::class.java
        )
        Assert.assertEquals(testPolicyName, policySetResponse.body.name)
        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCreatePolicyWithClientOnlyBasedToken() {
        var testPolicyName: String? = null
        try {

            val policySet = ObjectMapper()
                .readValue(File("src/test/resources/single-site-based-policy-set.json"), PolicySet::class.java)
            testPolicyName = policySet.name
            this.acsitSetUpFactory.acsZoneAdminRestTemplate.put(
                this.acsitSetUpFactory.acsUrl + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName,
                HttpEntity(policySet, this.acsitSetUpFactory.zone1Headers)
            )
        } finally {
            this.policyHelper.deletePolicySet(
                this.acsitSetUpFactory.acsZoneAdminRestTemplate,
                this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
            )
        }
    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testGetAllPolicySets(endpoint: String) {
        val testPolicyName = this.policyHelper.setTestPolicy(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.zone1Headers, endpoint,
            "src/test/resources/single-site-based-policy-set.json"
        )

        val getAllPolicySetsURL = endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH

        val policySetsResponse = this.acsitSetUpFactory.acsReadOnlyRestTemplate.exchange(
            getAllPolicySetsURL, HttpMethod.GET, HttpEntity<Any>(this.acsitSetUpFactory.zone1Headers),
            Array<PolicySet>::class.java
        )

        val policySets = policySetsResponse.body
        // should expect only one policySet per issuer, clientId and policySetId
        Assert.assertEquals(1, policySets.size)
        Assert.assertEquals(testPolicyName, policySets[0].name)

        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
    }

    private fun getPolicySet(
        acs: RestTemplate,
        policyName: String,
        headers: HttpHeaders,
        acsEndpointParam: String
    ): PolicySet {
        val policySetResponse = acs.exchange(
            acsEndpointParam + PolicyHelper.ACS_POLICY_SET_API_PATH + policyName, HttpMethod.GET,
            HttpEntity<Any>(headers), PolicySet::class.java
        )
        return policySetResponse.body
    }

    @AfterClass
    @Throws(Exception::class)
    fun cleanup() {
        this.privilegeHelper.deleteResources(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, this.acsitSetUpFactory.zone1Headers
        )
        this.privilegeHelper.deleteSubjects(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, this.acsitSetUpFactory.zone1Headers
        )
        this.policyHelper.deletePolicySets(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate,
            this.acsitSetUpFactory.acsUrl, this.acsitSetUpFactory.zone1Headers
        )
        this.acsitSetUpFactory.destroy()
    }
}
