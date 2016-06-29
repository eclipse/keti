package com.ge.predix.acs.request.context;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AcsRequestEnrichingFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        try {
            try {
                AcsRequestContextHolder.initialize();
                this.logger.trace("Initialized the Acs Request Context");

            } catch (RuntimeException e) { // Ensure that the filter chain is not aborted.
                this.logger.error(
                        "AcsRequestContext Initialization failed:Let the request go on irrespective." + e.getMessage());
            }
            filterChain.doFilter(request, response);
        } finally {
            // Very Critical...to recycle the Thread to the pool safely
            AcsRequestContextHolder.clear();
            this.logger.trace("Cleared the Acs Request Context");
        }
    }
}
