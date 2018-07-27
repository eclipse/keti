/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.security

import com.google.common.net.HttpHeaders
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.regex.Pattern
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val LOGGER_INSTANCE = LoggerFactory.getLogger(AbstractHttpMethodsFilter::class.java)
private val ACCEPTABLE_MIME_TYPES =
    HashSet(Arrays.asList(MimeTypeUtils.ALL, MimeTypeUtils.APPLICATION_JSON, MimeTypeUtils.TEXT_PLAIN))

private fun addCommonResponseHeaders(response: HttpServletResponse) {
    if (!response.containsHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS)) {
        response.addHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
    }
}

@Throws(IOException::class)
private fun sendMethodNotAllowedError(response: HttpServletResponse) {
    addCommonResponseHeaders(response)
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.reasonPhrase)
}

@Throws(IOException::class)
private fun sendNotAcceptableError(response: HttpServletResponse) {
    addCommonResponseHeaders(response)
    response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE.reasonPhrase)
}

abstract class AbstractHttpMethodsFilter(uriPatternsAndAllowedHttpMethods: Map<String, Set<HttpMethod>>) :
    OncePerRequestFilter() {

    private val uriPatternsAndAllowedHttpMethods: Map<String, Set<HttpMethod>> =
        Collections.unmodifiableMap(uriPatternsAndAllowedHttpMethods)

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val requestMethod = request.method

        if (HttpMethod.TRACE.matches(requestMethod)) {
            sendMethodNotAllowedError(response)
            return
        }

        val requestUri = request.requestURI

        if (!HttpMethod.OPTIONS.matches(requestMethod)) {
            for ((key, value) in this.uriPatternsAndAllowedHttpMethods) {
                if (Pattern.compile(key).matcher(requestUri).matches()) {
                    if (!value.contains(HttpMethod.resolve(requestMethod))) {
                        sendMethodNotAllowedError(response)
                        return
                    }

                    val acceptHeaderValue = request.getHeader(HttpHeaders.ACCEPT)
                    if (acceptHeaderValue != null) {
                        try {
                            val parsedMimeTypes = MimeTypeUtils.parseMimeTypes(acceptHeaderValue)
                            var foundAcceptableMimeType = false
                            for (parsedMimeType in parsedMimeTypes) {
                                // When checking for acceptable MIME types, strip out the character set
                                if (ACCEPTABLE_MIME_TYPES.contains(
                                        MimeType(parsedMimeType.type, parsedMimeType.subtype)
                                    )
                                ) {
                                    foundAcceptableMimeType = true
                                    break
                                }
                            }
                            if (!foundAcceptableMimeType) {
                                LOGGER_INSTANCE.error("Malformed Accept header sent in request: {}", acceptHeaderValue)
                                sendNotAcceptableError(response)
                                return
                            }
                        } catch (e: Exception) {
                            sendNotAcceptableError(response)
                            return
                        }
                    }

                    break
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
