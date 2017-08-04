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

package com.ge.predix.acceptance.test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.web.AcsApiUriTemplates;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.ZoneHelper;
import com.ge.predix.test.utils.v2.UaaTestUtil;

/**
 * @author 212319607
 */
@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:acceptance-test-spring-context.xml")
public class ACSAcceptanceIT extends AbstractTestNGSpringContextTests {

    @Value("${ACS_URL}")
    private String acsBaseUrl;

    @Autowired
    private Environment environment;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Value("${ACS_TESTING_UAA}")
    private String uaaUrl;

    @Value("${UAA_ADMIN_SECRET:adminsecret}")
    private String uaaAdminSecret;

    @Value("${ACS_SERVICE_ID:predix-acs}")
    private String serviceId;

    private OAuth2RestTemplate acsAdminRestTemplate;
    private OAuth2RestTemplate acsZoneRestTemplate;
    private HttpHeaders headersWithZoneSubdomain = ACSTestUtil.httpHeaders();
    private UaaTestUtil uaaTestUtil;
    private String acsZoneName;

    @BeforeClass
    public void setup() throws IOException {
        this.acsZoneName = this.zoneHelper.getRandomName(this.getClass().getSimpleName());
        this.headersWithZoneSubdomain.set("Predix-Zone-Id", this.acsZoneName);
        this.uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory, this.uaaUrl, this.uaaAdminSecret);

        this.acsAdminRestTemplate = this.uaaTestUtil.createAcsAdminClientAndGetTemplate(this.acsZoneName);
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZoneName,
                Collections.singletonList(this.uaaUrl + "/oauth/token"));
        this.acsZoneRestTemplate = this.uaaTestUtil.createZoneClientAndGetTemplate(this.acsZoneName, this.serviceId);

    }

    @Test
    public void testAcsHealth() {

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> heartbeatResponse =
                    restTemplate.exchange(this.acsBaseUrl + AcsApiUriTemplates.HEARTBEAT_URL, HttpMethod.GET,
                            new HttpEntity<>(this.headersWithZoneSubdomain), String.class);
            Assert.assertEquals(heartbeatResponse.getBody(), "alive", "ACS Heartbeat Check Failed");
        } catch (Exception e) {
            Assert.fail("Could not perform ACS Heartbeat Check: " + e.getMessage());
        }

        try {
            ResponseEntity<Map> healthStatus = restTemplate.exchange(this.acsBaseUrl + "/health", HttpMethod.GET,
                    new HttpEntity<>(this.headersWithZoneSubdomain), Map.class);
            Assert.assertNotNull(healthStatus);
            Assert.assertEquals(healthStatus.getBody().size(), 1);
            String acsStatus = (String) healthStatus.getBody().get("status");
            Assert.assertEquals(acsStatus, "UP", "ACS Health Check Failed: " + acsStatus);
        } catch (Exception e) {
            Assert.fail("Could not perform ACS Health Check: " + e.getMessage());
        }

    }

    @Test(dataProvider = "endpointProvider")
    public void testCompleteACSFlow(final String endpoint, final HttpHeaders headers,
            final PolicyEvaluationRequestV1 policyEvalRequest, final String subjectIdentifier) throws Exception {

        String testPolicyName = null;
        BaseSubject marissa = null;
        BaseResource testResource = null;
        try {
            testPolicyName = this.policyHelper.setTestPolicy(this.acsZoneRestTemplate, headers, endpoint,
                    "src/test/resources/testCompleteACSFlow.json");
            BaseSubject subject = new BaseSubject(subjectIdentifier);
            Attribute site = new Attribute();
            site.setIssuer("issuerId1");
            site.setName("site");
            site.setValue("sanramon");

            marissa = this.privilegeHelper.putSubject(this.acsZoneRestTemplate, subject, endpoint, headers, site);

            Attribute region = new Attribute();
            region.setIssuer("issuerId1");
            region.setName("region");
            region.setValue("testregion"); // test policy asserts on this value

            BaseResource resource = new BaseResource();
            resource.setResourceIdentifier("/alarms/sites/sanramon");

            testResource =
                    this.privilegeHelper.putResource(this.acsZoneRestTemplate, resource, endpoint, headers, region);

            ResponseEntity<PolicyEvaluationResult> evalResponse =
                    this.acsZoneRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                            new HttpEntity<>(policyEvalRequest, headers), PolicyEvaluationResult.class);

            Assert.assertEquals(evalResponse.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = evalResponse.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
        } finally {
            // delete policy
            if (null != testPolicyName) {
                this.acsZoneRestTemplate.exchange(endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName,
                        HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            }

            // delete attributes
            if (null != marissa) {
                this.acsZoneRestTemplate.exchange(
                        endpoint + PrivilegeHelper.ACS_SUBJECT_API_PATH + marissa.getSubjectIdentifier(),
                        HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            }
            if (null != testResource) {
                String encodedResource = URLEncoder.encode(testResource.getResourceIdentifier(), "UTF-8");
                URI uri = new URI(endpoint + PrivilegeHelper.ACS_RESOURCE_API_PATH + encodedResource);
                this.acsZoneRestTemplate.exchange(uri, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            }
        }
    }

    @DataProvider(name = "endpointProvider")
    public Object[][] getAcsEndpoint() throws Exception {
        PolicyEvaluationRequestV1 policyEvalForBob =
                this.policyHelper.createEvalRequest("GET", "bob", "/alarms/sites/sanramon", null);

        return new Object[][] { { this.acsBaseUrl, this.headersWithZoneSubdomain, policyEvalForBob, "bob" } };
    }

    private ResponseEntity<String> getMonitoringApiResponse(final HttpHeaders headers) {
        return new RestTemplate().exchange(URI.create(this.acsBaseUrl + AcsApiUriTemplates.HEARTBEAT_URL),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    // TODO: Remove this test when the "httpValidation" Spring profile is removed
    @Test
    public void testHttpValidationBasedOnActiveSpringProfile() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);

        if (!Arrays.asList(this.environment.getActiveProfiles()).contains("httpValidation")) {
            Assert.assertEquals(this.getMonitoringApiResponse(headers).getStatusCode(), HttpStatus.OK);
            return;
        }

        try {
            this.getMonitoringApiResponse(headers);
            Assert.fail("Expected an HttpMediaTypeNotAcceptableException exception to be thrown");
        } catch (final HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @AfterClass
    public void tearDown() {
        this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.acsZoneName);
        this.uaaTestUtil.deleteClient(this.acsAdminRestTemplate.getResource().getClientId());
        this.uaaTestUtil.deleteClient(this.acsZoneRestTemplate.getResource().getClientId());
    }

}
