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

import java.util.Arrays;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.monitoring.AcsMonitoringUtilities;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class AcsMonitoringIT extends AbstractTestNGSpringContextTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${ACS_URL}")
    private String acsUrl;

    @Value("${ACS_DEFAULT_ISSUER_ID}")
    private String trustedUaaTokenUrl;

    @Value("${ACS_HEALTH_CLIENT_ID:acs-health-client}")
    private String authorizedClientId;

    @Value("${ACS_UAA_URL}/oauth/token")
    private String untrustedUaaTokenUrl;

    @Value("${ACS_CLIENT_ID}")
    private String unauthorizedClientId;

    @Value("${ACS_CLIENT_SECRET}")
    private String clientSecret;

    @Autowired
    private Environment environment;

    private String getHealthUrl() {
        return this.acsUrl + "/health";
    }

    private ResponseEntity<String> hitHealthCheckUrl(final String uaaTokenUrl, final String clientId) {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(uaaTokenUrl);
        resource.setClientId(clientId);
        resource.setClientSecret(this.clientSecret);
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource);

        CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        restTemplate.setRequestFactory(requestFactory);

        return restTemplate.getForEntity(this.getHealthUrl(), String.class);
    }

    private static boolean isOverallStatusUp(final JsonNode jsonNode) {
        return jsonNode.hasNonNull(AcsMonitoringUtilities.STATUS)
                && jsonNode.get(AcsMonitoringUtilities.STATUS).asText().equalsIgnoreCase(Status.UP.toString());
    }

    private static boolean isDependencyStatusUp(final JsonNode jsonNode, final String dependency) {
        return isDependencyStatusAsExpected(jsonNode, dependency, Status.UP);
    }

    private static boolean isDependencyStatusAsExpected(final JsonNode jsonNode, final String dependency,
            final Status expectedStatus) {
        return jsonNode.hasNonNull(dependency) && jsonNode.get(dependency).hasNonNull(AcsMonitoringUtilities.STATUS)
                && jsonNode.get(dependency).get(AcsMonitoringUtilities.STATUS).asText()
                        .equalsIgnoreCase(expectedStatus.toString());
    }

    @Test(
            expectedExceptions = { HttpClientErrorException.class },
            expectedExceptionsMessageRegExp = "401 Unauthorized")
    public void testStatusWithUntrustedUaaTokenUrl() throws Exception {
        this.hitHealthCheckUrl(this.untrustedUaaTokenUrl, this.authorizedClientId);
    }

    @Test(expectedExceptions = { HttpClientErrorException.class }, expectedExceptionsMessageRegExp = "403 Forbidden")
    public void testStatusWithUnauthorizedClient() throws Exception {
        this.hitHealthCheckUrl(this.trustedUaaTokenUrl, this.unauthorizedClientId);
    }

    @Test
    public void testStatusWithAuthorizedClient() throws Exception {
        ResponseEntity<String> response = this.hitHealthCheckUrl(this.trustedUaaTokenUrl, this.authorizedClientId);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        JsonNode responseJson = OBJECT_MAPPER.readTree(response.getBody());
        Assert.assertTrue(responseJson.size() > 1);
        Assert.assertTrue(isOverallStatusUp(responseJson));
        Assert.assertTrue(isDependencyStatusUp(responseJson, "acsDb"));
        Assert.assertTrue(isDependencyStatusUp(responseJson, "uaa"));
        List<String> activeProfiles = Arrays.asList(this.environment.getActiveProfiles());
        Assert.assertTrue(activeProfiles.contains("titan") == isDependencyStatusUp(responseJson, "graphDb"));
        Assert.assertTrue(activeProfiles.contains("predix") == isDependencyStatusUp(responseJson, "zac"));
        if (activeProfiles.contains("redis") || activeProfiles.contains("cloud-redis")) {
            Assert.assertTrue(isDependencyStatusUp(responseJson, "decisionCache"));
            Assert.assertTrue(isDependencyStatusUp(responseJson, "resourceCache"));
            Assert.assertTrue(isDependencyStatusUp(responseJson, "subjectCache"));
        }
    }

    @Test
    public void testStatusWithoutAuthentication() throws Exception {
        ResponseEntity<String> response = new RestTemplate().getForEntity(this.getHealthUrl(), String.class);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        JsonNode responseJson = OBJECT_MAPPER.readTree(response.getBody());
        Assert.assertEquals(responseJson.size(), 1);
        Assert.assertTrue(isOverallStatusUp(responseJson));
    }
}
