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

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ZoneDoesNotExistExceptionHandler extends BaseRestApiControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneDoesNotExistExceptionHandler.class);

    public ZoneDoesNotExistExceptionHandler() {
        super(LOGGER);
    }

    @ExceptionHandler(ZoneDoesNotExistException.class)
    public ResponseEntity<JSONObject> handleException(final ZoneDoesNotExistException e) {
        LOGGER.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject() {{
                    put("error",
                            HttpStatus.BAD_REQUEST.getReasonPhrase());
                    put("message",
                            "Zone not found");
                }});
    }
}
