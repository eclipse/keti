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

package com.ge.predix.acs.commons.web;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Convenient ResponseEntity Builder methods to minimize boiler plate code.
 *
 * @author acs-engineers@ge.com
 */
public final class ResponseEntityBuilder {

    private ResponseEntityBuilder() {
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 201 with no location.
     *
     * @return The corresponding ResponseEntity
     */
    public static <T> ResponseEntity<T> created() {
        return created(null, false);
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 201 with a given location.
     *
     * @param location The location of the created resource
     * @return The corresponding ResponseEntity
     */
    public static <T> ResponseEntity<T> created(final String location) {
        // Resource creation by default return 201 "created"
        return created(location, false);
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 201/204 with a given location.
     *
     * @param location  The location of the created resource
     * @param noContent false means updated resource which returns 204, true means created resource which returns 201
     * @return The corresponding ResponseEntity
     */
    public static <T> ResponseEntity<T> created(final String location, final boolean noContent) {
        HttpStatus status = noContent ? HttpStatus.NO_CONTENT : HttpStatus.CREATED;

        if (location != null) {

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(location));
            return new ResponseEntity<>(headers, status);
        }
        return new ResponseEntity<>(status);
    }

    public static <T> ResponseEntity<T> created(final boolean noContent, final String uriTemplate,
            final String... keyValues) {
        URI resourceUri = UriTemplateUtils.expand(uriTemplate, keyValues);
        return created(resourceUri.getPath(), noContent);
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 200 with no response payload.
     *
     * @return The corresponding ResponseEntity
     */
    public static <T> ResponseEntity<T> ok() {
        return ok(null);
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 200 with a given response payload.
     *
     * @param response The response payload
     * @return The corresponding ResponseEntity
     */
    public static <T> ResponseEntity<T> ok(final T response) {
        if (response != null) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 204 with no response payload.
     *
     * @return The corresponding ResponseEntity
     */
    public static ResponseEntity<Void> noContent() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Creates a typed ResponseEntity with HTTP status code 200 with no response payload.
     *
     * @return The corresponding ResponseEntity
     */
    public static <T> ResponseEntity<T> notFound() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Response entity usually to reflect semantically invalid input data. Creates a typed ResponseEntity with HTTP
     * status code 422 with no response payload.
     *
     * @return The corresponding ResponseEntity
     */
    public static ResponseEntity<Void> unprocessable() {
        return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
