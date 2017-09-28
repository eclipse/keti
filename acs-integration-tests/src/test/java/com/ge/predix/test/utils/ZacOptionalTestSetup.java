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

package com.ge.predix.test.utils;

import static com.ge.predix.test.utils.ACSTestUtil.isServerListening;

import java.net.URI;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.testng.SkipException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ZacOptionalTestSetup implements OptionalTestSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZacOptionalTestSetup.class);

    @Autowired(required = false)
    private OAuth2RestTemplate zacRestTemplate;

    @Value("${ZAC_URL}")
    private String zacUrl;

    @Value("${ACS_SERVICE_ID:predix-acs}")
    private String serviceId;

    public static final String PREDIX_ZONE_ID = "Predix-Zone-Id";

    private ObjectMapper objectMapper = new ObjectMapper();

    public void setup(final String zoneId, final Map<String, Object> trustedIssuers) {
        assumeZacServerAvailable();
        try {
            ResponseEntity<String> response = registerServiceToZac(zoneId, trustedIssuers);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(String.format("Failed to register '%s' zone with ZAC.", zoneId));
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Can't process Json ", e);
        }
    }

    public void tearDown(final String zoneId) {
        deleteServiceFromZac(zoneId);

    }

    private ResponseEntity<String> registerServiceToZac(final String zoneId, final Map<String, Object> trustedIssuers)
            throws JsonProcessingException {
        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(this.PREDIX_ZONE_ID, zoneId);

        HttpEntity<String> requestEntity = new HttpEntity<>(this.objectMapper.writeValueAsString(trustedIssuers),
                headers);
        return this.zacRestTemplate
                .exchange(this.zacUrl + "/v1/registration/" + this.serviceId + "/" + zoneId, HttpMethod.PUT,
                        requestEntity, String.class);
    }

    private ResponseEntity<String> deleteServiceFromZac(final String zoneId) {
        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(this.PREDIX_ZONE_ID, zoneId);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        return this.zacRestTemplate
                .exchange(this.zacUrl + "/v1/registration/" + this.serviceId + "/" + zoneId, HttpMethod.DELETE,
                        requestEntity, String.class);
    }

    private void assumeZacServerAvailable() {
        // Not all tests use Spring so try to get the URL from the environment.
        if (StringUtils.isEmpty(this.zacUrl)) {
            this.zacUrl = System.getenv("ZAC_URL");
        }
        if (!isServerListening(URI.create(this.zacUrl))) {
            throw new SkipException("Skipping tests because ZAC is not available.");
        }
    }

}
