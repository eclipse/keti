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

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.testng.Assert;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.commons.exception.UntrustedIssuerException;

@SuppressWarnings({ "javadoc", "nls" })
public class BaseRestApiControllerAdviceTest {

    @Test
    public void testBaseRestApiControllerAdviceException() {

        RestApiExceptionHandler restApiExceptionHandler = new RestApiExceptionHandler();
        RestApiException e = new RestApiException("Internal server error");

        ResponseEntity<RestApiErrorResponse> actualErrorResponse = restApiExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getStatusCode().toString().equals("500"));
        Assert.assertTrue(
                actualErrorResponse.getBody().getErrorDetails().getErrorMessage().equals("Internal server error"));
    }

    @Test
    public void testBaseRestApiControllerAdviceHttpMediaTypeNotAcceptableException() {

        HttpMediaTypeNotAcceptableExceptionHandler httpMediaTypeNotAcceptableExceptionHandler = new
                HttpMediaTypeNotAcceptableExceptionHandler();

        HttpMediaTypeNotAcceptableException e = new HttpMediaTypeNotAcceptableException("Media Type Not Supported");

        RestApiErrorResponse actualErrorResponse = httpMediaTypeNotAcceptableExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Not Acceptable"));
    }

    @Test
    public void testBaseRestApiControllerAdviceHttpRequestMethodNotSupportedException() {

        HttpRequestMethodNotSupportedExceptionHandler httpRequestMethodNotSupportedExceptionHandler = new
                HttpRequestMethodNotSupportedExceptionHandler();

        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("GET");

        RestApiErrorResponse actualErrorResponse = httpRequestMethodNotSupportedExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Method Not Allowed"));
    }

    @Test
    public void testBaseRestApiControllerAdviceIllegalArgumentException() {

        IllegalArgumentExceptionHandler illegalArgumentExceptionHandler = new IllegalArgumentExceptionHandler();

        IllegalArgumentException e = new IllegalArgumentException("Arguments passed are invalid");

        RestApiErrorResponse actualErrorResponse = illegalArgumentExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(
                actualErrorResponse.getErrorDetails().getErrorMessage().equals("Arguments passed are invalid"));
    }

    @Test
    public void testBaseRestApiControllerAdviceUntrustedIssuerException() {

        UntrustedIssuerExceptionHandler untrustedIssuerExceptionHandler = new UntrustedIssuerExceptionHandler();

        UntrustedIssuerException e = new UntrustedIssuerException("Not a trusted Issuer");

        RestApiErrorResponse actualErrorResponse = untrustedIssuerExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Not a trusted Issuer"));

    }

    @Test
    public void testBaseRestApiControllerAdviceSecurityException() {

        SecurityExceptionHandler securityExceptionHandler = new SecurityExceptionHandler();

        SecurityException e = new SecurityException("Not a trusted Issuer");

        RestApiErrorResponse actualErrorResponse = securityExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Not a trusted Issuer"));

    }

    @Test
    public void testBaseRestApiControllerAdviceHttpMessageNotReadableException() {

        HttpMessageNotReadableExceptionHandler httpMessageNotReadableExceptionHandler = new
                HttpMessageNotReadableExceptionHandler();
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("{JSON}");

        RestApiErrorResponse actualErrorResponse = httpMessageNotReadableExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorCode().equals("FAILED"));
        Assert.assertTrue(
                actualErrorResponse.getErrorDetails().getErrorMessage().equals("Malformed JSON syntax. {JSON}"));
    }

    @Test
    public void testBaseRestApiControllerAdviceHttpMediaTypeNotSupportedException() {

        HttpMediaTypeNotSupportedExceptionHandler httpMediaTypeNotSupportedExceptionHandler = new
                HttpMediaTypeNotSupportedExceptionHandler();

        HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException("JSON");

        RestApiErrorResponse actualErrorResponse = httpMediaTypeNotSupportedExceptionHandler.handleException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Unsupported Media Type"));
    }
}
