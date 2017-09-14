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

package com.ge.predix.acs.config.tokenservice;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.ge.predix.uaa.token.lib.DefaultZoneConfiguration;
import com.ge.predix.uaa.token.lib.FastTokenServices;
import com.ge.predix.uaa.token.lib.ZoneAwareFastTokenService;

@Configuration
@Profile({ "public" })
public class ZoneAwareTokenServiceConfig {
    @Bean
    public FastTokenServices defaultFastTokenService(
            @Value("${TRUSTED_ISSUER_ID:${ACS_UAA_URL}/oauth/token}") final String trustedIssuer,
            @Value("${UAA_USE_HTTPS:false}") final boolean useHttps) {
        FastTokenServices fastTokenService = new FastTokenServices();
        fastTokenService.setStoreClaims(true);
        fastTokenService.setTrustedIssuers(Collections.singletonList(trustedIssuer));
        fastTokenService.setUseHttps(useHttps);
        return fastTokenService;
    }

    @Bean
    public DefaultZoneConfiguration defaultZoneConfig(
            @Value("${TRUSTED_ISSUER_ID:${ACS_UAA_URL}/oauth/token}") final String trustedIssuer) {
        DefaultZoneConfiguration defaultZoneConfig = new DefaultZoneConfiguration();
        defaultZoneConfig.setTrustedIssuerId(trustedIssuer);
        defaultZoneConfig.setAllowedUriPatterns(
                Arrays.asList("/v1/zone/**", "/health*", "/monitoring/heartbeat*"));
        return defaultZoneConfig;
    }

    @Bean
    public ZoneAwareFastTokenService tokenService(@Value("${ACS_SERVICE_ID:predix-acs}") final String acsServiceId,
            @Value("${ACS_BASE_DOMAIN:localhost}") final String acsBaseDomain,
            final FastTokenServices defaultFastTokenService, final DefaultZoneConfiguration defaultZoneConfig) {
        ZoneAwareFastTokenService tokenService = new ZoneAwareFastTokenService();
        tokenService.setDefaultFastTokenService(defaultFastTokenService);
        tokenService.setDefaultZoneConfig(defaultZoneConfig);
        tokenService.setServiceBaseDomain(acsBaseDomain);
        tokenService.setServiceId(acsServiceId);
        tokenService.setServiceZoneHeaders("Predix-Zone-Id,ACS-Zone-Subdomain");
        tokenService.setUseSubdomainsForZones(false);
        return tokenService;
    }
}
