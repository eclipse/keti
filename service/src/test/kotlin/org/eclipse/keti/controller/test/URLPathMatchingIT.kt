/*******************************************************************************
 * Copyright 2018 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.controller.test

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.utils.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test
class URLPathMatchingIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var wac: WebApplicationContext

    private var mockMvc: MockMvc? = null
    private val jsonUtils = JsonUtils()
    private val objectWriter = ObjectMapper().writer().withDefaultPrettyPrinter()
    private var zone: Zone? = null

    @BeforeClass
    fun setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }

    @Test(dataProvider = "nonMatchedUrlPatternDp")
    @Throws(Exception::class)
    fun testReturnNotFoundForNotMatchedURLs(request: RequestBuilder) {
        this.mockMvc!!.perform(request).andExpect(status().isNotFound)
    }

    @DataProvider
    @Throws(JsonProcessingException::class)
    fun nonMatchedUrlPatternDp(): Array<Array<Any?>> {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone::class.java)
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid")
        val zoneContent = this.objectWriter.writeValueAsString(this.zone)

        return arrayOf<Array<Any?>>(
            arrayOf(
                put("/ /v1/zone/zone-1", "zone-1").contentType(MediaType.APPLICATION_JSON).content(
                    zoneContent
                )
            ),
            arrayOf(
                put(
                    "/v1/ /zone/zone-1",
                    "zone-1"
                ).contentType(MediaType.APPLICATION_JSON).content(zoneContent)
            ),
            arrayOf(get("/ /v1/zone/zone-2").contentType(MediaType.APPLICATION_JSON)),
            arrayOf(get("/v1/ /zone/zone-2").contentType(MediaType.APPLICATION_JSON))
        )
    }
}
