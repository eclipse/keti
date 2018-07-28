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

package org.eclipse.keti.integration.test

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.IOException

private const val SWAGGER_API = "/v2/api-docs?group=acs"

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
class ACSCorsFilterIT : AbstractTestNGSpringContextTests() {

    @Value("\${ACS_URL}")
    private lateinit var acsBaseUrl: String

    private var client: HttpClient? = null

    @BeforeClass
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        client = HttpClientBuilder.create().useSystemProperties().build()
    }

    @Test
    @Throws(Exception::class)
    fun testCorsXHRRequestFromAllowedOriginForSwaggerUIApi() {
        val request = HttpGet(this.acsBaseUrl + SWAGGER_API)
        request.setHeader(HttpHeaders.ORIGIN, "http://someone.predix.io")
        request.setHeader("X-Requested-With", "true")
        val response = client!!.execute(request)
        println("Response Code : " + response.statusLine.statusCode)

        println(
            "Access-Control-Allow-Origin : " + response.getHeaders("Access-Control-Allow-Origin")[0].value
        )

        Assert.assertEquals(response.statusLine.statusCode, 200)

        Assert.assertTrue(response.containsHeader("Access-Control-Allow-Origin"))
    }

    @Test
    @Throws(Exception::class)
    fun testCorsXHRRequestFromNotWhitelistedOriginForSwaggerUIApi() {
        val request = HttpGet(this.acsBaseUrl + SWAGGER_API)
        request.setHeader(HttpHeaders.ORIGIN, "Origin: http://someone.predix.nert")
        request.setHeader("X-Requested-With", "true")
        val response = client!!.execute(request)
        println("Response Code : " + response.statusLine.statusCode)

        println("Access-Control-Allow-Origin : " + response.getHeaders("Access-Control-Allow-Origin").size)

        Assert.assertEquals(response.statusLine.statusCode, 403)
        Assert.assertFalse(response.containsHeader("Access-Control-Allow-Origin"))
    }

    @Test
    @Throws(Exception::class)
    fun testCorsXHRRequestFromWhitelistedOriginForNonSwaggerUIApi() {
        val request = HttpGet(this.acsBaseUrl + "/acs")
        request.setHeader(HttpHeaders.ORIGIN, "http://someone.predix.io")
        request.setHeader("X-Requested-With", "true")
        val response = client!!.execute(request)
        println("Response Code : " + response.statusLine.statusCode)

        println("Access-Control-Allow-Origin : " + response.getHeaders("Access-Control-Allow-Origin").size)

        Assert.assertEquals(response.statusLine.statusCode, 403)
        Assert.assertFalse(response.containsHeader("Access-Control-Allow-Origin"))
    }
}
