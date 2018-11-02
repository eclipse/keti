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
import java.util.Map;
import java.util.Map.Entry;
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
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell;
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler;
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

/**
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("nls")
@Component
public class GroovyConditionShell implements ConditionShell {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyConditionShell.class);

    private final GroovyShell shell;

    public GroovyConditionShell() {

        SecureASTCustomizer secureASTCustomizer = createSecureASTCustomizer();
        ImportCustomizer importCustomizer = createImportCustomizer();
        ASTTransformationCustomizer astTransformationCustomizer = createASTTransformationCustomizer();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);
        compilerConfiguration.addCompilationCustomizers(importCustomizer);
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);

        this.shell = new GroovyShell(GroovyConditionShell.class.getClassLoader(), compilerConfiguration);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.keti.acs.commons.conditions.ConditionShell#parse(java.lang. String )
     */
    @Override
    public ConditionScript parse(final String script) throws ConditionParsingException {
        if (StringUtils.isEmpty(script)) {
            throw new IllegalArgumentException("Script is null or empty.");
        }

        try {
            Script groovyScript = this.shell.parse(script);
            return new GroovyConditionScript(groovyScript);
        } catch (CompilationFailedException e) {
            throw new ConditionParsingException("Failed to validate the condition script.", script, e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.keti.acs.commons.conditions.ConditionShell#execute(java.lang. String, java.util.Map )
     */
    @Override
    public boolean execute(final String script, final Map<String, Object> boundVariables)
            throws ConditionParsingException {
        if (StringUtils.isEmpty(script)) {
            throw new IllegalArgumentException("Script is null or empty.");
        }

        try {
            Script groovyScript = this.shell.parse(script);

            if (LOGGER.isDebugEnabled()) {
                StringBuilder msgBuilder = new StringBuilder();
                msgBuilder.append("The script is bound to the following variables:\n");
                for (Entry<String, Object> entry : boundVariables.entrySet()) {
                    msgBuilder.append("* ").append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
                }
                LOGGER.debug(msgBuilder.toString());
            }

            Binding binding = new Binding(boundVariables);
            groovyScript.setBinding(binding);
            boolean result = (boolean) groovyScript.run();

            this.shell.getClassLoader().clearCache();
            GroovySystem.getMetaClassRegistry().removeMetaClass(groovyScript.getClass());

            return result;
        } catch (CompilationFailedException e) {
            throw new ConditionParsingException("Failed to parse the condition script.", script, e);
        } catch (ConditionAssertionFailedException e) {
            return false;
        }
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
        secureASTCustomizer.setImportsWhitelist(Arrays.asList(new String[] {}));
        // Disallow star imports by setting a blank whitelist.
        secureASTCustomizer.setStarImportsWhitelist(Arrays.asList(
                new String[] { "org.crsh.command.*", "org.crsh.cli.*", "org.crsh.groovy.*",
                        "org.eclipse.keti.acs.commons.policy.condition.*" }));
        // Set white list for constant type classes.
        secureASTCustomizer.setConstantTypesClassesWhiteList(Arrays.asList(
                new Class[] { Boolean.class, boolean.class, Collection.class, Double.class, double.class, Float.class,
                        float.class, Integer.class, int.class, Long.class, long.class, Object.class, String.class }));
        secureASTCustomizer.setReceiversClassesWhiteList(Arrays.asList(
                new Class[] { Boolean.class, Collection.class, Integer.class, Iterable.class, Object.class, Set.class,
                        String.class }));
        return secureASTCustomizer;
    }

    private static ASTTransformationCustomizer createASTTransformationCustomizer() {

        return new ASTTransformationCustomizer(singletonMap("extensions",
                singletonList("org.eclipse.keti.acs.commons.policy.condition.groovy.GroovySecureExtension")),
                CompileStatic.class);
    }
}
