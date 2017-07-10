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

import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings({ "javadoc", "nls" })
public class BaseRestApiControllerAdviceTest {

    @Test
    public void testBaseRestApiControllerAdvice() {

        RestErrorHandler errorHandler = Mockito.mock(RestErrorHandler.class);

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        Exception e = new Exception();

        ModelAndView errorResponse = new ModelAndView();
        errorResponse.setViewName("ErrorDetails");

        Mockito.when(errorHandler.createApiErrorResponse(e, request, response)).thenReturn(errorResponse);

        BaseRestApiControllerAdvice baseRestApiControllerAdvice = new BaseRestApiControllerAdvice();
        baseRestApiControllerAdvice.setRestErrorHandler(errorHandler);

        ModelAndView actualErrorResponse = baseRestApiControllerAdvice.handleException(e, request, response);

        Assert.assertNotNull(actualErrorResponse);
        Assert.assertEquals(actualErrorResponse.getViewName(), "ErrorDetails");

        Mockito.verify(errorHandler, Mockito.times(1)).createApiErrorResponse(e, request, response);
    }
}
