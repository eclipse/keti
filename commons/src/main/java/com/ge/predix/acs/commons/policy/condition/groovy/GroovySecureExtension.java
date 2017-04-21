/*******************************************************************************
 * Copyright 2016 General Electric Company. 
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
 *******************************************************************************/
package com.ge.predix.acs.commons.policy.condition.groovy;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;

import com.ge.predix.acs.commons.policy.condition.ResourceHandler;
import com.ge.predix.acs.commons.policy.condition.SubjectHandler;

public class GroovySecureExtension extends AbstractTypeCheckingExtension {

    public GroovySecureExtension(final StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    @Override
    public void onMethodSelection(final Expression expression, final MethodNode target) {
        // First the white list.
        if ((!"com.ge.predix.acs.commons.policy.condition.AbstractHandler".equals(target.getDeclaringClass().getName()))
                && (!"com.ge.predix.acs.commons.policy.condition.AbstractHandlers"
                .equals(target.getDeclaringClass().getName()))
                && (!"com.ge.predix.acs.commons.policy.condition.ResourceHandler"
                .equals(target.getDeclaringClass().getName()))
                && (!"com.ge.predix.acs.commons.policy.condition.SubjectHandler"
                .equals(target.getDeclaringClass().getName()))
                && (!"com.ge.predix.acs.commons.policy.condition.groovy.AttributeMatcher"
                .equals(target.getDeclaringClass().getName())) && (!"java.lang.Boolean"
                .equals(target.getDeclaringClass().getName())) && (!"java.lang.Integer"
                .equals(target.getDeclaringClass().getName())) && (!"java.lang.Iterable"
                .equals(target.getDeclaringClass().getName())) && (!"java.lang.Object"
                .equals(target.getDeclaringClass().getName()))
                // This means we allow collections of type Object.
                && (!"[Ljava.lang.Object;".equals(target.getDeclaringClass().getName())) && (!"java.lang.String"
                .equals(target.getDeclaringClass().getName())) && (!"java.util.Collection"
                .equals(target.getDeclaringClass().getName())) && (!"java.util.Set"
                .equals(target.getDeclaringClass().getName()))) {
            addStaticTypeError("Method call for '" + target.getDeclaringClass().getName() + "' class is not allowed!",
                    expression);
        }

        // Then the black list.
        if ("java.lang.System".equals(target.getDeclaringClass().getName())) {
            addStaticTypeError("Method call for 'java.lang.System' class is not allowed!", expression);
        }
        if ("groovy.util.Eval".equals(target.getDeclaringClass().getName())) {
            addStaticTypeError("Method call for 'groovy.util.Eval' class is not allowed!", expression);
        }
        if ("java.io".equals(target.getDeclaringClass().getName())) {
            addStaticTypeError("Method call for 'java.io' package is not allowed!", expression);
        }
        if ("execute".equals(target.getName())) {
            addStaticTypeError("Method call 'execute' is not allowed!", expression);
        }
    }

    @Override
    public boolean handleUnresolvedVariableExpression(final VariableExpression vexp) {

        if ("resource".equals(vexp.getName())) {
            makeDynamic(vexp, ClassHelper.makeCached(ResourceHandler.class));
            setHandled(true);
            return true;
        }
        if ("subject".equals(vexp.getName())) {
            makeDynamic(vexp, ClassHelper.makeCached(SubjectHandler.class));
            setHandled(true);
            return true;
        }
        if ("match".equals(vexp.getName())) {
            makeDynamic(vexp, ClassHelper.makeCached(AttributeMatcher.class));
            setHandled(true);
            return true;
        }

        return false;
    }
}
