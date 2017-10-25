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

    private static JsonUtils jsonUtils = new JsonUtils();
    @Autowired
    private PolicySetValidator policySetValidator;

    @BeforeClass
    public void setup() {
        ((PolicySetValidatorImpl) this.policySetValidator)
                .setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH, SUBSCRIBE, MESSAGE");
        ((PolicySetValidatorImpl) this.policySetValidator).init();
    }

    @Test(dataProvider = "invalidPolicyProvider")
    public void testUnsuccessfulSchemaValidation(final String fileName, final String cause, final String element) {
        try {
            this.policySetValidator.validatePolicySet(policySetFromFile(fileName));
            Assert.fail("Negative test case should have failed for file " + fileName);
        } catch (PolicySetValidationException e) {
            System.out.println(e.getMessage());
            Assert.assertTrue(e.getMessage().contains(cause),
                    String.format("Actual message [%s] does not contain expected cause [%s]", e.getMessage(), cause));
            Assert.assertTrue(e.getMessage().contains(element), String
                    .format("Actual message [%s] does not contain expected pointer [%s]", e.getMessage(), element));
        }
    }

    @DataProvider
    private Object[][] invalidPolicyProvider() {
        return new Object[][] { policyMissignEffect(), policySetMissingName(), policySetEmptyPolicies(),
                policySetMissingPolicies(), policyTargetMissingResource(), policyResourceMissingUriTemplate(),
                policyMissingCondition(), policySingleUnsupportedAction(), policyMultipleActionsOneUnsupported(),
                policyObligationIdEmpty(), policyObligationIdNull(), policyObligationIdsNotUnique(),
                policyObligationIdsNotFound(), obligationExpressionIdMissing(), obligationExpressionIdEmpty(),
                obligationExpressionIdNull(), obligationExpressionIdNotUnique(),
                obligationExpressionActionTemplateMissing(), obligationExpressionActionTemplateNull(),
                obligationExpressionActionTemplateEmpty(), obligationExpressionActionArgumentNameEmpty(),
                obligationExpressionActionArgumentNameNull(), obligationExpressionActionArgumentNameMissing(),
                obligationExpressionActionArgumentValueEmpty(), obligationExpressionActionArgumentValueNull(),
                obligationExpressionActionArgumentValueMissing(), obligationExpressionActionArgumentsNotUnique() };
    }

    private Object[] policyObligationIdsNotFound() {
        return new Object[] { "obligation/policy-obligation-ids-not-found.json",
                "Unable to find matching obligation expression", "orphan" };
    }

    private Object[] policyMissignEffect() {
        return new Object[] { "policyset/validator/test/missing-effect-policy.json", "missing: [\"effect\"]",
                "/policies/0" };
    }

    private Object[] policySetMissingName() {
        return new Object[] { "policyset/validator/test/missing-name-policy-set.json", "missing: [\"name\"]", "" };
    }

    private Object[] policySetEmptyPolicies() {
        return new Object[] { "policyset/validator/test/empty-policies-policy-set.json", "/properties/policies",
                "/policies" };
    }

    private Object[] policySetMissingPolicies() {
        return new Object[] { "policyset/validator/test/no-policies-policy-set.json", "/properties/policies",
                "/policies" };
    }

    private Object[] policyTargetMissingResource() {
        return new Object[] { "policyset/validator/test/missing-resource-policy-target.json", "missing: [\"resource\"]",
                "/policies/0/target" };
    }

    private Object[] policyResourceMissingUriTemplate() {
        return new Object[] { "policyset/validator/test/missing-uritemplate-policy-resource.json",
                "missing: [\"uriTemplate\"]", "/policies/0/target/resource" };
    }

    private Object[] policyMissingCondition() {
        return new Object[] { "policyset/validator/test/missing-condition-policy-condition.json",
                "missing: [\"condition\"]", "/policies/0/conditions/0" };
    }

    private Object[] policySingleUnsupportedAction() {
        return new Object[] { "policyset/validator/test/testMatchPolicyWithInvalidAction.json",
                "Policy Action validation failed", "[GET1]" };
    }

    private Object[] policyMultipleActionsOneUnsupported() {
        return new Object[] { "policyset/validator/test/testMatchPolicyWithMultipleActionsOneInvalid.json",
                "Policy Action validation failed", "[PUT1]" };
    }

    private Object[] policyObligationIdEmpty() {
        return new Object[] { "obligation/policy-obligation-id-blank.json",
                "string \"\" is too short (length: 0, required minimum: 1)", "/policies/0/obligationIds/1" };
    }

    private Object[] policyObligationIdNull() {
        return new Object[] { "obligation/policy-obligation-id-null.json",
                "(null) does not match any allowed primitive type (allowed: [\"string\"])",
                "/policies/0/obligationIds/1" };
    }

    private Object[] policyObligationIdsNotUnique() {
        return new Object[] { "obligation/policy-obligation-ids-not-unique.json",
                "array must not contain duplicate elements", "/policies/0/obligationIds" };
    }

    private Object[] obligationExpressionIdMissing() {
        return new Object[] { "obligation/obligation-expression-id-missing.json", "missing: [\"id\"]",
                "/obligationExpressions/0" };
    }

    private Object[] obligationExpressionIdEmpty() {
        return new Object[] { "obligation/obligation-expression-id-empty.json",
                "string \"\" is too short (length: 0, required minimum: 1)", "/obligationExpressions/0/id" };
    }

    private Object[] obligationExpressionIdNull() {
        return new Object[] { "obligation/obligation-expression-id-null.json",
                "string \"\" is too short (length: 0, required minimum: 1)", "/obligationExpressions/0/id" };
    }

    private Object[] obligationExpressionActionTemplateEmpty() {
        return new Object[] { "obligation/obligation-expression-action-template-empty.json", "cannot be null or empty",
                "actionTemplate" };
    }

    private Object[] obligationExpressionActionTemplateNull() {
        return new Object[] { "obligation/obligation-expression-action-template-null.json",
                "missing: [\"actionTemplate\"]", "/obligationExpressions/0" };
    }

    private Object[] obligationExpressionActionTemplateMissing() {
        return new Object[] { "obligation/obligation-expression-action-template-missing.json",
                "missing: [\"actionTemplate\"]", "/obligationExpressions/0" };
    }

    private Object[] obligationExpressionActionArgumentValueMissing() {
        return new Object[] { "obligation/obligation-expression-action-argument-value-missing.json",
                "missing: [\"value\"]", "/obligationExpressions/0/actionArguments/0" };
    }

    private Object[] obligationExpressionActionArgumentValueNull() {
        return new Object[] { "obligation/obligation-expression-action-argument-value-null.json",
                "missing: [\"value\"]", "/obligationExpressions/0/actionArguments/0" };
    }

    private Object[] obligationExpressionActionArgumentValueEmpty() {
        return new Object[] { "obligation/obligation-expression-action-argument-value-blank.json",
                "string \"\" is too short (length: 0, required minimum: 1)",
                "/obligationExpressions/0/actionArguments/0/value" };
    }

    private Object[] obligationExpressionActionArgumentNameMissing() {
        return new Object[] { "obligation/obligation-expression-action-argument-name-missing.json",
                "missing: [\"name\"]", "/obligationExpressions/0/actionArguments/0" };
    }

    private Object[] obligationExpressionActionArgumentNameNull() {
        return new Object[] { "obligation/obligation-expression-action-argument-name-null.json", "missing: [\"name\"]",
                "/obligationExpressions/0/actionArguments/0" };
    }

    private Object[] obligationExpressionActionArgumentNameEmpty() {
        return new Object[] { "obligation/obligation-expression-action-argument-name-blank.json",
                "string \"\" is too short (length: 0, required minimum: 1)",
                "/obligationExpressions/0/actionArguments/0/name" };
    }

    private Object[] obligationExpressionIdNotUnique() {
        return new Object[] { "obligation/obligation-expression-id-not-unique.json",
                "obligation expression has to be unique", "obligation1" };
    }

    private Object[] obligationExpressionActionArgumentsNotUnique() {
        return new Object[] { "obligation/obligation-expression-action-arguments-not-unique.json",
                "actionArguments have to be unique", "resource_site" };
    }

    @Test(dataProvider = "validPolicyProvider")
    public void testSuccessfulSchemaValidation(final String fileName) {
        this.policySetValidator.validatePolicySet(policySetFromFile(fileName));
    }

    @DataProvider
    private Object[][] validPolicyProvider() {
        return new Object[][] { { "set-with-2-policy.json" },
                { "policyset/validator/test/empty-attributes-target-subject.json" },
                { "policyset/validator/test/empty-conditions-policy.json" },
                { "policyset/validator/test/missing-condition-name-policy.json" },
                { "policyset/validator/test/missing-target-name-policy.json" },
                { "policyset/validator/test/policy-set-with-only-name-effect.json" },
                { "policyset/validator/test/testMatchPolicyWithMultipleActions.json" },
                { "policyset/validator/test/policyWithNullTargetAction.json" },
                { "policyset/validator/test/policyWithEmptyTargetAction.json" },
                { "obligation/policy-obligation-ids-null.json" }, { "obligation/policy-obligation-ids-blank.json" },
                { "obligation/obligation-expressions-empty.json" },
                { "obligation/set-with-obligation-expressions.json" },
                { "obligation/obligation-expression-action-arguments-missing.json" },
                { "obligation/obligation-expression-action-arguments-null.json" },
                { "obligation/obligation-expression-action-arguments-empty.json" } };
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

    @SuppressWarnings("unchecked")
    @Test
    public void testRemovalOfMultipleCachedConditions() {
        PolicySet policySet = policySetFromFile(
                "policyset/validator/test/multiple-policies-with-multiple-conditions.json");
        this.policySetValidator.validatePolicySet(policySet);
        GroovyConditionCache conditionCache = (GroovyConditionCache) ReflectionTestUtils
                .getField(this.policySetValidator, "conditionCache");
        Map<String, ConditionShell> cache = (Map<String, ConditionShell>) ReflectionTestUtils.getField(conditionCache,
                "cache");
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

    private static PolicySet policySetFromFile(final String filename) {
        PolicySet policySet = jsonUtils.deserializeFromFile(filename, PolicySet.class);
        Assert.assertNotNull(policySet);
        return policySet;
    }
}
