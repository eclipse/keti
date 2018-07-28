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
import org.eclipse.keti.acs.model.Attribute
import java.util.HashMap
import java.util.HashSet

/**
 * @author acs-engineers@ge.com
 */
abstract class AbstractHandler(
    val name: String,
    attributes: Set<Attribute>?
) {

    // This map is used to find all the values of a specific attribute (e.g. all
    // groups a subject belongs to).
    // NOTE: The intention is that this is an immutable data structure. Do not
    // expose methods that modify this.
    private val attributeTypeMap = HashMap<AttributeType, MutableSet<Attribute>>()

    // This map is used to check if an attribute of a specific value exists
    // (e.g. is a subject a member of a group).
    // NOTE: The intention is that this is an immutable data structure. Do not
    // expose methods that modify this.
    private val attributes = HashSet<Attribute>()

    init {
        if (null != attributes) {
            this.attributes.addAll(attributes)
        }
        this.attributes.forEach { this.indexAttributeByType(it) }
    }

    private fun indexAttributeByType(attribute: Attribute) {
        val attributeType = AttributeType(attribute.issuer, attribute.name)
        val attributesForType = this.attributeTypeMap[attributeType]
        if (null == attributesForType) {
            this.attributeTypeMap[attributeType] = mutableSetOf(attribute)
        } else {
            attributesForType.add(attribute)
        }
    }

    fun attributes(
        attributeIssuer: String,
        attributeName: String
    ): Set<String?> {
        val attributeSet = attributes(AttributeType(attributeIssuer, attributeName))
        return attributeSet.map { it.value }.toSet()
    }

    fun attributes(attributeType: AttributeType): Set<Attribute> {
        return this.attributeTypeMap.getOrDefault(attributeType, emptySet())
    }

    override fun toString(): String {
        return "AbstractHandler [attributeMap=" + this.attributeTypeMap + "]"
    }

    @Throws(ConditionAssertionFailedException::class)
    fun has(attributeType: AttributeType): AbstractHandler {

        if (!this.attributeTypeMap.containsKey(attributeType)) {
            throw ConditionAssertionFailedException(this.name + " does not have " + attributeType + ".")
        }

        return this
    }

    @Throws(ConditionAssertionFailedException::class)
    fun has(attribute: Attribute): AbstractHandler {

        if (!this.attributes.contains(attribute)) {
            throw ConditionAssertionFailedException(this.name + " does not have " + attribute + ".")
        }

        return this
    }

    fun and(handler: AbstractHandler): AbstractHandlers {

        return AbstractHandlers(this, handler)
    }
}
