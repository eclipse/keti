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

package org.eclipse.keti.acs.security

import com.fasterxml.jackson.core.JsonProcessingException
import org.eclipse.keti.acs.commons.web.HEALTH_URL
import org.eclipse.keti.acs.commons.web.HEARTBEAT_URL
import org.eclipse.keti.acs.commons.web.MANAGED_RESOURCES_URL
import org.eclipse.keti.acs.commons.web.POLICY_EVALUATION_URL
import org.eclipse.keti.acs.commons.web.POLICY_SETS_URL
import org.eclipse.keti.acs.commons.web.RESOURCE_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.SUBJECTS_URL
import org.eclipse.keti.acs.commons.web.SUBJECT_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.V1
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.collections.Lists
import java.net.URI
import java.util.Arrays

private val SUBJECT_URI = URI.create("$V1$SUBJECTS_URL/test")
private val SUBJECTS_URI = URI.create(V1 + SUBJECTS_URL)
private val RESOURCE_URI = URI.create("$V1$MANAGED_RESOURCES_URL/test")
private val RESOURCES_URI = URI.create(V1 + MANAGED_RESOURCES_URL)
private val POLICY_SET_URI = URI.create("$V1$POLICY_SETS_URL/test")
private val POLICY_SETS_URI = URI.create(V1 + POLICY_SETS_URL)
private val POLICY_EVAL_URI = URI.create(V1 + POLICY_EVALUATION_URL)
private val RESOURCE_CONNECTOR_URI = URI.create(
    V1 + RESOURCE_CONNECTOR_URL
)
private val SUBJECT_CONNECTOR_URI = URI.create(V1 + SUBJECT_CONNECTOR_URL)
private val ZONE_URI = URI.create("$V1/zone/test")
private val HEALTH_URI = URI.create(HEALTH_URL)

private const val CONTENT = "test content"
private const val MALFORMED_BEARER_TOKEN = "Bearer foo"

private fun postWithMalformedBearerToken(uri: URI): Array<Any?> {
    return post(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN))
}

private fun post(
    uri: URI,
    headers: HttpHeaders
): Array<Any?> {
    return arrayOf(
        MockMvcRequestBuilders.post(uri).headers(headers)
            .content(CONTENT).contentType(MediaType.APPLICATION_JSON)
    )
}

private fun putWithMalformedBearerToken(uri: URI): Array<Any?> {
    return put(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN))
}

private fun put(
    uri: URI,
    headers: HttpHeaders
): Array<Any?> {
    return arrayOf(
        MockMvcRequestBuilders.put(uri).headers(headers).content(CONTENT)
            .contentType(MediaType.APPLICATION_JSON)
    )
}

private fun getWithMalformedBearerToken(uri: URI): Array<Any?> {
    return get(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN))
}

private fun get(
    uri: URI,
    headers: HttpHeaders
): Array<Any?> {
    return arrayOf(MockMvcRequestBuilders.get(uri).headers(headers).accept(MediaType.APPLICATION_JSON))
}

private fun deleteWithMalformedBearerToken(uri: URI): Array<Any?> {
    return delete(uri, httpZoneHeaders(MALFORMED_BEARER_TOKEN))
}

private fun delete(
    uri: URI,
    headers: HttpHeaders
): Array<Any?> {
    return arrayOf(MockMvcRequestBuilders.delete(uri).headers(headers))
}

private fun httpHeaders(token: String): HttpHeaders {
    val httpHeaders = HttpHeaders()
    httpHeaders.add(HttpHeaders.AUTHORIZATION, token)
    return httpHeaders
}

private fun httpZoneHeaders(token: String): HttpHeaders {
    val httpHeaders = httpHeaders(token)
    httpHeaders.add("Predix-Zone-Id", "myzone")
    return httpHeaders
}

private fun combine(vararg testData: Array<Array<Any?>>): Array<Array<Any?>> {
    val result = Lists.newArrayList<Array<Any?>>()
    for (t in testData) {
        result.addAll(Arrays.asList(*t))
    }
    return result.toTypedArray()
}

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
class SecurityFilterChainTest : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var wac: WebApplicationContext

    private var mockMvc: MockMvc? = null

    @BeforeClass
    fun setup() {
        // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#test-mockmvc
        // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#web-app-security
        // https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#filter-ordering
        // http://projects.spring.io/spring-security-oauth/docs/oauth2.html
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(print()).build()
    }

    @Test(dataProvider = "anonymousRequestBuilder", enabled = false)
    @Throws(Exception::class)
    fun testAnonymousAccess(
        request: RequestBuilder,
        expectedStatus: ResultMatcher,
        expectedContent: ResultMatcher
    ) {
        this.mockMvc!!.perform(request).andExpect(expectedStatus).andExpect(expectedContent)
    }

    @Test(dataProvider = "invalidTokenRequestBuilder")
    @Throws(Exception::class)
    fun testInvalidTokenAccess(request: RequestBuilder) {
        this.mockMvc!!.perform(request).andExpect(status().isUnauthorized)
            .andExpect(content().string(containsString("invalid_token")))
    }

    @DataProvider
    private fun anonymousRequestBuilder(): Array<Array<Any?>> {
        return combine(testAnonymousHealth(), testAnonymousHeartbeat())
    }

    private fun testAnonymousHeartbeat(): Array<Array<Any?>> {
        return arrayOf(arrayOf(MockMvcRequestBuilders.get(HEARTBEAT_URL), status().isOk, content().string("alive")))
    }

    private fun testAnonymousHealth(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(
                MockMvcRequestBuilders.get(HEALTH_URL),
                status().isServiceUnavailable,
                content().string("{\"status\":\"DOWN\"}")
            )
        )
    }

    @DataProvider
    @Throws(JsonProcessingException::class)
    private fun invalidTokenRequestBuilder(): Array<Array<Any?>> {
        return combine(
            testHealth(), testZoneController(), testAttributeConnectorController(),
            testPolicyEvaluationController(), testPolicyManagementController(),
            testResourcePrivilegeManagementController(), testSubjectPrivilegeManagementController()
        )
    }

    private fun testHealth(): Array<Array<Any?>> {
        return arrayOf(get(HEALTH_URI, httpHeaders(MALFORMED_BEARER_TOKEN)))
    }

    private fun testZoneController(): Array<Array<Any?>> {
        return arrayOf(
            put(ZONE_URI, httpHeaders(MALFORMED_BEARER_TOKEN)),
            get(ZONE_URI, httpHeaders(MALFORMED_BEARER_TOKEN)),
            delete(ZONE_URI, httpHeaders(MALFORMED_BEARER_TOKEN))
        )
    }

    private fun testAttributeConnectorController(): Array<Array<Any?>> {
        return arrayOf(
            putWithMalformedBearerToken(RESOURCE_CONNECTOR_URI),
            getWithMalformedBearerToken(RESOURCE_CONNECTOR_URI),
            deleteWithMalformedBearerToken(RESOURCE_CONNECTOR_URI),
            putWithMalformedBearerToken(SUBJECT_CONNECTOR_URI),
            getWithMalformedBearerToken(SUBJECT_CONNECTOR_URI),
            deleteWithMalformedBearerToken(SUBJECT_CONNECTOR_URI)
        )
    }

    private fun testPolicyEvaluationController(): Array<Array<Any?>> {
        return arrayOf(postWithMalformedBearerToken(POLICY_EVAL_URI))
    }

    private fun testPolicyManagementController(): Array<Array<Any?>> {
        return arrayOf(
            putWithMalformedBearerToken(POLICY_SET_URI),
            getWithMalformedBearerToken(POLICY_SET_URI),
            deleteWithMalformedBearerToken(POLICY_SET_URI),
            getWithMalformedBearerToken(POLICY_SETS_URI)
        )
    }

    private fun testResourcePrivilegeManagementController(): Array<Array<Any?>> {
        return arrayOf(
            postWithMalformedBearerToken(RESOURCES_URI),
            getWithMalformedBearerToken(RESOURCES_URI),
            getWithMalformedBearerToken(RESOURCE_URI),
            putWithMalformedBearerToken(RESOURCE_URI),
            deleteWithMalformedBearerToken(RESOURCE_URI)
        )
    }

    private fun testSubjectPrivilegeManagementController(): Array<Array<Any?>> {
        return arrayOf(
            postWithMalformedBearerToken(SUBJECTS_URI),
            getWithMalformedBearerToken(SUBJECTS_URI),
            getWithMalformedBearerToken(SUBJECT_URI),
            putWithMalformedBearerToken(SUBJECT_URI),
            deleteWithMalformedBearerToken(SUBJECT_URI)
        )
    }
}
