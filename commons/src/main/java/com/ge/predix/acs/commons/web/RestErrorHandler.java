/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/
package com.ge.predix.acs.commons.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.ge.predix.acs.commons.exception.UntrustedIssuerException;

/**
 * Common Error Handler for REST APIs.
 *
 * @author 212360328
 *
 */
@Component
public class RestErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestErrorHandler.class);

    private static void populateResponsesFromException(final RestApiException restApiException,
            final HttpServletResponse httpServletResponse, final RestApiErrorResponse restApiErrorResponse) {
        httpServletResponse.setStatus(restApiException.getHttpStatusCode().value());
        restApiErrorResponse.setErrorMessage(restApiException.getMessage());
        restApiErrorResponse.setErrorCode(restApiException.getAppErrorCode());
    }

    /**
     * Handles the given exception and generates a response with error code and message description.
     *
     * @param e
     *            Given exception
     * @param request
     *            The http request
     * @param response
     *            The http response
     * @return The model view with the error response
     */
    @SuppressWarnings("nls")
    public ModelAndView createApiErrorResponse(final Exception e, final HttpServletRequest request,
            final HttpServletResponse response) {
        LOGGER.error(e.getMessage(), e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        RestApiErrorResponse restApiErrorResponse = new RestApiErrorResponse();

        if (e instanceof RestApiException) {
            populateResponsesFromException((RestApiException) e, response, restApiErrorResponse);
        } else if (HttpMediaTypeNotAcceptableException.class.isAssignableFrom(e.getClass())) {
            populateResponsesFromException(
                    new RestApiException(HttpStatus.NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE.getReasonPhrase()),
                    response, restApiErrorResponse);
        } else if (HttpRequestMethodNotSupportedException.class.isAssignableFrom(e.getClass())) {
            populateResponsesFromException(new RestApiException(HttpStatus.METHOD_NOT_ALLOWED,
                    HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase()), response, restApiErrorResponse);
        } else if (IllegalArgumentException.class.isAssignableFrom(e.getClass())) {
            // Illegal argument exceptions mapped to 400 errors by default
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            restApiErrorResponse.setErrorMessage(e.getMessage());
        } else if (UntrustedIssuerException.class.isAssignableFrom(e.getClass())
                || SecurityException.class.isAssignableFrom(e.getClass())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            restApiErrorResponse.setErrorMessage(e.getMessage());
        } else if (HttpMessageNotReadableException.class.isAssignableFrom(e.getClass())) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            restApiErrorResponse.setErrorMessage("Malformed JSON syntax. " + e.getLocalizedMessage());
        }

        return new ModelAndView(new MappingJackson2JsonView(), "ErrorDetails", restApiErrorResponse);
    }
}
