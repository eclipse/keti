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
package com.ge.predix.acs.commons.policy.condition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ge.predix.acs.commons.attribute.AttributeType;
import com.ge.predix.acs.model.Attribute;

public class AbstractHandlers {

    private final List<AbstractHandler> handlers = new ArrayList<AbstractHandler>();

    public AbstractHandlers(final AbstractHandler... handlers) {
        for (AbstractHandler handler : handlers) {
            this.handlers.add(handler);
        }
    }

    public AbstractHandlers add(final AbstractHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    public AbstractHandlers haveSame(final String criteriaAttributeIssuer, final String criteriaAttributeName)
            throws ConditionAssertionFailedException {

        if (this.handlers.isEmpty()) {
            return this;
        }

        AttributeType criteriaAttributeType = new AttributeType(criteriaAttributeIssuer, criteriaAttributeName);
        Iterator<AbstractHandler> iter = this.handlers.iterator();
        AbstractHandler current = iter.next();
        // If this fails it will throw an exception
        current.has(criteriaAttributeType);

        StringBuilder handlerNames = new StringBuilder();
        handlerNames.append(current.getName());
        while (iter.hasNext()) {
            AbstractHandler next = iter.next();
            handlerNames.append(" and " + next.getName());
            Set<Attribute> intersection = new HashSet<Attribute>(current.attributes(criteriaAttributeType));
            intersection.retainAll(next.attributes(criteriaAttributeType));
            if (intersection.isEmpty()) {
                throw new ConditionAssertionFailedException("No intersection exists between " + handlerNames.toString()
                        + " on " + criteriaAttributeType + ".");
            }
            current = next;
        }

        return this;
    }

    /**
     * Always returns true because if a condition was not successfully met one of the matchers above would throw an
     * exception.
     */
    public boolean result() {
        return true;
    }
}
