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

package org.eclipse.keti.acs.model

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.File
import java.io.IOException

private const val POLICY_1_FILE_PATH = "src/test/resources/policy-1.json"
private const val POLICY_UNSUPPORTED_EFFECT_FILE_PATH = "src/test/resources" + "/policy-unsupported-effect.json"

class PolicySetTest {
    private var policySet: PolicySet? = null

    @BeforeClass
    @Throws(IOException::class)
    fun beforeClass() {
        val file = File(POLICY_1_FILE_PATH)
        this.policySet = PolicySets.loadFromFile(file)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadPolicySet() {
        Assert.assertNotNull(this.policySet)
        Assert.assertEquals(this.policySet!!.policies.size, 7)
    }

    @Test
    fun testLoadTargetName() {
        Assert.assertEquals(this.policySet!!.policies[0].target!!.name, "When an operator reads a site")
    }

    @Test
    fun testLoadTargetResourceName() {
        Assert.assertEquals(this.policySet!!.policies[0].target!!.resource!!.name, "Site")
    }

    @Test
    fun testLoadTargetAction() {
        Assert.assertEquals(this.policySet!!.policies[0].target!!.action, "GET")
    }

    @Test
    fun testLoadTargetSubjectName() {
        Assert.assertEquals(this.policySet!!.policies[0].target!!.subject!!.name, "Operator")
    }

    @Test
    fun testLoadConditions() {
        Assert.assertEquals(this.policySet!!.policies[0].conditions.size, 1)
    }

    @Test
    fun testLoadConditionName() {
        Assert.assertEquals(
            this.policySet!!.policies[0].conditions[0].name, "is assigned to site"
        )
    }

    @Test
    fun testLoadEffect() {
        Assert.assertEquals(this.policySet!!.policies[0].effect, Effect.PERMIT)
    }

    @Test(expectedExceptions = [(InvalidFormatException::class)])
    @Throws(IOException::class)
    fun testLoadUnsupportedEffect() {
        val file = File(POLICY_UNSUPPORTED_EFFECT_FILE_PATH)
        PolicySets.loadFromFile(file)
    }
}
