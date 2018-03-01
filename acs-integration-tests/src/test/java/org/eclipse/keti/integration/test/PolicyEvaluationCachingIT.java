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

import static org.eclipse.keti.integration.test.SubjectResourceFixture.MARISSA_V1;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.model.Effect;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.eclipse.keti.acs.rest.PolicyEvaluationResult;
import org.eclipse.keti.test.TestConfig;
import org.eclipse.keti.test.utils.ACSITSetUpFactory;
import org.eclipse.keti.test.utils.PolicyHelper;
import org.eclipse.keti.test.utils.PrivilegeHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@SuppressWarnings({ "nls" })
public class PolicyEvaluationCachingIT extends AbstractTestNGSpringContextTests {

    private String acsUrl;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    private OAuth2RestTemplate acsAdminRestTemplate;
    private HttpHeaders acsZone1Headers;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.
        this.acsitSetUpFactory.setUp();
        this.acsUrl = this.acsitSetUpFactory.getAcsUrl();
        this.acsZone1Headers = this.acsitSetUpFactory.getZone1Headers();
        this.acsAdminRestTemplate = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();
    }

    @AfterMethod
    public void cleanup() throws Exception {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
        this.policyHelper.deletePolicySets(this.acsAdminRestTemplate, this.acsUrl, this.acsZone1Headers);
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

        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute());
        String policyFile = "src/test/resources/policies/single-site-based.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        policyFile = "src/test/resources/policies/deny-all.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when one of the policies in
     * a multiple policy set evaluation order list changes.
     */
    @Test
    public void testPolicyEvalCacheWithMultiplePolicySets() throws Exception {

        String indeterminatePolicyFile = "src/test/resources/policies/indeterminate.json";
        String denyAllPolicyFile = "src/test/resources/policies/deny-all.json";
        String siteBasedPolicyFile = "src/test/resources/policies/single-site-based.json";
        String endpoint = this.acsUrl;

        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, MARISSA_V1, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute());

        String indeterminatePolicySet = this.policyHelper
                .setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, indeterminatePolicyFile);
        String denyAllPolicySet = this.policyHelper
                .setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, denyAllPolicyFile);

        // test with a valid policy set evaluation order list
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createMultiplePolicySetsEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon",
                        Stream.of(indeterminatePolicySet, denyAllPolicySet)
                                .collect(Collectors.toCollection(LinkedHashSet::new)));

        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.DENY);

        // test with one of the policy sets changed from the evaluation order list
        String siteBasedPolicySet = this.policyHelper
                .setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, siteBasedPolicyFile);
        policyEvaluationRequest = this.policyHelper
                .createMultiplePolicySetsEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon",
                        Stream.of(indeterminatePolicySet, siteBasedPolicySet)
                                .collect(Collectors.toCollection(LinkedHashSet::new)));

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is created.
     */
    @Test
    public void testPolicyEvalCacheWhenResourceAdded() throws Exception {
        String endpoint = this.acsUrl;

        // create test subject
        BaseSubject subject = MARISSA_V1;
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());

        // create test policy set
        String policyFile = "src/test/resources/policies/single-org-based.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        // post policy evaluation request
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

        // at this point evaluation decision is cached
        // timestamps for subject and resource involved in the decision are also cached even though resource doesn't
        // exist yet

        // add a resource which is expected to reset resource cached timestamp and invalidate cached decision
        BaseResource resource = new BaseResource("/secured-by-value/sites/sanramon");
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultOrgAttribute());

        // post policy evaluation request; decision should be reevaluated.
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is updated with different attributes.
     */
    @Test
    public void testPolicyEvalCacheWhenResourceChanges() throws Exception {
        BaseResource resource = new BaseResource("/secured-by-value/sites/sanramon");
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultOrgAttribute());
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
        String policyFile = "src/test/resources/policies/single-org-based.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        // update resource with different attributes
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getAlternateOrgAttribute());

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subject
     * is created.
     */
    @Test
    public void testPolicyEvalCacheWhenSubjectAdded() throws Exception {
        String endpoint = this.acsUrl;

        //create test resource
        BaseResource resource = new BaseResource("/secured-by-value/sites/sanramon");
        this.privilegeHelper.putResource(this.acsAdminRestTemplate, resource, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute());

        // create test policy set
        String policyFile = "src/test/resources/policies/single-site-based.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        // post policy evaluation request
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);

        // at this point evaluation decision is cached
        // timestamps for resource and subject involved in the decision are also cached even though subject doesn't
        // exist yet

        // add a subject which is expected to reset subject cached timestamp and invalidate cached decision
        BaseSubject subject = MARISSA_V1;
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultAttribute());

        // post policy evaluation request; decision should be reevaluated.
        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subject is
     * updated or deleted.
     */
    @Test
    public void testPolicyEvalCacheWhenSubjectChanges() throws Exception {
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        String endpoint = this.acsUrl;

        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, MARISSA_V1, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute());
        String policyFile = "src/test/resources/single-site-based-policy-set.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                        new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.PERMIT);

        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, MARISSA_V1, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getAlternateAttribute());

        postForEntity = this.acsAdminRestTemplate.postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult.class);

        Assert.assertEquals(postForEntity.getStatusCode(), HttpStatus.OK);
        responseBody = postForEntity.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.NOT_APPLICABLE);
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when a policy has multiple
     * resource attribute URI templates that match the request.
     */
    @Test
    public void testPolicyWithMultAttrUriTemplatatesEvalCache() throws Exception {
        BaseSubject subject = MARISSA_V1;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest("GET", MARISSA_V1.getSubjectIdentifier(), "/v1/site/1/plant/asset/1", null);
        String endpoint = this.acsUrl;

        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, endpoint, this.acsZone1Headers,
                this.privilegeHelper.getDefaultAttribute(), this.privilegeHelper.getDefaultOrgAttribute());
        String policyFile = "src/test/resources/policies/multiple-attribute-uri-templates.json";
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, this.acsZone1Headers, endpoint, policyFile);

        ResponseEntity<PolicyEvaluationResult> postForEntity = this.acsAdminRestTemplate
                .postForEntity(endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
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

    }

    @AfterClass
    public void destroy() {
        this.acsitSetUpFactory.destroy();
    }

}
