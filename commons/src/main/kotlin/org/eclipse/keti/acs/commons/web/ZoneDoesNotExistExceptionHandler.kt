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

import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

private val LOGGER = LoggerFactory.getLogger(ZoneDoesNotExistExceptionHandler::class.java)

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ZoneDoesNotExistExceptionHandler : BaseRestApiControllerAdvice(LOGGER) {

    @ExceptionHandler(ZoneDoesNotExistException::class)
    fun handleException(e: ZoneDoesNotExistException): ResponseEntity<JSONObject> {
        LOGGER.error(e.message, e)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
            .body(object : JSONObject() {
                init {
                    put(
                        "error", HttpStatus.BAD_REQUEST.reasonPhrase
                    )
                    put(
                        "message", "Zone not found"
                    )
                }
            })
    }
}
