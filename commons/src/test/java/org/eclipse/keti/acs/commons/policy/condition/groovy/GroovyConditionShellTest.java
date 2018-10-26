/*******************************************************************************
 * Copyright 2018 General Electric Company
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

import org.eclipse.keti.acs.commons.policy.condition.ConditionParsingException;
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell;
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler;
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler;
import org.eclipse.keti.acs.model.Attribute;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for Groovy policy condition script parsing and validation.
 *
 * @author acs-engineers@ge.com
 */
public class GroovyConditionShellTest {
    private static final ResourceHandler RESOURCE_HANDLER = new ResourceHandler(new HashSet<Attribute>(), "", "");
    private static final SubjectHandler SUBJECT_HANDLER = new SubjectHandler(new HashSet<Attribute>());
    private static final AttributeMatcher ATTRIBUTE_MATCHER = new AttributeMatcher();

    private static Map<String, Object> emptyBindingMap = new HashMap<>();

    private ConditionShell shell = new GroovyConditionShell();

    /**
     * Test a policy condition parsing and compilation using an allowed policy operations.
     *
     * @throws ConditionParsingException
     *             this test should not throw this exception.
     */
    @Test(dataProvider = "validScript")
    public void testParseValidScript(final String script, final Map<String, Object> boundVariables,
            final boolean expectedResult) throws ConditionParsingException {
        ConditionScript parsedScript = this.shell.parse(script);
        Assert.assertNotNull(parsedScript);
    }

    @Test(dataProvider = "validScript")
    public void testExecuteValidScript(final String script, final Map<String, Object> boundVariables,
            final boolean expectedResult) throws ConditionParsingException {
        Assert.assertEquals(this.shell.execute(script, boundVariables), expectedResult);
    }

    @DataProvider
    public Object[][] validScript() {
        return new Object[][] { { "\"a\".equals(\"a\")", new HashMap<>(), true },
                { "\"a\".equals(\"b\")", new HashMap<>(), false }, { "resource == \"b\"", getSingleBinding(), false },
                { "resource != subject", getMultipleBindings(), true },
                { "match.any(resource.attributes(\"\",\"\"), subject.attributes(\"\",\"\"))",
                        getMultipleBindingsWithMatcher(), false },
                { "resource = null; resource == null;", getSingleBinding(), true } };
    }

    private static Map<String, Object> getMultipleBindingsWithMatcher() {
        Map<String, Object> parameter = getMultipleBindings();
        parameter.put("match", ATTRIBUTE_MATCHER);
        return parameter;
    }

    private static Map<String, Object> getMultipleBindings() {
        Map<String, Object> parameter = getSingleBinding();
        parameter.put("subject", SUBJECT_HANDLER);
        return parameter;
    }

    private static Map<String, Object> getSingleBinding() {
        Map<String, Object> parameter = emptyBindingMap;
        parameter.put("resource", RESOURCE_HANDLER);
        return parameter;
    }

    /**
     * Test a policy condition parsing and compilation for a blank/null script.
     *
     * @throws ConditionParsingException
     *             this test should not throw exception
     */
    @Test(dataProvider = "illegalScript", expectedExceptions = IllegalArgumentException.class)
    public void testParseBlankScript(final String script) throws ConditionParsingException {
        this.shell.parse(script);
    }

    @Test(dataProvider = "illegalScript", expectedExceptions = IllegalArgumentException.class)
    public void testExecuteBlankScript(final String script) throws ConditionParsingException {
        this.shell.execute(script, emptyBindingMap);
    }

    @DataProvider
    public Object[][] illegalScript() {
        return new Object[][] { { "" }, { null } };
    }

    /**
     * Test the execution of a policy condition which does not result in a boolean value.
     *
     * @throws ConditionParsingException
     */
    @Test(dataProvider = "invalidScript", expectedExceptions = ClassCastException.class)
    public void testExecuteInvalidScript(final String script) throws ConditionParsingException {
        this.shell.execute(script, emptyBindingMap);
    }

    @DataProvider
    public Object[][] invalidScript() {
        return new Object[][] { { "\"a\".concat(\"b\")" } };
    }

    /**
     * Test a policy condition parsing and compilation for black-listed scripts.
     *
     * @throws ConditionParsingException
     *             we expect this exception for this test.
     */
    @Test(dataProvider = "blackListedScript", expectedExceptions = ConditionParsingException.class)
    public void testParseBlackListedScript(final String script) throws ConditionParsingException {
        this.shell.parse(script);
    }

    @Test(dataProvider = "blackListedScript", expectedExceptions = ConditionParsingException.class)
    public void testExecuteBlackListedScript(final String script) throws ConditionParsingException {
        this.shell.execute(script, emptyBindingMap);
    }

    @DataProvider
    public Object[][] blackListedScript() {
        return new Object[][] { { "System.exit(0)" },
                { "((System)(Class.forName(\"java.lang.System\")).newInstance()).exit(0);" },
                { "Thread.currentThread().toString()" }, { "def c = System; c.exit(-1);" },
                { "((Object)System).exit(-1);" }, { "Class.forName('java.lang.System').exit(-1);" },
                { "('java.lang.System' as Class).exit(-1);" }, { "import static java.lang.System.exit; exit(-1);" },
                { "Eval.me(' 2 * 4 + 2');" }, { "'env'.execute();" },
                { "new ResourceHandler().getClass().getClassLoader().loadClass(System.class.getName())."
                        + "getMethod(\"exit\");" } };
    }
}
