package com.ge.predix.integration.test;

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.RESOURCE_CONNECTOR_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.SUBJECT_CONNECTOR_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.test.utils.PolicyHelper.PREDIX_ZONE_ID;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.test.TestConfig;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class AttributeConnectorConfigurationIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;
    @Autowired
    private ZoneHelper zoneHelper;
    @Autowired
    private ZacTestUtil zacTestUtil;
    @Autowired
    private Environment env;

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;
    @Value("${ACS_UAA_URL}")
    private String uaaUrl;
    @Value("${zone1UaaUrl}/oauth/token")
    private String zone1TokenUrl;
    @Value("${connectorReadClientId:acs_connector_read_only_client}")
    private String zone1ConnectorReadClientId;
    @Value("${connectorReadClientSecret:s3cr3t}")
    private String zone1ConnectorReadClientSecret;
    @Value("${connectorAdminClientId:acs_connector_admin_client}")
    private String zone1ConnectorAdminClientId;
    @Value("${connectorAdminClientSecret:s3cr3t}")
    private String zone1ConnectorAdminClientSecret;
    @Value("${zone1AdminClientId:zone1AdminClient}")
    private String zone1AdminClientId;
    @Value("${zone1AdminClientSecret:zone1AdminClientSecret}")
    private String zone1AdminClientSecret;

    private String acsUrl;
    private OAuth2RestTemplate acsAdmin;
    private OAuth2RestTemplate zone1Admin;
    private OAuth2RestTemplate zone1ConnectorAdmin;
    private OAuth2RestTemplate zone1ConnectorReadClient;
    private HttpHeaders zone1Headers;

    @BeforeClass
    public void setup() throws Exception {
        TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.
 
        this.acsUrl = this.zoneHelper.getAcsBaseURL();
        this.zone1ConnectorAdmin = this.acsRestTemplateFactory.getOAuth2RestTemplateForClient(this.zone1TokenUrl,
                this.zone1ConnectorAdminClientId, this.zone1ConnectorAdminClientSecret);
        this.zone1ConnectorReadClient = this.acsRestTemplateFactory.getOAuth2RestTemplateForClient(this.zone1TokenUrl,
                this.zone1ConnectorReadClientId, this.zone1ConnectorReadClientSecret);

        this.zone1Headers = new HttpHeaders();
        this.zone1Headers.set(PREDIX_ZONE_ID, this.acsZone1Name);

        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws Exception {
        this.zacTestUtil.assumeZacServerAvailable();
        this.acsAdmin = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.zone1Admin = this.acsRestTemplateFactory.getACSZone1Template();
        this.zoneHelper.createTestZone(this.acsAdmin, this.acsZone1Name, true);
    }

    private void setupPublicACS() throws Exception {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.uaaUrl);
        uaaTestUtil.setup(Arrays.asList(new String[] { this.acsZone1Name }));
        uaaTestUtil.setupAcsZoneClient(this.acsZone1Name, this.zone1AdminClientId, this.zone1AdminClientSecret);

        this.acsAdmin = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.zone1Admin = this.acsRestTemplateFactory.getOAuth2RestTemplateForZone1AdminClient();
        this.zoneHelper.createTestZone(this.acsAdmin, this.acsZone1Name, false);
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testPutGetDeleteConnector(final String endpointUrl) throws Exception {
        AttributeConnector expectedConnector = new AttributeConnector();
        expectedConnector.setMaxCachedIntervalMinutes(100);
        expectedConnector.setAdapters(Collections.singleton(new AttributeAdapterConnection("http://my-endpoint.com",
                "http://my-uaa.com", "my-client", "my-secret")));
        try {
            this.zone1ConnectorAdmin.put(this.acsUrl + V1 + endpointUrl,
                    new HttpEntity<>(expectedConnector, this.zone1Headers));
        } catch (Exception e) {
            Assert.fail("Unable to create attribute connector. " + e.getMessage());
        }

        try {
            ResponseEntity<AttributeConnector> response = this.zone1ConnectorReadClient.exchange(
                    this.acsUrl + V1 + endpointUrl, HttpMethod.GET, new HttpEntity<>(this.zone1Headers),
                    AttributeConnector.class);
            Assert.assertEquals(response.getBody(), expectedConnector);
        } catch (Exception e) {
            Assert.fail("Unable to retrieve attribute connector." + e.getMessage());
        } finally {
            this.zone1ConnectorAdmin.exchange(this.acsUrl + V1 + endpointUrl, HttpMethod.DELETE,
                    new HttpEntity<>(this.zone1Headers), String.class);
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testCreateConnectorDeniedWithoutOauthToken(final String endpointUrl) throws Exception {
        RestTemplate acs = new RestTemplate();
        try {
            acs.put(this.acsUrl + V1 + endpointUrl,
                    new HttpEntity<>(new AttributeConnector(), this.zone1Headers));
            Assert.fail("No exception thrown when configuring connector without a token.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testCreateConnectorDeniedWithoutSufficientScope(final String endpointUrl) throws Exception {
        try {
            this.zone1ConnectorReadClient.put(this.acsUrl + V1 + endpointUrl,
                    new HttpEntity<>(new AttributeConnector(), this.zone1Headers));
            Assert.fail("No exception thrown when creating connector without sufficient scope.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    // Due to the issue in spring security, 403 Forbidden response from the server, is received as a 400 Bad Request
    // error code because error is not correctly translated by the JSON deserializer
    //https://github.com/spring-projects/spring-security-oauth/issues/191
    @Test(dataProvider = "requestUrlProvider")
    public void testGetConnectorDeniedWithoutSufficientScope(final String endpointUrl) throws Exception {
        try {
            this.zone1Admin.exchange(this.acsUrl + V1 + endpointUrl, HttpMethod.GET,
                    new HttpEntity<>(this.zone1Headers), AttributeConnector.class);
            Assert.fail("No exception thrown when retrieving connector without sufficient scope.");
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        } catch (OAuth2Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getHttpErrorCode(), HttpStatus.BAD_REQUEST.value());
        }
    }

    @Test(dataProvider = "requestUrlProvider")
    public void testDeleteConnectorDeniedWithoutSufficientScope(final String endpointUrl) throws Exception {
        try {
            this.zone1ConnectorReadClient.exchange(this.acsUrl + V1 + endpointUrl, HttpMethod.DELETE,
                    new HttpEntity<>(this.zone1Headers), String.class);
            Assert.fail("No exception thrown when deleting connector without sufficient scope.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @DataProvider(name = "requestUrlProvider")
    private Object[][] requestUrlProvider() {
        return new String[][] { { RESOURCE_CONNECTOR_URL }, { SUBJECT_CONNECTOR_URL } };
    }
}