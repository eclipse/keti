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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ge.predix.acs.commons.attribute.AttributeType;
import com.ge.predix.acs.model.Attribute;

/**
 *
 * @author 212314537
 */
@SuppressWarnings({ "nls", "javadoc" })
public abstract class AbstractHandler {
    private final String name;

    // This map is used to find all the values of a specific attribute (e.g. all
    // groups a subject belongs to).
    // NOTE: The intention is that this is an immutable data structure. Do not
    // expose methods that modify this.
    private final Map<AttributeType, Set<Attribute>> attributeTypeMap = new HashMap<>();

    // This map is used to check if an attribute of a specific value exists
    // (e.g. is a subject a member of a group).
    // NOTE: The intention is that this is an immutable data structure. Do not
    // expose methods that modify this.
    private final Set<Attribute> attributes = new HashSet<>();

    public AbstractHandler(final String name, final Set<Attribute> attributes) {
        this.name = name;
        if (null != attributes) {
            this.attributes.addAll(attributes);
        }
        this.attributes.forEach(attribute -> indexAttributeByType(attribute));
    }

    private void indexAttributeByType(final Attribute attribute) {
        AttributeType attributeType = new AttributeType(attribute.getIssuer(), attribute.getName());
        Set<Attribute> attributesForType = this.attributeTypeMap.get(attributeType);
        if (null == attributesForType) {
            this.attributeTypeMap.put(attributeType, new HashSet<>(Arrays.asList(attribute)));
        } else {
            attributesForType.add(attribute);
        }
    }

    public Set<String> attributes(final String attributeIssuer, final String attributeName) {
        Set<Attribute> attributeSet = attributes(new AttributeType(attributeIssuer, attributeName));
        return attributeSet.stream().map(attribute -> attribute.getValue()).collect(Collectors.toSet());
    }

    protected Set<Attribute> attributes(final AttributeType attributeType) {
        return this.attributeTypeMap.getOrDefault(attributeType, Collections.emptySet());
    }

    @Override
    public String toString() {
        return "AbstractHandler [attributeMap=" + this.attributeTypeMap + "]";
    }

    public AbstractHandler has(final AttributeType attributeType) throws ConditionAssertionFailedException {

        if (!this.attributeTypeMap.containsKey(attributeType)) {
            throw new ConditionAssertionFailedException(this.name + " does not have " + attributeType + ".");
        }

        return this;
    }

    public AbstractHandler has(final Attribute attribute) throws ConditionAssertionFailedException {

        if (!this.attributes.contains(attribute)) {
            throw new ConditionAssertionFailedException(this.name + " does not have " + attribute + ".");
        }

        return this;
    }

    public AbstractHandlers and(final AbstractHandler handler) {

        return new AbstractHandlers(this, handler);
    }

    public String getName() {
        return this.name;
    }
}
