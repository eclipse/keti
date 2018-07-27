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

import groovy.lang.Binding
import groovy.lang.Script
import org.eclipse.keti.acs.commons.policy.condition.ConditionScript
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(GroovyConditionScript::class.java)

/**
 * @author acs-engineers@ge.com
 */

/**
 * @param script
 * the script object.
 */
class GroovyConditionScript(private val script: Script) : ConditionScript {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.keti.acs.commons.conditions.ConditionScript#execute(java.util .Map)
     */
    override fun execute(boundVariables: Map<String, Any>): Boolean {
        if (LOGGER.isDebugEnabled) {
            val msgBuilder = StringBuilder()
            msgBuilder.append("The script is bound to the following variables:\n")
            for ((key, value) in boundVariables) {
                msgBuilder.append("* ").append(key).append(":").append(value).append("\n")
            }
            LOGGER.debug(msgBuilder.toString())
        }

        val binding = Binding(boundVariables)
        this.script.binding = binding
        return this.script.run() as Boolean
    }
}
