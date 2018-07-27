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

package org.eclipse.keti.acceptance.test.zone.admin

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.ACS_POLICY_SET_API_PATH
import org.eclipse.keti.test.utils.ACS_VERSION
import org.eclipse.keti.test.utils.CreatePolicyStatus
import org.eclipse.keti.test.utils.PREDIX_ZONE_ID
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.eclipse.keti.test.utils.httpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException
import org.springframework.web.client.HttpClientErrorException
import org.testng.Assert
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

//CHECKSTYLE:OFF
//Turning checkstyle off because the way these cucumber tests are named do not conform to the checkstyle rules.
class ZoneEnforcementStepsDefinitions {

    private var zone1Name: String? = null
    private var zone2Name: String? = null

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    @Autowired
    internal lateinit var env: Environment

    private var acsUrl: String? = null

    private var zone1Headers: HttpHeaders? = null

    private val subject = BaseSubject("subject_id_1")

    private val resource = BaseResource("resource_id_1")

    private var responseEntity: ResponseEntity<BaseSubject>? = null

    private var responseEntityForResource: ResponseEntity<BaseResource>? = null

    private var status: Int = 0

    private var testPolicyName: String? = null

    private var policyset: ResponseEntity<PolicySet>? = null
    private var acsZone1Template: OAuth2RestTemplate? = null
    private var acsZone2Template: OAuth2RestTemplate? = null

    @Before
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()
        this.acsUrl = this.acsitSetUpFactory.acsUrl
        this.zone1Headers = this.acsitSetUpFactory.zone1Headers
        this.acsZone1Template = this.acsitSetUpFactory.acsZoneAdminRestTemplate
        this.acsZone2Template = this.acsitSetUpFactory.acsZone2AdminRestTemplate
        this.zone1Name = this.acsitSetUpFactory.zone1.name
        this.zone2Name = this.acsitSetUpFactory.zone2.name
    }

    @Given("^zone 1 and zone (.*?)")
    @Throws(Throwable::class)
    fun given_zone_1_and_zone(subdomainSuffix: String) {
        // just checking zones are created ok
        Assert.assertNotNull(this.acsitSetUpFactory.zone1.name)
        Assert.assertNotNull(this.acsitSetUpFactory.zone2.name)
    }

    @When("^client_two does a PUT on (.*?) with (.*?) in zone (.*?)$")
    @Throws(Throwable::class)
    fun client_two_does_a_PUT_on_subject_with_subject_id__in_zone(
        api: String,
        identifier: String,
        subdomainSuffix: String
    ) {
        val zoneHeaders = httpHeaders()
        val acsTemplate = this.acsZone2Template
        zoneHeaders.set(PREDIX_ZONE_ID, getZoneName(subdomainSuffix))

        try {
            when (api) {
                "subject" -> this.privilegeHelper.putSubject(
                    acsTemplate!!, this.subject, this.acsUrl, zoneHeaders,
                    this.privilegeHelper.defaultAttribute
                )
                "resource" -> this.privilegeHelper.putResource(
                    acsTemplate!!, this.resource, this.acsUrl!!, zoneHeaders,
                    this.privilegeHelper.defaultAttribute
                )
                "policy-set" -> {
                    this.testPolicyName = "single-action-defined-policy-set"
                    val s = this.policyHelper.createPolicySet(
                        "src/test/resources/single-action-defined-policy-set.json", acsTemplate!!, zoneHeaders
                    )
                    Assert.assertEquals(s, CreatePolicyStatus.SUCCESS)
                }
                else -> Assert.fail("Api $api does not match/is not yet implemented for this test code.")
            }

        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to PUT identifier: $identifier for api: $api", e)
        }

    }

    @When("^client_two does a GET on (.*?) with (.*?) in zone (.*?)$")
    @Throws(Throwable::class)
    fun client_two_does_a_GET_on_subject_with_subject_id__in_zone(
        api: String,
        identifier: String,
        subdomainSuffix: String
    ) {

        val acsTemplate = this.acsZone2Template
        val encodedIdentifier = URLEncoder.encode(identifier, "UTF-8")
        val zoneHeaders = httpHeaders()
        // differentiate between zone 1 and zone 2, which will have slightly different uris
        zoneHeaders.set(PREDIX_ZONE_ID, getZoneName(subdomainSuffix))

        val uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier)
        try {
            when (api) {
                "subject" -> {
                    this.responseEntity = acsTemplate!!.exchange(
                        uri, HttpMethod.GET, HttpEntity<Any>(zoneHeaders),
                        BaseSubject::class.java
                    )
                    this.status = this.responseEntity!!.statusCode.value()
                }
                "resource" -> {
                    this.responseEntityForResource = acsTemplate!!.exchange(
                        uri, HttpMethod.GET,
                        HttpEntity<Any>(zoneHeaders), BaseResource::class.java
                    )
                    this.status = this.responseEntityForResource!!.statusCode.value()
                }
                "policy-set" -> {
                    this.policyset = acsTemplate!!.exchange(
                        this.acsUrl + ACS_POLICY_SET_API_PATH + this.testPolicyName, HttpMethod.GET,
                        HttpEntity<Any>(zoneHeaders), PolicySet::class.java
                    )
                    this.status = this.policyset!!.statusCode.value()
                }
                else -> Assert.fail("Api $api does not match/is not yet implemented for this test code.")
            }
        } catch (e: AccessTokenRequiredException) {
            this.status = HttpStatus.FORBIDDEN.value()
        } catch (e: HttpClientErrorException) {
            this.status = HttpStatus.FORBIDDEN.value()
        }

    }

    @When("^client_one does a PUT on (.*?) with (.*?) in zone 1$")
    @Throws(Throwable::class)
    fun client_one_does_a_PUT_on_identifier_in_test_zone(
        api: String,
        identifier: String
    ) {
        val acsTemplate = this.acsZone1Template
        try {
            when (api) {
                "subject" -> this.privilegeHelper.putSubject(
                    acsTemplate!!, this.subject, this.acsUrl, this.zone1Headers!!,
                    this.privilegeHelper.defaultAttribute
                )
                "resource" -> this.privilegeHelper.putResource(
                    acsTemplate!!, this.resource, this.acsUrl!!, this.zone1Headers!!,
                    this.privilegeHelper.defaultAttribute
                )
                "policy-set" -> {
                    this.testPolicyName = "single-action-defined-policy-set"
                    this.policyHelper.createPolicySet(
                        "src/test/resources/single-action-defined-policy-set.json",
                        acsTemplate!!, this.zone1Headers!!
                    )
                }
                else -> Assert.fail("Api $api does not match/is not yet implemented for this test code.")
            }
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to PUT identifier: $identifier for api: $api")
        }

    }

    @When("^client_one does a GET on (.*?) with (.*?) in zone 1$")
    @Throws(Throwable::class)
    fun client_one_does_a_GET_on_api_with_identifier_in_test_zone_dev(
        api: String,
        identifier: String
    ) {
        val acsTemplate = this.acsZone1Template
        val encodedIdentifier = URLEncoder.encode(identifier, "UTF-8")
        val uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier)

        try {
            when (api) {
                "subject" -> {
                    this.responseEntity = acsTemplate!!.exchange(
                        uri, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers),
                        BaseSubject::class.java
                    )
                    this.status = this.responseEntity!!.statusCode.value()
                }
                "resource" -> {
                    this.responseEntityForResource = acsTemplate!!.exchange(
                        uri, HttpMethod.GET,
                        HttpEntity<Any>(this.zone1Headers), BaseResource::class.java
                    )
                    this.status = this.responseEntityForResource!!.statusCode.value()
                }
                "policy-set" -> {
                    this.policyset = acsTemplate!!.exchange(
                        this.acsUrl + ACS_POLICY_SET_API_PATH + this.testPolicyName, HttpMethod.GET,
                        HttpEntity<Any>(this.zone1Headers), PolicySet::class.java
                    )
                    this.status = this.policyset!!.statusCode.value()
                }
                else -> Assert.fail("Api $api does not match/is not yet implemented for this test code.")
            }
        } catch (e: HttpClientErrorException) {
            e.printStackTrace()
            Assert.fail("Unable to GET identifier: $identifier for api: $api")
        }

    }

    @When("^client_one does a DELETE on (.*?) with (.*?) in zone 1$")
    @Throws(Throwable::class)
    fun client_one_does_a_DELETE_on_api_with_identifier_in_test_zone_dev(
        api: String,
        identifier: String
    ) {
        val encodedIdentifier = URLEncoder.encode(identifier, "UTF-8")
        val uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier)
        try {
            this.status = this.acsZone1Template!!
                .exchange(uri, HttpMethod.DELETE, HttpEntity<Any>(this.zone1Headers), ResponseEntity::class.java)
                .statusCode.value()
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to DELETE identifier: $identifier for api: $api")
        }

    }

    @When("^client_two does a DELETE on (.*?) with (.*?) in zone (.*?)$")
    @Throws(Throwable::class)
    fun client_two_does_a_DELETE_on_api_with_identifier_in_test_zone_dev(
        api: String,
        identifier: String,
        zone: String
    ) {

        val acsTemplate = this.acsZone2Template
        val zoneName = getZoneName(zone)

        val zoneHeaders = httpHeaders()
        zoneHeaders.set(PREDIX_ZONE_ID, zoneName)

        val encodedIdentifier = URLEncoder.encode(identifier, "UTF-8")
        val uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier)
        try {
            this.status = acsTemplate!!
                .exchange(uri, HttpMethod.DELETE, HttpEntity<Any>(zoneHeaders), ResponseEntity::class.java)
                .statusCode.value()
        } catch (e: HttpClientErrorException) {
            Assert.fail("Unable to DELETE identifier: $identifier for api: $api")
        }

    }

    private fun getZoneName(subdomain: String): String? {
        return when (subdomain) {
            "1" -> zone1Name
            "2" -> zone2Name
            else -> throw IllegalArgumentException("Unexpected zone id from feature file")
        }
    }

    @Then("^the request has status code (\\d+)$")
    @Throws(Throwable::class)
    fun the_request_has_status_code(statusCode: Int) {
        // Asserts are done in when statements because global status variable
        // gets reset before this check is done
        Assert.assertEquals(this.status, statusCode)
    }

    @After
    fun cleanAfterScenario() {
        this.acsitSetUpFactory.destroy()
    }
}
