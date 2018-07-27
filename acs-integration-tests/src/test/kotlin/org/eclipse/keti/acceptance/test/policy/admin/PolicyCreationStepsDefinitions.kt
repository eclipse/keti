/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acceptance.test.policy.admin

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.CreatePolicyStatus
import org.eclipse.keti.test.utils.DEFAULT_SUBJECT_ID
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.springframework.beans.factory.annotation.Autowired
import org.testng.Assert
import java.io.IOException

/**
 *
 * @author acs-engineers@ge.com
 */
class PolicyCreationStepsDefinitions {

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var testPolicyName: String? = null

    private lateinit var status: CreatePolicyStatus

    @Before
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        this.acsitSetUpFactory.setUp()
    }

    @Given("^A policy with no action defined$")
    @Throws(Throwable::class)
    fun a_policy_with_no_action_defined() {
        this.testPolicyName = "no-defined-action-policy-set"
        this.status = this.policyHelper.createPolicySet(
            "src/test/resources/no-defined-action-policy-set.json",
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, this.acsitSetUpFactory.zone1Headers
        )
    }

    @Given("^A policy with single valid action defined$")
    @Throws(Throwable::class)
    fun a_policy_with_single_valid_action_defined() {
        this.testPolicyName = "single-action-defined-policy-set"
        this.status = this.policyHelper.createPolicySet(
            "src/test/resources/single-action-defined-policy-set.json",
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, this.acsitSetUpFactory.zone1Headers
        )
    }

    @Given("^A policy with multiple valid actions defined$")
    @Throws(Throwable::class)
    fun a_policy_with_multiple_valid_actions_defined() {
        this.testPolicyName = "multiple-actions-defined-policy-set"
        this.status = this.policyHelper.createPolicySet(
            "src/test/resources/multiple-actions-defined-policy-set.json",
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, this.acsitSetUpFactory.zone1Headers
        )
    }

    @Then("^the policy creation returns (.*)$")
    @Throws(Throwable::class)
    fun the_policy_creation_returns(effect: String) {
        Assert.assertEquals(this.status.toString(), effect)
    }

    @Given("^A policy with single invalid action defined")
    @Throws(Throwable::class)
    fun policy_with_single_invalid_action_defined() {
        this.testPolicyName = "single-invalid-action-defined-policy-set"
        this.status = this.policyHelper.createPolicySet(
            "src/test/resources/single-invalid-action-defined-policy-set.json",
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, this.acsitSetUpFactory.zone1Headers
        )
    }

    @Given("^A policy with multiple actions containing one invalid action defined")
    @Throws(Throwable::class)
    fun policy_with_multiple_actions_containing_one_invalid_action_defined() {
        this.testPolicyName = "multiple-actions-with-single-invalid-action-defined-policy-set"
        this.status = this.policyHelper.createPolicySet(
            "src/test/resources/multiple-actions-with-single-invalid-action-defined-policy-set.json",
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, this.acsitSetUpFactory.zone1Headers
        )
    }

    @After
    @Throws(Exception::class)
    fun cleanAfterScenario() {
        val acsBaseUrl = this.acsitSetUpFactory.acsUrl
        this.policyHelper.deletePolicySet(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, acsBaseUrl,
            this.testPolicyName, this.acsitSetUpFactory.zone1Headers
        )
        this.privilegeHelper.deleteSubject(
            this.acsitSetUpFactory.acsZoneAdminRestTemplate, acsBaseUrl,
            DEFAULT_SUBJECT_ID, this.acsitSetUpFactory.zone1Headers
        )
        this.acsitSetUpFactory.destroy()
    }
}
