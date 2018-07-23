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

package org.eclipse.keti.acs.commons.web

import org.eclipse.keti.acs.commons.exception.UntrustedIssuerException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.testng.Assert
import org.testng.annotations.Test

class BaseRestApiControllerAdviceTest {

    @Test
    fun testBaseRestApiControllerAdviceException() {

        val restApiExceptionHandler = RestApiExceptionHandler()
        val e = RestApiException("Internal server error")

        val actualErrorResponse = restApiExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.statusCode.toString() == "500")
        Assert.assertTrue(
            actualErrorResponse.body.errorDetails.errorMessage == "Internal server error"
        )
    }

    @Test
    fun testBaseRestApiControllerAdviceHttpMediaTypeNotAcceptableException() {

        val httpMediaTypeNotAcceptableExceptionHandler = HttpMediaTypeNotAcceptableExceptionHandler()

        val e = HttpMediaTypeNotAcceptableException("Media Type Not Supported")

        val actualErrorResponse = httpMediaTypeNotAcceptableExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.errorDetails.errorMessage == "Not Acceptable")
    }

    @Test
    fun testBaseRestApiControllerAdviceHttpRequestMethodNotSupportedException() {

        val httpRequestMethodNotSupportedExceptionHandler = HttpRequestMethodNotSupportedExceptionHandler()

        val e = HttpRequestMethodNotSupportedException("GET")

        val actualErrorResponse = httpRequestMethodNotSupportedExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.errorDetails.errorMessage == "Method Not Allowed")
    }

    @Test
    fun testBaseRestApiControllerAdviceIllegalArgumentException() {

        val illegalArgumentExceptionHandler = IllegalArgumentExceptionHandler()

        val e = IllegalArgumentException("Arguments passed are invalid")

        val actualErrorResponse = illegalArgumentExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(
            actualErrorResponse.errorDetails.errorMessage == "Arguments passed are invalid"
        )
    }

    @Test
    fun testBaseRestApiControllerAdviceUntrustedIssuerException() {

        val untrustedIssuerExceptionHandler = UntrustedIssuerExceptionHandler()

        val e = UntrustedIssuerException("Not a trusted Issuer")

        val actualErrorResponse = untrustedIssuerExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.errorDetails.errorMessage == "Not a trusted Issuer")

    }

    @Test
    fun testBaseRestApiControllerAdviceSecurityException() {

        val securityExceptionHandler = SecurityExceptionHandler()

        val e = SecurityException("Not a trusted Issuer")

        val actualErrorResponse = securityExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.errorDetails.errorMessage == "Not a trusted Issuer")

    }

    @Test
    fun testBaseRestApiControllerAdviceHttpMessageNotReadableException() {

        val httpMessageNotReadableExceptionHandler = HttpMessageNotReadableExceptionHandler()
        val e = HttpMessageNotReadableException("{JSON}")

        val actualErrorResponse = httpMessageNotReadableExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.errorDetails.errorCode == "FAILED")
        Assert.assertTrue(
            actualErrorResponse.errorDetails.errorMessage == "Malformed JSON syntax. {JSON}"
        )
    }

    @Test
    fun testBaseRestApiControllerAdviceHttpMediaTypeNotSupportedException() {

        val httpMediaTypeNotSupportedExceptionHandler = HttpMediaTypeNotSupportedExceptionHandler()

        val e = HttpMediaTypeNotSupportedException("JSON")

        val actualErrorResponse = httpMediaTypeNotSupportedExceptionHandler.handleException(e)

        Assert.assertNotNull(actualErrorResponse)
        Assert.assertTrue(actualErrorResponse.errorDetails.errorMessage == "Unsupported Media Type")
    }
}
