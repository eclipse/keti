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
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.FBI
import org.eclipse.keti.acs.testutils.SITE_BASEMENT
import org.eclipse.keti.acs.testutils.SITE_QUANTICO
import org.eclipse.keti.acs.testutils.SPECIAL_AGENTS_GROUP
import org.eclipse.keti.acs.testutils.SPECIAL_AGENTS_GROUP_ATTRIBUTE
import org.eclipse.keti.acs.testutils.TOP_SECRET_CLASSIFICATION
import org.eclipse.keti.acs.testutils.TOP_SECRET_GROUP
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.createSubjectHierarchy
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.zone.management.ZoneService
import org.hamcrest.Matchers.`is`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.testng.SkipException
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.net.URLEncoder

private val OBJECT_MAPPER = ObjectMapper()
private val TEST_UTILS = TestUtils()
private val TEST_ZONE =
    TEST_UTILS.createTestZone("SubjectMgmtControllerIT")

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class HierarchicalSubjectsIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var zoneService: ZoneService

    @Autowired
    private lateinit var wac: WebApplicationContext

    @Autowired
    private lateinit var configurableEnvironment: ConfigurableEnvironment

    @BeforeClass
    @Throws(Exception::class)
    fun beforeClass() {
        if (!listOf(*this.configurableEnvironment.activeProfiles).contains("graph")) {
            throw SkipException("This test only applies when using the \"graph\" profile")
        }

        this.zoneService.upsertZone(TEST_ZONE)
        mockSecurityContext(TEST_ZONE)
        mockAcsRequestContext()
    }

    @Test
    @Throws(Exception::class)
    fun testPostAndGetHierarchicalSubjects() {
        val subjects = createSubjectHierarchy()

        // Append a list of resources
        val postContext = TEST_UTILS.createWACWithCustomPOSTRequestBuilder(
            this.wac, TEST_ZONE.subdomain, SUBJECT_BASE_URL
        )

        postContext.mockMvc.perform(
            postContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(subjects)
            )
        ).andExpect(status().isNoContent)

        // Get the child subject without query string specifier.
        val getContext0 = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac,
            TEST_ZONE.subdomain,
            SUBJECT_BASE_URL + "/" + URLEncoder.encode(AGENT_MULDER, "UTF-8")
        )

        getContext0.mockMvc.perform(getContext0.builder).andExpect(status().isOk)
            .andExpect(jsonPath("subjectIdentifier", `is`(AGENT_MULDER)))
            .andExpect(jsonPath("attributes[0].name", `is`<String>(SITE_BASEMENT.name)))
            .andExpect(jsonPath("attributes[0].value", `is`<String>(SITE_BASEMENT.value)))
            .andExpect(jsonPath("attributes[0].issuer", `is`<String>(SITE_BASEMENT.issuer)))
            .andExpect(jsonPath("attributes[1]").doesNotExist())

        // Get the child subject without inherited attributes.
        val getContext1 = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac,
            TEST_ZONE.subdomain,
            "$SUBJECT_BASE_URL/" + URLEncoder.encode(
                AGENT_MULDER,
                "UTF-8"
            ) + "?includeInheritedAttributes=false"
        )

        getContext1.mockMvc.perform(getContext1.builder).andExpect(status().isOk)
            .andExpect(jsonPath("subjectIdentifier", `is`(AGENT_MULDER)))
            .andExpect(jsonPath("attributes[0].name", `is`<String>(SITE_BASEMENT.name)))
            .andExpect(jsonPath("attributes[0].value", `is`<String>(SITE_BASEMENT.value)))
            .andExpect(jsonPath("attributes[0].issuer", `is`<String>(SITE_BASEMENT.issuer)))
            .andExpect(jsonPath("attributes[1]").doesNotExist())

        // Get the child subject with inherited attributes.
        val getContext2 = TEST_UTILS.createWACWithCustomGETRequestBuilder(
            this.wac,
            TEST_ZONE.subdomain,
            "$SUBJECT_BASE_URL/" + URLEncoder.encode(
                AGENT_MULDER,
                "UTF-8"
            ) + "?includeInheritedAttributes=true"
        )

        getContext2.mockMvc.perform(getContext2.builder).andExpect(status().isOk)
            .andExpect(jsonPath("subjectIdentifier", `is`(AGENT_MULDER)))
            .andExpect(jsonPath("attributes[0].name", `is`<String>(TOP_SECRET_CLASSIFICATION.name)))
            .andExpect(jsonPath("attributes[0].value", `is`<String>(TOP_SECRET_CLASSIFICATION.value)))
            .andExpect(jsonPath("attributes[0].issuer", `is`<String>(TOP_SECRET_CLASSIFICATION.issuer)))
            .andExpect(jsonPath("attributes[1].name", `is`<String>(SITE_QUANTICO.name)))
            .andExpect(jsonPath("attributes[1].value", `is`<String>(SITE_QUANTICO.value)))
            .andExpect(jsonPath("attributes[1].issuer", `is`<String>(SITE_QUANTICO.issuer)))
            .andExpect(jsonPath("attributes[2].name", `is`<String>(SPECIAL_AGENTS_GROUP_ATTRIBUTE.name)))
            .andExpect(jsonPath("attributes[2].value", `is`<String>(SPECIAL_AGENTS_GROUP_ATTRIBUTE.value)))
            .andExpect(jsonPath("attributes[2].issuer", `is`<String>(SPECIAL_AGENTS_GROUP_ATTRIBUTE.issuer)))
            .andExpect(jsonPath("attributes[3].name", `is`<String>(SITE_BASEMENT.name)))
            .andExpect(jsonPath("attributes[3].value", `is`<String>(SITE_BASEMENT.value)))
            .andExpect(jsonPath("attributes[3].issuer", `is`<String>(SITE_BASEMENT.issuer)))

        // Clean up after ourselves.
        deleteSubject(AGENT_MULDER)
        deleteSubject(TOP_SECRET_GROUP)
        deleteSubject(SPECIAL_AGENTS_GROUP)
        deleteSubject(FBI)
    }

    @Throws(Exception::class)
    private fun deleteSubject(subjectIdentifier: String) {
        val subjectToDeleteURI = ("$SUBJECT_BASE_URL/" + URLEncoder.encode(
            subjectIdentifier,
            "UTF-8"
        ))

        val deleteContext = TEST_UTILS.createWACWithCustomDELETERequestBuilder(
            this.wac, TEST_ZONE.subdomain, subjectToDeleteURI
        )

        deleteContext.mockMvc.perform(deleteContext.builder).andExpect(status().isNoContent)
    }
}
