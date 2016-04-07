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
package com.ge.predix.integration.test;

import static com.ge.predix.integration.test.SubjectResourceFixture.JLO_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.JOE_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.MARISSA_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.PETE_V1;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class AccessControlServiceIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private ACSTestUtil acsTestUtil;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String acsZone2Name;

    @Value("${ZONE3_NAME:testzone3}")
    private String acsZone3Name;

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

    private String acsZone1Url;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private OAuth2RestTemplate acsReadOnlyRestTemplate;
    private OAuth2RestTemplate acsNoPolicyScopeRestTemplate;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.acsReadOnlyRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithReadOnlyPolicyAccess();
        this.acsNoPolicyScopeRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithNoAcsScope();

        this.zoneHelper.createPrimaryTestZone();
        this.acsZone1Url = this.zoneHelper.getZone1Url();
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        this.acsZone1Url = this.zoneHelper.getZone1Url();
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.uaaUrl);
        uaaTestUtil.setup(Arrays.asList(new String[] {this.acsZone1Name, this.acsZone2Name, this.acsZone3Name}));

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.acsReadOnlyRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForReadOnlyClient();
        this.acsNoPolicyScopeRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForNoPolicyScopeClient();

        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, false);
    }

    @Test(dataProvider = "subjectProvider")
    public void testPolicyEvalWithFirstMatchDeny(final BaseSubject subject,
            final PolicyEvaluationRequestV1 policyEvaluationRequest, final String endpoint) throws Exception {

        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, null,
                this.privilegeHelper.getDefaultAttribute());

        String policyFile = "src/test/resources/multiple-site-based-policy-set.json";
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH, policyEvaluationRequest,
                PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
        this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                subject.getSubjectIdentifier());
    }

    @Test(dataProvider = "subjectProvider")
    public void testPolicyEvalPermit(final BaseSubject subject, final PolicyEvaluationRequestV1 policyEvaluationRequest,
            final String endpoint) throws Exception {
        String testPolicyName = null;
        try {

            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, null,
                    this.privilegeHelper.getDefaultAttribute());
            String policyFile = "src/test/resources/single-site-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH, policyEvaluationRequest,
                    PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            PolicyEvaluationRequestV1 policyEvaluationRequest2 = this.policyHelper
                    .createEvalRequest(subject.getSubjectIdentifier(), "ny");

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    policyEvaluationRequest2, PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
            }
        }
    }

    // TODO: Delete resource. Currently it's causing "405 Request method 'DELETE' not supported" error
    // Not deleting resource prevents from running this test repeatedly.
    // @Test
    @Test
    public void testPolicyEvalWithAttributeUriTemplate() throws Exception {
        String testPolicyName = null;

        // This is the extracted resource URI using attributeUriTemplate. See test policy.
        String testResourceId = "/asset/1223";
        try {
            // OAuth2RestTemplate acsRestTemplate = this.acsAdminRestTemplate;

            // set policy
            String policyFile = "src/test/resources/policies/policy-set-with-attribute-uri-template.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.acsZone1Url,
                    policyFile);

            // Policy Eval. without setting required attribute on resource. Should return DENY
            // Note resourceId sent to eval request is the complete URI, from which /asset/1223 will be
            // extracted by ACS, using "attributeUriTemplate": "/v1/region/report{attribute_uri}"
            PolicyEvaluationRequestV1 evalRequest = this.policyHelper.createEvalRequest("GET", "testSubject",
                    "/v1/region/report/asset/1223", null);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    this.acsZone1Url + PolicyHelper.ACS_POLICY_EVAL_API_PATH, evalRequest,
                    PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            Assert.assertEquals(postForEntity.getBody().getEffect(), Effect.DENY);

            // Set resource attribute and evaluate again. expect PERMIT
            // createResource adds a 'site' attribute with value 'sanramon' used by our test policy
            BaseResource testResource = this.privilegeHelper.createResource(testResourceId);
            this.privilegeHelper.postResources(this.acsAdminRestTemplate, this.acsZone1Url, testResource);

            postForEntity = this.acsAdminRestTemplate.postForEntity(
                    this.acsZone1Url + PolicyHelper.ACS_POLICY_EVAL_API_PATH, evalRequest,
                    PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            Assert.assertEquals(postForEntity.getBody().getEffect(), Effect.PERMIT);

        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
            }
            // this.privilegeHelper.deleteResource(this.acsAdminRestTemplate, this.acsZone1Url, testResourceId);
        }

    }

    @Test(dataProvider = "endpointProvider")
    public void testCreationOfValidPolicy(final String endpoint) throws Exception {
        String policyFile = "src/test/resources/single-site-based-policy-set.json";
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
        PolicySet policySetSaved = this.getPolicySet(this.acsAdminRestTemplate, testPolicyName, endpoint);
        Assert.assertEquals(testPolicyName, policySetSaved.getName());
        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
    }

    @Test(dataProvider = "endpointProvider")
    public void testMultiplePolicySetCreation(final String endpoint) throws Exception {
        String policyFile = "src/test/resources/single-site-based-policy-set.json";
        String testPolicyName = "";
        boolean policyCreated = false;
        try {
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
            policyCreated = true;

            try {
                policyFile = "src/test/resources/multiple-site-based-policy-set.json";
                testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
                Assert.fail("testPolicyCreationInValidPolicy should have failed");
            } catch (HttpClientErrorException e) {
                this.acsTestUtil.assertExceptionResponseBody(e, "Multiple policy sets");
                Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
                return;
            }
        } finally {
            if (policyCreated) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
            }
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyCreationInValidPolicy(final String endpoint) throws Exception {
        String testPolicyName = "";
        try {
            String policyFile = "src/test/resources/missing-policy-set-name-policy.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            this.acsTestUtil.assertExceptionResponseBody(e, "policy set name is missing");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
        Assert.fail("testPolicyCreationInValidPolicy should have failed");
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyCreationInValidWithBadPolicySetNamePolicy(final String endpoint) throws Exception {
        String testPolicyName = "";
        try {
            String policyFile = "src/test/resources/policy-set-with-only-name-effect.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            this.acsTestUtil.assertExceptionResponseBody(e, "is not URI friendly");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
        Assert.fail("testPolicyCreationInValidPolicy should have failed");
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyCreationJsonSchemaInvalidPolicySet(final String endpoint) throws Exception {
        String testPolicyName = "";
        try {
            String policyFile = "src/test/resources/invalid-json-schema-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            this.acsTestUtil.assertExceptionResponseBody(e, "JSON Schema validation");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
        Assert.fail("testPolicyCreationInValidPolicy should have failed");
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyEvalNotApplicable(final String endpoint) throws Exception {
        String testPolicyName = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, MARISSA_V1, this.acsZone1Url, null,
                    this.privilegeHelper.getDefaultAttribute());

            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint, policyFile);

            PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                    .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH, policyEvaluationRequest,
                    PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } catch (Exception e) {
            Assert.fail("testPolicyEvalNotApplicable should have NOT failed " + endpoint + " " + e.getMessage());
        } finally {
            this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
            this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                    MARISSA_V1.getSubjectIdentifier());
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyUpdateWithNoOauthToken(final String endpoint)
            throws JsonParseException, JsonMappingException, IOException {
        RestTemplate acs = new RestTemplate();
        // Use vanilla rest template with no oauth token.
        try {
            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            this.policyHelper.setTestPolicy(acs, null, endpoint, policyFile);
            Assert.fail("No exception thrown when making request without token.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNAUTHORIZED);
        }

    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyEvalWithNoOauthToken(final String endpoint) {
        RestTemplate acs = new RestTemplate();
        // Use vanilla rest template with no oauth token.
        try {
            acs.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    this.policyHelper.createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon"),
                    PolicyEvaluationResult.class);
            Assert.fail("No exception thrown when making policy evaluation request without token.");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyUpdateWithInsufficientScope(final String endpoint) throws Exception {
        String testPolicyName;
        try {
            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsNoPolicyScopeRestTemplate, null, endpoint,
                    policyFile);
            this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
            Assert.fail("No exception when trying to create policy set with no acs scope");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyUpdateWithReadOnlyAccess(final String endpoint) throws Exception {
        try {
            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            this.policyHelper.setTestPolicy(this.acsReadOnlyRestTemplate, null, endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyReadWithReadOnlyAccess(final String endpoint) throws Exception {
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint,
                "src/test/resources/single-site-based-policy-set.json");

        ResponseEntity<PolicySet> policySetResponse = this.acsReadOnlyRestTemplate
                .getForEntity(endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName, PolicySet.class);
        Assert.assertEquals(testPolicyName, policySetResponse.getBody().getName());
        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
    }

    @Test
    public void testCreatePolicyWithClientOnlyBasedToken() throws Exception {
        String testPolicyName = null;
        try {

            PolicySet policySet = new ObjectMapper()
                    .readValue(new File("src/test/resources/single-site-based-policy-set.json"), PolicySet.class);
            testPolicyName = policySet.getName();
            this.acsAdminRestTemplate.put(this.acsZone1Url + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName,
                    policySet);
        } finally {
            this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testGetAllPolicySets(final String endpoint) throws Exception {
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, endpoint,
                "src/test/resources/single-site-based-policy-set.json");

        String getAllPolicySetsURL = endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH;

        ResponseEntity<PolicySet[]> policySetsResponse = this.acsReadOnlyRestTemplate.getForEntity(getAllPolicySetsURL,
                PolicySet[].class);

        PolicySet[] policySets = policySetsResponse.getBody();
        // should expect only one policySet per issuer, clientId and policySetId
        Assert.assertEquals(1, policySets.length);
        Assert.assertEquals(testPolicyName, policySets[0].getName());

        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsZone1Url, testPolicyName);
    }

    private PolicySet getPolicySet(final RestTemplate acs, final String policyName, final String acsEndpointParam) {
        ResponseEntity<PolicySet> policySetResponse = acs
                .getForEntity(acsEndpointParam + PolicyHelper.ACS_POLICY_SET_API_PATH + policyName, PolicySet.class);
        return policySetResponse.getBody();
    }

    @DataProvider(name = "subjectProvider")
    public Object[][] getSubject() {
        Object[][] data = new Object[][] {
                { MARISSA_V1, this.policyHelper.createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsZone1Url },
                { JOE_V1, this.policyHelper.createEvalRequest(JOE_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsZone1Url },
                { PETE_V1, this.policyHelper.createEvalRequest(PETE_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsZone1Url },
                { JLO_V1, this.policyHelper.createEvalRequest(JLO_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsZone1Url } };
        return data;
    }

    @DataProvider(name = "endpointProvider")
    public Object[][] getAcsEndpoint() {
        Object[][] data = new Object[][] { { this.acsZone1Url } };
        return data;
    }

}
