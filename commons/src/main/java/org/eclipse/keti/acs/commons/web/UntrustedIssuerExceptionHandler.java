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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.eclipse.keti.acs.commons.exception.UntrustedIssuerException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UntrustedIssuerExceptionHandler extends BaseRestApiControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(UntrustedIssuerExceptionHandler.class);

    public UntrustedIssuerExceptionHandler() {
        super(LOGGER);
    }

    @ExceptionHandler(UntrustedIssuerException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public RestApiErrorResponse handleException(final UntrustedIssuerException e) {
        return super.handleException(e,  e.getMessage());
    }

}
