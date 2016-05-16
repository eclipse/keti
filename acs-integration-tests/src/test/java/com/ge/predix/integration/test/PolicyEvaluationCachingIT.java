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

import static com.ge.predix.integration.test.SubjectResourceFixture.BOB_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.MARISSA_V1;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_RESOURCE_IDENTIFIER;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_SUBJECT_ID;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.test.TestConfig;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@SuppressWarnings({ "nls" })
public class PolicyEvaluationCachingIT extends AbstractTestNGSpringContextTests {

    static final JsonUtils JSON_UTILS = new JsonUtils();
    private static final String NOT_MATCHING_ACTION = "HEAD";

    @Value("${acsUrl:http://localhost:8181}")
    private String acsUrl;

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

    private String acsZone1Name;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    @Autowired
    private ZoneHelper zoneHelper;

    private OAuth2RestTemplate acsAdminRestTemplate;
    private UaaTestUtil uaaTestUtil;
    private final HttpHeaders acsZone1Headers = new HttpHeaders();

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.

        this.acsZone1Name = this.zoneHelper.getZone1Name();
        this.acsZone1Headers.add("Predix-Zone-Id", this.acsZone1Name);
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.zoneHelper.createPrimaryTestZone();
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        this.uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(), this.uaaUrl);
        this.uaaTestUtil.setup(Arrays.asList(new String[] { this.acsZone1Name }));
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, false);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when a policy set changes.
     * Policy set name that is used to derive part of cache key does not change.
     */
    @Test
    public void testPolicyEvalCacheWhenPolicySetChanges() throws Exception {
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute());
            String policyFile = "src/test/resources/policies/single-site-based.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            policyFile = "src/test/resources/policies/deny-all.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.DENY);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when the name of a policy
     * set changes as well as the policies it contains.
     */
    @Test
    public void testPolicyEvalCacheWhenPolicySetNameChanges() throws Exception {
        String testPolicyName = null;
        String endpoint = this.acsUrl;
        try {
            String policyFile = "src/test/resources/policies/indeterminate.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);
            PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper.createEvalRequest(NOT_MATCHING_ACTION,
                    DEFAULT_SUBJECT_ID, DEFAULT_RESOURCE_IDENTIFIER, null);
            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
            this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(), testPolicyName,
                    this.acsZone1Headers);

            policyFile = "src/test/resources/policies/permit-admin-only-using-condition.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            policyEvaluationRequest = this.policyHelper.createEvalRequest(NOT_MATCHING_ACTION, DEFAULT_SUBJECT_ID,
                    DEFAULT_RESOURCE_IDENTIFIER, null);
            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.DENY);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is updated.
     */
    @Test
    public void testPolicyEvalCacheWhenResourceChanges() throws Exception {
        BaseResource resource = new BaseResource("/secured-by-value/sites/sanramon");
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultOrgAttribute());
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
            String policyFile = "src/test/resources/policies/single-org-based.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getAlternateOrgAttribute());

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
                this.privilegeHelper.deleteResource(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        resource.getResourceIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is deleted.
     */
    @Test
    public void testPolicyEvalCacheWhenResourceDeleted() throws Exception {
        BaseResource resource = new BaseResource("/secured-by-value/sites/sanramon");
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultOrgAttribute());
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
            String policyFile = "src/test/resources/policies/single-org-based.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            this.privilegeHelper.deleteResource(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                    resource.getResourceIdentifier(), this.acsZone1Headers);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is updated and there is no target in the policy set.
     */
    @Test
    public void testPolicyEvalCacheWhenResourceChangesNoTargetInPolicySet() throws Exception {
        BaseResource resource = new BaseResource("/secured-by-value/sites/sanramon");
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper.createEvalRequest("GET",
                MARISSA_V1.getSubjectIdentifier(), "/secured-by-value/sites/sanramon", null);
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultOrgAttribute());
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
            String policyFile = "src/test/resources/policies/single-org-based-no-target.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getAlternateOrgAttribute());

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
                this.privilegeHelper.deleteResource(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        resource.getResourceIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when a policy has multiple
     * resource attribute URI templates that match the request.
     */
    @Test
    public void testPolicyWithMultAttrUriTemplatatesEvalCache() throws Exception {
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper.createEvalRequest("GET",
                MARISSA_V1.getSubjectIdentifier(), "/v1/site/1/plant/asset/1", null);
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
            String policyFile = "src/test/resources/policies/multiple-attribute-uri-templates.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

            BaseResource siteResource = new BaseResource("/site/1");
            siteResource.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getDefaultOrgAttribute() })));
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, siteResource, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultOrgAttribute());

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resources
     * are updated in a batch.
     */
    @Test
    public void testPolicyEvalCacheWhenResourcesChange() throws Exception {
        BaseResource resource1 = new BaseResource("/secured-by-value/sites/sanramon");
        BaseResource resource2 = new BaseResource("/secured-by-value/sites/sanfrancisco");
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest1 = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        PolicyEvaluationRequestV1 policyEvaluationRequest2 = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanfrancisco");
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            resource1.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getDefaultOrgAttribute() })));
            resource2.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getDefaultOrgAttribute() })));
            this.privilegeHelper.postResources(this.acsAdminRestTemplate, endpoint, this.acsZone1Headers, resource1,
                    resource2);
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
            String policyFile = "src/test/resources/policies/single-org-based.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest1, this.acsZone1Headers), PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest2, this.acsZone1Headers), PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            resource1.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getAlternateOrgAttribute() })));
            resource2.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getAlternateOrgAttribute() })));
            this.privilegeHelper.postResources(this.acsAdminRestTemplate, endpoint, this.acsZone1Headers, resource1,
                    resource2);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest1, this.acsZone1Headers), PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest2, this.acsZone1Headers), PolicyEvaluationResult.class);
            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
                this.privilegeHelper.deleteResource(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        resource1.getResourceIdentifier(), this.acsZone1Headers);
                this.privilegeHelper.deleteResource(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        resource2.getResourceIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subject is
     * updated.
     */
    @Test
    public void testPolicyEvalCacheWhenSubjectChanges() throws Exception {
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute());
            String policyFile = "src/test/resources/single-site-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getAlternateAttribute());

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject.getSubjectIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subjects
     * are updated in a batch.
     */
    @Test
    public void testPolicyEvalCacheWhenSubjectsChange() throws Exception {
        BaseSubject subject1 = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest1 = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");

        BaseSubject subject2 = BOB_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest2 = this.policyHelper
                .createEvalRequest(BOB_V1.getSubjectIdentifier(), "sanramon");

        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.postSubjectsWithDefaultAttributes(this.acsAdminRestTemplate, endpoint,
                    this.acsZone1Headers, subject1, subject2);
            String policyFile = "src/test/resources/single-site-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest1, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest2, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            subject1.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getAlternateAttribute() })));
            subject2.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getAlternateAttribute() })));
            this.privilegeHelper.postSubjects(this.acsAdminRestTemplate, endpoint, this.acsZone1Headers, subject1,
                    subject2);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest1, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest2, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject1.getSubjectIdentifier(), this.acsZone1Headers);
                this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        subject2.getSubjectIdentifier(), this.acsZone1Headers);
            }
        }
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subject is
     * deleted.
     */
    @Test
    public void testPolicyEvalCacheWhenSubjectDeleted() throws Exception {
        BaseSubject subject = BOB_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(subject.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        String testPolicyName = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                    this.privilegeHelper.getDefaultAttribute());
            String policyFile = "src/test/resources/policies/single-site-subject-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint,
                    policyFile);

            ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate.postForEntity(
                    endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            PolicyEvaluationResult responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

            this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                    subject.getSubjectIdentifier(), this.acsZone1Headers);

            postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

            Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
            responseBody = postForEntity.getBody();
            Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zoneHelper.getZone1Url(),
                        testPolicyName, this.acsZone1Headers);
            }
        }
    }

    @AfterClass
    public void cleanup() throws Exception {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.policyHelper.deletePolicySets(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
    }
}
