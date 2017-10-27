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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ge.predix.acs.commons.exception.UntrustedIssuerException;

@ControllerAdvice
public class BaseRestApiControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseRestApiControllerAdvice.class);

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<RestApiErrorResponse> handleRestApiException(final RestApiException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(e.getMessage());
        restApiErrorResponse.getErrorDetails().setErrorCode(e.getAppErrorCode());
        return ResponseEntity.status(e.getHttpStatusCode().value()).contentType(MediaType.APPLICATION_JSON)
                .body(restApiErrorResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    public RestApiErrorResponse handleHttpMediaTypeNotAcceptableException(final HttpMediaTypeNotAcceptableException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase());
        return restApiErrorResponse;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public RestApiErrorResponse handleHttpRequestMethodNotSupportedException(
            final HttpRequestMethodNotSupportedException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
        return restApiErrorResponse;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestApiErrorResponse handleIllegalArgumentException(final IllegalArgumentException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(e.getMessage());
        return restApiErrorResponse;
    }

    @ExceptionHandler({ UntrustedIssuerException.class, SecurityException.class })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public RestApiErrorResponse handleUntrustedIssuerOrSecurityException(final Exception e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(e.getMessage());
        return restApiErrorResponse;

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestApiErrorResponse handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage("Malformed JSON syntax. " + e.getLocalizedMessage());
        return restApiErrorResponse;

    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ResponseBody
    public RestApiErrorResponse handleHttpMediaTypeNotSupportedException(final HttpMediaTypeNotSupportedException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
        return restApiErrorResponse;

    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public RestApiErrorResponse handleException(final Exception e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        return restApiErrorResponse;
    }

}
