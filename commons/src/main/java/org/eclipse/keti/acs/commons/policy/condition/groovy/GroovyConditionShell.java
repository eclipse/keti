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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.eclipse.keti.acs.commons.policy.condition.AbstractHandler;
import org.eclipse.keti.acs.commons.policy.condition.ConditionAssertionFailedException;
import org.eclipse.keti.acs.commons.policy.condition.ConditionParsingException;
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler;
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler;

import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

/**
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("nls")
public class GroovyConditionShell {

    private final GroovyConditionCache conditionCache;
    private final GroovyShell shell;

    public GroovyConditionShell(final GroovyConditionCache conditionCache) {
        this.conditionCache = conditionCache;

        SecureASTCustomizer secureASTCustomizer = createSecureASTCustomizer();
        ImportCustomizer importCustomizer = createImportCustomizer();
        ASTTransformationCustomizer astTransformationCustomizer = createASTTransformationCustomizer();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);
        compilerConfiguration.addCompilationCustomizers(importCustomizer);
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);
        compilerConfiguration.getOptimizationOptions().put(CompilerConfiguration.INVOKEDYNAMIC, true);

        this.shell = new GroovyShell(this.getClass().getClassLoader(), compilerConfiguration);
    }

    /**
     * Validates the script & generates condition script object.
     *
     * @param script
     *            the policy condition string
     * @return a Script object instance capable of executing the policy condition.
     * @throws ConditionParsingException
     *             on validation error
     */
    public ConditionScript parse(final String script) throws ConditionParsingException {
        return parse(script, true);
    }

    private ConditionScript parse(
        final String script,
        final boolean removeLoadedClasses
    ) throws ConditionParsingException {
        if (StringUtils.isEmpty(script)) {
            throw new IllegalArgumentException("Script is null or empty.");
        }

        try {
            ConditionScript compiledScript = conditionCache.get(script);
            if (compiledScript == null) {
                Script groovyScript = this.shell.parse(script);
                compiledScript = new GroovyConditionScript(groovyScript);
                conditionCache.put(script, compiledScript);
            }
            return compiledScript;
        } catch (CompilationFailedException e) {
            throw new ConditionParsingException("Failed to parse the condition script.", script, e);
        } finally {
            if (removeLoadedClasses) {
                removeLoadedClasses();
            }
        }
    }

    /**
     * Validates & executes the policy condition script.
     *
     * @param script
     *            the policy condition string
     * @param boundVariables
     *            variable bindings of the script
     * @return result of executing the policy condition script.
     * @throws ConditionParsingException
     *             on script validation error
     */
    public boolean execute(final String script, final Map<String, Object> boundVariables)
            throws ConditionParsingException {
        ConditionScript conditionScript = parse(script, false);

        try {
            return conditionScript.execute(boundVariables);
        } catch (ConditionAssertionFailedException e) {
            return false;
        } finally {
            removeLoadedClasses();
        }
    }

    private void removeLoadedClasses() {
        for (Class<?> groovyClass : shell.getClassLoader().getLoadedClasses()) {
            GroovySystem.getMetaClassRegistry().removeMetaClass(groovyClass);
        }
        shell.resetLoadedClasses();
    }

    private static ImportCustomizer createImportCustomizer() {
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addImports(ResourceHandler.class.getCanonicalName());
        importCustomizer.addImports(SubjectHandler.class.getCanonicalName());
        importCustomizer.addImports(AbstractHandler.class.getCanonicalName());
        return importCustomizer;
    }

    private static SecureASTCustomizer createSecureASTCustomizer() {
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        // Allow closures.
        secureASTCustomizer.setClosuresAllowed(true);
        // Disallow method definition.
        secureASTCustomizer.setMethodDefinitionAllowed(false);
        // Disallow all imports by setting a blank whitelist.
        secureASTCustomizer.setImportsWhitelist(Collections.emptyList());
        // Disallow star imports by setting a blank whitelist.
        secureASTCustomizer.setStarImportsWhitelist(Arrays.asList(
                "org.crsh.command.*", "org.crsh.cli.*", "org.crsh.groovy.*",
                "org.eclipse.keti.acs.commons.policy.condition.*"));
        // Set white list for constant type classes.
        secureASTCustomizer.setConstantTypesClassesWhiteList(Arrays.asList(
                Boolean.class, boolean.class, Collection.class, Double.class, double.class, Float.class,
                float.class, Integer.class, int.class, Long.class, long.class, Object.class, String.class));
        secureASTCustomizer.setReceiversClassesWhiteList(Arrays.asList(
                Boolean.class, Collection.class, Integer.class, Iterable.class, Object.class, Set.class,
                String.class));
        return secureASTCustomizer;
    }

    private static ASTTransformationCustomizer createASTTransformationCustomizer() {

        return new ASTTransformationCustomizer(singletonMap("extensions",
                singletonList("org.eclipse.keti.acs.commons.policy.condition.groovy.GroovySecureExtension")),
                CompileStatic.class);
    }
}
