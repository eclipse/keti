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

package org.eclipse.keti.acs.commons.policy.condition

import org.eclipse.keti.acs.commons.attribute.AttributeType
import java.util.ArrayList
import java.util.HashSet

class AbstractHandlers(vararg handlers: AbstractHandler) {

    private val handlers = ArrayList<AbstractHandler>()

    init {
        for (handler in handlers) {
            this.handlers.add(handler)
        }
    }

    fun add(handler: AbstractHandler): AbstractHandlers {
        this.handlers.add(handler)
        return this
    }

    fun haveSame(
        criteriaAttributeIssuer: String,
        criteriaAttributeName: String
    ): AbstractHandlers {

        if (this.handlers.isEmpty()) {
            return this
        }

        val criteriaAttributeType = AttributeType(criteriaAttributeIssuer, criteriaAttributeName)
        val iter = this.handlers.iterator()
        var current = iter.next()
        // If this fails it will throw an exception
        current.has(criteriaAttributeType)

        val handlerNames = StringBuilder()
        handlerNames.append(current.name)
        while (iter.hasNext()) {
            val next = iter.next()
            handlerNames.append(" and " + next.name)
            val intersection = HashSet(current.attributes(criteriaAttributeType))
            intersection.retainAll(next.attributes(criteriaAttributeType))
            if (intersection.isEmpty()) {
                throw ConditionAssertionFailedException(
                    "No intersection exists between " + handlerNames.toString() + " on " + criteriaAttributeType + "."
                )
            }
            current = next
        }

        return this
    }

    /**
     * Always returns true because if a condition was not successfully met one of the matchers above would throw an
     * exception.
     */
    fun result(): Boolean {
        return true
    }
}
