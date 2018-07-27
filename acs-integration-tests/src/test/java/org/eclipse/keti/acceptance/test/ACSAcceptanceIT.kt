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

package org.eclipse.keti.acceptance.test

import org.eclipse.keti.acs.commons.web.HEARTBEAT_URL
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.util.Arrays

/**
 * @author acs-engineers@ge.com
 */
@ContextConfiguration("classpath:acceptance-test-spring-context.xml")
class ACSAcceptanceIT : AbstractTestNGSpringContextTests() {

    private var acsBaseUrl: String? = null

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var acsZoneRestTemplate: OAuth2RestTemplate? = null

    private var headersWithZoneSubdomain: HttpHeaders? = null

    val acsEndpoint: Array<Array<Any?>>
        @DataProvider(name = "endpointProvider")
        @Throws(Exception::class)
        get() {
            val policyEvalForBob = this.policyHelper.createEvalRequest(
                "GET", "bob",
                "/alarms/sites/sanramon", null
            )

            return arrayOf(arrayOf(this.acsBaseUrl, this.headersWithZoneSubdomain, policyEvalForBob, "bob"))
        }

    @BeforeClass
    @Throws(IOException::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()
        this.headersWithZoneSubdomain = this.acsitSetUpFactory.zone1Headers
        this.acsZoneRestTemplate = this.acsitSetUpFactory.acsZoneAdminRestTemplate
        this.acsBaseUrl = this.acsitSetUpFactory.acsUrl

    }

    @Test
    fun testAcsHealth() {

        val restTemplate = RestTemplate()
        try {
            val heartbeatResponse = restTemplate.exchange(
                this.acsBaseUrl!! + HEARTBEAT_URL, HttpMethod.GET,
                HttpEntity<Any>(this.headersWithZoneSubdomain), String::class.java
            )
            Assert.assertEquals(heartbeatResponse.body, "alive", "ACS Heartbeat Check Failed")
        } catch (e: Exception) {
            Assert.fail("Could not perform ACS Heartbeat Check: " + e.message)
        }

        try {
            val healthStatus = restTemplate.exchange(
                this.acsBaseUrl!! + "/health", HttpMethod.GET,
                HttpEntity<Any>(this.headersWithZoneSubdomain), Map::class.java
            )
            Assert.assertNotNull(healthStatus)
            Assert.assertEquals(healthStatus.body.size, 1)
            val acsStatus = healthStatus.body["status"] as String
            Assert.assertEquals(acsStatus, "UP", "ACS Health Check Failed: $acsStatus")
        } catch (e: Exception) {
            Assert.fail("Could not perform ACS Health Check: " + e.message)
        }

    }

    @Test(dataProvider = "endpointProvider")
    @Throws(Exception::class)
    fun testCompleteACSFlow(
        endpoint: String,
        headers: HttpHeaders,
        policyEvalRequest: PolicyEvaluationRequestV1,
        subjectIdentifier: String
    ) {

        var testPolicyName: String? = null
        var marissa: BaseSubject? = null
        var testResource: BaseResource? = null
        try {
            testPolicyName = this.policyHelper.setTestPolicy(
                this.acsZoneRestTemplate, headers, endpoint,
                "src/test/resources/testCompleteACSFlow.json"
            )
            val subject = BaseSubject(subjectIdentifier)
            val site = Attribute()
            site.issuer = "issuerId1"
            site.name = "site"
            site.value = "sanramon"

            marissa = this.privilegeHelper.putSubject(this.acsZoneRestTemplate, subject, endpoint, headers, site)

            val region = Attribute()
            region.issuer = "issuerId1"
            region.name = "region"
            region.value = "testregion" // test policy asserts on this value

            val resource = BaseResource()
            resource.resourceIdentifier = "/alarms/sites/sanramon"

            testResource = this.privilegeHelper.putResource(
                this.acsZoneRestTemplate, resource, endpoint, headers,
                region
            )

            val evalResponse = this.acsZoneRestTemplate!!.postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH, HttpEntity(policyEvalRequest, headers),
                PolicyEvaluationResult::class.java
            )

            Assert.assertEquals(evalResponse.statusCode, HttpStatus.OK)
            val responseBody = evalResponse.body
            Assert.assertEquals(responseBody.effect, Effect.PERMIT)
        } finally {
            // delete policy
            if (null != testPolicyName) {
                this.acsZoneRestTemplate!!.exchange(
                    endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName,
                    HttpMethod.DELETE, HttpEntity<Any>(headers), String::class.java
                )
            }

            // delete attributes
            if (null != marissa) {
                this.acsZoneRestTemplate!!.exchange(
                    endpoint + PrivilegeHelper.ACS_SUBJECT_API_PATH + marissa.subjectIdentifier,
                    HttpMethod.DELETE, HttpEntity<Any>(headers), String::class.java
                )
            }
            if (null != testResource) {
                val encodedResource = URLEncoder.encode(testResource.resourceIdentifier!!, "UTF-8")
                val uri = URI(endpoint + PrivilegeHelper.ACS_RESOURCE_API_PATH + encodedResource)
                this.acsZoneRestTemplate!!
                    .exchange(uri, HttpMethod.DELETE, HttpEntity<Any>(headers), String::class.java)
            }
        }
    }

    private fun getMonitoringApiResponse(headers: HttpHeaders): ResponseEntity<String> {
        return RestTemplate().exchange(
            URI.create(this.acsBaseUrl!! + HEARTBEAT_URL),
            HttpMethod.GET, HttpEntity<Any>(headers), String::class.java
        )
    }

    // TODO: Remove this test when the "httpValidation" Spring profile is removed
    @Test
    @Throws(Exception::class)
    fun testHttpValidationBasedOnActiveSpringProfile() {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)

        if (!Arrays.asList(*this.environment.activeProfiles).contains("httpValidation")) {
            Assert.assertEquals(this.getMonitoringApiResponse(headers).statusCode, HttpStatus.OK)
            return
        }

        try {
            this.getMonitoringApiResponse(headers)
            Assert.fail("Expected an HttpMediaTypeNotAcceptableException exception to be thrown")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.NOT_ACCEPTABLE)
        }

    }

    @AfterClass
    fun tearDown() {
        this.acsitSetUpFactory.destroy()
    }

}
