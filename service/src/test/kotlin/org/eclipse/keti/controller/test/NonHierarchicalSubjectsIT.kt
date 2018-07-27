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
import org.eclipse.keti.acs.commons.web.PARENTS_ATTR_NOT_SUPPORTED_MSG
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.testutils.mockAcsRequestContext
import org.eclipse.keti.acs.testutils.mockSecurityContext
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.springframework.beans.factory.annotation.Autowired
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
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

private val OBJECT_MAPPER = ObjectMapper()
private val JSON_UTILS = JsonUtils()
private val TEST_UTILS = TestUtils()
private val TEST_ZONE =
    TEST_UTILS.createTestZone("SubjectMgmtControllerIT")

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class NonHierarchicalSubjectsIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var zoneService: ZoneService

    @Autowired
    private lateinit var wac: WebApplicationContext

    @Autowired
    private lateinit var configurableEnvironment: ConfigurableEnvironment

    @BeforeClass
    @Throws(Exception::class)
    fun beforeClass() {
        if (listOf(*this.configurableEnvironment.activeProfiles).contains("graph")) {
            throw SkipException("This test only applies when NOT using the \"graph\" profile")
        }

        this.zoneService.upsertZone(TEST_ZONE)
        mockSecurityContext(TEST_ZONE)
        mockAcsRequestContext()
    }

    @Test
    @Throws(Exception::class)
    fun testSubjectWithParentsFailWhenNotUsingGraph() {
        val subject = JSON_UTILS.deserializeFromFile(
            "controller-test/a-subject-with-parents.json", BaseSubject::class.java
        )
        Assert.assertNotNull(subject)

        val putContext = TEST_UTILS.createWACWithCustomPUTRequestBuilder(
            this.wac,
            TEST_ZONE.subdomain,
            "$SUBJECT_BASE_URL/dave-with-parents"
        )
        val result = putContext.mockMvc.perform(
            putContext.builder.contentType(MediaType.APPLICATION_JSON).content(
                OBJECT_MAPPER.writeValueAsString(subject)
            )
        ).andExpect(status().isNotImplemented).andReturn()

        Assert.assertEquals(
            result.response.contentAsString,
            "{\"ErrorDetails\":{\"errorCode\":\"FAILURE\",\"errorMessage\":\"$PARENTS_ATTR_NOT_SUPPORTED_MSG\"}}"
        )
    }
}
