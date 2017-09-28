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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.utils.ACSITSetUpFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@SuppressWarnings({ "nls" })
public class PolicyEvalCachingWithGraphDBIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    private OAuth2RestTemplate acsAdminRestTemplate;

    private HttpHeaders acsZone1Headers;

    private static final String ISSUER_URI = "acs.example.org";
    private static final Attribute TOP_SECRET_CLASSIFICATION = new Attribute(ISSUER_URI, "classification",
            "top secret");
    private static final Attribute SPECIAL_AGENTS_GROUP_ATTRIBUTE = new Attribute(ISSUER_URI, "group",
            "special-agents");

    private static final String FBI = "fbi";
    private static final String SPECIAL_AGENTS_GROUP = "special-agents";
    private static final String AGENT_MULDER = "mulder";
    private static final String AGENT_SCULLY = "scully";
    public static final String EVIDENCE_SCULLYS_TESTIMONY_ID = "/evidence/scullys-testimony";
    private String acsUrl;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.acsitSetUpFactory.setUp();
        this.acsZone1Headers = this.acsitSetUpFactory.getZone1Headers();
        this.acsAdminRestTemplate = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();
        this.acsUrl = this.acsitSetUpFactory.getAcsUrl();
    }

    @AfterMethod
    public void cleanup() throws Exception {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.policyHelper.deletePolicySets(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated for a subject and its
     * descendants, when attributes are changed for the parent subject.
     */
    @Test
    public void testPolicyEvalCacheInvalidationWhenSubjectParentChanges() throws Exception {
        BaseSubject fbi = new BaseSubject(this.FBI);

        BaseSubject specialAgentsGroup = new BaseSubject(this.SPECIAL_AGENTS_GROUP);
        specialAgentsGroup
                .setParents(new HashSet<>(Arrays.asList(new Parent[] { new Parent(fbi.getSubjectIdentifier()) })));

        BaseSubject agentMulder = new BaseSubject(this.AGENT_MULDER);
        agentMulder.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(specialAgentsGroup.getSubjectIdentifier()) })));

        BaseSubject agentScully = new BaseSubject(this.AGENT_SCULLY);
        agentScully.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(specialAgentsGroup.getSubjectIdentifier()) })));

        BaseResource scullysTestimony = new BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID);

        PolicyEvaluationRequestV1 mulderPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest("GET", agentMulder.getSubjectIdentifier(), EVIDENCE_SCULLYS_TESTIMONY_ID, null);
        PolicyEvaluationRequestV1 scullyPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest("GET", agentScully.getSubjectIdentifier(), EVIDENCE_SCULLYS_TESTIMONY_ID, null);

        String endpoint = this.acsUrl;

        // Set up fbi <-- specialAgentsGroup <-- (agentMulder, agentScully) subject hierarchy
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, fbi, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, specialAgentsGroup, endpoint, this.acsZone1Headers,
                this.SPECIAL_AGENTS_GROUP_ATTRIBUTE);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, agentMulder, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, agentScully, endpoint, this.acsZone1Headers);

        // Set up resource
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, scullysTestimony, endpoint, this.acsZone1Headers,
                this.SPECIAL_AGENTS_GROUP_ATTRIBUTE, this.TOP_SECRET_CLASSIFICATION);

        // Set up policy
        String policyFile = "src/test/resources/policies/complete-sample-policy-set-2.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        // Verify that policy is evaluated to DENY since top secret classification is not set
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(mulderPolicyEvaluationRequest, this.acsZone1Headers),
                        PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(scullyPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        // Change parent subject to add top secret classification
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, specialAgentsGroup, endpoint, this.acsZone1Headers,
                this.SPECIAL_AGENTS_GROUP_ATTRIBUTE, this.TOP_SECRET_CLASSIFICATION);

        // Verify that policy is evaluated to PERMIT since top secret classification is now propogated from the parent
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(mulderPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(scullyPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);
        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated for a resource and its
     * descendants, when attributes are changed for the parent resource.
     */
    @Test
    public void testPolicyEvalCacheInvalidationWhenResourceParentChanges() throws Exception {
        BaseResource grandparentResource = new BaseResource("/secured-by-value/sites/east-bay");
        BaseResource parentResource = new BaseResource("/secured-by-value/sites/sanramon");
        BaseResource childResource = new BaseResource("/secured-by-value/sites/basement");

        parentResource.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(grandparentResource.getResourceIdentifier()) })));

        childResource.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(parentResource.getResourceIdentifier()) })));

        BaseSubject agentMulder = new BaseSubject(this.AGENT_MULDER);

        PolicyEvaluationRequestV1 sanramonPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest(agentMulder.getSubjectIdentifier(), "sanramon");

        PolicyEvaluationRequestV1 basementPolicyEvaluationRequest = this.policyHelper
                .createEvalRequest(agentMulder.getSubjectIdentifier(), "basement");

        String endpoint = this.acsUrl;

        this.privilegeHelper.putResource(this.acsAdminRestTemplate, grandparentResource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultOrgAttribute());
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, parentResource, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, childResource, endpoint, this.acsZone1Headers);
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, agentMulder, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());

        String policyFile = "src/test/resources/policies/single-org-based.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        // Subject policy evaluation request for site "sanramon"
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(sanramonPolicyEvaluationRequest, this.acsZone1Headers),
                        PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        // Subject policy evaluation request for site "basement"
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(basementPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        // Change grandparent resource attributes from DefaultOrgAttribute to AlternateOrgAttribute
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, grandparentResource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getAlternateOrgAttribute());

        // Subject policy evaluation request for site "sanramon"
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(sanramonPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

        // Subject policy evaluation request for site "basement"
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(basementPolicyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
    }

    @AfterClass
    public void destroy() {
        this.acsitSetUpFactory.destroy();
    }

}
