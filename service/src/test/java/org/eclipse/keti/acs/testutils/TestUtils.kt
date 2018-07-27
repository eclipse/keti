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

package org.eclipse.keti.acs.testutils

import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.zone.management.ZoneService
import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.net.URI
import java.net.URISyntaxException

/**
 * @author acs-engineers@ge.com
 */
class TestUtils {

    // to set the a field of object for testing
    fun setField(
        target: Any,
        name: String,
        value: Any
    ) {

        // check if the object is a proxy object
        if (AopUtils.isAopProxy(target) && target is Advised) {
            try {
                ReflectionTestUtils.setField(target.targetSource.target, name, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            ReflectionTestUtils.setField(target, name, value)
        }
    }

    fun createZone(
        name: String,
        subdomain: String
    ): Zone {
        val zone = Zone()
        zone.name = name
        zone.subdomain = subdomain
        return zone
    }

    fun createTestZone(testName: String): Zone {
        return Zone("$testName.zone", "$testName-subdomain", "")
    }

    fun setupTestZone(
        testName: String,
        zoneService: ZoneService
    ): Zone {
        val testZone = createTestZone(testName)
        zoneService.upsertZone(testZone)
        mockSecurityContext(testZone)
        mockAcsRequestContext()
        return testZone
    }

    @Throws(URISyntaxException::class)
    fun createWACWithCustomGETRequestBuilder(
        wac: WebApplicationContext,
        subdomain: String?,
        resourceURI: String
    ): MockMvcContext {
        val result = MockMvcContext()
        result.builder = MockMvcRequestBuilders.get(URI("http://$subdomain.localhost/$resourceURI"))
            .accept(MediaType.APPLICATION_JSON)
        result.mockMvc =
            MockMvcBuilders.webAppContextSetup(wac).defaultRequest<DefaultMockMvcBuilder>(result.builder).build()
        return result
    }

    @Throws(URISyntaxException::class)
    fun createWACWithCustomDELETERequestBuilder(
        wac: WebApplicationContext,
        subdomain: String?,
        resourceURI: String
    ): MockMvcContext {
        val result = MockMvcContext()
        result.builder = MockMvcRequestBuilders.delete(URI("http://$subdomain.localhost/$resourceURI"))
        result.mockMvc =
            MockMvcBuilders.webAppContextSetup(wac).defaultRequest<DefaultMockMvcBuilder>(result.builder).build()
        return result
    }

    @Throws(URISyntaxException::class)
    fun createWACWithCustomPUTRequestBuilder(
        wac: WebApplicationContext,
        subdomain: String?,
        resourceURI: String
    ): MockMvcContext {
        val result = MockMvcContext()
        result.builder = MockMvcRequestBuilders.put(URI("http://$subdomain.localhost/$resourceURI"))
        result.mockMvc =
            MockMvcBuilders.webAppContextSetup(wac).defaultRequest<DefaultMockMvcBuilder>(result.builder).build()
        return result
    }

    @Throws(URISyntaxException::class)
    fun createWACWithCustomPOSTRequestBuilder(
        wac: WebApplicationContext,
        subdomain: String?,
        resourceURI: String
    ): MockMvcContext {
        val result = MockMvcContext()
        result.builder = MockMvcRequestBuilders.post(URI("http://$subdomain.localhost/$resourceURI"))
        result.mockMvc =
            MockMvcBuilders.webAppContextSetup(wac).defaultRequest<DefaultMockMvcBuilder>(result.builder).build()
        return result
    }
}
