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

package com.ge.predix.acceptance.test.policy.admin;

import static com.ge.predix.test.utils.PrivilegeHelper.DEFAULT_SUBJECT_ID;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.test.utils.CreatePolicyStatus;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.ZoneHelper;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

/**
 *
 * @author 212338046
 */
@SuppressWarnings({ "nls" })
public class PolicyCreationStepsDefinitions {

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    private String testPolicyName;

    CreatePolicyStatus status;

    @Before
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.zoneHelper.createPrimaryTestZone();
    }

    @Given("^A policy with no action defined$")
    public void a_policy_with_no_action_defined() throws Throwable {
        this.testPolicyName = "no-defined-action-policy-set";
        this.status = this.policyHelper.createPolicySet("src/test/resources/no-defined-action-policy-set.json");
    }

    @Given("^A policy with single valid action defined$")
    public void a_policy_with_single_valid_action_defined() throws Throwable {
        this.testPolicyName = "single-action-defined-policy-set";
        this.status = this.policyHelper.createPolicySet("src/test/resources/single-action-defined-policy-set.json");
    }

    @Given("^A policy with multiple valid actions defined$")
    public void a_policy_with_multiple_valid_actions_defined() throws Throwable {
        this.testPolicyName = "multiple-actions-defined-policy-set";
        this.status = this.policyHelper.createPolicySet("src/test/resources/multiple-actions-defined-policy-set.json");
    }

    @Then("^the policy creation returns (.*)$")
    public void the_policy_creation_returns(final String effect) throws Throwable {
        Assert.assertEquals(this.status.toString(), effect);
    }

    @Given("^A policy with single invalid action defined")
    public void policy_with_single_invalid_action_defined() throws Throwable {
        this.testPolicyName = "single-invalid-action-defined-policy-set";
        this.status = this.policyHelper
                .createPolicySet("src/test/resources/single-invalid-action-defined-policy-set.json");
    }

    @Given("^A policy with multiple actions containing one invalid action defined")
    public void policy_with_multiple_actions_containing_one_invalid_action_defined() throws Throwable {
        this.testPolicyName = "multiple-actions-with-single-invalid-action-defined-policy-set";
        this.status = this.policyHelper.createPolicySet(
                "src/test/resources/multiple-actions-with-single-invalid-action-defined-policy-set.json");
    }

    @After
    public void cleanAfterScenario() {
        this.policyHelper.deletePolicySet(this.testPolicyName);
        this.privilegeHelper.deleteSubject(DEFAULT_SUBJECT_ID);
    }
}
