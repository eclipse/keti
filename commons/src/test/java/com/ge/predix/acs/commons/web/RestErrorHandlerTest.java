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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings({ "javadoc", "nls" })
public class RestErrorHandlerTest {

    @Test
    public void testException() {
        RestErrorHandler errorHandler = new RestErrorHandler();

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        Exception e = new Exception("Descriptive Error Message");

        ModelAndView errorResponse = errorHandler.createApiErrorResponse(e, request, response);

        // The default error status code is 500
        Assert.assertEquals(response.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR.value());

        // Not null error response containing an ErrorDetails model
        Assert.assertNotNull(errorResponse);
        Assert.assertNotNull(errorResponse.getModel().get("ErrorDetails"));

        // Default response payload error code and message
        assertRestApiErrorResponse(errorResponse, "FAILED", "Operation Failed");

    }

    @Test
    public void testIllegalArgumentException() {
        RestErrorHandler errorHandler = new RestErrorHandler();

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        Exception e = new IllegalArgumentException("Descriptive Error Message");

        ModelAndView errorResponse = errorHandler.createApiErrorResponse(e, request, response);

        // The default error status code for IllegalArgumentException is 400
        Assert.assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value());

        Assert.assertNotNull(errorResponse);
        Assert.assertNotNull(errorResponse.getModel().get("ErrorDetails"));

        // Response payload with default error code and the
        // IllegalArgumentException's message
        assertRestApiErrorResponse(errorResponse, "FAILED", "Descriptive Error Message");
    }

    @Test
    public void testRestApiException() {
        RestErrorHandler errorHandler = new RestErrorHandler();

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        Exception e = new RestApiException(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "Not acceptable Error Message");

        ModelAndView errorResponse = errorHandler.createApiErrorResponse(e, request, response);

        // Custom error status code 406
        Assert.assertEquals(response.getStatus(), HttpStatus.NOT_ACCEPTABLE.value());

        Assert.assertNotNull(errorResponse);
        Assert.assertNotNull(errorResponse.getModel().get("ErrorDetails"));

        // Response payload with customer error code and message
        assertRestApiErrorResponse(errorResponse, "NOT_ACCEPTABLE", "Not acceptable Error Message");

    }

    private void assertRestApiErrorResponse(final ModelAndView errorResponse, final String code, final String message) {
        RestApiErrorResponse restApiErrorResponse = (RestApiErrorResponse) errorResponse.getModel().get("ErrorDetails");

        Assert.assertEquals(restApiErrorResponse.getErrorCode(), code);

        Assert.assertEquals(restApiErrorResponse.getErrorMessage(), message);
    }
}
