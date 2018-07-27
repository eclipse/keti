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

package org.eclipse.keti.acs.service.policy.validation

import org.eclipse.keti.acs.commons.policy.condition.ConditionShell
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.model.Condition
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.utils.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.ArrayList

/**
 * @author acs-engineers@ge.com
 */
@Test
@ContextConfiguration(
    classes = [(GroovyConditionCache::class), (GroovyConditionShell::class), (PolicySetValidatorImpl::class)]
)
class PolicySetValidatorTest : AbstractTestNGSpringContextTests() {

    private val jsonUtils = JsonUtils()

    @Autowired
    private lateinit var policySetValidator: PolicySetValidator

    @DataProvider(name = "invalidPolicyProvider")
    fun invalidPolicies(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(
            arrayOf("policyset/validator/test/missing-effect-policy.json", "missing: [\"effect\"]"),
            arrayOf("policyset/validator/test/missing-name-policy-set.json", "missing: [\"name\"]"),
            arrayOf("policyset/validator/test/empty-policies-policy-set.json", "/properties/policies"),
            arrayOf("policyset/validator/test/no-policies-policy-set.json", "/properties/policies"),
            arrayOf("policyset/validator/test/missing-resource-policy-target.json", "missing: [\"resource\"]"),
            arrayOf("policyset/validator/test/missing-uritemplate-policy-resource.json", "missing: [\"uriTemplate\"]"),
            arrayOf("policyset/validator/test/missing-condition-policy-condition.json", "missing: [\"condition\"]"),
            arrayOf(
                "policyset/validator/test/testMatchPolicyWithInvalidAction.json",
                "Policy Action validation failed"
            ),
            arrayOf(
                "policyset/validator/test/testMatchPolicyWithMultipleActionsOneInvalid.json",
                "Policy Action validation failed"
            )
        )
    }

    @BeforeClass
    fun setup() {
        val policySetValidatorImpl = (this.policySetValidator as PolicySetValidatorImpl)
        policySetValidatorImpl.setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE")
        policySetValidatorImpl.init()
    }

    @Test
    fun testSuccesfulSchemaValidation() {

        val policySet = this.jsonUtils.deserializeFromFile("set-with-2-policy.json", PolicySet::class.java)

        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test(dataProvider = "invalidPolicyProvider")
    fun testUnsuccessfulSchemaValidation(
        fileName: String,
        causeSubstring: String
    ) {

        val policySet = this.jsonUtils.deserializeFromFile(fileName, PolicySet::class.java)
        Assert.assertNotNull(policySet)
        try {
            this.policySetValidator.validatePolicySet(policySet!!)
            Assert.fail("Negative test case should have failed for file $fileName")
        } catch (e: PolicySetValidationException) {
            Assert.assertTrue(
                e.message!!.contains(causeSubstring),
                String.format("Expected %s vs Actual %s", causeSubstring, e.message)
            )
        }
    }

    @Test
    fun testSuccesfulEmptySubjectAttributesSchemaValidation() {

        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/empty-attributes-target-subject.json", PolicySet::class.java)
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test
    fun testSuccesfulEmptyConditionsPolicySchemaValidation() {

        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/empty-conditions-policy.json", PolicySet::class.java)
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test
    fun testSuccesfulMissingConditionNameSchemaValidation() {

        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/missing-condition-name-policy.json", PolicySet::class.java)
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test
    fun testSuccesfulMissingTargetNameSchemaValidation() {

        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/missing-target-name-policy.json", PolicySet::class.java)
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test
    fun testSuccesfulOnlyPolicySetNameAndEffectSchemaValidation() {

        val policySet = this.jsonUtils
            .deserializeFromFile(
                "policyset/validator/test/policy-set-with-only-name-effect.json",
                PolicySet::class.java
            )
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test
    fun testEmptyPolicyConditions() {
        this.policySetValidator.validatePolicyConditions(ArrayList())
    }

    @Test
    fun testNullPolicyConditions() {
        this.policySetValidator.validatePolicyConditions(null)
    }

    @Test
    fun testPolicyConditionsWithOneValidCondition() {
        val condition = Condition("'a'.equals('b')")
        val conditionScripts = this.policySetValidator
            .validatePolicyConditions(listOf(condition))
        Assert.assertEquals(conditionScripts.size, 1)
        Assert.assertNotNull(conditionScripts[0])
    }

    @Test
    fun testPolicyConditionsWithMultipleValidCondition() {
        val condition = Condition("'a'.equals('b')")
        val condition2 = Condition("'a'.equals('c')")
        val conditionScripts = this.policySetValidator
            .validatePolicyConditions(listOf(condition, condition2))
        Assert.assertEquals(conditionScripts.size, 2)
        Assert.assertNotNull(conditionScripts[0])
        Assert.assertNotNull(conditionScripts[1])
    }

    @Test(expectedExceptions = [(PolicySetValidationException::class)])
    fun testPolicyConditionsWithOneInvalidCondition() {
        val condition = Condition("System.exit(0)")
        val condition2 = Condition("'a'.equals('c')")
        this.policySetValidator.validatePolicyConditions(listOf(condition, condition2))
    }

    @Test
    fun testMatchPolicyWithMultipleActions() {
        val policySet = this.jsonUtils.deserializeFromFile(
            "policyset/validator/test/testMatchPolicyWithMultipleActions.json", PolicySet::class.java
        )
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
    }

    @Test
    fun testpolicyWithNullTargetAction() {
        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/policyWithNullTargetAction.json", PolicySet::class.java)
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
        Assert.assertNull(policySet.policies[0].target!!.action)
    }

    @Test
    fun testpolicyWithEmptyTargetAction() {
        val policySet = this.jsonUtils
            .deserializeFromFile("policyset/validator/test/policyWithEmptyTargetAction.json", PolicySet::class.java)
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
        Assert.assertNull(policySet.policies[0].target!!.action)
    }

    @Test
    fun testRemovalOfMultipleCachedConditions() {
        val policySet = this.jsonUtils.deserializeFromFile(
            "policyset/validator/test/multiple-policies-with-multiple-conditions.json", PolicySet::class.java
        )
        Assert.assertNotNull(policySet)
        this.policySetValidator.validatePolicySet(policySet!!)
        val conditionCache =
            ReflectionTestUtils.getField(this.policySetValidator, "conditionCache") as GroovyConditionCache
        val cache = ReflectionTestUtils.getField(conditionCache, "cache") as Map<String, ConditionShell>
        val cacheSize = cache.size
        Assert.assertTrue(cacheSize > 0)
        this.policySetValidator.removeCachedConditions(policySet)
        Assert.assertEquals(cache.size, cacheSize - 3)
        for (policy in policySet.policies) {
            for (condition in policy.conditions) {
                Assert.assertNull(conditionCache[condition.condition!!])
            }
        }
    }
}
