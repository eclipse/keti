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

import org.eclipse.keti.acs.commons.web.RESOURCE_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.SUBJECT_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
class AttributeConnectorConfigurationIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var acsUrl: String? = null
    private var zone1Admin: OAuth2RestTemplate? = null
    private var zone1ConnectorAdmin: OAuth2RestTemplate? = null
    private var zone1ConnectorReadClient: OAuth2RestTemplate? = null
    private var zone1Headers: HttpHeaders? = null

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()

        this.acsUrl = this.acsitSetUpFactory.acsUrl

        this.zone1Admin = this.acsitSetUpFactory.acsZoneAdminRestTemplate

        this.zone1ConnectorAdmin = this.acsitSetUpFactory
            .getAcsZoneConnectorAdminRestTemplate(this.acsitSetUpFactory.acsZone1Name)
        this.zone1ConnectorReadClient = this.acsitSetUpFactory
            .getAcsZoneConnectorReadRestTemplate(this.acsitSetUpFactory.acsZone1Name)

        this.zone1Headers = this.acsitSetUpFactory.zone1Headers

    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testPutGetDeleteConnector(endpointUrl: String) {
        val expectedConnector = AttributeConnector()
        expectedConnector.maxCachedIntervalMinutes = 100
        expectedConnector.adapters = setOf(
            AttributeAdapterConnection(
                "https://my-endpoint.com",
                "https://my-uaa.com", "my-client", "my-secret"
            )
        )
        try {
            this.zone1ConnectorAdmin!!.put(
                this.acsUrl + V1 + endpointUrl,
                HttpEntity(expectedConnector, this.zone1Headers)
            )
        } catch (e: Exception) {
            Assert.fail("Unable to create attribute connector. " + e.message)
        }

        try {
            val response = this.zone1ConnectorReadClient!!.exchange(
                this.acsUrl + V1 + endpointUrl, HttpMethod.GET, HttpEntity<Any>(this.zone1Headers),
                AttributeConnector::class.java
            )
            Assert.assertEquals(response.body, expectedConnector)
        } catch (e: Exception) {
            Assert.fail("Unable to retrieve attribute connector." + e.message)
        } finally {
            this.zone1ConnectorAdmin!!.exchange(
                this.acsUrl + V1 + endpointUrl, HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), String::class.java
            )
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testCreateConnectorDeniedWithoutOauthToken(endpointUrl: String) {
        val acs = RestTemplate()
        try {
            acs.put(
                this.acsUrl + V1 + endpointUrl,
                HttpEntity(AttributeConnector(), this.zone1Headers)
            )
            Assert.fail("No exception thrown when configuring connector without a token.")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.UNAUTHORIZED)
        }

    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testCreateConnectorDeniedWithoutSufficientScope(endpointUrl: String) {
        try {
            this.zone1ConnectorReadClient!!.put(
                this.acsUrl + V1 + endpointUrl,
                HttpEntity(AttributeConnector(), this.zone1Headers)
            )
            Assert.fail("No exception thrown when creating connector without sufficient scope.")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.FORBIDDEN)
        }

    }

    // Due to the issue in spring security, 403 Forbidden response from the server, is received as a 400 Bad Request
    // error code because error is not correctly translated by the JSON deserializer
    //https://github.com/spring-projects/spring-security-oauth/issues/191
    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testGetConnectorDeniedWithoutSufficientScope(endpointUrl: String) {
        try {
            this.zone1Admin!!.exchange(
                this.acsUrl + V1 + endpointUrl, HttpMethod.GET,
                HttpEntity<Any>(this.zone1Headers), AttributeConnector::class.java
            )
            Assert.fail("No exception thrown when retrieving connector without sufficient scope.")
        } catch (e: HttpClientErrorException) {
            e.printStackTrace()
            Assert.assertEquals(e.statusCode, HttpStatus.FORBIDDEN)
        } catch (e: OAuth2Exception) {
            e.printStackTrace()
            Assert.assertEquals(e.httpErrorCode, HttpStatus.BAD_REQUEST.value())
        }

    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testDeleteConnectorDeniedWithoutSufficientScope(endpointUrl: String) {
        try {
            this.zone1ConnectorReadClient!!.exchange(
                this.acsUrl + V1 + endpointUrl, HttpMethod.DELETE,
                HttpEntity<Any>(this.zone1Headers), String::class.java
            )
            Assert.fail("No exception thrown when deleting connector without sufficient scope.")
        } catch (e: HttpClientErrorException) {
            Assert.assertEquals(e.statusCode, HttpStatus.FORBIDDEN)
        }

    }

    @DataProvider(name = "requestUrlProvider")
    private fun requestUrlProvider(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(arrayOf(RESOURCE_CONNECTOR_URL), arrayOf(SUBJECT_CONNECTOR_URL))
    }

    @AfterClass
    @Throws(Exception::class)
    fun cleanup() {
        this.acsitSetUpFactory.destroy()
    }
}
