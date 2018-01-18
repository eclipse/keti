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

package org.eclipse.keti.acs.commons.policy.condition.groovy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.commons.policy.condition.ConditionParsingException;
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell;
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler;
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler;
import org.eclipse.keti.acs.model.Attribute;

/**
 * Tests for Groovy policy condition script execution.
 *
 * @author acs-engineers@ge.com
 */
public class GroovyConditionScriptTest {
    private ConditionShell shell;

    @BeforeClass
    public void setup() {
        this.shell = new GroovyConditionShell();
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using strings constants.
     *
     * @throws ConditionParsingException
     */
    @Test
    public void testScriptExecutionPositiveEqualsNoBinding() throws ConditionParsingException {
        String script = "\"a\".equals(\"a\")"; //$NON-NLS-1$
        ConditionScript parsedScript = this.shell.parse(script);
        Assert.assertEquals(parsedScript.execute(new HashMap<String, Object>()), true);
    }

    /**
     * Test the execution of a policy condition, which should evaluate to false, using strings constants.
     *
     * @throws ConditionParsingException
     */
    @Test
    public void testScriptExecutionNotEqualsNoBinding() throws ConditionParsingException {
        String script = "\"a\".equals(\"b\")";
        ConditionScript parsedScript = this.shell.parse(script);
        Assert.assertEquals(parsedScript.execute(new HashMap<String, Object>()), false);
    }

    /**
     * Test the execution of a policy condition which does not result in a boolean value.
     *
     * @throws ConditionParsingException
     */
    @Test(expectedExceptions = ClassCastException.class)
    public void testScriptExecutionWithNonBooleanReturnNoBinding() throws ConditionParsingException {
        String script = "\"a\".concat(\"b\")";
        ConditionScript parsedScript = this.shell.parse(script);
        Assert.assertEquals(parsedScript.execute(new HashMap<String, Object>()), false);
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using strings variable binding.
     *
     * @throws ConditionParsingException
     */
    @Test
    public void testScriptExecutionPositiveEqualsSingleBinding() throws ConditionParsingException {
        String script = "resource == \"b\""; //$NON-NLS-1$
        ConditionScript parsedScript = this.shell.parse(script);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("resource", new ResourceHandler(new HashSet<Attribute>(), "", ""));
        Assert.assertEquals(parsedScript.execute(parameter), false);
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using multiple string variable
     * bindings.
     *
     * @throws ConditionParsingException
     */
    @Test
    public void testScriptExecutionPositiveEqualsMultipleBindings() throws ConditionParsingException {
        String script = "resource != subject"; //$NON-NLS-1$
        ConditionScript parsedScript = this.shell.parse(script);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("resource", new ResourceHandler(new HashSet<Attribute>(), "", ""));
        parameter.put("subject", new SubjectHandler(new HashSet<Attribute>()));
        Assert.assertEquals(parsedScript.execute(parameter), true);
    }

    /**
     * Test the execution of a policy condition, which should evaluate to true, using multiple string variable
     * bindings.
     *
     * @throws ConditionParsingException
     */
    @Test
    public void testScriptExecutionPositiveEqualsMultipleBindingsWithMatcher() throws ConditionParsingException {
        String script = "match.any(resource.attributes(\"\",\"\"), subject.attributes(\"\",\"\"))"; //$NON-NLS-1$
        ConditionScript parsedScript = this.shell.parse(script);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("resource", new ResourceHandler(new HashSet<Attribute>(), "", ""));
        parameter.put("subject", new SubjectHandler(new HashSet<Attribute>()));
        parameter.put("match", new AttributeMatcher());
        Assert.assertEquals(parsedScript.execute(parameter), false);
    }

    /**
     * Test the execution of a policy condition, which uses reflection.
     *
     * @throws ConditionParsingException
     */
    @Test(expectedExceptions = ConditionParsingException.class)
    public void testScriptExecutionUsingReflection() throws ConditionParsingException {

        String script = "new ResourceHandler().getClass().getClassLoader().loadClass(System.class.getName())."
                + "getMethod(\"exit\");";
        ConditionScript parsedScript = this.shell.parse(script);
        Map<String, Object> parameter = new HashMap<>();
        Assert.assertNotNull(parsedScript.execute(parameter));
    }

    /**
     * Test the execution of a policy condition, which tries to assign null, to a variable.
     *
     * @throws ConditionParsingException
     */
    @Test
    public void testScriptExecutionUsingAssignment() throws ConditionParsingException {
        String script = "resource = null; resource == null;";
        ConditionScript parsedScript = this.shell.parse(script);
        Map<String, Object> parameter = new HashMap<>();
        final ResourceHandler resourceHandler = new ResourceHandler(new HashSet<Attribute>(), "", "");
        parameter.put("resource", resourceHandler);
        Assert.assertEquals(parsedScript.execute(parameter), true);
        Assert.assertNotNull(resourceHandler);
    }
}
