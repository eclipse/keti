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

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity
import java.net.URI

/**
 * Convenient ResponseEntity Builder methods to minimize boiler plate code.
 *
 * @author acs-engineers@ge.com
 */
/**
 * Creates a typed ResponseEntity with HTTP status code 201 with no location.
 *
 * @return The corresponding ResponseEntity
 */
fun <T> created(): ResponseEntity<T> {
    return created(null, false)
}

/**
 * Creates a typed ResponseEntity with HTTP status code 201 with a given location.
 *
 * @param location The location of the created resource
 * @return The corresponding ResponseEntity
 */
fun <T> created(location: String): ResponseEntity<T> {
    // Resource creation by default return 201 "created"
    return created(location, false)
}

/**
 * Creates a typed ResponseEntity with HTTP status code 201/204 with a given location.
 *
 * @param location  The location of the created resource
 * @param noContent false means updated resource which returns 204, true means created resource which returns 201
 * @return The corresponding ResponseEntity
 */
fun <T> created(
    location: String?,
    noContent: Boolean
): ResponseEntity<T> {
    val status = if (noContent) HttpStatus.NO_CONTENT else HttpStatus.CREATED

    if (location != null) {

        val headers = HttpHeaders()
        headers.location = URI.create(location)
        return ResponseEntity(headers, status)
    }
    return ResponseEntity(status)
}

fun <T> created(
    noContent: Boolean,
    uriTemplate: String,
    vararg keyValues: String
): ResponseEntity<T> {
    val resourceUri = expand(uriTemplate, *keyValues)
    return created(resourceUri.path, noContent)
}

/**
 * Creates a typed ResponseEntity with HTTP status code 200 with no response payload.
 *
 * @return The corresponding ResponseEntity
 */
fun <T> ok(): ResponseEntity<T> {
    return ok(null)
}

/**
 * Creates a typed ResponseEntity with HTTP status code 200 with a given response payload.
 *
 * @param response The response payload
 * @return The corresponding ResponseEntity
 */
fun <T> ok(response: T?): ResponseEntity<T> {
    return if (response != null) {
        ResponseEntity(response, OK)
    } else ResponseEntity(OK)
}

/**
 * Creates a typed ResponseEntity with HTTP status code 204 with no response payload.
 *
 * @return The corresponding ResponseEntity
 */
fun noContent(): ResponseEntity<Void> {
    return ResponseEntity(NO_CONTENT)
}

/**
 * Creates a typed ResponseEntity with HTTP status code 200 with no response payload.
 *
 * @return The corresponding ResponseEntity
 */
fun <T> notFound(): ResponseEntity<T> {
    return ResponseEntity(NOT_FOUND)
}

/**
 * Response entity usually to reflect semantically invalid input data. Creates a typed ResponseEntity with HTTP
 * status code 422 with no response payload.
 *
 * @return The corresponding ResponseEntity
 */
fun unprocessable(): ResponseEntity<Void> {
    return ResponseEntity(UNPROCESSABLE_ENTITY)
}
