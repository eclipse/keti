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

package org.eclipse.keti.acs.config.tokenservice

import com.ge.predix.uaa.token.lib.DefaultZoneConfiguration
import com.ge.predix.uaa.token.lib.FastTokenServices
import com.ge.predix.uaa.token.lib.ZoneAwareFastTokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.Arrays

@Configuration
@Profile("public")
class ZoneAwareTokenServiceConfig {

    @Bean
    fun defaultFastTokenService(
        @Value("\${TRUSTED_ISSUER_ID:\${ACS_UAA_URL}/oauth/token}")
        trustedIssuer: String,
        @Value("\${UAA_USE_HTTPS:false}")
        useHttps: Boolean
    ): FastTokenServices {
        val fastTokenService = FastTokenServices()
        fastTokenService.isStoreClaims = true
        fastTokenService.setTrustedIssuers(listOf(trustedIssuer))
        fastTokenService.setUseHttps(useHttps)
        return fastTokenService
    }

    @Bean
    fun defaultZoneConfig(
        @Value("\${TRUSTED_ISSUER_ID:\${ACS_UAA_URL}/oauth/token}")
        trustedIssuer: String
    ): DefaultZoneConfiguration {
        val defaultZoneConfig = DefaultZoneConfiguration()
        defaultZoneConfig.trustedIssuerId = trustedIssuer
        defaultZoneConfig.allowedUriPatterns = Arrays.asList("/v1/zone/**", "/health*", "/monitoring/heartbeat*")
        return defaultZoneConfig
    }

    @Bean
    fun tokenService(
        @Value("\${ACS_SERVICE_ID:predix-acs}")
        acsServiceId: String,
        @Value("\${ACS_BASE_DOMAIN:localhost}")
        acsBaseDomain: String,
        defaultFastTokenService: FastTokenServices,
        defaultZoneConfig: DefaultZoneConfiguration
    ): ZoneAwareFastTokenService {
        val tokenService = ZoneAwareFastTokenService()
        tokenService.defaultFastTokenService = defaultFastTokenService
        tokenService.setDefaultZoneConfig(defaultZoneConfig)
        tokenService.setServiceBaseDomain(acsBaseDomain)
        tokenService.serviceId = acsServiceId
        tokenService.setServiceZoneHeaders("Predix-Zone-Id,ACS-Zone-Subdomain")
        tokenService.isUseSubdomainsForZones = false
        return tokenService
    }
}
