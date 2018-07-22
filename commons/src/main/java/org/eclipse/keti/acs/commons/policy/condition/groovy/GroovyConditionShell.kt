/*******************************************************************************
 * Copyright 2017 General Electric Company
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

import groovy.lang.GroovyShell
import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.eclipse.keti.acs.commons.policy.condition.AbstractHandler
import org.eclipse.keti.acs.commons.policy.condition.ConditionParsingException
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript
import org.eclipse.keti.acs.commons.policy.condition.ConditionShell
import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler
import org.springframework.stereotype.Component
import java.util.Collections.singletonMap

/**
 * @author acs-engineers@ge.com
 */
@Component
class GroovyConditionShell : ConditionShell {

    private val shell: GroovyShell

    init {

        val secureASTCustomizer = createSecureASTCustomizer()
        val importCustomizer = createImportCustomizer()
        val astTransformationCustomizer = createASTTransformationCustomizer()

        val compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.addCompilationCustomizers(secureASTCustomizer)
        compilerConfiguration.addCompilationCustomizers(importCustomizer)
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer)

        this.shell = GroovyShell(compilerConfiguration)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.keti.acs.commons.conditions.ConditionShell#parse(java.lang. String )
     */
    @Throws(ConditionParsingException::class)
    override fun parse(script: String): ConditionScript {
        if (StringUtils.isEmpty(script)) {
            throw IllegalArgumentException("Script is null or empty.")
        }

        try {
            val groovyScript = this.shell.parse(script)
            return GroovyConditionScript(groovyScript)
        } catch (e: CompilationFailedException) {
            throw ConditionParsingException("Failed to validate the condition script.", script, e)
        }

    }

    private fun createImportCustomizer(): ImportCustomizer {
        val importCustomizer = ImportCustomizer()
        importCustomizer.addImports(ResourceHandler::class.java.canonicalName)
        importCustomizer.addImports(SubjectHandler::class.java.canonicalName)
        importCustomizer.addImports(AbstractHandler::class.java.canonicalName)
        return importCustomizer
    }

    private fun createSecureASTCustomizer(): SecureASTCustomizer {
        val secureASTCustomizer = SecureASTCustomizer()
        // Allow closures.
        secureASTCustomizer.isClosuresAllowed = true
        // Disallow method definition.
        secureASTCustomizer.isMethodDefinitionAllowed = false
        // Disallow all imports by setting a blank whitelist.
        secureASTCustomizer.importsWhitelist = emptyList()
        // Disallow star imports by setting a blank whitelist.
        secureASTCustomizer.starImportsWhitelist = listOf(
            "org.crsh.command.*", "org.crsh.cli.*", "org.crsh.groovy.*", "org.eclipse.keti.acs.commons.policy.condition.*"
        )
        // Set white list for constant type classes.
        secureASTCustomizer.setConstantTypesClassesWhiteList(
            listOf(
                Boolean::class.java,
                Boolean::class.javaPrimitiveType,
                Collection::class.java,
                Double::class.java,
                Double::class.javaPrimitiveType,
                Float::class.java,
                Float::class.javaPrimitiveType,
                Integer::class.java,
                Integer::class.javaPrimitiveType,
                Long::class.java,
                Long::class.javaPrimitiveType,
                Any::class.java,
                String::class.java
            )
        )
        secureASTCustomizer.setReceiversClassesWhiteList(
            listOf(
                Boolean::class.java,
                Collection::class.java,
                Integer::class.java,
                Iterable::class.java,
                Any::class.java,
                Set::class.java,
                String::class.java
            )
        )
        return secureASTCustomizer
    }

    private fun createASTTransformationCustomizer(): ASTTransformationCustomizer {

        return ASTTransformationCustomizer(
            singletonMap(
                "extensions", listOf("org.eclipse.keti.acs.commons.policy.condition.groovy.GroovySecureExtension")
            ), CompileStatic::class.java
        )
    }
}
