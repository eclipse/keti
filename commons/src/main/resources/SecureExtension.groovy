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
 *******************************************************************************/

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport
import com.ge.predix.acs.commons.policy.condition.groovy.AttributeMatcher
import com.ge.predix.acs.commons.policy.condition.ResourceHandler;
import com.ge.predix.acs.commons.policy.condition.SubjectHandler;

class SecureExtension extends GroovyTypeCheckingExtensionSupport.TypeCheckingDSL {
    @Override
    Object run() {
        // disallow calls on System
        onMethodSelection { expr, methodNode ->
            //System.out.println("***** onMethodSelection *****")

            // First the white list.
            if ((methodNode.declaringClass.name != 'com.ge.predix.acs.commons.policy.condition.AbstractHandler')
                && (methodNode.declaringClass.name != 'com.ge.predix.acs.commons.policy.condition.ResourceHandler')
                && (methodNode.declaringClass.name != 'com.ge.predix.acs.commons.policy.condition.SubjectHandler')
                && (methodNode.declaringClass.name != 'com.ge.predix.acs.commons.policy.condition.groovy.AttributeMatcher')
                && (methodNode.declaringClass.name != 'java.lang.Boolean')
                && (methodNode.declaringClass.name != 'java.lang.Integer')
                && (methodNode.declaringClass.name != 'java.lang.Iterable')
                && (methodNode.declaringClass.name != 'java.lang.Object')
                // This means we allow collections of type Object.
                && (methodNode.declaringClass.name != '[Ljava.lang.Object;')
                && (methodNode.declaringClass.name != 'java.lang.String')
                && (methodNode.declaringClass.name != 'java.util.Collection')
                && (methodNode.declaringClass.name != 'java.util.Set')) {
                addStaticTypeError("Method call for '" + methodNode.declaringClass.name + "' class is not allowed!", expr)
            }
        
            // Then the black list.
            if (methodNode.declaringClass.name == 'java.lang.System') {
                addStaticTypeError("Method call for 'java.lang.System' class is not allowed!", expr)
            }
            if (methodNode.declaringClass.name == 'groovy.util.Eval') {
                addStaticTypeError("Method call for 'groovy.util.Eval' class is not allowed!", expr)
            }
            if (methodNode.declaringClass.name.startsWith('java.io')) {
                addStaticTypeError("Method call for 'java.io' package is not allowed!", expr)
            }
            if (methodNode.name == 'execute') {
                addStaticTypeError("Method call 'execute' is not allowed!", expr)
            }
        }

/*
        unresolvedVariable { var ->
            System.out.println("***** unresolvedVariable *****")

            if (var.name == 'resource') {
                return makeDynamic(var, ClassHelper.makeCached(ResourceHandler.class))
            }
            if (var.name == 'subject') {
                return makeDynamic(var, ClassHelper.makeCached(SubjectHandler.class))
            }
            if (var.name == 'match') {
                return makeDynamic(var, ClassHelper.makeCached(AttributeMatcher.class))
            }
        }
*/

        unresolvedVariable { var ->
            //System.out.println("***** unresolvedVariable *****")

            if ('resource' == var.name) {
                storeType(var, classNodeFor(ResourceHandler.class))
                handled = true
            }
            if ('subject' == var.name) {
                storeType(var, classNodeFor(SubjectHandler.class))
                handled = true
            }
            if ('match' == var.name) {
                storeType(var, classNodeFor(AttributeMatcher.class))
                handled = true
            }
        }
    }
}