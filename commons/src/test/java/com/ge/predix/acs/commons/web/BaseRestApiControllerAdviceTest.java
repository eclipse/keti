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
 *******************************************************************************/

package com.ge.predix.acs.commons.web;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.exception.UntrustedIssuerException;

@SuppressWarnings({ "javadoc", "nls" })
public class BaseRestApiControllerAdviceTest {

    @Test
    public void testBaseRestApiControllerAdviceException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();
        RestApiException e = new RestApiException("Internal server error");

        ResponseEntity<RestApiErrorResponse> actualErrorResponse = baseRestApiControllerAdvice
                .handleRestApiException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getStatusCode().toString().equals("500"));
        Assert.assertTrue(actualErrorResponse.getBody().getErrorDetails().getErrorMessage()
                .equals("Internal server error"));
    }

    @Test
    public void testBaseRestApiControllerAdviceHttpMediaTypeNotAcceptableException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();

        HttpMediaTypeNotAcceptableException e = new HttpMediaTypeNotAcceptableException("Media Type Not Supported");

        RestApiErrorResponse actualErrorResponse = baseRestApiControllerAdvice
                .handleHttpMediaTypeNotAcceptableException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Not Acceptable"));
    }

    @Test
    public void testBaseRestApiControllerAdviceHttpRequestMethodNotSupportedException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();

        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("GET");

        RestApiErrorResponse actualErrorResponse = baseRestApiControllerAdvice
                .handleHttpRequestMethodNotSupportedException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Method Not Allowed"));
    }

    @Test
    public void testBaseRestApiControllerAdviceIllegalArgumentException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();

        IllegalArgumentException e = new IllegalArgumentException("Arguments passed are invalid");

        RestApiErrorResponse actualErrorResponse = baseRestApiControllerAdvice.handleIllegalArgumentException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(
                actualErrorResponse.getErrorDetails().getErrorMessage().equals("Arguments passed are invalid"));
    }

    @Test
    public void testBaseRestApiControllerAdviceUntrustedIssuerException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();

        UntrustedIssuerException e = new UntrustedIssuerException("Not a trusted Issuer");

        RestApiErrorResponse actualErrorResponse = baseRestApiControllerAdvice
                .handleUntrustedIssuerOrSecurityException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Not a trusted Issuer"));

    }

    @Test
    public void testBaseRestApiControllerAdviceHttpMessageNotReadableException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("{JSON}");

        RestApiErrorResponse actualErrorResponse = baseRestApiControllerAdvice.handleHttpMessageNotReadableException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorCode().equals("FAILED"));
        Assert.assertTrue(
                actualErrorResponse.getErrorDetails().getErrorMessage().equals("Malformed JSON syntax. {JSON}"));
    }

    @Test
    public void testBaseRestApiControllerAdviceHttpMediaTypeNotSupportedException() {

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();

        HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException("JSON");

        RestApiErrorResponse actualErrorResponse = baseRestApiControllerAdvice
                .handleHttpMediaTypeNotSupportedException(e);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertTrue(actualErrorResponse.getErrorDetails().getErrorMessage().equals("Unsupported Media Type"));
    }
}
