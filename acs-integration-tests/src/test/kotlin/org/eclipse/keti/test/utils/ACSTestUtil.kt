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

package org.eclipse.keti.test.utils

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.testng.Assert
import java.net.ConnectException
import java.net.URI

// used in all integration tests - if changed, it will be reflected on all tests
const val ACS_VERSION = "/v1"

fun httpHeaders(): HttpHeaders {
    val httpHeaders = HttpHeaders()
    httpHeaders.add(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
    return httpHeaders
}

fun isServerListening(url: URI): Boolean {
    val restTemplate = RestTemplate()
    try {
        restTemplate.getForObject(url, String::class.java)
    } catch (e: RestClientException) {
        if (e.cause is ConnectException) {
            return false
        }
    }

    return true
}

/**
 * @author acs-engineers@ge.com
 */
@Component
class ACSTestUtil {

    fun assertExceptionResponseBody(
        e: HttpClientErrorException,
        message: String
    ) {
        val responseBody = e.responseBodyAsString
        Assert.assertNotNull(responseBody)
        Assert.assertTrue(
            responseBody.contains(message),
            String.format("Expected=[%s], Actual=[%s]", message, responseBody)
        )
    }
}
