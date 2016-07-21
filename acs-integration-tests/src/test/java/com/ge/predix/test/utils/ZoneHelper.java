/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/
package com.ge.predix.test.utils;

import static com.ge.predix.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.rest.Zone;

@Component
public class ZoneHelper {

    @Value("${acs.zone.header.name}")
    private String acsZoneHeaderName;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneHelper.class);

    public static final String ACS_ZONE_API_PATH = ACS_VERSION + "/zone/";

    @Value("${CF_BASE_DOMAIN:localhost}")
    private String cfBaseDomain;

    @Value("${ACS_CF_DOMAIN:}")
    private String acsCFDomain;

    @Value("${acsUrl}")
    private String acsBaseUrl;

    @Value("${ZONE1_NAME:testzone1}")
    private String zone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String zone2Name;

    @Value("${zone1UaaUrl}/oauth/token")
    private String primaryZoneIssuerId;

    @Value("${zone1UaaUrl}/check_token")
    private String uaaCheckTokenURL;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Value("${ZAC_URL:http://localhost:8888}")
    private String zacUrl;

    @Value("${ACS_SERVICE_ID:predix-acs}")
    private String service_id;

    @Autowired
    private OAuth2RestTemplate zacRestTemplate;

    public String getZone1Url() {
        return getZoneSpecificUrl(this.zone1Name);
    }

    public String getAcsBaseURL() {
        return this.acsBaseUrl;
    }

    public String getZoneSpecificUrl(final String zoneId) {
        URI uri = null;
        String zoneurl = null;
        try {
            uri = new URI(this.acsBaseUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        // if running locally, acsDomain is not needed in uri
        if (this.acsCFDomain == null || this.acsCFDomain.isEmpty()) {
            zoneurl = uri.getScheme() + "://" + zoneId + "." + this.cfBaseDomain;
        } else {
            zoneurl = uri.getScheme() + "://" + zoneId + "." + this.acsCFDomain + "." + this.cfBaseDomain;
        }
        if (uri.getPort() != -1) {
            zoneurl += ":" + uri.getPort();
        }
        return zoneurl;
    }

    public Zone createPrimaryTestZone() throws JsonParseException, JsonMappingException, IOException {
        return createTestZone(this.acsRestTemplateFactory.getACSTemplateWithPolicyScope(), this.zone1Name, true,
                getPrimaryZoneIssuer());
    }

    public Zone createTestZone2() throws JsonParseException, JsonMappingException, IOException {
        return createTestZone(this.acsRestTemplateFactory.getACSTemplateWithPolicyScope(), this.zone2Name, true,
                getPrimaryZoneIssuer());
    }

    public Zone createTestZone(final RestTemplate restTemplate, final String zoneId, final boolean registerWithZac)
            throws JsonParseException, JsonMappingException, IOException {
        Map<String, Object> trustedIssuers = null;
        if (registerWithZac) trustedIssuers = getPrimaryZoneIssuer();
        return createTestZone(restTemplate, zoneId, registerWithZac, trustedIssuers);
    }

    public Zone createTestZone(final RestTemplate restTemplate, final String zoneId, final boolean registerWithZac,
            final Map<String, Object> trustedIssuers) throws JsonParseException, JsonMappingException, IOException {
        Zone zone = new Zone(zoneId, zoneId, "Zone for integration testing.");
        if (registerWithZac) {
            ResponseEntity<String> response = registerServicetoZac(zoneId, trustedIssuers);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(String.format("Failed to register '%s' zone with ZAC.", zoneId));
            }
        }

        restTemplate.put(this.acsBaseUrl + ACS_ZONE_API_PATH + zoneId, zone);
        return zone;
    }

    public Map<String, Object> getPrimaryZoneIssuer() throws IOException, JsonParseException, JsonMappingException {
        Map<String, Object> trustedIssuers = new HashMap<>();
        trustedIssuers.put("trustedIssuerIds", Arrays.asList(this.primaryZoneIssuerId));
        return trustedIssuers;
    }

    public ResponseEntity<String> registerServicetoZac(final String zoneId, final Map<String, Object> trustedIssuers)
            throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(this.acsZoneHeaderName, zoneId);

        HttpEntity<String> requestEntity = new HttpEntity<>(new ObjectMapper().writeValueAsString(trustedIssuers),
                headers);

        ResponseEntity<String> response = this.zacRestTemplate.exchange(
                this.zacUrl + "/v1/registration/" + this.service_id + "/" + zoneId, HttpMethod.PUT, requestEntity,
                String.class);
        return response;
    }

    public ResponseEntity<String> deleteServiceFromZac(final String zoneId) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(this.acsZoneHeaderName, zoneId);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.zacRestTemplate.exchange(
                this.zacUrl + "/v1/registration/" + this.service_id + "/" + zoneId, HttpMethod.DELETE, requestEntity,
                String.class);
        return response;
    }

    // this method always appends the suffix to the original test zone
    // sub-domain to create a new zone
    // which is CI friendly
    public Zone createZone(final String zoneName, final String subdomain, final String description)
            throws JsonParseException, JsonMappingException, IOException {
        return createZone(zoneName, subdomain, description, getPrimaryZoneIssuer());
    }

    public Zone createZone(final String zoneName, final String subdomain, final String description,
            final Map<String, Object> trustedIssuers) throws JsonParseException, JsonMappingException, IOException {
        Zone zone = new Zone(zoneName, subdomain, description);
        RestTemplate acs = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        registerServicetoZac(subdomain, trustedIssuers);
        acs.put(this.acsBaseUrl + ACS_ZONE_API_PATH + zoneName, zone);
        return zone;
    }

    public Zone retrieveZone(final String zoneName) {
        try {

            RestTemplate acs = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
            ResponseEntity<Zone> responseEntity = acs.getForEntity(this.acsBaseUrl + ACS_ZONE_API_PATH + zoneName,
                    Zone.class);
            return responseEntity.getBody();

        } catch (RestClientException e) {
            LOGGER.error("Unexpected exception while retrieving a Zone with name " + zoneName, e);
            return null;
        }
    }

    public HttpStatus deleteZone(final String zoneName) {
        return deleteZone(this.acsRestTemplateFactory.getACSTemplateWithPolicyScope(), zoneName, true);
    }

    public HttpStatus deleteZone(final RestTemplate restTemplate, final String zoneName,
            final boolean registeredWithZac) {
        try {
            restTemplate.delete(this.acsBaseUrl + ACS_ZONE_API_PATH + zoneName);
            if (registeredWithZac)
                deleteServiceFromZac(zoneName);
            return HttpStatus.NO_CONTENT;
        } catch (HttpClientErrorException httpException) {
            return httpException.getStatusCode();
        } catch (JsonProcessingException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } catch (RestClientException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public String getZone1Name() {
        return this.zone1Name;
    }
    
    public String getZone2Name() {
        return this.zone2Name;
    }

    public String getZone2Url() {
        return getZoneSpecificUrl(this.zone2Name);
    }
}
