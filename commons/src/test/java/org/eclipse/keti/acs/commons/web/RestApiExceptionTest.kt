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

import org.springframework.http.HttpStatus
import org.testng.Assert
import org.testng.annotations.Test

class RestApiExceptionTest {

    @Test
    fun testRestApiExceptionDefaultConstructor() {
        val apiException = RestApiException()
        Assert.assertEquals(apiException.message, null)
        Assert.assertEquals(apiException.appErrorCode, "FAILURE")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testRestApiExceptionWithMessage() {
        val apiException = RestApiException("message")
        Assert.assertEquals(apiException.message, "message")
        Assert.assertEquals(apiException.appErrorCode, "FAILURE")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testRestApiExceptionWithException() {
        val runtimeException = RuntimeException("Runtime exception message")
        val apiException = RestApiException(runtimeException)

        Assert.assertEquals(apiException.cause, runtimeException)
        Assert.assertEquals(apiException.message, "java.lang.RuntimeException: Runtime exception message")
        Assert.assertEquals(apiException.appErrorCode, "FAILURE")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testRestApiExceptionWithMessageException() {
        val runtimeException = RuntimeException()
        val apiException = RestApiException("message", runtimeException)

        Assert.assertEquals(apiException.cause, runtimeException)
        Assert.assertEquals(apiException.message, "message")
        Assert.assertEquals(apiException.appErrorCode, "FAILURE")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testRestApiExceptionWithMessageExceptionBooleans() {
        val runtimeException = RuntimeException()
        val apiException = RestApiException("message", runtimeException, true, true)

        Assert.assertEquals(apiException.cause, runtimeException)
        Assert.assertEquals(apiException.message, "message")
        Assert.assertEquals(apiException.appErrorCode, "FAILURE")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun testRestApiExceptionWithStatusMessage() {
        val apiException = RestApiException(HttpStatus.OK, "code", "message")

        Assert.assertEquals(apiException.appErrorCode, "code")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.OK)
        Assert.assertEquals(apiException.message, "message")
        Assert.assertEquals(apiException.cause, null)
    }

    @Test
    fun testRestApiExceptionWithStatusCodeMessage() {
        val apiException = RestApiException(HttpStatus.OK, "code", "message")

        Assert.assertEquals(apiException.appErrorCode, "code")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.OK)
        Assert.assertEquals(apiException.message, "message")
        Assert.assertEquals(apiException.cause, null)
    }

    @Test
    fun testRestApiExceptionWithStatusCause() {
        val runtimeException = RuntimeException("Runtime exception message")
        val apiException = RestApiException(HttpStatus.OK, runtimeException)

        Assert.assertEquals(apiException.appErrorCode, "FAILURE")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.OK)
        Assert.assertEquals(apiException.message, "java.lang.RuntimeException: Runtime exception message")
        Assert.assertEquals(apiException.cause, runtimeException)
    }

    @Test
    fun testRestApiExceptionWithStatusMessageCause() {
        val runtimeException = RuntimeException()
        val apiException = RestApiException(HttpStatus.OK, "code", "message", runtimeException)

        Assert.assertEquals(apiException.appErrorCode, "code")
        Assert.assertEquals(apiException.httpStatusCode, HttpStatus.OK)
        Assert.assertEquals(apiException.message, "message")
        Assert.assertEquals(apiException.cause, runtimeException)
    }

}
