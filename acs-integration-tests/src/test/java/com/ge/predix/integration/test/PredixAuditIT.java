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

package com.ge.predix.integration.test;

import java.time.Instant;
import java.util.Collections;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.audit.rest.PredixAuditRequest;
import com.ge.predix.audit.rest.PredixAuditResponse;
import com.ge.predix.test.utils.ACSITSetUpFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.ZoneFactory;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class PredixAuditIT extends AbstractTestNGSpringContextTests {

    @Value("${AUDIT_QUERY_URL}")
    private String auditQueryUrl;

    @Value("${AUDIT_ZONE_ID}")
    private String auditZoneId;

    @Value("${AUDIT_UAA_URL}")
    private String auditUaaUrl;

    @Value("${AUDIT_UAA_CLIENT_ID}")
    private String auditClientId;

    @Value("${AUDIT_UAA_CLIENT_SECRET}")
    private String auditClientSecret;

    @Value("${zone1UaaUrl}/oauth/token")
    private String zoneTrustedIssuer;

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private ZoneFactory zoneFactory;

    private OAuth2RestTemplate auditRestTemplate;

    //TODO: Successful execution of these tests is not confirmed.
    // These tests should be enabled once audit is available in integration space.
    @BeforeClass(enabled = false)
    public void setup() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();

        resource.setAccessTokenUri(this.auditUaaUrl);
        resource.setClientId(this.auditClientId);
        resource.setClientSecret(this.auditClientSecret);
        this.auditRestTemplate = new OAuth2RestTemplate(resource);
        HttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.auditRestTemplate.setRequestFactory(requestFactory);
    }

    @Test(enabled = false)
    public void testAudit() throws Exception {
        long startTime = Instant.now().toEpochMilli();

        String zoneId = "predix-audit-zone";
        OAuth2RestTemplate acsAdminRestTemplate = this.acsitSetUpFactory.getAcsAdminRestTemplate(zoneId);
        this.zoneFactory.createTestZone(acsAdminRestTemplate, zoneId,
                Collections.singletonList(this.zoneTrustedIssuer));

        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Predix-Zone-Id", this.auditZoneId);
        Thread.sleep(5000);
        PredixAuditRequest request = new PredixAuditRequest(1, 10, startTime, Instant.now().toEpochMilli());
        ResponseEntity<PredixAuditResponse> response = this.auditRestTemplate.postForEntity(this.auditQueryUrl,
                new HttpEntity<>(request, headers), PredixAuditResponse.class);
        Assert.assertTrue(response.getBody().getContent().size() >= 1);
    }

}
