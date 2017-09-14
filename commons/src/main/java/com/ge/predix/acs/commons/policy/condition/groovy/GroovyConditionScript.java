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

package com.ge.predix.acs.commons.policy.condition.groovy;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ge.predix.acs.commons.policy.condition.ConditionScript;

import groovy.lang.Binding;
import groovy.lang.Script;

/**
 *
 * @author 212314537
 */
@SuppressWarnings("nls")
public class GroovyConditionScript implements ConditionScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyConditionScript.class);

    private final Script script;

    /**
     * @param script
     *            the script object.
     */
    public GroovyConditionScript(final Script script) {
        super();
        this.script = script;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ge.predix.acs.commons.conditions.ConditionScript#execute(java.util .Map)
     */
    @Override
    public boolean execute(final Map<String, Object> boundVariables) {
        if (LOGGER.isDebugEnabled()) {
            StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append("The script is bound to the following variables:\n");
            for (Entry<String, Object> entry : boundVariables.entrySet()) {
                msgBuilder.append("* ").append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }
            LOGGER.debug(msgBuilder.toString());
        }

        Binding binding = new Binding(boundVariables);
        this.script.setBinding(binding);
        return (boolean) this.script.run();
    }

}
