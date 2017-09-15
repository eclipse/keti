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

package com.ge.predix.acs.model;

import java.io.File;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class PolicySetTest {

    private static final String POLICY_1_FILE_PATH = "src/test/resources/policy-1.json";
    private static final String POLICY_UNSUPPORTED_EFFECT_FILE_PATH = "src/test/resources"
            + "/policy-unsupported-effect.json";
    private PolicySet policySet;

    @BeforeClass
    public void beforeClass() throws IOException {
        File file = new File(POLICY_1_FILE_PATH);
        this.policySet = PolicySets.loadFromFile(file);
    }

    @Test
    public void testLoadPolicySet() throws Exception {
        Assert.assertNotNull(this.policySet);
        Assert.assertEquals(this.policySet.getPolicies().size(), 7);
    }

    @Test
    public void testLoadTargetName() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getTarget().getName(), "When an operator reads a site");
    }

    @Test
    public void testLoadTargetResourceName() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getTarget().getResource().getName(), "Site");
    }

    @Test
    public void testLoadTargetAction() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getTarget().getAction(), "GET");
    }

    @Test
    public void testLoadTargetSubjectName() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getTarget().getSubject().getName(), "Operator");
    }

    @Test
    public void testLoadConditions() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getConditions().size(), 1);
    }

    @Test
    public void testLoadConditionName() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getConditions().get(0).getName(),
                "is assigned to site");
    }

    @Test
    public void testLoadEffect() {
        Assert.assertEquals(this.policySet.getPolicies().get(0).getEffect(), Effect.PERMIT);
    }

    @Test(expectedExceptions = InvalidFormatException.class)
    public void testLoadUnsupportedEffect() throws IOException {
        File file = new File(POLICY_UNSUPPORTED_EFFECT_FILE_PATH);
        PolicySets.loadFromFile(file);
    }
}
