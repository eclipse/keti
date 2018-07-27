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
import org.eclipse.keti.acs.privilege.management.INCORRECT_PARAMETER_TYPE_ERROR
import org.eclipse.keti.acs.privilege.management.INCORRECT_PARAMETER_TYPE_MESSAGE
import org.eclipse.keti.acs.privilege.management.INHERITED_ATTRIBUTES_REQUEST_PARAMETER
import org.eclipse.keti.acs.request.context.AcsRequestContext
import org.eclipse.keti.acs.request.context.acsRequestContext
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.isIn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.net.URLEncoder
import java.util.HashMap

internal const val RESOURCE_BASE_URL = "/v1/resource"
private val OBJECT_MAPPER = ObjectMapper()
private val JSON_UTILS = JsonUtils()
private val TEST_UTILS = TestUtils()

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class ResourcePrivilegeManagementControllerIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var wac: WebApplicationContext

    @Autowired
    private lateinit var zoneService: ZoneService

    private var testZone: Zone? = null

    private var testZone2: Zone? = null

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.testZone = TEST_UTILS.setupTestZone("ResourceMgmtControllerIT", zoneService)

    }

    @Test
    @Throws(Exception::class)
    fun testResourceInvalidMediaTypeResponseStatusCheck() {

        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api"
        // create resource in first zone
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE).content("testString")
        ).andExpect(status().isUnsupportedMediaType)

    }

    @Test
    @Throws(Exception::class)
    fun resourceZoneDoesNotExistException() {
        // NOTE: To throw a ZoneDoesNotExistException, we must ensure that the AcsRequestContext in the
        //       SpringSecurityZoneResolver class returns a null ZoneEntity
        mockSecurityContext(null)
        mockAcsRequestContext()
        val resource = JSON_UTILS.deserializeFromFile(
            "controller-test/a-resource.json", BaseResource::class.java
        )
        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api"
        // create resource in first zone
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        val resultActions = putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(resource)
            )
        )
        resultActions.andExpect(status().isBadRequest)
        resultActions.andReturn().response.contentAsString!!.contentEquals("Zone not found")
        mockSecurityContext(this.testZone)
        mockAcsRequestContext()
    }

    @Test
    @Throws(Exception::class)
    fun testSameResourceDifferentZones() {
        val resource = JSON_UTILS.deserializeFromFile(
            "controller-test/a-resource.json", BaseResource::class.java
        )
        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api"
        // create resource in first zone
        var putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(resource))
        ).andExpect(status().isCreated)

        // create resource in second zone
        this.testZone2 = TEST_UTILS.setupTestZone("ResourceMgmtControllerIT2", zoneService)
        putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone2!!.subdomain, thisUri
        )
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(resource))
        ).andExpect(status().isCreated)
        // we expect both resources to be create in each zone
        // set security context back to first test zone
        mockSecurityContext(this.testZone)
    }

    @Test
    @Throws(Exception::class)
    fun testPOSTResources() {

        val resources = JSON_UTILS.deserializeFromFile(
            "controller-test/resources-collection.json", List::class.java
        )
        Assert.assertNotNull(resources)

        // Append a list of resources
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, RESOURCE_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    resources
                )
            )
        ).andExpect(status().isNoContent)

        // Get the list of resources
        var getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, RESOURCE_BASE_URL)
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk).andExpect(
            jsonPath(
                "$[0].resourceIdentifier",
                isIn(arrayOf("/services/secured-api", "/services/reports/01928374102398741235123"))
            )
        ).andExpect(
            jsonPath(
                "$[1].resourceIdentifier",
                isIn(arrayOf("/services/secured-api", "/services/reports/01928374102398741235123"))
            )
        ).andExpect(jsonPath("$[0].attributes[0].value", isIn(arrayOf("sales", "admin"))))
            .andExpect(jsonPath("$[0].attributes[0].issuer", `is`("https://acs.attributes.int")))
            .andExpect(jsonPath("$[1].attributes[0].value", isIn(arrayOf("sales", "admin"))))
            .andExpect(jsonPath("$[1].attributes[0].issuer", `is`("https://acs.attributes.int")))

        val resource = JSON_UTILS.deserializeFromFile(
            "controller-test/a-resource.json", BaseResource::class.java
        )
        Assert.assertNotNull(resource)

        val aResourceURI = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api"

        // Update a given resource
        val updateContext =
            TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, aResourceURI)
        updateContext.mockMvc.perform(
            updateContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    resource
                )
            )
        ).andExpect(status().isNoContent)

        // Get a given resource
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, aResourceURI
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk)
            .andExpect(jsonPath("resourceIdentifier", `is`("/services/secured-api")))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("supervisor", "it"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        var deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone!!.subdomain, aResourceURI)

        // Delete a given resource
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)

        // Delete a given resource
        deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(
            this.wac, this.testZone!!.subdomain, "$RESOURCE_BASE_URL/01928374102398741235123"
        )
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)

        // Make sure resource does not exist anymore
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, "/tenant/ge/resource/0192837410239874"
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isNotFound)

        // Make sure resource does not exist anymore
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, "/tenant/ge/resource/01928374102398741235123"
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun testZoneDoesNotExist() {

        val testZone3 = Zone("name", "subdomain", "description")
        mockSecurityContext(testZone3)

        val newMap = HashMap<AcsRequestContext.ACSRequestContextAttribute, Any?>()
        newMap[AcsRequestContext.ACSRequestContextAttribute.ZONE_ENTITY] = null

        ReflectionTestUtils.setField(
            acsRequestContext, "unModifiableRequestContextMap", newMap
        )

        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, testZone3.subdomain, "$RESOURCE_BASE_URL/test-resource"
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isBadRequest)
            .andExpect(jsonPath(INCORRECT_PARAMETER_TYPE_ERROR, `is`("Bad Request"))).andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_MESSAGE, `is`("Zone not found")
                )
            )
        mockAcsRequestContext()
    }

    @Test
    @Throws(Exception::class)
    fun testPOSTResourcesMissingResourceId() {
        val resources = JSON_UTILS.deserializeFromFile(
            "controller-test/missing-resourceIdentifier-resources-collection.json", List::class.java
        )

        Assert.assertNotNull(resources)

        // Append a list of resources
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, RESOURCE_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    resources
                )
            )
        ).andExpect(status().isUnprocessableEntity)

    }

    @Test
    @Throws(Exception::class)
    fun testPOSTResourcesMissingIdentifier() {
        val resources = JSON_UTILS
            .deserializeFromFile("controller-test/missing-identifier-resources-collection.json", List::class.java)

        Assert.assertNotNull(resources)

        // Append a list of resources
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, RESOURCE_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    resources
                )
            )
        ).andExpect(status().isUnprocessableEntity)

    }

    @Test
    @Throws(Exception::class)
    fun testPOSTResourcesEmptyIdentifier() {
        val resources = JSON_UTILS
            .deserializeFromFile("controller-test/empty-identifier-resources-collection.json", List::class.java)

        Assert.assertNotNull(resources)

        // Append a list of resources
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, RESOURCE_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    resources
                )
            )
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun testResourceIdentifierMismatch() {

        val resource = JSON_UTILS.deserializeFromFile(
            "controller-test/a-mismatched-resourceid.json", BaseResource::class.java
        )
        Assert.assertNotNull(resource)

        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api"

        // Update a given resource
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(resource))
        ).andExpect(status().isUnprocessableEntity)

    }

    @Test
    @Throws(Exception::class)
    fun testTypeMismatchForQueryParameter() {

        // GET a given resource
        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api?includeInheritedAttributes=true)"
        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        getContext.mockMvc.perform(getContext.builder.contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest).andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_ERROR, `is`(
                        HttpStatus.BAD_REQUEST.reasonPhrase
                    )
                )
            ).andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_MESSAGE, `is`(
                        "Request Parameter $INHERITED_ATTRIBUTES_REQUEST_PARAMETER must be a boolean value"
                    )
                )
            )

    }

    @Test
    @Throws(Exception::class)
    fun testPUTResourceNoResourceId() {

        val resource = JSON_UTILS
            .deserializeFromFile("controller-test/no-resourceIdentifier-resource.json", BaseResource::class.java)
        Assert.assertNotNull(resource)

        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api%2Fsubresource"
        // Update a given resource
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(resource))
        ).andExpect(status().is2xxSuccessful)

        // Get a given resource
        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk)
            .andExpect(jsonPath("resourceIdentifier", `is`("/services/secured-api/subresource")))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("supervisor", "it"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        // Delete a given resource
        val deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)
    }

    @Test
    @Throws(Exception::class)
    fun testPUTCreateResourceThenUpdateNoResourceIdentifier() {

        val resource = JSON_UTILS
            .deserializeFromFile("controller-test/with-resourceIdentifier-resource.json", BaseResource::class.java)
        Assert.assertNotNull(resource)

        val thisUri = "$RESOURCE_BASE_URL/%2Fservices%2Fsecured-api%2Fsubresource"

        var putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(resource))
        ).andExpect(status().isCreated)

        // Get a given resource
        var getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk)
            .andExpect(jsonPath("resourceIdentifier", `is`<String>(resource!!.resourceIdentifier)))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("supervisor", "it"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        // Ensure we can update resource without a resource identifier in json
        // payload
        // In this case, the resource identifier must be part of the URI.
        val resourceNoResourceIdentifier = JSON_UTILS
            .deserializeFromFile("controller-test/no-resourceIdentifier-resource.json", BaseResource::class.java)
        Assert.assertNotNull(resourceNoResourceIdentifier)

        // Update a given resource
        putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain, thisUri
        )
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(
                    resourceNoResourceIdentifier
                )
            )
        ).andExpect(status().isNoContent)

        // Get a given resource
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain, thisUri
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk)
            .andExpect(jsonPath("resourceIdentifier", `is`<String>(resource.resourceIdentifier)))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("supervisor", "it"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        // Delete a given resource
        val deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)

    }

    @Test
    @Throws(Exception::class)
    fun testPathVariablesWithSpecialCharacters() {

        // If the given string violates RFC&nbsp;2396 it will throw http 422
        // error
        // The following characters are valid: @#!$*&()_-+=[]:;{}'~`,
        val decoded = "/services/special/@#!$*&()_-+=[]:;{}'~`,"
        val encoded = URLEncoder.encode(decoded, "UTF-8")
        val thisUri = "$RESOURCE_BASE_URL/$encoded"

        val resource = JSON_UTILS
            .deserializeFromFile("controller-test/special-character-resource-identifier.json", BaseResource::class.java)
        Assert.assertNotNull(resource)

        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(resource))
        ).andExpect(status().isCreated)

        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk)
            .andExpect(jsonPath("resourceIdentifier", `is`(decoded)))
    }
}
