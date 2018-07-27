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

import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.ACSTestUtil
import org.eclipse.keti.test.utils.PREDIX_ZONE_ID
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.eclipse.keti.test.utils.httpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.web.client.HttpClientErrorException
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

@Test
@ContextConfiguration("classpath:integration-test-spring-context.xml")
class DefaultZoneAuthorizationIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var zone2Name: String? = null

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()
        this.zone2Name = this.acsitSetUpFactory.zone2.name
    }

    @AfterClass
    fun cleanup() {
        this.acsitSetUpFactory.destroy()

    }

    /**
     * 1. Create a token from zone issuer with scopes for accessing: a. zone specific resources, AND b.
     * acs.zones.admin
     *
     * 2. Try to access a zone specific resource . This should work 3. Try to access /v1/zone - THIS SHOULD FAIL
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun testAccessGlobalResourceWithZoneIssuer() {
        val zone2AcsTemplate = this.acsitSetUpFactory.acsZone2AdminRestTemplate

        val zoneTwoHeaders = httpHeaders()
        zoneTwoHeaders.set(PREDIX_ZONE_ID, this.zone2Name)

        // Write a resource to zone2. This should work
        val responseEntity = this.privilegeHelper.postResources(
            zone2AcsTemplate,
            this.acsitSetUpFactory.acsUrl, zoneTwoHeaders, BaseResource("/sites/sanramon")
        )
        Assert.assertEquals(responseEntity.statusCode, HttpStatus.NO_CONTENT)

        // Try to get global resource from global/baseUrl. This should FAIL
        try {
            zone2AcsTemplate.exchange(
                this.acsitSetUpFactory.acsUrl + "/v1/zone/" + this.zone2Name, HttpMethod.GET,
                null, Zone::class.java
            )
            Assert.fail("Able to access non-zone specific resource with a zone specific issuer token!")
        } catch (e: HttpClientErrorException) {
            // expected
        }

        // Try to get global resource from zone2Url. This should FAIL
        try {
            zone2AcsTemplate.exchange(
                this.acsitSetUpFactory.acsUrl + "/v1/zone/" + this.zone2Name, HttpMethod.GET,
                HttpEntity<Any>(zoneTwoHeaders), Zone::class.java
            )
            Assert
                .fail("Able to access non-zone specific resource from a zone specific URL, " + "with a zone specific issuer token!")
        } catch (e: InvalidRequestException) {
            // expected
        }

    }

}
