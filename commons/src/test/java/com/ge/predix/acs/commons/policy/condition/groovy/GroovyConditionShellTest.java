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

package com.ge.predix.acs.commons.policy.condition.groovy;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.commons.policy.condition.ConditionParsingException;
import com.ge.predix.acs.commons.policy.condition.ConditionScript;
import com.ge.predix.acs.commons.policy.condition.ConditionShell;

/**
 * Tests for Groovy policy condition script parsing and validation.
 *
 * @author acs-engineers@ge.com
 */
public class GroovyConditionShellTest {
    private ConditionShell shell;

    @BeforeClass
    public void setup() {
        this.shell = new GroovyConditionShell();
    }

    /**
     * Test a policy condition parsing and compilation using an allowed policy operations.
     *
     * @throws ConditionParsingException
     *             this test should not throw this exception.
     */
    @Test
    public void testValidateScriptWithValidScript() throws ConditionParsingException {
        String script = "\"a\".equals(\"a\")";
        ConditionScript parsedScript = this.shell.parse(script);
        Assert.assertNotNull(parsedScript);
    }

    /**
     * Test a policy condition parsing and compilation for a blank script.
     *
     * @throws ConditionParsingException
     *             this test should not throw exception
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateEmptyScript() throws ConditionParsingException {
        String script = "";
        this.shell.parse(script);
    }

    /**
     * Test a policy condition parsing and compilation for a null script.
     *
     * @throws ConditionParsingException
     *             this test should not throw exception
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateNullScript() throws ConditionParsingException {
        ConditionScript parsedScript = this.shell.parse(null);
        Assert.assertNotNull(parsedScript);
    }

    /**
     * Test a policy condition parsing and compilation for a script with for loop.
     *
     * @throws ConditionParsingException
     *             we expect this exception for this test.
     */
    @Test
    public void testValidateScriptWithForLoop() throws ConditionParsingException {
        String script = "for (int i = 0; i < 5; i++) {}";
        this.shell.parse(script);
    }

    /**
     * Test a policy condition parsing and compilation for a script trying to invoke System.exit(0).
     *
     * @throws ConditionParsingException
     *             we expect this exception for this test.
     */
    @Test(expectedExceptions = ConditionParsingException.class)
    public void testValidateScriptWithSystemInvocation() throws ConditionParsingException {
        String script = "System.exit(0)";
        this.shell.parse(script);
    }

    /**
     * Test a policy condition validation for a script trying to use reflection.
     *
     * @throws ConditionParsingException
     *             we expect this exception for this test.
     */
    @Test(expectedExceptions = ConditionParsingException.class)
    public void testValidateScriptWithReflection() throws ConditionParsingException {
        String script = "((System)(Class.forName(\"java.lang.System\")).newInstance()).exit(0);";
        this.shell.parse(script);
    }

    /**
     * Test policy condition parsing and compilation for a script that uses threads which returns a value.
     *
     * @throws ConditionParsingException
     *             we expect this exception for this test.
     */
    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithThreadInvocation() throws ConditionParsingException {
        String script = "Thread.currentThread().toString()";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithSystemInvocation1() throws ConditionParsingException {
        String script = "def c = System; c.exit(-1);";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithSystemInvocation2() throws ConditionParsingException {
        String script = "((Object)System).exit(-1);";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithSystemInvocation3() throws ConditionParsingException {
        String script = "Class.forName('java.lang.System').exit(-1);";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithSystemInvocation4() throws ConditionParsingException {
        String script = "('java.lang.System' as Class).exit(-1);";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithSystemInvocation5() throws ConditionParsingException {
        String script = "import static java.lang.System.exit; exit(-1);";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithEval() throws ConditionParsingException {
        String script = "Eval.me(' 2 * 4 + 2');";
        this.shell.parse(script);
    }

    @Test(expectedExceptions = ConditionParsingException.class)
    public void testParseScriptWithStringExecute() throws ConditionParsingException {
        String script = "'env'.execute();";
        this.shell.parse(script);
    }

}
