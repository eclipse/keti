package com.ge.predix.acs.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.net.HttpHeaders;

public abstract class AbstractHttpMethodsFilter extends OncePerRequestFilter {

    private final Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods;

    public AbstractHttpMethodsFilter(final Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods) {
        this.uriPatternsAndAllowedHttpMethods = Collections.unmodifiableMap(uriPatternsAndAllowedHttpMethods);
    }

    private static void addCommonResponseHeaders(final HttpServletResponse response) {
        if (!response.containsHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS)) {
            response.addHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");
        }
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        String requestMethod = request.getMethod();

        if (HttpMethod.TRACE.matches(requestMethod)) {
            addCommonResponseHeaders(response);
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
            return;
        }

        String requestUri = request.getRequestURI();

        if (!HttpMethod.OPTIONS.matches(requestMethod)) {
            for (Map.Entry<String,
                    Set<HttpMethod>> uriPatternsAndAllowedHttpMethodsEntry : this.uriPatternsAndAllowedHttpMethods
                            .entrySet()) {
                if (Pattern.compile(uriPatternsAndAllowedHttpMethodsEntry.getKey()).matcher(requestUri).matches()) {
                    if (!uriPatternsAndAllowedHttpMethodsEntry.getValue().contains(HttpMethod.resolve(requestMethod))) {
                        addCommonResponseHeaders(response);
                        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
                        return;
                    }

                    String acceptHeaderValue = request.getHeader(HttpHeaders.ACCEPT);
                    Pattern validAcceptHeaderRegex = Pattern.compile("\\A(\\*/\\*)|(application/json)|(text/plain)\\Z");

                    if (acceptHeaderValue != null && !validAcceptHeaderRegex.matcher(acceptHeaderValue).matches()) {
                        addCommonResponseHeaders(response);
                        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                                HttpStatus.NOT_ACCEPTABLE.getReasonPhrase());
                        return;
                    }

                    break;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
