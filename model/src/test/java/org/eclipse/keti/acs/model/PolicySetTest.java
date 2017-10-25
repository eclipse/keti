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

package org.eclipse.keti.acs.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class PolicySetTest {

    private static final String POLICY_1_FILE_PATH = "src/test/resources/policy-1.json";
    private static final String POLICY_UNSUPPORTED_EFFECT_FILE_PATH = "src/test/resources"
            + "/policy-unsupported-effect.json";
    private static final String POLICY_WITH_OBLIGATIONS = "src/test/resources/policy-with-obligations.json";
    private static final String POLICY_UNSUPPORTED_OBLIGATION_TYPE = "src/test/resources"
            + "/policy-unsupported-obligation-type.json";

    @Test
    public void testLoadPolicySet() throws Exception {
        File file = new File(POLICY_1_FILE_PATH);
        PolicySet policySet = PolicySets.loadFromFile(file);

        Assert.assertNotNull(policySet);
        Assert.assertEquals(policySet.getPolicies().size(), 7);
        Assert.assertTrue(policySet.getObligationExpressions().isEmpty());

        Policy policy = policySet.getPolicies().get(0);
        Assert.assertNotNull(policy);
        Assert.assertEquals(policy.getTarget().getName(), "When an operator reads a site");
        Assert.assertEquals(policy.getTarget().getResource().getName(), "Site");
        Assert.assertEquals(policy.getTarget().getAction(), "GET");
        Assert.assertEquals(policy.getTarget().getSubject().getName(), "Operator");
        Assert.assertEquals(policy.getConditions().size(), 1);
        Assert.assertEquals(policy.getConditions().get(0).getName(), "is assigned to site");
        Assert.assertEquals(policy.getEffect(), Effect.PERMIT);
        Assert.assertTrue(policy.getObligationIds().isEmpty());
    }

    @Test(expectedExceptions = InvalidFormatException.class)
    public void testLoadUnsupportedEffect() throws IOException {
        File file = new File(POLICY_UNSUPPORTED_EFFECT_FILE_PATH);
        PolicySets.loadFromFile(file);
    }

    @Test
    public void testLoadPolicySetWithObligationExpressions() throws IOException {
        File file = new File(POLICY_WITH_OBLIGATIONS);
        PolicySet policySet = PolicySets.loadFromFile(file);
        Assert.assertNotNull(policySet);

        Policy policy = policySet.getPolicies().get(0);
        Assert.assertNotNull(policy);

        List<String> obligationIds = policy.getObligationIds();
        Assert.assertNotNull(obligationIds);
        List<String> expectedObligationIds = Arrays.asList("one", "two");
        Assert.assertEquals(obligationIds.size(), 2);
        Assert.assertEquals(obligationIds, expectedObligationIds);

        List<ObligationExpression> obligationExpressions = policySet.getObligationExpressions();
        Assert.assertNotNull(obligationExpressions);
        Assert.assertEquals(obligationExpressions.size(), 2);

        ObligationExpression obligationExpression = obligationExpressions.get(0);
        Assert.assertNotNull(obligationExpression);
        Assert.assertEquals(obligationExpression.getId(), "one");
        Assert.assertEquals(obligationExpression.getType(), ObligationType.CUSTOM);
        Assert.assertNotNull(obligationExpression.getActionTemplate());
        Assert.assertEquals(
                PolicySets.OBJECT_MAPPER.convertValue(obligationExpression.getActionTemplate(), JsonNode.class)
                        .get("sqlStatement").toString(),
                "\"SELECT * FROM employee_records where site = '$resource_site' AND id='$record_id'\"");
        List<ActionArgument> actonArguments = obligationExpression.getActionArguments();
        Assert.assertNotNull(actonArguments);
        Assert.assertEquals(actonArguments.size(), 2);
        Assert.assertEquals(actonArguments.get(0).getName(), "resource_site");
        Assert.assertEquals(actonArguments.get(1).getName(), "record_id");

        obligationExpression = obligationExpressions.get(1);
        Assert.assertNotNull(obligationExpression);
        Assert.assertEquals(obligationExpression.getId(), "two");
        Assert.assertEquals(obligationExpression.getType(), ObligationType.CUSTOM);
        Assert.assertNotNull(obligationExpression.getActionTemplate());
        Assert.assertEquals(PolicySets.OBJECT_MAPPER
                .convertValue(obligationExpression.getActionTemplate(), JsonNode.class).get("sqlStatement").toString(),
                "\"SELECT * FROM employee_records LIMIT 100\"");
        Assert.assertTrue(obligationExpression.getActionArguments().isEmpty());
    }

    @Test(expectedExceptions = InvalidFormatException.class)
    public void loadUnsupportedObligationType() throws IOException {
        File file = new File(POLICY_UNSUPPORTED_OBLIGATION_TYPE);
        PolicySets.loadFromFile(file);
    }
}
