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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RestApiExceptionHandler extends BaseRestApiControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiExceptionHandler.class);

    public RestApiExceptionHandler() {
        super(LOGGER);
    }

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<RestApiErrorResponse> handleException(final RestApiException e) {
        LOGGER.error(e.getMessage(), e);
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();
        restApiErrorResponse.getErrorDetails().setErrorMessage(e.getMessage());
        restApiErrorResponse.getErrorDetails().setErrorCode(e.getAppErrorCode());
        return ResponseEntity.status(e.getHttpStatusCode().value()).contentType(MediaType.APPLICATION_JSON)
                .body(restApiErrorResponse);
    }

}
