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

package org.eclipse.keti.acs.commons.web

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

private val LOGGER = LoggerFactory.getLogger(RestApiExceptionHandler::class.java)

@ControllerAdvice
class RestApiExceptionHandler : BaseRestApiControllerAdvice(LOGGER) {

    @ExceptionHandler(RestApiException::class)
    fun handleException(e: RestApiException): ResponseEntity<RestApiErrorResponse> {
        LOGGER.error(e.message, e)
        val restApiErrorResponse = RestApiErrorResponse()
        restApiErrorResponse.errorDetails.errorMessage = e.message!!
        restApiErrorResponse.errorDetails.errorCode = e.appErrorCode
        return ResponseEntity.status(e.httpStatusCode.value()).contentType(MediaType.APPLICATION_JSON)
            .body(restApiErrorResponse)
    }
}
