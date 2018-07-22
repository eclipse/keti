/*******************************************************************************
 * Copyright 2017 General Electric Company
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

private const val FAILURE = "FAILURE"

/**
 * Controllers implementing the restful api of the acs, should throw this kind of exception which is handled by the
 * error handler to generate a json error response payload.
 *
 * @author acs-engineers@ge.com
 */
class RestApiException : RuntimeException {

    val httpStatusCode: HttpStatus
    /**
     * @return the appErrorCode
     */
    val appErrorCode: String

    constructor() : super() {
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        this.appErrorCode = FAILURE
    }

    constructor(message: String) : super(message) {
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        this.appErrorCode = FAILURE
    }

    constructor(cause: Throwable) : super(cause) {
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        this.appErrorCode = FAILURE
    }

    constructor(
        message: String,
        cause: Throwable
    ) : super(message, cause) {
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        this.appErrorCode = FAILURE
    }

    constructor(
        message: String,
        cause: Throwable,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace) {
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        this.appErrorCode = FAILURE
    }

    constructor(
        httpStatusCode: HttpStatus,
        message: String
    ) : super(message) {
        this.httpStatusCode = httpStatusCode
        this.appErrorCode = FAILURE
    }

    constructor(
        httpStatusCode: HttpStatus,
        appErrorCode: String,
        message: String
    ) : super(message) {
        this.httpStatusCode = httpStatusCode
        this.appErrorCode = appErrorCode
    }

    constructor(
        httpStatusCode: HttpStatus,
        cause: Throwable
    ) : super(cause) {
        this.httpStatusCode = httpStatusCode
        this.appErrorCode = FAILURE
    }

    constructor(
        httpStatusCode: HttpStatus,
        message: String,
        cause: Throwable
    ) : super(message, cause) {
        this.httpStatusCode = httpStatusCode
        this.appErrorCode = FAILURE
    }

    constructor(
        httpStatusCode: HttpStatus,
        appErrorCode: String,
        message: String,
        cause: Throwable
    ) : super(message, cause) {
        this.httpStatusCode = httpStatusCode
        this.appErrorCode = appErrorCode
    }
}
