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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

import com.ge.predix.uaa.token.lib.DefaultZoneConfiguration;
import com.ge.predix.uaa.token.lib.ZacTokenService;

@Configuration
@Profile({ "predix" })
public class ZacTokenServiceConfig {
    @Bean
    public DefaultZoneConfiguration defaultZoneConfig(@Value("${ACS_DEFAULT_ISSUER_ID}") final String trustedIssuer) {
        DefaultZoneConfiguration defaultZoneConfig = new DefaultZoneConfiguration();
        defaultZoneConfig.setTrustedIssuerId(trustedIssuer);
        defaultZoneConfig.setAllowedUriPatterns(
                Arrays.asList("/v1/zone/**", "/health*", "/monitoring/heartbeat*"));
        return defaultZoneConfig;
    }

    @Bean
    OAuth2RestTemplate zacRestTemplate(
            @Value("${ZAC_UAA_TOKEN_URL:${ZAC_UAA_URL}/oauth/token}") final String accessTokenUri,
            @Value("${ZAC_CLIENT_ID}") final String clientId,
            @Value("${ZAC_CLIENT_SECRET}") final String clientSecret) {
        ClientCredentialsResourceDetails clientCredentials = new ClientCredentialsResourceDetails();
        clientCredentials.setAccessTokenUri(accessTokenUri);
        clientCredentials.setClientId(clientId);
        clientCredentials.setClientSecret(clientSecret);
        return new OAuth2RestTemplate(clientCredentials);
    }

    @Bean
    public ZacTokenService tokenService(@Value("${ACS_SERVICE_ID}") final String acsServiceId,
            @Value("${ACS_BASE_DOMAIN}") final String acsBaseDomain, @Value("${ZAC_URL}") final String zacUrl,
            final OAuth2RestTemplate zacRestTemplate, final DefaultZoneConfiguration defaultZoneConfig) {
        ZacTokenService tokenService = new ZacTokenService();
        tokenService.setServiceId(acsServiceId);
        tokenService.setZacUrl(zacUrl);
        tokenService.setServiceZoneHeaders("Predix-Zone-Id,ACS-Zone-Subdomain");
        tokenService.setDefaultZoneConfig(defaultZoneConfig);
        tokenService.setServiceBaseDomain(acsBaseDomain);
        tokenService.setStoreClaims(true);
        tokenService.setOauth2RestTemplate(zacRestTemplate);
        tokenService.setUseSubdomainsForZones(false);
        return tokenService;
    }
}
