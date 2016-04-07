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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

/**
 * @author 212319607
 */
@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:acceptance-test-spring-context.xml")
public class ACSAcceptanceIT extends AbstractTestNGSpringContextTests {

    @Value("${acsUrl:http://localhost:8181}")
    private String acsBaseUrl;

    private String testZoneSubdomain;

    private HttpHeaders headersWithZoneSubdomain;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String acsZone2Name;

    @Value("${ZONE3_NAME:testzone3}")
    private String acsZone3Name;

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

    private OAuth2RestTemplate acsAdminRestTemplate;
    private boolean registerWithZac;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
        this.headersWithZoneSubdomain = new HttpHeaders();
        this.headersWithZoneSubdomain.set("Predix-Zone-Id", this.testZoneSubdomain);
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.registerWithZac = true;
        this.testZoneSubdomain = this.zoneHelper
                .createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac).getSubdomain();
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.uaaUrl);
        uaaTestUtil.setup(Arrays.asList(new String[] { this.acsZone1Name, this.acsZone2Name, this.acsZone3Name }));

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.registerWithZac = false;
        this.testZoneSubdomain = this.zoneHelper
                .createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac).getSubdomain();
    }

    @Test(groups = { "acsHealthCheck" })
    public void testAcsHealth() {

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> heartbeatResponse = restTemplate.exchange(this.acsBaseUrl + "/monitoring/heartbeat",
                    HttpMethod.GET, new HttpEntity<>(this.headersWithZoneSubdomain), String.class);
            Assert.assertEquals(heartbeatResponse.getBody(), "alive", "ACS Heartbeat Check Failed");
        } catch (Exception e) {
            Assert.fail("Could not perform ACS Heartbeat Check: " + e.getMessage());
        }

        try {
            @SuppressWarnings("rawtypes")
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
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, headers, endpoint,
                    "src/test/resources/testCompleteACSFlow.json");
            BaseSubject subject = new BaseSubject(subjectIdentifier);
            Attribute site = new Attribute();
            site.setIssuer("issuerId1");
            site.setName("site");
            site.setValue("sanramon");

            marissa = this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, headers, site);

            Attribute region = new Attribute();
            region.setIssuer("issuerId1");
            region.setName("region");
            region.setValue("testregion"); // test policy asserts on this value

            BaseResource resource = new BaseResource();
            resource.setResourceIdentifier("/alarms/sites/sanramon");

            testResource = this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, headers,
                    region);

            ResponseEntity<PolicyEvaluationResult> evalResponse = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH, new HttpEntity<>(policyEvalRequest, headers),
                    PolicyEvaluationResult.class);

            Assert.assertEquals(evalResponse.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = evalResponse.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
        } finally {
            // delete policy
            if (null != testPolicyName) {
                this.acsAdminRestTemplate.exchange(endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName,
                        HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            }

            // delete attributes
            if (null != marissa) {
                this.acsAdminRestTemplate.exchange(
                        endpoint + PrivilegeHelper.ACS_SUBJECT_API_PATH + marissa.getSubjectIdentifier(),
                        HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            }
            if (null != testResource) {
                String encodedResource = URLEncoder.encode(testResource.getResourceIdentifier(), "UTF-8");
                URI uri = new URI(endpoint + PrivilegeHelper.ACS_RESOURCE_API_PATH + encodedResource);
                this.acsAdminRestTemplate.exchange(uri, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            }
        }
    }

    @DataProvider(name = "endpointProvider")
    public Object[][] getAcsEndpoint() throws Exception {
        PolicyEvaluationRequestV1 policyEvalForBob = this.policyHelper.createEvalRequest("GET", "bob",
                "/alarms/sites/sanramon", null);

        Object[][] data = new Object[][] {
                { this.acsBaseUrl, this.headersWithZoneSubdomain, policyEvalForBob, "bob" } };
        return data;
    }

    @AfterClass
    public void cleanUp() {
        this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

}
