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

package org.eclipse.keti.acs.service.policy.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell;
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache;
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell;
import org.eclipse.keti.acs.model.Condition;
import org.eclipse.keti.acs.model.Policy;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.utils.JsonUtils;

/**
 *
 * @author acs-engineers@ge.com
 */
@Test
@ContextConfiguration(
        classes = { GroovyConditionCache.class, GroovyConditionShell.class, PolicySetValidatorImpl.class })
public class PolicySetValidatorTest extends AbstractTestNGSpringContextTests {

    private final JsonUtils jsonUtils = new JsonUtils();
    @Autowired
    private PolicySetValidator policySetValidator;

    @BeforeClass
    public void setup() {
        ((PolicySetValidatorImpl) this.policySetValidator)
                .setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE");
        ((PolicySetValidatorImpl) this.policySetValidator).init();
    }

    @Test
    public void testSuccesfulSchemaValidation() {

        PolicySet policySet = this.jsonUtils.deserializeFromFile("set-with-2-policy.json", PolicySet.class);

        this.policySetValidator.validatePolicySet(policySet);
    }

    @Test(dataProvider = "invalidPolicyProvider")
    public void testUnsuccessfulSchemaValidation(final String fileName, final String causeSubstring) {

        PolicySet policySet = this.jsonUtils.deserializeFromFile(fileName, PolicySet.class);
        Assert.assertNotNull(policySet);
        try {
            this.policySetValidator.validatePolicySet(policySet);
            Assert.fail("Negative test case should have failed for file " + fileName);
        } catch (PolicySetValidationException e) {
            Assert.assertTrue(e.getMessage().contains(causeSubstring),
                    String.format("Expected %s vs Actual %s", causeSubstring, e.getMessage()));
        }
    }

    @DataProvider(name = "invalidPolicyProvider")
    public Object[][] getInvalidPolicies() {
        Object[][] data = new Object[][] {
                { "policyset/validator/test/missing-effect-policy.json", "missing: [\"effect\"]" },
                { "policyset/validator/test/missing-name-policy-set.json", "missing: [\"name\"]" },
                { "policyset/validator/test/empty-policies-policy-set.json", "/properties/policies" },
                { "policyset/validator/test/no-policies-policy-set.json", "/properties/policies" },
                { "policyset/validator/test/missing-resource-policy-target.json", "missing: [\"resource\"]" },
                { "policyset/validator/test/missing-uritemplate-policy-resource.json", "missing: [\"uriTemplate\"]" },
                { "policyset/validator/test/missing-condition-policy-condition.json", "missing: [\"condition\"]" },
                { "policyset/validator/test/testMatchPolicyWithInvalidAction.json", "Policy Action validation failed" },
                { "policyset/validator/test/testMatchPolicyWithMultipleActionsOneInvalid.json",
                        "Policy Action validation failed" }, };
        return data;
    }

    @Test
    public void testSuccesfulEmptySubjectAttributesSchemaValidation() {

        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/empty-attributes-target-subject.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
    }

    @Test
    public void testSuccesfulEmptyConditionsPolicySchemaValidation() {

        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/empty-conditions-policy.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
    }

    @Test
    public void testSuccesfulMissingConditionNameSchemaValidation() {

        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/missing-condition-name-policy.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
    }

    @Test
    public void testSuccesfulMissingTargetNameSchemaValidation() {

        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/missing-target-name-policy.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
    }

    @Test
    public void testSuccesfulOnlyPolicySetNameAndEffectSchemaValidation() {

        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/policy-set-with-only-name-effect.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
    }

    @Test
    public void testEmptyPolicyConditions() {
        this.policySetValidator.validatePolicyConditions(new ArrayList<Condition>());
    }

    @Test
    public void testNullPolicyConditions() {
        this.policySetValidator.validatePolicyConditions(null);
    }

    @Test
    public void testPolicyConditionsWithOneValidCondition() {
        Condition condition = new Condition("'a'.equals('b')");
        List<ConditionScript> conditionScripts = this.policySetValidator
                .validatePolicyConditions(Arrays.asList(condition));
        Assert.assertEquals(conditionScripts.size(), 1);
        Assert.assertNotNull(conditionScripts.get(0));
    }

    @Test
    public void testPolicyConditionsWithMultipleValidCondition() {
        Condition condition = new Condition("'a'.equals('b')");
        Condition condition2 = new Condition("'a'.equals('c')");
        List<ConditionScript> conditionScripts = this.policySetValidator
                .validatePolicyConditions(Arrays.asList(condition, condition2));
        Assert.assertEquals(conditionScripts.size(), 2);
        Assert.assertNotNull(conditionScripts.get(0));
        Assert.assertNotNull(conditionScripts.get(1));
    }

    @Test(expectedExceptions = { PolicySetValidationException.class })
    public void testPolicyConditionsWithOneInvalidCondition() {
        Condition condition = new Condition("System.exit(0)");
        Condition condition2 = new Condition("'a'.equals('c')");
        this.policySetValidator.validatePolicyConditions(Arrays.asList(condition, condition2));

    }

    @Test
    public void testMatchPolicyWithMultipleActions() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile(
                "policyset/validator/test/testMatchPolicyWithMultipleActions.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);

    }

    @Test
    public void testpolicyWithNullTargetAction() {
        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/policyWithNullTargetAction.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
        Assert.assertNull(policySet.getPolicies().get(0).getTarget().getAction());
    }

    @Test
    public void testpolicyWithEmptyTargetAction() {
        PolicySet policySet = this.jsonUtils
                .deserializeFromFile("policyset/validator/test/policyWithEmptyTargetAction.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
        Assert.assertNull(policySet.getPolicies().get(0).getTarget().getAction());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemovalOfMultipleCachedConditions() {
        PolicySet policySet = this.jsonUtils.deserializeFromFile(
                "policyset/validator/test/multiple-policies-with-multiple-conditions.json", PolicySet.class);
        Assert.assertNotNull(policySet);
        this.policySetValidator.validatePolicySet(policySet);
        GroovyConditionCache conditionCache =
                (GroovyConditionCache) ReflectionTestUtils.getField(this.policySetValidator, "conditionCache");
        Map<String, ConditionShell> cache =
                (Map<String, ConditionShell>) ReflectionTestUtils.getField(conditionCache, "cache");
        int cacheSize = cache.size();
        Assert.assertTrue(cacheSize > 0);
        this.policySetValidator.removeCachedConditions(policySet);
        Assert.assertEquals(cache.size(), cacheSize - 3);
        for (Policy policy : policySet.getPolicies()) {
            for (Condition condition : policy.getConditions()) {
                Assert.assertNull(conditionCache.get(condition.getCondition()));
            }
        }
    }

}
