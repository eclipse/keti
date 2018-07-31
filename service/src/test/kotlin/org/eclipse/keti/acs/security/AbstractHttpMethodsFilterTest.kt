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

package org.eclipse.keti.acs.security

import org.eclipse.jetty.http.MimeTypes
import org.eclipse.keti.acs.commons.web.ok
import org.mockito.InjectMocks
import org.mockito.MockitoAnnotations
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.net.URI
import java.util.Collections

private const val V1_DUMMY = "/v1/dummy"

class AbstractHttpMethodsFilterTest {

    @InjectMocks
    private lateinit var dummyController: DummyController

    private var mockMvc: MockMvc? = null

    private class DummyHttpMethodsFilter internal constructor() :
        AbstractHttpMethodsFilter(Collections.singletonMap("\\A$V1_DUMMY/??\\Z", setOf(HttpMethod.GET)))

    @RestController
    private class DummyController {

        @RequestMapping(method = [(RequestMethod.GET)], value = [V1_DUMMY])
        fun dummy(): ResponseEntity<String> {
            return ok()
        }
    }

    @BeforeClass
    fun setup() {
        MockitoAnnotations.initMocks(this)
        this.mockMvc = MockMvcBuilders.standaloneSetup(this.dummyController)
            .addFilters<StandaloneMockMvcBuilder>(DummyHttpMethodsFilter()).build()
    }

    @Test
    @Throws(Exception::class)
    fun testWithNoAcceptHeaderInRequest() {
        this.mockMvc!!.perform(MockMvcRequestBuilders.request(HttpMethod.GET, URI.create(V1_DUMMY)))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test(dataProvider = "mediaTypesAndExpectedStatuses")
    @Throws(Exception::class)
    fun testUnacceptableMediaTypes(
        mediaType: String,
        resultMatcher: ResultMatcher
    ) {
        this.mockMvc!!.perform(
            MockMvcRequestBuilders.request(HttpMethod.GET, URI.create(V1_DUMMY))
                .header(HttpHeaders.ACCEPT, mediaType)
        ).andExpect(resultMatcher)
    }

    @DataProvider
    fun mediaTypesAndExpectedStatuses(): Array<Array<out Any?>> {
        return arrayOf(
            arrayOf(MediaType.ALL_VALUE, MockMvcResultMatchers.status().isOk),
            arrayOf(MediaType.APPLICATION_JSON_VALUE, MockMvcResultMatchers.status().isOk),
            arrayOf(MimeTypes.Type.APPLICATION_JSON_UTF_8.toString(), MockMvcResultMatchers.status().isOk),
            arrayOf(MediaType.APPLICATION_JSON_VALUE + ", application/*+json", MockMvcResultMatchers.status().isOk),
            arrayOf(MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk),
            arrayOf(MimeTypes.Type.TEXT_PLAIN_UTF_8.toString(), MockMvcResultMatchers.status().isOk),
            arrayOf("text/*+plain, " + MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk),
            arrayOf("fake/type, " + MediaType.TEXT_PLAIN_VALUE, MockMvcResultMatchers.status().isOk),
            arrayOf("fake/type", MockMvcResultMatchers.status().isNotAcceptable)
        )
    }
}
