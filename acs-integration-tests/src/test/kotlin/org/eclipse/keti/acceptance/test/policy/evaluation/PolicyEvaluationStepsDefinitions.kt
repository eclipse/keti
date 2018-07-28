/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acceptance.test.policy.evaluation

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.DEFAULT_ACTION
import org.eclipse.keti.test.utils.DEFAULT_ATTRIBUTE_ISSUER
import org.eclipse.keti.test.utils.DEFAULT_RESOURCE_IDENTIFIER
import org.eclipse.keti.test.utils.DEFAULT_SUBJECT_ID
import org.eclipse.keti.test.utils.DEFAULT_SUBJECT_IDENTIFIER
import org.eclipse.keti.test.utils.NOT_MATCHING_ACTION
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.Assert
import java.io.IOException
import java.util.HashSet

/**
 * BDD tests for Policy Evaluation service.
 *
 * @author acs-engineers@ge.com
 */
class PolicyEvaluationStepsDefinitions : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var testPolicyName: String? = null
    private var policyEvaluationResponse: ResponseEntity<PolicyEvaluationResult>? = null
    private var acsUrl: String? = null
    private var zone1Headers: HttpHeaders? = null
    private var acsAdminRestTemplate: OAuth2RestTemplate? = null

    @Before
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()
        this.acsUrl = this.acsitSetUpFactory.acsUrl
        this.zone1Headers = this.acsitSetUpFactory.zone1Headers
        this.acsAdminRestTemplate = this.acsitSetUpFactory.acsZoneAdminRestTemplate
    }

    /*
     * Scenario: policy evaluation request which returns permit Given I have a Given A policy set that allows access
     * to all When Any evaluation request Then policy evaluation returns PERMIT
     */
    @Given("^A policy set that allows access to all$")
    @Throws(Throwable::class)
    fun policyDefinitionAllowsAccessToAll() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/permit-all.json"
        )
    }

    /*
     * Scenario: policy evaluation request which returns deny Given A policy set that allows access to none When Any
     * evaluation request Then policy evaluation returns DENY
     */
    @Given("^A policy set that allows access to none$")
    @Throws(Throwable::class)
    fun policyDefinitionAllowsAccessToNone() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/deny-all.json"
        )
    }

    @When("^Any evaluation request$")
    @Throws(Throwable::class)
    fun anyEvaluationRequest() {
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createRandomEvalRequest()
        )
    }

    @Then("^policy evaluation returns (.*)$")
    @Throws(Throwable::class)
    fun policyEvaluationReturns(effect: String) {
        Assert.assertEquals(this.policyEvaluationResponse!!.statusCode, HttpStatus.OK)
        val responseBody = this.policyEvaluationResponse!!.body
        Assert.assertEquals(responseBody.effect, Effect.valueOf(effect))
    }

    @Then("^policy evaluation response includes subject attribute (.*) with the value (.*)$")
    @Throws(Throwable::class)
    fun policyEvaluationReturns(
        attributeName: String,
        attributeValue: String
    ) {
        Assert.assertEquals(this.policyEvaluationResponse!!.statusCode, HttpStatus.OK)
        val subjectAttributes = HashSet(
            this.policyEvaluationResponse!!.body.subjectAttributes
        )
        Assert.assertTrue(
            subjectAttributes.contains(Attribute(DEFAULT_ATTRIBUTE_ISSUER, attributeName, attributeValue)),
            String.format(
                "Subject Attributes expected to include attribute = (%s, %s, %s)",
                DEFAULT_ATTRIBUTE_ISSUER, attributeName, attributeValue
            )
        )
    }

    @After
    @Throws(Exception::class)
    fun cleanAfterScenario() {
        this.policyHelper.deletePolicySet(
            this.acsAdminRestTemplate!!, this.acsUrl!!, this.testPolicyName,
            this.zone1Headers!!
        )
        this.privilegeHelper.deleteSubject(
            this.acsAdminRestTemplate!!, this.acsUrl!!, DEFAULT_SUBJECT_ID,
            this.zone1Headers
        )
        this.acsitSetUpFactory.destroy()
    }

    /*
     * Scenario: policy evaluation request which returns deny Given A policy set that allows access only to subject
     * with role administrator When Evaluation request which has the subject attribute role with the value
     * administrator Then policy evaluation returns PERMIT
     */
    @Given("^A policy set that allows access only to subject with role (.*) using condition$")
    @Throws(Throwable::class)
    fun policyDefinitionAllowsAccessToOnlyAdminsUsingCondition(subjectAttribute: String) {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/permit-admin-only-using-condition-policy-set.json"
        )
    }

    @When("^Evaluation request which has the subject attribute role with the value (.*)$")
    @Throws(Throwable::class)
    fun evaluationRequestWithSubjectAttribute(subjectAttribute: String) {

        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute(DEFAULT_ATTRIBUTE_ISSUER, "role", subjectAttribute))
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                DEFAULT_ACTION, DEFAULT_SUBJECT_ID,
                DEFAULT_RESOURCE_IDENTIFIER, subjectAttributes
            )
        )
    }

    @When("^Evaluation request for resource (.*) which has the subject attribute (.*) with the value (.*)$")
    @Throws(Throwable::class)
    fun evaluationRequestForResourceWithSubjectAttribute(
        resourceIdentifier: String,
        attributeName: String,
        attributeValue: String
    ) {

        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute(DEFAULT_ATTRIBUTE_ISSUER, attributeName, attributeValue))
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                DEFAULT_ACTION, DEFAULT_SUBJECT_ID,
                resourceIdentifier, subjectAttributes
            )
        )
    }

    /*
     * Given A policy set that allows access only to subject with role administrator using matcher*
     */
    @Given("^A policy set that allows access only to subject with role .* using matcher$")
    @Throws(Throwable::class)
    fun policyDefinitionAllowsAccessToOnlyAdminsUsingMatcher() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/permit-admin-only-using-matcher-policy-set.json"
        )
    }

    @Given("^ACS has subject attribute (.*) with value (.*) for the subject$")
    @Throws(Throwable::class)
    fun acs_has_subject_attribute(
        attributeName: String,
        value: String
    ) {
        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, DEFAULT_SUBJECT_IDENTIFIER, this.zone1Headers!!,
            Attribute(DEFAULT_ATTRIBUTE_ISSUER, attributeName, value)
        )
    }

    @When("^Evaluation request which has no subject attribute$")
    @Throws(Throwable::class)
    fun evaluation_request_which_has_no_subject_attribute() {
        val subjectAttributes = emptySet<Attribute>()
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                DEFAULT_ACTION, DEFAULT_SUBJECT_ID,
                DEFAULT_RESOURCE_IDENTIFIER, subjectAttributes
            )
        )
    }

    @Given("^A policy set that allows access only to subject with role administrator and site sanramon$")
    @Throws(Throwable::class)
    fun a_policy_set_that_allows_access_only_to_subject_with_role_administrator_and_site_sanramon() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/permit-admin-with-site-access-matcher-and-condition.json"
        )
    }

    @Given("^an existing policy with no defined action$")
    @Throws(Throwable::class)
    fun an_existing_policy_with_no_defined_action() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/no-defined-action-policy-set.json"
        )
    }

    @Given("^an existing policy set stored in ACS with multiple actions$")
    @Throws(Throwable::class)
    fun an_existing_policy_set_stored_in_ACS_with_multiple_actions() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/multiple-actions-defined-policy-set.json"
        )
    }

    @Given("^an existing policy set stored in ACS with a single action$")
    @Throws(Throwable::class)
    fun an_existing_policy_set_stored_in_ACS_with_a_single_action() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/single-action-defined-policy-set.json"
        )
    }

    @Given("^an existing policy with empty defined action$")
    @Throws(Throwable::class)
    fun an_existing_policy_with_empty_defined_action() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/single-empty-action-defined-policy-set.json"
        )
    }

    @Given("^an existing policy with null defined action$")
    @Throws(Throwable::class)
    fun an_existing_policy_with_null_defined_action() {
        this.testPolicyName = this.policyHelper.setTestPolicy(
            this.acsAdminRestTemplate!!, this.zone1Headers!!, this.acsUrl!!,
            "src/test/resources/single-null-action-defined-policy-set.json"
        )
    }

    @When("^A policy evaluation is requested with any HTTP action$")
    @Throws(Throwable::class)
    fun a_policy_evaluation_is_requested_with_any_HTTP_action() {
        val subjectAttributes = emptySet<Attribute>()
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                DEFAULT_ACTION, DEFAULT_SUBJECT_ID,
                DEFAULT_RESOURCE_IDENTIFIER, subjectAttributes
            )
        )
    }

    @When("^A policy evaluation is requested with an HTTP action matching .*$")
    @Throws(Throwable::class)
    fun a_policy_evaluation_is_requested_with_an_HTTP_action_matching() {
        val subjectAttributes = emptySet<Attribute>()
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                DEFAULT_ACTION, DEFAULT_SUBJECT_ID,
                DEFAULT_RESOURCE_IDENTIFIER, subjectAttributes
            )
        )
    }

    @When("^A policy evaluation is requested with an HTTP action not matching .*$")
    @Throws(Throwable::class)
    fun a_policy_evaluation_is_requested_with_an_HTTP_action_not_matching() {
        val subjectAttributes = emptySet<Attribute>()
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                NOT_MATCHING_ACTION, DEFAULT_SUBJECT_ID,
                DEFAULT_RESOURCE_IDENTIFIER, subjectAttributes
            )
        )
    }

    @When("^Evaluation request which has subject attribute which are null$")
    @Throws(Throwable::class)
    fun evaluation_request_which_has_subject_attribute_which_are_null() {
        this.policyEvaluationResponse = this.policyHelper.sendEvaluationRequest(
            this.acsAdminRestTemplate!!,
            this.zone1Headers!!, this.policyHelper.createEvalRequest(
                NOT_MATCHING_ACTION, DEFAULT_SUBJECT_ID,
                DEFAULT_RESOURCE_IDENTIFIER, null
            )
        )
    }
}
