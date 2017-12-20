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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.rest.Zone;

@Component
@Scope("prototype")
public class ACSITSetUpFactoryPublic implements ACSITSetUpFactory {

    private String acsZone1Name;
    private String acsZone2Name;
    private String acsZone3Name;

    @Value("${ACS_TESTING_UAA}")
    private String uaaUrl;

    @Value("${UAA_ADMIN_SECRET:adminsecret}")
    private String uaaAdminSecret;

    @Value("${ACS_SERVICE_ID:predix-acs}")
    private String serviceId;

    private static final String OAUTH_ENDPOINT = "/oauth/token";

    private String acsUrl;
    private HttpHeaders zone1Headers;
    private HttpHeaders zone3Headers;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private OAuth2RestTemplate acsZonesAdminRestTemplate;
    private OAuth2RestTemplate acsZone1RestTemplate;
    private OAuth2RestTemplate acsZone2RestTemplate;
    private OAuth2RestTemplate acsReadOnlyRestTemplate;
    private OAuth2RestTemplate acsNoPolicyScopeRestTemplate;
    private Zone zone1;
    private Zone zone2;

    private UAAACSClientsUtil uaaTestUtil;

    @Autowired
    private ZoneFactory zoneFactory;

    @Override
    public void setUp() throws IOException {
        // TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.
        this.acsUrl = this.zoneFactory.getAcsBaseURL();

        this.acsZone1Name = ZoneFactory.getRandomName(this.getClass().getSimpleName());
        this.acsZone2Name = ZoneFactory.getRandomName(this.getClass().getSimpleName());
        this.acsZone3Name = ZoneFactory.getRandomName(this.getClass().getSimpleName());

        this.zone1Headers = ACSTestUtil.httpHeaders();
        this.zone1Headers.set(PolicyHelper.PREDIX_ZONE_ID, this.acsZone1Name);

        this.zone3Headers = ACSTestUtil.httpHeaders();
        this.zone3Headers.set(PolicyHelper.PREDIX_ZONE_ID, this.acsZone3Name);

        this.uaaTestUtil = new UAAACSClientsUtil(this.uaaUrl, this.uaaAdminSecret);

        this.acsAdminRestTemplate = this.uaaTestUtil.createAcsAdminClientAndGetTemplate(this.acsZone1Name);
        this.acsZonesAdminRestTemplate = this.uaaTestUtil
                .createAcsAdminClient(Arrays.asList(this.acsZone1Name, this.acsZone2Name, this.acsZone3Name));
        this.acsReadOnlyRestTemplate = this.uaaTestUtil.createReadOnlyPolicyScopeClient(this.acsZone1Name);
        this.acsNoPolicyScopeRestTemplate = this.uaaTestUtil.createNoPolicyScopeClient(this.acsZone1Name);
        this.zone1 = this.zoneFactory.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name,
                Collections.singletonList(this.uaaUrl + OAUTH_ENDPOINT));
        this.acsZone1RestTemplate = this.uaaTestUtil.createZoneClientAndGetTemplate(this.acsZone1Name, this.serviceId);

        this.zone2 = this.zoneFactory.createTestZone(this.acsAdminRestTemplate, this.acsZone2Name,
                Collections.singletonList(this.uaaUrl + OAUTH_ENDPOINT));
        this.acsZone2RestTemplate = this.uaaTestUtil.createZoneClientAndGetTemplate(this.acsZone2Name, this.serviceId);
    }

    @Override
    public void destroy() {
        this.zoneFactory.deleteZone(this.acsAdminRestTemplate, this.acsZone1Name);
        this.zoneFactory.deleteZone(this.acsAdminRestTemplate, this.acsZone2Name);
        this.uaaTestUtil.deleteClient(this.acsAdminRestTemplate.getResource().getClientId());
        this.uaaTestUtil.deleteClient(this.acsZone1RestTemplate.getResource().getClientId());
        this.uaaTestUtil.deleteClient(this.acsZone2RestTemplate.getResource().getClientId());
        this.uaaTestUtil.deleteClient(this.acsReadOnlyRestTemplate.getResource().getClientId());
        this.uaaTestUtil.deleteClient(this.acsNoPolicyScopeRestTemplate.getResource().getClientId());
        this.uaaTestUtil.deleteClient(this.acsZonesAdminRestTemplate.getResource().getClientId());
    }

    @Override
    public String getAcsUrl() {

        return this.acsUrl;
    }

    @Override
    public HttpHeaders getZone1Headers() {

        return this.zone1Headers;
    }

    @Override
    public OAuth2RestTemplate getAcsZoneAdminRestTemplate() {
        return this.acsZone1RestTemplate;
    }

    @Override
    public OAuth2RestTemplate getAcsZone2AdminRestTemplate() {

        return this.acsZone2RestTemplate;
    }

    @Override
    public OAuth2RestTemplate getAcsReadOnlyRestTemplate() {

        return this.acsReadOnlyRestTemplate;
    }

    @Override
    public OAuth2RestTemplate getAcsNoPolicyScopeRestTemplate() {

        return this.acsNoPolicyScopeRestTemplate;
    }

    @Override
    public Zone getZone1() {
        return this.zone1;
    }

    @Override
    public Zone getZone2() {
        return this.zone2;
    }

    @Override
    public String getAcsZone1Name() {
        return this.acsZone1Name;
    }

    @Override
    public String getAcsZone2Name() {
        return this.acsZone2Name;
    }

    @Override
    public String getAcsZone3Name() {
        return this.acsZone3Name;
    }

    @Override
    public HttpHeaders getZone3Headers() {
        return this.zone3Headers;
    }

    @Override
    public OAuth2RestTemplate getAcsZonesAdminRestTemplate() {
        return this.acsZonesAdminRestTemplate;
    }

    @Override
    public OAuth2RestTemplate getAcsZoneConnectorAdminRestTemplate(final String zone) {
        return this.uaaTestUtil.createAdminConnectorScopeClient(zone);
    }

    @Override
    public OAuth2RestTemplate getAcsZoneConnectorReadRestTemplate(final String zone) {
        return this.uaaTestUtil.createReadOnlyConnectorScopeClient(zone);
    }

    @Override
    public OAuth2RestTemplate getAcsAdminRestTemplate(final String zone) {
        return new UAAACSClientsUtil(this.uaaUrl, this.uaaAdminSecret).createAcsAdminClient(Arrays.asList(zone));
    }
}