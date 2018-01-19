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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.springframework.stereotype.Component;

import org.eclipse.keti.acs.commons.policy.condition.AbstractHandler;
import org.eclipse.keti.acs.commons.policy.condition.ConditionParsingException;
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript;
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell;
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler;
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

/**
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("nls")
@Component
public class GroovyConditionShell implements ConditionShell {

    private final GroovyShell shell;

    public GroovyConditionShell() {

        SecureASTCustomizer secureASTCustomizer = createSecureASTCustomizer();
        ImportCustomizer importCustomizer = createImportCustomizer();
        ASTTransformationCustomizer astTransformationCustomizer = createASTTransformationCustomizer();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer);
        compilerConfiguration.addCompilationCustomizers(importCustomizer);
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);

        this.shell = new GroovyShell(compilerConfiguration);
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

    private ASTTransformationCustomizer createASTTransformationCustomizer() {

        return new ASTTransformationCustomizer(singletonMap("extensions",
                singletonList("org.eclipse.keti.acs.commons.policy.condition.groovy.GroovySecureExtension")),
                CompileStatic.class);
    }
}
