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
        if ((!target.getDeclaringClass().getName().equals("com.ge.predix.acs.commons.policy.condition.AbstractHandler"))
                && (!target.getDeclaringClass().getName()
                        .equals("com.ge.predix.acs.commons.policy.condition.AbstractHandlers"))
                && (!target.getDeclaringClass().getName()
                        .equals("com.ge.predix.acs.commons.policy.condition.ResourceHandler"))
                && (!target.getDeclaringClass().getName()
                        .equals("com.ge.predix.acs.commons.policy.condition.SubjectHandler"))
                && (!target.getDeclaringClass().getName()
                        .equals("com.ge.predix.acs.commons.policy.condition.groovy.AttributeMatcher"))
                && (!target.getDeclaringClass().getName().equals("java.lang.Boolean"))
                && (!target.getDeclaringClass().getName().equals("java.lang.Integer"))
                && (!target.getDeclaringClass().getName().equals("java.lang.Iterable"))
                && (!target.getDeclaringClass().getName().equals("java.lang.Object"))
                // This means we allow collections of type Object.
                && (!target.getDeclaringClass().getName().equals("[Ljava.lang.Object;"))
                && (!target.getDeclaringClass().getName().equals("java.lang.String"))
                && (!target.getDeclaringClass().getName().equals("java.util.Collection"))
                && (!target.getDeclaringClass().getName().equals("java.util.Set"))) {
            addStaticTypeError("Method call for '" + target.getDeclaringClass().getName() + "' class is not allowed!",
                    expression);
        }

        // Then the black list.
        if (target.getDeclaringClass().getName().equals("java.lang.System")) {
            addStaticTypeError("Method call for 'java.lang.System' class is not allowed!", expression);
        }
        if (target.getDeclaringClass().getName().equals("groovy.util.Eval")) {
            addStaticTypeError("Method call for 'groovy.util.Eval' class is not allowed!", expression);
        }
        if (target.getDeclaringClass().getName().equals("java.io")) {
            addStaticTypeError("Method call for 'java.io' package is not allowed!", expression);
        }
        if (target.getName().equals("execute")) {
            addStaticTypeError("Method call 'execute' is not allowed!", expression);
        }
    }

    @Override
    public boolean handleUnresolvedVariableExpression(final VariableExpression vexp) {

        if (vexp.getName().equals("resource")) {
            makeDynamic(vexp, ClassHelper.makeCached(ResourceHandler.class));
            setHandled(true);
            return true;
        }
        if (vexp.getName().equals("subject")) {
            makeDynamic(vexp, ClassHelper.makeCached(SubjectHandler.class));
            setHandled(true);
            return true;
        }
        if (vexp.getName().equals("match")) {
            makeDynamic(vexp, ClassHelper.makeCached(AttributeMatcher.class));
            setHandled(true);
            return true;
        }

        return false;
    }
}
