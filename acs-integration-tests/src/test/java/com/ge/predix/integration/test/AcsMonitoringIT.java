package com.ge.predix.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.monitoring.AcsMonitoringUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

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
        return new OAuth2RestTemplate(resource).getForEntity(this.getHealthUrl(), String.class);
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
            expectedExceptions = { OAuth2AccessDeniedException.class },
            expectedExceptionsMessageRegExp = "Invalid token.*")
    public void testStatusWithUntrustedUaaTokenUrl() throws Exception {
        this.hitHealthCheckUrl(this.untrustedUaaTokenUrl, this.authorizedClientId);
    }

    @Test(expectedExceptions = { OAuth2Exception.class }, expectedExceptionsMessageRegExp = "Forbidden")
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
        Assert.assertTrue((activeProfiles.contains("redis")
                || activeProfiles.contains("cloud-redis")) == isDependencyStatusAsExpected(responseJson,
                        "decisionCache", Status.UNKNOWN));
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
