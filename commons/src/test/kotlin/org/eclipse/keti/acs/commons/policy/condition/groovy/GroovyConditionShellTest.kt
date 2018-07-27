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
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Tests for Groovy policy condition script parsing and validation.
 *
 * @author acs-engineers@ge.com
 */
class GroovyConditionShellTest {

    private var shell: ConditionShell? = null

    @BeforeClass
    fun setup() {
        this.shell = GroovyConditionShell()
    }

    /**
     * Test a policy condition parsing and compilation using an allowed policy operations.
     *
     * @throws ConditionParsingException
     * this test should not throw this exception.
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testValidateScriptWithValidScript() {
        val script = "\"a\".equals(\"a\")"
        val parsedScript = this.shell!!.parse(script)
        Assert.assertNotNull(parsedScript)
    }

    /**
     * Test a policy condition parsing and compilation for a blank script.
     *
     * @throws ConditionParsingException
     * this test should not throw exception
     */
    @Test(expectedExceptions = [(IllegalArgumentException::class)])
    @Throws(ConditionParsingException::class)
    fun testValidateEmptyScript() {
        val script = ""
        this.shell!!.parse(script)
    }

    /**
     * Test a policy condition parsing and compilation for a null script.
     *
     * @throws ConditionParsingException
     * this test should not throw exception
     */
    @Test(expectedExceptions = [(IllegalArgumentException::class)])
    @Throws(ConditionParsingException::class)
    fun testValidateNullScript() {
        val parsedScript = this.shell!!.parse(null)
        Assert.assertNotNull(parsedScript)
    }

    /**
     * Test a policy condition parsing and compilation for a script with for loop.
     *
     * @throws ConditionParsingException
     * we expect this exception for this test.
     */
    @Test
    @Throws(ConditionParsingException::class)
    fun testValidateScriptWithForLoop() {
        val script = "for (int i = 0; i < 5; i++) {}"
        this.shell!!.parse(script)
    }

    /**
     * Test a policy condition parsing and compilation for a script trying to invoke System.exit(0).
     *
     * @throws ConditionParsingException
     * we expect this exception for this test.
     */
    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testValidateScriptWithSystemInvocation() {
        val script = "System.exit(0)"
        this.shell!!.parse(script)
    }

    /**
     * Test a policy condition validation for a script trying to use reflection.
     *
     * @throws ConditionParsingException
     * we expect this exception for this test.
     */
    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testValidateScriptWithReflection() {
        val script = "((System)(Class.forName(\"java.lang.System\")).newInstance()).exit(0);"
        this.shell!!.parse(script)
    }

    /**
     * Test policy condition parsing and compilation for a script that uses threads which returns a value.
     *
     * @throws ConditionParsingException
     * we expect this exception for this test.
     */
    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithThreadInvocation() {
        val script = "Thread.currentThread().toString()"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithSystemInvocation1() {
        val script = "def c = System; c.exit(-1);"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithSystemInvocation2() {
        val script = "((Object)System).exit(-1);"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithSystemInvocation3() {
        val script = "Class.forName('java.lang.System').exit(-1);"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithSystemInvocation4() {
        val script = "('java.lang.System' as Class).exit(-1);"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithSystemInvocation5() {
        val script = "import static java.lang.System.exit; exit(-1);"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithEval() {
        val script = "Eval.me(' 2 * 4 + 2');"
        this.shell!!.parse(script)
    }

    @Test(expectedExceptions = [(ConditionParsingException::class)])
    @Throws(ConditionParsingException::class)
    fun testParseScriptWithStringExecute() {
        val script = "'env'.execute();"
        this.shell!!.parse(script)
    }

}
