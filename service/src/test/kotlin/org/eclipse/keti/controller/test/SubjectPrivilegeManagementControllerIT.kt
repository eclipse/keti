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
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.isIn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.HashMap

private val OBJECT_MAPPER = ObjectMapper()
internal const val SUBJECT_BASE_URL = "/v1/subject"
private val JSON_UTILS = JsonUtils()
private val TEST_UTILS = TestUtils()

/**
 * @author acs-engineers@ge.com
 */
@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class SubjectPrivilegeManagementControllerIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var zoneService: ZoneService

    @Autowired
    private lateinit var wac: WebApplicationContext

    private var testZone: Zone? = null
    private var testZone2: Zone? = null

    @BeforeClass
    fun setup() {
        this.testZone = TEST_UTILS.setupTestZone("SubjectMgmtControllerIT", zoneService)
    }

    @Test
    @Throws(Exception::class)
    fun subjectZoneDoesNotExistException() {
        // NOTE: To throw a ZoneDoesNotExistException, we must ensure that the AcsRequestContext in the
        //       SpringSecurityZoneResolver class returns a null ZoneEntity
        mockSecurityContext(null)
        mockAcsRequestContext()

        val subject = JSON_UTILS.deserializeFromFile("controller-test/a-subject.json", BaseSubject::class.java)
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(
            this.wac,
            "zoneDoesNotExist", SUBJECT_BASE_URL + '/'.toString() + subject!!.subjectIdentifier
        )
        val resultActions = putContext.mockMvc.perform(
            putContext.builder
                .contentType(MediaType.APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(subject))
        )
        resultActions.andExpect(status().isBadRequest)
        resultActions.andReturn().response.contentAsString!!.contentEquals("Zone not found")
        mockSecurityContext(this.testZone)
        mockAcsRequestContext()
    }

    @Test
    @Throws(Exception::class)
    fun testSameSubjectDifferentZones() {
        val subject = JSON_UTILS.deserializeFromFile("controller-test/a-subject.json", BaseSubject::class.java)
        val thisUri = "$SUBJECT_BASE_URL/dave"
        // create subject in first zone
        var putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))
        ).andExpect(status().isCreated)

        // create subject in second zone
        this.testZone2 = TEST_UTILS.setupTestZone("SubjectMgmtControllerIT2", zoneService)

        putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone2!!.subdomain,
            thisUri
        )
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))
        ).andExpect(status().isCreated)
        // we expect both subjects to be create in each zone
        // set security context back to first test zone
        mockSecurityContext(this.testZone)
    }

    @Test
    @Throws(Exception::class)
    fun testSubjectInvalidMediaTypeResponseStatusCheck() {

        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.TEXT_PLAIN)
                .content("testString")
        ).andExpect(status().isUnsupportedMediaType)
    }

    @Test
    @Throws(Exception::class)
    fun testSubjects() {
        val subjects = JSON_UTILS.deserializeFromFile(
            "controller-test/subjects-collection.json",
            List::class.java
        )
        Assert.assertNotNull(subjects)

        // Append a list of subjects
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))
        ).andExpect(status().isNoContent)

        // Get the list of subjects
        var getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)

        val resultActions = getContext.mockMvc.perform(getContext.builder)
        assertSubjects(resultActions, 2, arrayOf("dave", "vineet"))
        assertSubjectsAttributes(
            resultActions, 2, 2, arrayOf("group", "department"),
            arrayOf("sales", "admin"), arrayOf("https://acs.attributes.int")
        )

        val subject = JSON_UTILS.deserializeFromFile("controller-test/a-subject.json", BaseSubject::class.java)
        Assert.assertNotNull(subject)

        // Update a given subject

        var thisUri = "$SUBJECT_BASE_URL/dave"

        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))
        ).andExpect(status().isNoContent)

        // Get a given subject
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            thisUri
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isOk)
            .andExpect(jsonPath("subjectIdentifier", `is`("dave")))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("supervisor", "it"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        // Delete resources from created collection
        thisUri = "$SUBJECT_BASE_URL/vineet"
        var deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)
        // Make sure subject does not exist anymore
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            thisUri
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isNotFound)
        thisUri = "$SUBJECT_BASE_URL/dave"
        deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(
            this.wac, this.testZone!!.subdomain,
            thisUri
        )
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)

        // Make sure subject does not exist anymore
        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            thisUri
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isNotFound)
    }

    /*
        * This test posts a collection of subjects where one is missing an subject identifier
        */
    @Test
    @Throws(Exception::class)
    fun testPOSTSubjectsMissingSubjectIdentifier() {

        val subjects = JSON_UTILS
            .deserializeFromFile("controller-test/missing-subjectidentifier-collection.json", List::class.java)

        Assert.assertNotNull(subjects)

        // Append a list of subjects
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        postContext.mockMvc
            .perform(
                postContext.builder.contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(subjects))
            )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun testPOSTSubjectsEmptyIdentifier() {

        val subjects = JSON_UTILS
            .deserializeFromFile("controller-test/empty-identifier-subjects-collection.json", List::class.java)
        Assert.assertNotNull(subjects)

        // Append a list of subjects
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        postContext.mockMvc
            .perform(
                postContext.builder.contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(subjects))
            )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun testPUTSubjectIdentifierMismatch() {
        val subject = JSON_UTILS.deserializeFromFile(
            "controller-test/a-mismatched-subjectidentifier.json",
            BaseSubject::class.java
        )
        Assert.assertNotNull(subject)

        // Update a given resource
        val thisUri = "$SUBJECT_BASE_URL/dave"
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc
            .perform(
                putContext.builder.contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(subject))
            )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun testTypeMismatchForQueryParameter() {

        // GET a given resource
        val thisUri = "$SUBJECT_BASE_URL/dave?includeInheritedAttributes=true)"
        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        getContext.mockMvc
            .perform(getContext.builder.contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest)
            .andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_ERROR, `is`(
                        HttpStatus
                            .BAD_REQUEST.reasonPhrase
                    )
                )
            )
            .andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_MESSAGE,
                    `is`(
                        "Request Parameter " + INHERITED_ATTRIBUTES_REQUEST_PARAMETER +
                        " must be a boolean value"
                    )
                )
            )
    }

    /*
        * This tests putting a single subject that is does not have a subject identifier
        */
    @Test
    @Throws(Exception::class)
    fun testPUTSubjectNoSubjectIdentifier() {

        val subject = JSON_UTILS.deserializeFromFile(
            "controller-test/no-subjectidentifier-subject.json",
            BaseSubject::class.java
        )
        Assert.assertNotNull(subject)

        // Update a given resource
        val thisUri = "$SUBJECT_BASE_URL/fermin"
        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))
        ).andExpect(status().is2xxSuccessful)

        // Get a given resource
        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        val resultActions = getContext.mockMvc.perform(getContext.builder)

        resultActions.andExpect(status().isOk).andExpect(jsonPath("subjectIdentifier", `is`("fermin")))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("sales", "admin"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        // Delete a given resource
        val deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)

        // Make sure subject does not exist anymore
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun testPUTCreateSubjectThenUpdateNoSubjectIdentifier() {

        val subject = JSON_UTILS.deserializeFromFile(
            "controller-test/with-subjectidentifier-subject.json",
            BaseSubject::class.java
        )
        Assert.assertNotNull(subject)

        val subjectIdentifier = "fermin"
        val thisUri = "$SUBJECT_BASE_URL/$subjectIdentifier"

        var putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subject))
        ).andExpect(status().isCreated)

        var getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        var resultActions = getContext.mockMvc.perform(getContext.builder)

        resultActions.andExpect(status().isOk).andExpect(jsonPath("subjectIdentifier", `is`(subjectIdentifier)))
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("admin", "sales"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        val subjectNoSubjectIdentifier = JSON_UTILS
            .deserializeFromFile("controller-test/no-subjectidentifier-subject.json", BaseSubject::class.java)
        Assert.assertNotNull(subjectNoSubjectIdentifier)

        putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            thisUri
        )
        putContext.mockMvc
            .perform(
                putContext.builder.contentType(MediaType.APPLICATION_JSON)
                    .content(OBJECT_MAPPER.writeValueAsString(subjectNoSubjectIdentifier))
            )
            .andExpect(status().isNoContent)

        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            thisUri
        )
        resultActions = getContext.mockMvc.perform(getContext.builder)

        resultActions.andExpect(status().isOk).andExpect(
            jsonPath(
                "subjectIdentifier",
                `is`(subjectIdentifier)
            )
        )
            .andExpect(jsonPath("attributes[0].value", isIn(arrayOf("admin", "sales"))))
            .andExpect(jsonPath("attributes[0].issuer", `is`("https://acs.attributes.int")))

        // Delete a given resource
        val deleteContext =
            TEST_UTILS.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone!!.subdomain, thisUri)
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)
    }

    @Test
    @Throws(Exception::class)
    fun testZoneDoesNotExist() {
        val testZone3 = Zone("name", "subdomain", "description")
        mockSecurityContext(testZone3)

        val newMap = HashMap<AcsRequestContext.ACSRequestContextAttribute, Any?>()
        newMap[AcsRequestContext.ACSRequestContextAttribute.ZONE_ENTITY] = null

        ReflectionTestUtils.setField(
            acsRequestContext,
            "unModifiableRequestContextMap", newMap
        )

        val getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, testZone3.subdomain,
            "$SUBJECT_BASE_URL/test-subject"
        )
        getContext.mockMvc.perform(getContext.builder).andExpect(status().isBadRequest)
            .andExpect(jsonPath(INCORRECT_PARAMETER_TYPE_ERROR, `is`("Bad Request")))
            .andExpect(
                jsonPath(
                    INCORRECT_PARAMETER_TYPE_MESSAGE,
                    `is`("Zone not found")
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun testPOSTCreateSubjectThenUpdateAttributes() {
        // appending two subjects, the key one for this test is dave.
        var subjects = JSON_UTILS.deserializeFromFile(
            "controller-test/subjects-collection.json",
            List::class.java
        )
        Assert.assertNotNull(subjects)

        // Append a list of subjects
        var postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))
        ).andExpect(status().isNoContent)

        var getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        var resultActions = getContext.mockMvc.perform(getContext.builder)

        assertSubjects(resultActions, 2, arrayOf("dave", "vineet"))
        assertSubjectsAttributes(
            resultActions, 2, 2, arrayOf("group", "department"),
            arrayOf("admin", "sales"), arrayOf("https://acs.attributes.int")
        )

        subjects = JSON_UTILS
            .deserializeFromFile(
                "controller-test/subjects-collection-with-different-attributes.json",
                List::class.java
            )
        Assert.assertNotNull(subjects)

        postContext = TEST_UTILS.createWACWithCustomPOSTRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            SUBJECT_BASE_URL
        )
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))
        ).andExpect(status().isNoContent)

        getContext = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac, this.testZone!!.subdomain,
            SUBJECT_BASE_URL
        )
        resultActions = getContext.mockMvc.perform(getContext.builder)
        assertSubjects(resultActions, 2, arrayOf("dave", "vineet"))
        assertSubjectsAttributes(
            resultActions, 2, 2, arrayOf("group", "department"),
            arrayOf("different", "sales"), arrayOf("https://acs.attributes.int")
        )

        // delete dave
        var deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(
            this.wac, this.testZone!!.subdomain,
            "$SUBJECT_BASE_URL/dave"
        )
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)

        // delete vineet
        deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(
            this.wac, this.testZone!!.subdomain,
            "$SUBJECT_BASE_URL/vineet"
        )
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)
    }

    @Test
    @Throws(Exception::class)
    fun testPOSTCreateSubjectNoDuplicates() {
        // appending two subjects, the key one for this test is dave.

        val subjects = JSON_UTILS
            .deserializeFromFile("controller-test/a-single-subject-collection.json", List::class.java)
        Assert.assertNotNull(subjects)

        // Append a list of subjects
        val postContext =
            TEST_UTILS.createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(subjects))
        ).andExpect(status().isNoContent)

        val getContext =
            TEST_UTILS.createWACWithCustomGETRequestBuilder(this.wac, this.testZone!!.subdomain, SUBJECT_BASE_URL)
        val resultActions = getContext.mockMvc.perform(getContext.builder)
        assertSubjects(resultActions, 1, arrayOf("dave"))

        val deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(
            this.wac, this.testZone!!.subdomain,
            "$SUBJECT_BASE_URL/dave"
        )
        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)
    }

    @Throws(Exception::class)
    private fun assertSubjects(
        resultActions: ResultActions,
        size: Int,
        identifiers: Array<String>
    ) {
        resultActions.andExpect(status().isOk).andExpect(jsonPath("$", hasSize<Any>(size)))

        for (i in 0 until size) {
            val subjectIdentifierPath = String.format("$[%s].subjectIdentifier", i)
            resultActions.andExpect(jsonPath(subjectIdentifierPath, isIn(identifiers)))
        }
    }

    @Throws(Exception::class)
    private fun assertSubjectsAttributes(
        resultActions: ResultActions,
        numOfSubjects: Int,
        numOfAttrs: Int,
        attrNames: Array<String>,
        attrValues: Array<String>,
        attrIssuers: Array<String>
    ) {
        resultActions.andExpect(status().isOk).andExpect(jsonPath("$", hasSize<Any>(numOfSubjects)))

        for (i in 0 until numOfSubjects) {
            for (j in 0 until numOfAttrs) {
                val attrValuePath = String.format("$[%s].attributes[%s].value", i, j)
                val attrNamePath = String.format("$[%s].attributes[%s].name", i, j)
                val attrIssuerPath = String.format("$[%s].attributes[%s].issuer", i, j)

                resultActions.andExpect(jsonPath(attrValuePath, isIn(attrValues)))
                    .andExpect(jsonPath(attrNamePath, isIn(attrNames)))
                    .andExpect(jsonPath(attrIssuerPath, isIn(attrIssuers)))
            }
        }
    }
}
