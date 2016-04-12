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

package com.ge.predix.acceptance.test.policy.evaluation;

import static com.ge.predix.test.utils.PolicyHelper.DEFAULT_ACTION;
import static com.ge.predix.test.utils.PolicyHelper.NOT_MATCHING_ACTION;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_ATTRIBUTE_ISSUER;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_RESOURCE_IDENTIFIER;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_SUBJECT_ID;
import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_SUBJECT_IDENTIFIER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * BDD tests for Policy Evaluation service.
 *
 * @author 212406427
 */
@SuppressWarnings({ "nls" })
public class PolicyEvaluationStepsDefinitions extends AbstractTestNGSpringContextTests {

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

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

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    private String testPolicyName;
    private ResponseEntity<PolicyEvaluationResult> policyEvaluationResponse;
    private String zone1Url;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private boolean registerWithZac;

    @Before
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.zone1Url = this.zoneHelper.getZone1Url();
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.registerWithZac = true;
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.uaaUrl);
        uaaTestUtil.setup(Arrays.asList(new String[] { this.acsZone1Name }));

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.registerWithZac = false;
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

    /*
     * Scenario: policy evaluation request which returns permit Given I have a Given A policy set that allows access
     * to all When Any evaluation request Then policy evaluation returns PERMIT
     */
    @Given("^A policy set that allows access to all$")
    public void policyDefinitionAllowsAccessToAll() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/permit-all.json");
    }

    /*
     * Scenario: policy evaluation request which returns deny Given A policy set that allows access to none When Any
     * evaluation request Then policy evaluation returns DENY
     */
    @Given("^A policy set that allows access to none$")
    public void policyDefinitionAllowsAccessToNone() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/deny-all.json");
    }

    @When("^Any evaluation request$")
    public void anyEvaluationRequest() throws Throwable {
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createRandomEvalRequest());
    }

    @Then("^policy evaluation returns (.*)$")
    public void policyEvaluationReturns(final String effect) throws Throwable {
        Assert.assertEquals(this.policyEvaluationResponse.getStatusCode(), HttpStatus.OK);
        PolicyEvaluationResult responseBody = this.policyEvaluationResponse.getBody();
        Assert.assertEquals(responseBody.getEffect(), Effect.valueOf(effect));
    }

    @Then("^policy evaluation response includes subject attribute (.*) with the value (.*)$")
    public void policyEvaluationReturns(final String attributeName, final String attributeValue) throws Throwable {
        Assert.assertEquals(this.policyEvaluationResponse.getStatusCode(), HttpStatus.OK);
        Set<Attribute> subjectAttributes = new HashSet<Attribute>(
                this.policyEvaluationResponse.getBody().getSubjectAttributes());
        Assert.assertTrue(
                subjectAttributes.contains(new Attribute(DEFAULT_ATTRIBUTE_ISSUER, attributeName, attributeValue)),
                String.format("Subject Attributes expected to include attribute = (%s, %s, %s)",
                        DEFAULT_ATTRIBUTE_ISSUER, attributeName, attributeValue));
    }

    @After
    public void cleanAfterScenario() throws Exception {
        this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.zone1Url, this.testPolicyName);
        this.privilegeHelper.deleteSubject(this.acsAdminRestTemplate, this.zone1Url, DEFAULT_SUBJECT_ID);
    }

    /*
     * Scenario: policy evaluation request which returns deny Given A policy set that allows access only to subject
     * with role administrator When Evaluation request which has the subject attribute role with the value
     * administrator Then policy evaluation returns PERMIT
     */
    @Given("^A policy set that allows access only to subject with role (.*) using condition$")
    public void policyDefinitionAllowsAccessToOnlyAdminsUsingCondition(final String subjectAttribute) throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/permit-admin-only-using-condition-policy-set.json");
    }

    @When("^Evaluation request which has the subject attribute role with the value (.*)$")
    public void evaluationRequestWithSubjectAttribute(final String subjectAttribute) throws Throwable {

        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        subjectAttributes.add(new Attribute(DEFAULT_ATTRIBUTE_ISSUER, "role", subjectAttribute));
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(DEFAULT_ACTION, DEFAULT_SUBJECT_ID, DEFAULT_RESOURCE_IDENTIFIER,
                        subjectAttributes));
    }

    @When("^Evaluation request for resource (.*) which has the subject attribute (.*) with the value (.*)$")
    public void evaluationRequestForResourceWithSubjectAttribute(final String resourceIdentifier,
            final String attributeName, final String attributeValue) throws Throwable {

        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        subjectAttributes.add(new Attribute(DEFAULT_ATTRIBUTE_ISSUER, attributeName, attributeValue));
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(DEFAULT_ACTION, DEFAULT_SUBJECT_ID, resourceIdentifier,
                        subjectAttributes));
    }

    /*
     * Given A policy set that allows access only to subject with role administrator using matcher*
     */
    @Given("^A policy set that allows access only to subject with role .* using matcher$")
    public void policyDefinitionAllowsAccessToOnlyAdminsUsingMatcher() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/permit-admin-only-using-matcher-policy-set.json");
    }

    @Given("^ACS has subject attribute (.*) with value (.*) for the subject$")
    public void acs_has_subject_attribute(final String attributeName, final String value) throws Throwable {
        this.privilegeHelper.putSubject(this.acsAdminRestTemplate, DEFAULT_SUBJECT_IDENTIFIER,
                new Attribute(DEFAULT_ATTRIBUTE_ISSUER, attributeName, value));
    }

    @When("^Evaluation request which has no subject attribute$")
    public void evaluation_request_which_has_no_subject_attribute() throws Throwable {
        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(DEFAULT_ACTION, DEFAULT_SUBJECT_ID, DEFAULT_RESOURCE_IDENTIFIER,
                        subjectAttributes));
    }

    @Given("^A policy set that allows access only to subject with role administrator and site sanramon$")
    public void a_policy_set_that_allows_access_only_to_subject_with_role_administrator_and_site_sanramon()
            throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/permit-admin-with-site-access-matcher-and-condition.json");
    }

    @Given("^an existing policy with no defined action$")
    public void an_existing_policy_with_no_defined_action() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/no-defined-action-policy-set.json");
    }

    @Given("^an existing policy set stored in ACS with multiple actions$")
    public void an_existing_policy_set_stored_in_ACS_with_multiple_actions() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/multiple-actions-defined-policy-set.json");
    }

    @Given("^an existing policy set stored in ACS with a single action$")
    public void an_existing_policy_set_stored_in_ACS_with_a_single_action() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/single-action-defined-policy-set.json");
    }

    @Given("^an existing policy with empty defined action$")
    public void an_existing_policy_with_empty_defined_action() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/single-empty-action-defined-policy-set.json");
    }

    @Given("^an existing policy with null defined action$")
    public void an_existing_policy_with_null_defined_action() throws Throwable {
        this.testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, null, this.zone1Url,
                "src/test/resources/single-null-action-defined-policy-set.json");
    }

    @When("^A policy evaluation is requested with any HTTP action$")
    public void a_policy_evaluation_is_requested_with_any_HTTP_action() throws Throwable {
        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(DEFAULT_ACTION, DEFAULT_SUBJECT_ID, DEFAULT_RESOURCE_IDENTIFIER,
                        subjectAttributes));
    }

    @When("^A policy evaluation is requested with an HTTP action matching .*$")
    public void a_policy_evaluation_is_requested_with_an_HTTP_action_matching() throws Throwable {
        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(DEFAULT_ACTION, DEFAULT_SUBJECT_ID, DEFAULT_RESOURCE_IDENTIFIER,
                        subjectAttributes));
    }

    @When("^A policy evaluation is requested with an HTTP action not matching .*$")
    public void a_policy_evaluation_is_requested_with_an_HTTP_action_not_matching() throws Throwable {
        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(NOT_MATCHING_ACTION, DEFAULT_SUBJECT_ID,
                        DEFAULT_RESOURCE_IDENTIFIER, subjectAttributes));
    }

    @When("^Evaluation request which has subject attribute which are null$")
    public void evaluation_request_which_has_subject_attribute_which_are_null() throws Throwable {
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(this.acsAdminRestTemplate,
                this.policyHelper.createEvalRequest(NOT_MATCHING_ACTION, DEFAULT_SUBJECT_ID,
                        DEFAULT_RESOURCE_IDENTIFIER, null));
    }

}
