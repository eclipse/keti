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

package com.ge.predix.test.utils;

import static com.ge.predix.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ge.predix.acs.rest.Zone;

@Component
@Profile({ "public", "public-titan" })
public class ZoneFactory {

    @Value("${ACS_URL}")
    private String acsBaseUrl;

    @Value("${CF_BASE_DOMAIN:localhost}")
    private String cfBaseDomain;

    public static final String ACS_ZONE_API_PATH = ACS_VERSION + "/zone/";

    /**
     * Creates a random zone name. Having a random zone name avoids test collisions when executing in parallel
     * 
     * @param clazz
     *            a classname to pass
     * @return String randomize name of the zone
     */
    static String getRandomName(final String clazz) {
        return clazz + UUID.randomUUID().toString();
    }

    /**
     * Creates desired Zone. Makes a call to the zone API and creates a zone in order to execute your set of test
     * against it.
     * 
     * @param restTemplate
     * @param zoneId
     * @param trustedIssuerIds
     * @return Zone
     * @throws IOException
     */
    public Zone createTestZone(final RestTemplate restTemplate, final String zoneId,
            final List<String> trustedIssuerIds) throws IOException {
        Zone zone = new Zone(zoneId, zoneId, "Zone for integration testing.");
        restTemplate.put(this.acsBaseUrl + ACS_ZONE_API_PATH + zoneId, zone);
        return zone;
    }

    /**
     * Deletes desired Zone. Makes a client call that deletes the desired zone. This method should be use after the
     * set of tests for that zone are finished.
     * 
     * @param restTemplate
     * @param zoneName
     * @return HttpStatus
     */
    public HttpStatus deleteZone(final RestTemplate restTemplate, final String zoneName) {
        try {
            restTemplate.delete(this.acsBaseUrl + ACS_ZONE_API_PATH + zoneName);
            return HttpStatus.NO_CONTENT;
        } catch (HttpClientErrorException httpException) {
            return httpException.getStatusCode();
        } catch (RestClientException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
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
        zoneurl = uri.getScheme() + "://" + zoneId + '.' + getServiceDomain();
        if (uri.getPort() != -1) {
            zoneurl += ':' + uri.getPort();
        }
        return zoneurl;
    }

    public String getServiceDomain() {
        return this.cfBaseDomain;
    }

}
