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

import org.springframework.http.HttpStatus;

/**
 * Controllers implementing the restful api of the acs, should throw this kind of exception which is handled by the
 * error handler to generate a json error response payload.
 *
 * @author 212360328
 */
@SuppressWarnings({ "javadoc", "nls" })
public class RestApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String FAILURE = "FAILURE";

    private final HttpStatus httpStatusCode;
    private final String appErrorCode;

    public RestApiException() {
        super();
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final String message) {
        super(message);
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final Throwable cause) {
        super(cause);
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final String message, final Throwable cause) {
        super(message, cause);
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final HttpStatus httpStatusCode, final String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final HttpStatus httpStatusCode, final String appErrorCode, final String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.appErrorCode = appErrorCode;
    }

    public RestApiException(final HttpStatus httpStatusCode, final Throwable cause) {
        super(cause);
        this.httpStatusCode = httpStatusCode;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final HttpStatus httpStatusCode, final String message, final Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.appErrorCode = FAILURE;
    }

    public RestApiException(final HttpStatus httpStatusCode, final String appErrorCode, final String message,
            final Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.appErrorCode = appErrorCode;
    }

    public HttpStatus getHttpStatusCode() {
        return this.httpStatusCode;
    }

    /**
     * @return the appErrorCode
     */
    public String getAppErrorCode() {
        return this.appErrorCode;
    }

}
