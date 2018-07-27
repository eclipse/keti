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

package org.eclipse.keti.acs.request.context

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val LOGGER = LoggerFactory.getLogger(AcsRequestEnrichingFilter::class.java)

@Component
class AcsRequestEnrichingFilter : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            try {
                initialize()
                LOGGER.trace("Initialized the Acs Request Context")
            } catch (e: RuntimeException) { // Ensure that the filter chain is not aborted.
                LOGGER.error("AcsRequestContext Initialization failed:Let the request go on irrespective.", e)
            }

            filterChain.doFilter(request, response)
        } finally {
            // Very Critical...to recycle the Thread to the pool safely
            clear()
            LOGGER.trace("Cleared the Acs Request Context")
        }
    }
}
