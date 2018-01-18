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

package org.eclipse.keti.acs.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.net.HttpHeaders;

public abstract class AbstractHttpMethodsFilter extends OncePerRequestFilter {

    private final Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods;

    private static final Logger LOGGER_INSTANCE = LoggerFactory.getLogger(AbstractHttpMethodsFilter.class);
    private static final Set<MimeType> ACCEPTABLE_MIME_TYPES =
            new HashSet<>(Arrays.asList(MimeTypeUtils.ALL, MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.TEXT_PLAIN));

    public AbstractHttpMethodsFilter(final Map<String, Set<HttpMethod>> uriPatternsAndAllowedHttpMethods) {
        this.uriPatternsAndAllowedHttpMethods = Collections.unmodifiableMap(uriPatternsAndAllowedHttpMethods);
    }

    private static void addCommonResponseHeaders(final HttpServletResponse response) {
        if (!response.containsHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS)) {
            response.addHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");
        }
    }

    private static void sendMethodNotAllowedError(final HttpServletResponse response) throws IOException {
        addCommonResponseHeaders(response);
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
    }

    private static void sendNotAcceptableError(final HttpServletResponse response) throws IOException {
        addCommonResponseHeaders(response);
        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE.getReasonPhrase());
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        String requestMethod = request.getMethod();

        if (HttpMethod.TRACE.matches(requestMethod)) {
            sendMethodNotAllowedError(response);
            return;
        }

        String requestUri = request.getRequestURI();

        if (!HttpMethod.OPTIONS.matches(requestMethod)) {
            for (Map.Entry<String,
                    Set<HttpMethod>> uriPatternsAndAllowedHttpMethodsEntry : this.uriPatternsAndAllowedHttpMethods
                            .entrySet()) {
                if (Pattern.compile(uriPatternsAndAllowedHttpMethodsEntry.getKey()).matcher(requestUri).matches()) {
                    if (!uriPatternsAndAllowedHttpMethodsEntry.getValue().contains(HttpMethod.resolve(requestMethod))) {
                        sendMethodNotAllowedError(response);
                        return;
                    }

                    String acceptHeaderValue = request.getHeader(HttpHeaders.ACCEPT);
                    if (acceptHeaderValue != null) {
                        try {
                            List<MimeType> parsedMimeTypes = MimeTypeUtils.parseMimeTypes(acceptHeaderValue);
                            boolean foundAcceptableMimeType = false;
                            for (MimeType parsedMimeType : parsedMimeTypes) {
                                // When checking for acceptable MIME types, strip out the character set
                                if (ACCEPTABLE_MIME_TYPES.contains(
                                        new MimeType(parsedMimeType.getType(), parsedMimeType.getSubtype()))) {
                                    foundAcceptableMimeType = true;
                                    break;
                                }
                            }
                            if (!foundAcceptableMimeType) {
                                LOGGER_INSTANCE.error("Malformed Accept header sent in request: {}", acceptHeaderValue);
                                sendNotAcceptableError(response);
                                return;
                            }
                        } catch (Exception e) {
                            sendNotAcceptableError(response);
                            return;
                        }
                    }

                    break;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
