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

package org.eclipse.keti.acs.commons.policy.condition.groovy

import org.eclipse.keti.acs.commons.policy.condition.ConditionParsingException
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.HashMap
import java.util.HashSet

/**
 * Tests for Groovy policy condition script execution.
 *
 * @author acs-engineers@ge.com
 */
class GroovyConditionScriptTest {

    private var shell: ConditionShell? = null

    @BeforeClass
    fun setup() {
        this.shell = GroovyConditionShell()
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using strings constants.
     *
     * @throws ConditionParsingException
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionPositiveEqualsNoBinding() {
        val script = "\"a\".equals(\"a\")" // $NON-NLS-1$
        val parsedScript = this.shell!!.parse(script)
        Assert.assertEquals(parsedScript.execute(HashMap()), true)
    }

    /**
     * Test the execution of a policy condition, which should evaluate to false, using strings constants.
     *
     * @throws ConditionParsingException
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionNotEqualsNoBinding() {
        val script = "\"a\".equals(\"b\")"
        val parsedScript = this.shell!!.parse(script)
        Assert.assertEquals(parsedScript.execute(HashMap()), false)
    }

    /**
     * Test the execution of a policy condition which does not result in a boolean value.
     *
     * @throws ConditionParsingException
     */
    @Test(expectedExceptions = [(ClassCastException::class)])
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionWithNonBooleanReturnNoBinding() {
        val script = "\"a\".concat(\"b\")"
        val parsedScript = this.shell!!.parse(script)
        Assert.assertEquals(parsedScript.execute(HashMap()), false)
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using strings variable binding.
     *
     * @throws ConditionParsingException
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionPositiveEqualsSingleBinding() {
        val script = "resource == \"b\"" // $NON-NLS-1$
        val parsedScript = this.shell!!.parse(script)
        val parameter = HashMap<String, Any>()
        parameter["resource"] = ResourceHandler(HashSet(), "", "")
        Assert.assertEquals(parsedScript.execute(parameter), false)
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using multiple string variable
     * bindings.
     *
     * @throws ConditionParsingException
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionPositiveEqualsMultipleBindings() {
        val script = "resource != subject" // $NON-NLS-1$
        val parsedScript = this.shell!!.parse(script)
        val parameter = HashMap<String, Any>()
        parameter["resource"] = ResourceHandler(HashSet(), "", "")
        parameter["subject"] = SubjectHandler(HashSet())
        Assert.assertEquals(parsedScript.execute(parameter), true)
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using multiple string variable
     * bindings.
     *
     * @throws ConditionParsingException
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionPositiveEqualsMultipleBindingsWithMatcher() {
        val script = "match.any(resource.attributes(\"\",\"\"), subject.attributes(\"\",\"\"))" // $NON-NLS-1$
        val parsedScript = this.shell!!.parse(script)
        val parameter = HashMap<String, Any>()
        parameter["resource"] = ResourceHandler(HashSet(), "", "")
        parameter["subject"] = SubjectHandler(HashSet())
        parameter["match"] = AttributeMatcher()
        Assert.assertEquals(parsedScript.execute(parameter), false)
    }

    /**
     * Test the execution of a policy condition, which uses reflection.
     *
     * @throws ConditionParsingException
     */
    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionUsingReflection() {

        val script =
            "new ResourceHandler().getClass().getClassLoader().loadClass(System.class.getName())." + "getMethod(\"exit\");"
        val parsedScript = this.shell!!.parse(script)
        val parameter = HashMap<String, Any>()
        Assert.assertNotNull(parsedScript.execute(parameter))
    }

    /**
     * Test the execution of a policy condition, which tries to assign null, to a variable.
     *
     * @throws ConditionParsingException
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testScriptExecutionUsingAssignment() {
        val script = "resource = null; resource == null;"
        val parsedScript = this.shell!!.parse(script)
        val parameter = HashMap<String, Any>()
        val resourceHandler = ResourceHandler(HashSet(), "", "")
        parameter["resource"] = resourceHandler
        Assert.assertEquals(parsedScript.execute(parameter), true)
        Assert.assertNotNull(resourceHandler)
    }
}
