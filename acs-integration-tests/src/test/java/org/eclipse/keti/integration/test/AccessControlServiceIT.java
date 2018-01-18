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

package org.eclipse.keti.integration.test;

import static org.eclipse.keti.integration.test.SubjectResourceFixture.JLO_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.JOE_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.MARISSA_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.PETE_V1;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.keti.acs.model.Effect;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.eclipse.keti.acs.rest.PolicyEvaluationResult;
import org.eclipse.keti.test.utils.ACSITSetUpFactory;
import org.eclipse.keti.test.utils.ACSTestUtil;
import org.eclipse.keti.test.utils.PolicyHelper;
import org.eclipse.keti.test.utils.PrivilegeHelper;

@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class AccessControlServiceIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private ACSTestUtil acsTestUtil;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.acsitSetUpFactory.setUp();
    }

    @Test(dataProvider = "subjectProvider")
    public void testPolicyEvalWithFirstMatchDeny(final BaseSubject subject,
            final PolicyEvaluationRequestV1 policyEvaluationRequest, final String endpoint) throws Exception {

        this.privilegeHelper.putSubject(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(), subject, endpoint,
                this.acsitSetUpFactory.getZone1Headers(), this.privilegeHelper.getDefaultAttribute());

        String policyFile = "src/test/resources/multiple-site-based-policy-set.json";
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate()
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsitSetUpFactory.getZone1Headers()),
                        PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
        this.privilegeHelper.deleteSubject(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), subject.getSubjectIdentifier(),
                this.acsitSetUpFactory.getZone1Headers());
    }

    @Test(dataProvider = "subjectProvider")
    public void testPolicyEvalPermit(final BaseSubject subject, final PolicyEvaluationRequestV1 policyEvaluationRequest,
            final String endpoint) throws Exception {
        String testPolicyName = null;
        try {

            this.privilegeHelper.putSubject(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(), subject, endpoint,
                    this.acsitSetUpFactory.getZone1Headers(), this.privilegeHelper.getDefaultAttribute());
            String policyFile = "src/test/resources/single-site-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate()
                    .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                            new HttpEntity<>(policyEvaluationRequest, this.acsitSetUpFactory.getZone1Headers()),
                            PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            PolicyEvaluationRequestV1 policyEvaluationRequest2 = this.policyHelper
                    .createEvalRequest(subject.getSubjectIdentifier(), "ny");

            postForEntity = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate().postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest2, this.acsitSetUpFactory.getZone1Headers()),
                    PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                        this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
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
            // OAuth2RestTemplate acsRestTemplate = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();

            // set policy
            String policyFile = "src/test/resources/policies/policy-set-with-attribute-uri-template.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), this.acsitSetUpFactory.getAcsUrl(), policyFile);

            // Policy Eval. without setting required attribute on resource. Should return DENY
            // Note resourceId sent to eval request is the complete URI, from which /asset/1223 will be
            // extracted by ACS, using "attributeUriTemplate": "/v1/region/report{attribute_uri}"
            PolicyEvaluationRequestV1 evalRequest = this.policyHelper.createEvalRequest("GET", "testSubject",
                    "/v1/region/report/asset/1223", null);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate()
                    .postForEntity(this.acsitSetUpFactory.getAcsUrl() + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                            new HttpEntity<>(evalRequest, this.acsitSetUpFactory.getZone1Headers()),
                            PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            Assert.assertEquals(postForEntity.getBody().getEffect(), Effect.DENY);

            // Set resource attribute and evaluate again. expect PERMIT
            // createResource adds a 'site' attribute with value 'sanramon' used by our test policy
            BaseResource testResource = this.privilegeHelper.createResource(testResourceId);
            this.privilegeHelper.postResources(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getAcsUrl(), this.acsitSetUpFactory.getZone1Headers(), testResource);

            postForEntity = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate().postForEntity(
                    this.acsitSetUpFactory.getAcsUrl() + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(evalRequest, this.acsitSetUpFactory.getZone1Headers()),
                    PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            Assert.assertEquals(postForEntity.getBody().getEffect(), Effect.PERMIT);

        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                        this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
            }
        }

    }

    @Test(dataProvider = "endpointProvider")
    public void testCreationOfValidPolicy(final String endpoint) throws Exception {
        String policyFile = "src/test/resources/single-site-based-policy-set.json";
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
        PolicySet policySetSaved = this.getPolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                testPolicyName, this.acsitSetUpFactory.getZone1Headers(), endpoint);
        Assert.assertEquals(testPolicyName, policySetSaved.getName());
        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyCreationInValidPolicy(final String endpoint) throws Exception {
        String testPolicyName = "";
        try {
            String policyFile = "src/test/resources/missing-policy-set-name-policy.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            this.acsTestUtil.assertExceptionResponseBody(e, "policy set name is missing");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
        Assert.fail("testPolicyCreationInValidPolicy should have failed");
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyCreationInValidWithBadPolicySetNamePolicy(final String endpoint) throws Exception {
        String testPolicyName = "";
        try {
            String policyFile = "src/test/resources/policy-set-with-only-name-effect.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            this.acsTestUtil.assertExceptionResponseBody(e, "is not URI friendly");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
        Assert.fail("testPolicyCreationInValidPolicy should have failed");
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyCreationJsonSchemaInvalidPolicySet(final String endpoint) throws Exception {
        String testPolicyName = "";
        try {
            String policyFile = "src/test/resources/invalid-json-schema-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            this.acsTestUtil.assertExceptionResponseBody(e, "JSON Schema validation");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
        Assert.fail("testPolicyCreationInValidPolicy should have failed");
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyEvalNotApplicable(final String endpoint) throws Exception {
        String testPolicyName = null;
        try {
            this.privilegeHelper.putSubject(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(), MARISSA_V1,
                    this.acsitSetUpFactory.getAcsUrl(), this.acsitSetUpFactory.getZone1Headers(),
                    this.privilegeHelper.getDefaultAttribute());

            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);

            PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                    .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate()
                    .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                            new HttpEntity<>(policyEvaluationRequest, this.acsitSetUpFactory.getZone1Headers()),
                            PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } catch (Exception e) {
            Assert.fail("testPolicyEvalNotApplicable should have NOT failed " + endpoint + " " + e.getMessage());
        } finally {
            this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
            this.privilegeHelper.deleteSubject(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getAcsUrl(), MARISSA_V1.getSubjectIdentifier(),
                    this.acsitSetUpFactory.getZone1Headers());
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyUpdateWithNoOauthToken(final String endpoint)
            throws JsonParseException, JsonMappingException, IOException {
        RestTemplate acs = new RestTemplate();
        // Use vanilla rest template with no oauth token.
        try {
            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            this.policyHelper.setTestPolicy(acs, this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
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
                    new HttpEntity<>(this.policyHelper.createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon"),
                            this.acsitSetUpFactory.getZone1Headers()),
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
            testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsNoPolicyScopeRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
            this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
            Assert.fail("No exception when trying to create policy set with no acs scope");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyUpdateWithReadOnlyAccess(final String endpoint) throws Exception {
        try {
            String policyFile = "src/test/resources/policy-set-with-multiple-policies-na-with-condition.json";
            this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsReadOnlyRestTemplate(),
                    this.acsitSetUpFactory.getZone1Headers(), endpoint, policyFile);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN);
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testPolicyReadWithReadOnlyAccess(final String endpoint) throws Exception {
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getZone1Headers(), endpoint,
                "src/test/resources/single-site-based-policy-set.json");

        ResponseEntity<PolicySet> policySetResponse = this.acsitSetUpFactory.getAcsReadOnlyRestTemplate().exchange(
                endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName, HttpMethod.GET,
                new HttpEntity<>(this.acsitSetUpFactory.getZone1Headers()), PolicySet.class);
        Assert.assertEquals(testPolicyName, policySetResponse.getBody().getName());
        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
    }

    @Test
    public void testCreatePolicyWithClientOnlyBasedToken() throws Exception {
        String testPolicyName = null;
        try {

            PolicySet policySet = new ObjectMapper()
                    .readValue(new File("src/test/resources/single-site-based-policy-set.json"), PolicySet.class);
            testPolicyName = policySet.getName();
            this.acsitSetUpFactory.getAcsZoneAdminRestTemplate().put(
                    this.acsitSetUpFactory.getAcsUrl() + PolicyHelper.ACS_POLICY_SET_API_PATH + testPolicyName,
                    new HttpEntity<>(policySet, this.acsitSetUpFactory.getZone1Headers()));
        } finally {
            this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                    this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
        }
    }

    @Test(dataProvider = "endpointProvider")
    public void testGetAllPolicySets(final String endpoint) throws Exception {
        String testPolicyName = this.policyHelper.setTestPolicy(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getZone1Headers(), endpoint,
                "src/test/resources/single-site-based-policy-set.json");

        String getAllPolicySetsURL = endpoint + PolicyHelper.ACS_POLICY_SET_API_PATH;

        ResponseEntity<PolicySet[]> policySetsResponse = this.acsitSetUpFactory.getAcsReadOnlyRestTemplate().exchange(
                getAllPolicySetsURL, HttpMethod.GET, new HttpEntity<>(this.acsitSetUpFactory.getZone1Headers()),
                PolicySet[].class);

        PolicySet[] policySets = policySetsResponse.getBody();
        // should expect only one policySet per issuer, clientId and policySetId
        Assert.assertEquals(1, policySets.length);
        Assert.assertEquals(testPolicyName, policySets[0].getName());

        this.policyHelper.deletePolicySet(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), testPolicyName, this.acsitSetUpFactory.getZone1Headers());
    }

    private PolicySet getPolicySet(final RestTemplate acs, final String policyName, final HttpHeaders headers,
            final String acsEndpointParam) {
        ResponseEntity<PolicySet> policySetResponse = acs.exchange(
                acsEndpointParam + PolicyHelper.ACS_POLICY_SET_API_PATH + policyName, HttpMethod.GET,
                new HttpEntity<>(headers), PolicySet.class);
        return policySetResponse.getBody();
    }

    @DataProvider(name = "subjectProvider")
    public Object[][] getSubject() {
        Object[][] data = new Object[][] {
                { MARISSA_V1, this.policyHelper.createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsitSetUpFactory.getAcsUrl() },
                { JOE_V1, this.policyHelper.createEvalRequest(JOE_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsitSetUpFactory.getAcsUrl() },
                { PETE_V1, this.policyHelper.createEvalRequest(PETE_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsitSetUpFactory.getAcsUrl() },
                { JLO_V1, this.policyHelper.createEvalRequest(JLO_V1.getSubjectIdentifier(), "sanramon"),
                        this.acsitSetUpFactory.getAcsUrl() } };
        return data;
    }

    @DataProvider(name = "endpointProvider")
    public Object[][] getAcsEndpoint() {
        Object[][] data = new Object[][] { { this.acsitSetUpFactory.getAcsUrl() } };
        return data;
    }

    @AfterClass
    public void cleanup() throws Exception {
        this.privilegeHelper.deleteResources(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), this.acsitSetUpFactory.getZone1Headers());
        this.privilegeHelper.deleteSubjects(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), this.acsitSetUpFactory.getZone1Headers());
        this.policyHelper.deletePolicySets(this.acsitSetUpFactory.getAcsZoneAdminRestTemplate(),
                this.acsitSetUpFactory.getAcsUrl(), this.acsitSetUpFactory.getZone1Headers());
        this.acsitSetUpFactory.destroy();
    }
}
