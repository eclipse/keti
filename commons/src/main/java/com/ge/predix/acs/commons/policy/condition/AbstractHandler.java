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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ge.predix.acs.commons.attribute.Attribute;
import com.ge.predix.acs.commons.attribute.AttributeType;
import com.ge.predix.acs.commons.attribute.ScopeType;
import com.ge.predix.acs.commons.attribute.ScopedAttribute;
import com.ge.predix.acs.commons.attribute.ScopedAttributeCriteria;

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
    private final Map<AttributeType, Set<ScopedAttribute>> attributeTypeMap = new HashMap<>();

    // This map is used to check if an attribute of a specific value exists
    // (e.g. is a subject a member of a group).
    // NOTE: The intention is that this is an immutable data structure. Do not
    // expose methods that modify this.
    private final Map<Attribute, ScopedAttribute> attributeMap = new HashMap<Attribute, ScopedAttribute>();

    public AbstractHandler(final String name, final Set<com.ge.predix.acs.model.Attribute> modelAttributeSet) {
        this.name = name;

        if (modelAttributeSet != null) {
            for (com.ge.predix.acs.model.Attribute modelAttribute : modelAttributeSet) {
                ScopedAttribute scopedAttribute = new ScopedAttribute(modelAttribute);
                this.attributeMap.put(scopedAttribute.getAttribute(), scopedAttribute);
                Set<ScopedAttribute> attributeValues = this.attributeTypeMap
                        .get(scopedAttribute.getAttribute().getType());
                if (attributeValues == null) {
                    attributeValues = new HashSet<>();
                }
                attributeValues.add(scopedAttribute);
                this.attributeTypeMap.put(scopedAttribute.getAttribute().getType(), attributeValues);
            }
        }
    }

    /**
     * Groovy beans getter for attribute.
     *
     * @param attributeIssuer
     *            Attribute's Issuer
     * @param attributeName
     *            Attribute's Name
     * @return Attribute Value
     */
    public Set<String> attributes(final String attributeIssuer, final String attributeName) {
        Set<ScopedAttribute> scopedAttributes = this.attributeTypeMap
                .get(new AttributeType(attributeIssuer, attributeName));
        if (scopedAttributes == null) {
            return Collections.emptySet();
        }
        Set<String> attributes = new HashSet<String>();
        for (ScopedAttribute scopedAttribute : scopedAttributes) {
            attributes.add(scopedAttribute.getAttribute().getValue());
        }
        return attributes;
    }

    protected Set<Attribute> attributes(final AttributeType attributeType) {
        Set<Attribute> resultAttributes = new HashSet<Attribute>();
        Set<ScopedAttribute> scopedAttributes = this.attributeTypeMap.get(attributeType);
        for (ScopedAttribute scopedAttribute : scopedAttributes) {
            resultAttributes.add(scopedAttribute.getAttribute());
        }
        return resultAttributes;
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

        if (!this.attributeMap.containsKey(attribute)) {
            throw new ConditionAssertionFailedException(this.name + " does not have " + attribute + ".");
        }

        return this;
    }

    public AbstractHandler has(final ScopedAttributeCriteria criteria) throws ConditionAssertionFailedException {

        // First we need to determine if the subject of resource has the
        // attribute at all.
        has(criteria.getAttribute());

        ScopeType criteriaScopeType = criteria.getScopeType();

        ScopedAttribute scopedAttribute = this.attributeMap.get(criteria.getAttribute());

        if (scopedAttribute.getScopes().isEmpty()) {
            throw new ConditionAssertionFailedException(
                    this.name + " " + scopedAttribute.getAttribute() + " is not scoped.");
        }

        // Look for a matching scope in the subject scopes.
        boolean isInScope = false;
        boolean isScopeTypeMatch = false;
        for (Attribute scope : scopedAttribute.getScopes()) {
            // If the scope attribute type is not a match for the criteria move
            // on to the next scope.
            isScopeTypeMatch = isScopeTypeMatch(scope, criteriaScopeType);
            if (!isScopeTypeMatch) {
                continue;
            }

            // If the scope attribute is present in the resource attributes then
            // the subject is in scope.
            isInScope = isInScope(scope, criteriaScopeType);
            if (isInScope) {
                // This is the success condition. We are in scope so we don't
                // have to look further.
                break;
            }
        }

        // If we exhausted our search for the right scope there may be one of
        // two reasons why it failed.
        if (!isInScope && !isScopeTypeMatch) {
            throw new ConditionAssertionFailedException(this.name + " " + scopedAttribute.getAttribute()
                    + " does not have " + criteria.getScopeType() + ".");
        } else if (!isInScope && isScopeTypeMatch) {
            throw new ConditionAssertionFailedException("Failed to match " + criteria + ".");
        }

        return this;
    }

    private boolean isScopeTypeMatch(final Attribute scope, final ScopeType criteriaScopeType) {
        return scope.getType().equals(criteriaScopeType.getAttributeType());
    }

    private boolean isInScope(final Attribute scope, final ScopeType criteriaScopeType) {
        try {
            criteriaScopeType.getResourceHandler().has(scope);
            return true;
        } catch (ConditionAssertionFailedException e) {
            return false;
        }
    }

    public AbstractHandlers and(final AbstractHandler handler) {

        return new AbstractHandlers(this, handler);
    }

    public String getName() {
        return this.name;
    }
}
