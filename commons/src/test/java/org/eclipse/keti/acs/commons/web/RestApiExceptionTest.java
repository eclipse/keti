/*******************************************************************************
 * Copyright 2017 General Electric Company
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

package org.eclipse.keti.acs.commons.web;

import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings({ "javadoc", "nls" })
public class RestApiExceptionTest {

    @Test
    public void testRestApiExceptionDefaultConstructor() {
        RestApiException apiException = new RestApiException();
        Assert.assertEquals(apiException.getMessage(), null);
        Assert.assertEquals(apiException.getAppErrorCode(), "FAILURE");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testRestApiExceptionWithMessage() {
        RestApiException apiException = new RestApiException("message");
        Assert.assertEquals(apiException.getMessage(), "message");
        Assert.assertEquals(apiException.getAppErrorCode(), "FAILURE");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testRestApiExceptionWithException() {
        RuntimeException runtimeException = new RuntimeException("Runtime exception message");
        RestApiException apiException = new RestApiException(runtimeException);

        Assert.assertEquals(apiException.getCause(), runtimeException);
        Assert.assertEquals(apiException.getMessage(), "java.lang.RuntimeException: Runtime exception message");
        Assert.assertEquals(apiException.getAppErrorCode(), "FAILURE");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testRestApiExceptionWithMessageException() {
        RuntimeException runtimeException = new RuntimeException();
        RestApiException apiException = new RestApiException("message", runtimeException);

        Assert.assertEquals(apiException.getCause(), runtimeException);
        Assert.assertEquals(apiException.getMessage(), "message");
        Assert.assertEquals(apiException.getAppErrorCode(), "FAILURE");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testRestApiExceptionWithMessageExceptionBooleans() {
        RuntimeException runtimeException = new RuntimeException();
        RestApiException apiException = new RestApiException("message", runtimeException, true, true);

        Assert.assertEquals(apiException.getCause(), runtimeException);
        Assert.assertEquals(apiException.getMessage(), "message");
        Assert.assertEquals(apiException.getAppErrorCode(), "FAILURE");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testRestApiExceptionWithStatusMessage() {
        RestApiException apiException = new RestApiException(HttpStatus.OK, "code", "message");

        Assert.assertEquals(apiException.getAppErrorCode(), "code");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.OK);
        Assert.assertEquals(apiException.getMessage(), "message");
        Assert.assertEquals(apiException.getCause(), null);
    }

    @Test
    public void testRestApiExceptionWithStatusCodeMessage() {
        RestApiException apiException = new RestApiException(HttpStatus.OK, "code", "message");

        Assert.assertEquals(apiException.getAppErrorCode(), "code");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.OK);
        Assert.assertEquals(apiException.getMessage(), "message");
        Assert.assertEquals(apiException.getCause(), null);
    }

    @Test
    public void testRestApiExceptionWithStatusCause() {
        RuntimeException runtimeException = new RuntimeException("Runtime exception message");
        RestApiException apiException = new RestApiException(HttpStatus.OK, runtimeException);

        Assert.assertEquals(apiException.getAppErrorCode(), "FAILURE");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.OK);
        Assert.assertEquals(apiException.getMessage(), "java.lang.RuntimeException: Runtime exception message");
        Assert.assertEquals(apiException.getCause(), runtimeException);
    }

    @Test
    public void testRestApiExceptionWithStatusMessageCause() {
        RuntimeException runtimeException = new RuntimeException();
        RestApiException apiException = new RestApiException(HttpStatus.OK, "code", "message", runtimeException);

        Assert.assertEquals(apiException.getAppErrorCode(), "code");
        Assert.assertEquals(apiException.getHttpStatusCode(), HttpStatus.OK);
        Assert.assertEquals(apiException.getMessage(), "message");
        Assert.assertEquals(apiException.getCause(), runtimeException);
    }

}
