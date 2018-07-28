/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs

import org.eclipse.keti.acs.service.InvalidACSRequestException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.stereotype.Component

/**
 * Retrieve the authentication context.
 *
 * @author acs-engineers@ge.com
 */
@Component
class SpringSecurityPolicyContextResolver : PolicyContextResolver {

    override val issuerIdOrFail: String
        get() {
            var issuer: String? = null
            val oAuth2Request = authentication.oAuth2Request

            if (oAuth2Request != null) {
                val requestParameters = oAuth2Request.requestParameters

                if (requestParameters != null && requestParameters.containsKey("iss")) {
                    issuer = requestParameters["iss"]
                }
            }

            if (issuer == null) {
                throw InvalidACSRequestException("Authetication issuer cannot be null")
            }

            return issuer
        }

    override val clientIdOrFail: String
        get() {
            var clientId: String? = null

            val oAuth2Request = authentication.oAuth2Request
            if (oAuth2Request != null) {
                clientId = oAuth2Request.clientId
            }
            if (clientId == null) {
                throw InvalidACSRequestException("Authetication clientId cannot be null")
            }

            return clientId
        }

    private // postcondition check that the oAuth2authentication should never be
    // null
    val authentication: OAuth2Authentication
        get() = SecurityContextHolder.getContext().authentication as OAuth2Authentication
}
