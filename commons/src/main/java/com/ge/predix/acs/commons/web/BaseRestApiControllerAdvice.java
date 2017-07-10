package com.ge.predix.acs.commons.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class BaseRestApiControllerAdvice {

    @Autowired
    private RestErrorHandler restErrorHandler;

    /**
     * Annotated method that handles a given exception.
     *
     * @param e        The given Exception
     * @param request  The http request
     * @param response The http response
     * @return A model view containing a error response object
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(final Exception e, final HttpServletRequest request,
            final HttpServletResponse response) {

        return this.restErrorHandler.createApiErrorResponse(e, request, response);

    }

    /**
     * @param restErrorHandler the error handler
     */
    public void setRestErrorHandler(final RestErrorHandler restErrorHandler) {
        this.restErrorHandler = restErrorHandler;
    }
}
