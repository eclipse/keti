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

package org.eclipse.keti.acs.commons.policy.condition.groovy

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor

import org.eclipse.keti.acs.commons.policy.condition.ResourceHandler
import org.eclipse.keti.acs.commons.policy.condition.SubjectHandler

class GroovySecureExtension(typeCheckingVisitor: StaticTypeCheckingVisitor) :
    AbstractTypeCheckingExtension(typeCheckingVisitor) {

    override fun onMethodSelection(
        expression: Expression?,
        target: MethodNode?
    ) {
        // First the white list.
        if ("org.eclipse.keti.acs.commons.policy.condition.AbstractHandler" != target!!.declaringClass.name && "org.eclipse.keti.acs.commons.policy.condition.AbstractHandlers" != target.declaringClass.name && "org.eclipse.keti.acs.commons.policy.condition.ResourceHandler" != target.declaringClass.name && "org.eclipse.keti.acs.commons.policy.condition.SubjectHandler" != target.declaringClass.name && "org.eclipse.keti.acs.commons.policy.condition.groovy.AttributeMatcher" != target.declaringClass.name && "java.lang.Boolean" != target.declaringClass.name && "java.lang.Integer" != target.declaringClass.name && "java.lang.Iterable" != target.declaringClass.name && "java.lang.Object" != target.declaringClass.name &&
            // This means we allow collections of type Object.
            "[Ljava.lang.Object;" != target.declaringClass.name && "java.lang.String" != target.declaringClass.name && "java.util.Collection" != target.declaringClass.name && "java.util.Set" != target.declaringClass.name
        ) {
            addStaticTypeError(
                "Method call for '" + target.declaringClass.name + "' class is not allowed!", expression
            )
        }

        // Then the black list.
        if ("java.lang.System" == target.declaringClass.name) {
            addStaticTypeError("Method call for 'java.lang.System' class is not allowed!", expression)
        }
        if ("groovy.util.Eval" == target.declaringClass.name) {
            addStaticTypeError("Method call for 'groovy.util.Eval' class is not allowed!", expression)
        }
        if ("java.io" == target.declaringClass.name) {
            addStaticTypeError("Method call for 'java.io' package is not allowed!", expression)
        }
        if ("execute" == target.name) {
            addStaticTypeError("Method call 'execute' is not allowed!", expression)
        }
    }

    override fun handleUnresolvedVariableExpression(vexp: VariableExpression?): Boolean {

        if ("resource" == vexp!!.name) {
            makeDynamic(vexp, ClassHelper.makeCached(ResourceHandler::class.java))
            setHandled(true)
            return true
        }
        if ("subject" == vexp.name) {
            makeDynamic(vexp, ClassHelper.makeCached(SubjectHandler::class.java))
            setHandled(true)
            return true
        }
        if ("match" == vexp.name) {
            makeDynamic(vexp, ClassHelper.makeCached(AttributeMatcher::class.java))
            setHandled(true)
            return true
        }

        return false
    }
}
