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
import org.eclipse.keti.acs.commons.web.RESOURCE_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.SUBJECT_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.commons.web.ZONE_URL
import org.eclipse.keti.acs.privilege.management.INCORRECT_PARAMETER_TYPE_ERROR
import org.eclipse.keti.acs.privilege.management.INCORRECT_PARAMETER_TYPE_MESSAGE
import org.eclipse.keti.acs.request.context.AcsRequestContext
import org.eclipse.keti.acs.request.context.acsRequestContext
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.MockAcsRequestContext
import org.eclipse.keti.acs.testutils.MockSecurityContext
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.utils.JsonUtils
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.HashMap

private const val V1_ZONE_URL = V1 + ZONE_URL
private const val V1_RESOURCE_CONNECTOR_URL = V1 + RESOURCE_CONNECTOR_URL
private const val V1_SUBJECT_CONNECTOR_URL = V1 + SUBJECT_CONNECTOR_URL

private val TEST_UTILS = TestUtils()

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class AttributeConnectorControllerIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var wac: WebApplicationContext

    private val jsonUtils = JsonUtils()
    private val objectWriter = ObjectMapper().writer().withDefaultPrettyPrinter()

    private var mockMvc: MockMvc? = null

    @BeforeClass
    fun setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testAttributeInvalidMediaTypeResponseStatusCheck(endpointUrl: String) {
        this.mockMvc!!.perform(
            put(endpointUrl).contentType(MediaType.APPLICATION_PDF_VALUE).content("testString")
        ).andExpect(status().isUnsupportedMediaType)
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testCreateAndGetAndDeleteConnector(endpointUrl: String) {
        createZone1AndAssert()

        val resourceConfig = this.jsonUtils
            .deserializeFromFile("controller-test/createAttributeConnector.json", AttributeConnector::class.java)
        Assert.assertNotNull(resourceConfig, "createAttributeConnector.json file not found or invalid")
        val resourceConfigContent = this.objectWriter.writeValueAsString(resourceConfig)
        this.mockMvc!!.perform(
            put(endpointUrl).contentType(MediaType.APPLICATION_JSON).content(resourceConfigContent)
        ).andExpect(status().isCreated)

        this.mockMvc!!.perform(get(endpointUrl)).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("isActive", equalTo(true))).andExpect(jsonPath("maxCachedIntervalMinutes", equalTo(60)))
            .andExpect(jsonPath("adapters[0].adapterEndpoint", equalTo("https://my-adapter.com")))
            .andExpect(jsonPath("adapters[0].uaaTokenUrl", equalTo("https://my-uaa.com")))
            .andExpect(jsonPath("adapters[0].uaaClientId", equalTo("adapter-client")))
            .andExpect(jsonPath("adapters[0].uaaClientSecret", equalTo("**********"))).andExpect(status().isOk)

        this.mockMvc!!.perform(delete(endpointUrl)).andExpect(status().isNoContent)
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testGetResourceConnectorWhichDoesNotExists(endpointUrl: String) {
        createZone1AndAssert()
        this.mockMvc!!.perform(get(endpointUrl)).andExpect(status().isNotFound)
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testDeleteResourceConnectorWhichDoesNotExist(endpointUrl: String) {
        createZone1AndAssert()
        this.mockMvc!!.perform(delete(endpointUrl)).andExpect(status().isNotFound)
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testCreateResourceConnectorWithEmptyAdapters(endpointUrl: String) {
        createZone1AndAssert()
        val connector = this.jsonUtils.deserializeFromFile(
            "controller-test/createAttributeConnectorWithEmptyAdapters.json", AttributeConnector::class.java
        )
        Assert.assertNotNull(connector, "createAttributeConnectorWithEmptyAdapters.json file not found or invalid")
        val connectorContent = this.objectWriter.writeValueAsString(connector)

        this.mockMvc!!.perform(
            put(endpointUrl).contentType(MediaType.APPLICATION_JSON).content(connectorContent)
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testCreateResourceConnectorWithTwoAdapters(endpointUrl: String) {
        createZone1AndAssert()
        val connector = this.jsonUtils.deserializeFromFile(
            "controller-test/createAttributeConnectorWithTwoAdapters.json", AttributeConnector::class.java
        )
        Assert.assertNotNull(connector, "createAttributeConnectorWithTwoAdapters.json file not found or invalid")
        val connectorContent = this.objectWriter.writeValueAsString(connector)

        this.mockMvc!!.perform(
            put(endpointUrl).contentType(MediaType.APPLICATION_JSON).content(connectorContent)
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test(dataProvider = "requestUrlProvider")
    @Throws(Exception::class)
    fun testCreateResourceConnectorWithCachedIntervalBelowThreshold(endpointUrl: String) {
        createZone1AndAssert()
        val connector = this.jsonUtils.deserializeFromFile(
            "controller-test/createAttributeConnectorWithLowValueForCache.json", AttributeConnector::class.java
        )
        Assert.assertNotNull(connector, "createAttributeConnectorWithLowValueForCache.json file not found or invalid")
        val connectorContent = this.objectWriter.writeValueAsString(connector)

        this.mockMvc!!.perform(
            put(endpointUrl).contentType(MediaType.APPLICATION_JSON).content(connectorContent)
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun testZoneDoesNotExist() {
        val testZone3 = Zone("name", "subdomain", "description")
        MockSecurityContext.mockSecurityContext(testZone3)

        val newMap = HashMap<AcsRequestContext.ACSRequestContextAttribute, Any?>()
        newMap[AcsRequestContext.ACSRequestContextAttribute.ZONE_ENTITY] = null

        ReflectionTestUtils.setField(
            acsRequestContext, "unModifiableRequestContextMap", newMap
        )

        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, testZone3.subdomain, "/v1/connector/resource"
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isBadRequest)
            .andExpect(jsonPath(INCORRECT_PARAMETER_TYPE_ERROR, `is`("Bad Request"))).andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_MESSAGE, `is`("Zone not found")
                )
            )
    }

    @Throws(JsonProcessingException::class, Exception::class)
    private fun createZone1AndAssert() {
        val zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone::class.java)
        Assert.assertNotNull(zone, "createZone.json file not found or invalid")
        val zoneContent = this.objectWriter.writeValueAsString(zone)
        this.mockMvc!!
            .perform(put(V1_ZONE_URL, zone!!.name).contentType(MediaType.APPLICATION_JSON).content(zoneContent))
            .andExpect(status().isCreated)

        MockSecurityContext.mockSecurityContext(zone)
        MockAcsRequestContext.mockAcsRequestContext()
    }

    @DataProvider
    private fun requestUrlProvider(): Array<Array<String>> {
        return arrayOf(arrayOf(V1_RESOURCE_CONNECTOR_URL), arrayOf(V1_SUBJECT_CONNECTOR_URL))
    }
}
